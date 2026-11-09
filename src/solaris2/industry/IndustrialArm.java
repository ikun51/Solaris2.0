package solaris2.industry;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.game.EventType;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 由 scripts/建筑/工业机械臂.js + scripts/lib/SS.js 移植而来。
 */
public class IndustrialArm extends StorageBlock{

    public static final Seq<IndustrialArmBuild> armDrawList = new Seq<>();
    private static boolean armDrawRegistered = false;

    // 与原版 SS.js 配置保持一致：四段关节臂，每段 20px，合计 80px
    public int[] segments = {20, 20, 20, 20};
    /** 框选 / 自动搜索范围（格），与原版 JS 一致为 9（臂够不到的目标只会伸过去、不会触发抓取） */
    public float range = 9f;
    public float armThick = 4f;
    public int ikIterations = 8;
    public float ikDamping = 0.6f;

    public int maxCarry = 100;
    public float liquidCarry = 80f;
    public float grabCooldown = 60f;
    public float dumpCooldown = 45f;
    public float dumpLinger = 60f;
    public float hoverRange = 0.5f;
    public float hoverTime = 120f;
    public float autoScanInterval = 60f;
    public float moveLerp = 0.02f;
    public int maxTargetsCount = 4;
    public int tickInterval = 2;
    public float itemFanSize = 5f;

    public TextureRegion texArm, texGrip;

    public final Color colorSource = Color.valueOf("4aa4ff");
    public final Color colorTarget = Color.valueOf("ffd64d");

    public IndustrialArm(String name){
        super(name);

        configurable = true;
        update = true;
        hasItems = true;
        hasLiquids = true;
        itemCapacity = maxCarry;
        liquidCapacity = liquidCarry;

        buildType = IndustrialArmBuild::new;
    }

    @Override
    public void load(){
        super.load();
        texArm = Core.atlas.find(name + "-臂");
        texGrip = Core.atlas.find(name + "-爪");
    }

    @Override
    public void setStats(){
        super.setStats();
        float totalLen = 0f;
        for(int len : segments) totalLen += len;

        stats.add(new Stat("作业半径", StatCat.function), Math.round(range * 10f) / 10f + " 格");
        stats.add(new Stat("臂长", StatCat.function), Math.round(totalLen / tilesize * 10f) / 10f + " 格");
        stats.add(new Stat("单次携带", StatCat.function), maxCarry + " 个物品 / " + (int)liquidCarry + " 单位液体");
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * tilesize, Pal.accent);
    }

    /** 由方块构造时注册一次。 */
    public void registerDrawEvents(){
        if(armDrawRegistered) return;
        armDrawRegistered = true;

        Events.on(EventType.WorldLoadEvent.class, e -> {
            armDrawList.clear();
            int w = world.width(), h = world.height();
            for(int y = 0; y < h; y++){
                for(int x = 0; x < w; x++){
                    Building b = world.tile(x, y) == null ? null : world.tile(x, y).build;
                    if(b != null && b.block instanceof IndustrialArm){
                        armDrawList.add((IndustrialArmBuild)b);
                    }
                }
            }
        });

        Events.run(EventType.Trigger.draw, () -> {
            for(int i = armDrawList.size - 1; i >= 0; i--){
                IndustrialArmBuild b = armDrawList.get(i);
                if(b == null || b.tile == null || !b.isValid() || b.block == null
                    || world.tile(b.tile.x, b.tile.y) != b.tile){
                    armDrawList.remove(i);
                    continue;
                }
                try{
                    b.drawArmFX();
                }catch(Throwable ignored){
                }
            }
        });
    }

    public static class ArmTarget{
        public final Building building;
        public final boolean grab;
        public final boolean liquid;

        public ArmTarget(Building building, boolean grab, boolean liquid){
            this.building = building;
            this.grab = grab;
            this.liquid = liquid;
        }
    }

    public class IndustrialArmBuild extends StorageBuild{

        public boolean active = false;
        public int mode = 0;
        public int sourcePos = -1;
        public final IntSeq targets = new IntSeq();

        public Building autoSource, autoTarget, autoLiquidSource, autoLiquidTarget;
        public float scanTimer, grabCd, dumpCd, lingerCd;
        public Building lingerTarget = null;

        public boolean batchActive = false;
        public int hoverTarget = -1;
        public float hoverCd = 0;
        public int deliverIndex = 0;
        public boolean grabFlip = false;

        public int grabMax = maxCarry;
        public float grabLiquidMax = liquidCarry;
        public int tickC = 0;

        public float mx, my, endX, endY;
        public float[] segAngles;
        public Vec2[] segPts;

        @Override
        public void created(){
            super.created();

            int n = segments.length;
            mx = x;
            my = y;
            segAngles = new float[n];
            segPts = new Vec2[n + 1];
            segPts[0] = new Vec2(x, y);
            for(int i = 0; i < n; i++){
                segPts[i + 1] = new Vec2(segPts[i].x, segPts[i].y + segments[i]);
                segAngles[i] = Mathf.PI / 2f;
            }
            endX = segPts[n].x;
            endY = segPts[n].y;

            registerDrawEvents();
            armDrawList.add(this);
        }

        @Override
        public void buildConfiguration(Table table){
            Table r1 = table.table().left().get();
            r1.add("[accent]开关").left().padRight(6);
            Button btn = r1.button(Tex.pane, Styles.clearTogglei, 40f, () -> configure(!active)).get();
            btn.getStyle().up = Styles.black3;
            btn.getStyle().down = Styles.flatOver;
            btn.getStyle().checked = Styles.accentDrawable;
            btn.update(() -> {
                btn.setChecked(active);
                btn.clearChildren();
                btn.add(active ? "[#7db25a]运行" : "[red]停止").center();
            });
            table.row();

            Table r2 = table.table().left().padTop(4).get();
            r2.add("[accent]模式").left().padRight(6);
            Button modeBtn = r2.button(Tex.pane, Styles.clearTogglei, 40f, () -> configure((mode + 1) % 3)).get();
            modeBtn.getStyle().up = Styles.black3;
            modeBtn.getStyle().down = Styles.flatOver;
            modeBtn.getStyle().checked = Styles.accentDrawable;
            modeBtn.update(() -> {
                modeBtn.setChecked(mode != 0);
                modeBtn.clearChildren();
                modeBtn.add(mode == 0 ? "[#a9d8ff]自动" : (mode == 1 ? "[#ffd64d]物品" : "[#5ba4cf]流体")).center();
            });
            table.row();

            Table r3 = table.table().left().padTop(4).get();
            r3.add("[accent]物品提取").left().padRight(6);
            TextField f1 = r3.field(String.valueOf(grabMax), text -> {
                int v = Strings.parseInt(text, -1);
                if(v > 0) grabMax = v;
            }).get();
            f1.setFilter(TextField.TextFieldFilter.digitsOnly);
            table.row();

            Table r4 = table.table().left().padTop(4).get();
            r4.add("[accent]液体提取").left().padRight(6);
            TextField f2 = r4.field(String.valueOf((int)grabLiquidMax), text -> {
                int v = Strings.parseInt(text, -1);
                if(v > 0) grabLiquidMax = v;
            }).get();
            f2.setFilter(TextField.TextFieldFilter.digitsOnly);
        }

        @Override
        public void configured(Unit builder, Object value){
            if(value == null) return;

            if(value instanceof Number number){
                mode = ((number.intValue() % 3) + 3) % 3;
                return;
            }

            String str = String.valueOf(value);
            if(str.equals("true")){
                active = true;
            }else if(str.equals("false")){
                active = false;
            }
        }

        @Override
        public boolean onConfigureBuildTapped(Building who){
            if(!validSelect(who)) return false;

            int pos = who.pos();
            if(pos == sourcePos){
                if(targets.size >= maxTargetsCount) return false;
                targets.add(pos);
                sourcePos = -1;
            }else if(targets.contains(pos)){
                targets.removeValue(pos);
            }else{
                sourcePos = pos;
            }

            configure(null);
            return false;
        }

        public boolean validSelect(Building who){
            if(who == null || who == this) return false;
            if(who.team != team) return false;
            if(!who.block.hasItems && !who.block.hasLiquids) return false;
            if(who.items == null && who.liquids == null) return false;

            float dx = who.x - x;
            float dy = who.y - y;
            return Mathf.sqrt(dx * dx + dy * dy) <= range * tilesize;
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();

            Color col = Pal.accent;
            if(sourcePos != -1){
                col = colorSource;
            }else if(targets.size > 0){
                col = colorTarget;
            }
            Drawf.dashCircle(x, y, range * tilesize, col);

            if(sourcePos != -1){
                Building s = world.build(sourcePos);
                if(s != null) drawDiamond(s, colorSource);
            }
            for(int i = 0; i < targets.size; i++){
                Building t = world.build(targets.get(i));
                if(t != null) drawDiamond(t, colorTarget);
            }
        }

        public void drawDiamond(Building b, Color color){
            float r = b.block.size * tilesize / 2f + 3f;
            Drawf.square(b.x, b.y, r, 45f, color);
        }

        public void drawArmFX(){
            final float radDeg = 57.2958f;
            int n = segments.length;
            Vec2[] pts = segPts;

            Draw.z(Layer.block);
            Draw.rect(block.region, x, y, drawrot());
            Draw.z(Layer.blockOver - 0.01f);
            Draw.color(Color.valueOf("333333"));
            float r = armThick;
            for(int i = 1; i < n; i++){
                float diff = segAngles[i] - segAngles[i - 1];
                while(diff > Mathf.PI) diff -= 2f * Mathf.PI;
                while(diff < -Mathf.PI) diff += 2f * Mathf.PI;
                if(Math.abs(diff) < 0.05f) continue;

                int side = diff >= 0 ? -1 : 1;
                float a0 = segAngles[i - 1] + side * (Mathf.PI / 2f);
                float cx = pts[i].x, cy = pts[i].y;
                int steps = Math.max(2, Mathf.ceil(Math.abs(diff) / (Mathf.PI / 8f)));
                for(int j = 0; j < steps; j++){
                    float t0 = a0 + diff * j / steps;
                    float t1 = a0 + diff * (j + 1) / steps;
                    Fill.tri(cx, cy,
                        cx + Mathf.cos(t0) * r, cy + Mathf.sin(t0) * r,
                        cx + Mathf.cos(t1) * r, cy + Mathf.sin(t1) * r);
                }
            }
            Fill.circle(pts[n].x, pts[n].y, r);
            Draw.color(Color.white);

            if(Core.atlas.isFound(texArm)){
                for(int i = 0; i < n; i++){
                    float ang = segAngles[i] * radDeg;
                    Draw.z(Layer.blockOver);
                    //与原版 JS 一致：每段都画整张臂贴图，按段长拉伸
                    Draw.rect(texArm, (pts[i].x + pts[i + 1].x) / 2f, (pts[i].y + pts[i + 1].y) / 2f, segments[i], armThick * 2f, ang);
                }
            }
            if(Core.atlas.isFound(texGrip)){
                Draw.z(Layer.blockOver);
                Draw.rect(texGrip, endX, endY, segAngles[n - 1] * radDeg);
            }

            drawCarried();
            Draw.reset();
        }

        public void drawCarried(){
            if(items.total() > 0){
                int n = segments.length;
                float a = segAngles[n - 1];
                Seq<Item> all = content.items();
                for(int i = 0; i < all.size; i++){
                    Item it = all.get(i);
                    if(items.get(it) > 0){
                        Draw.rect(it.uiIcon, endX, endY, itemFanSize, itemFanSize, a * 57.2958f);
                        break;
                    }
                }
            }
            if(liquids.currentAmount() > 0.01f){
                Seq<Liquid> allL = content.liquids();
                for(int i = 0; i < allL.size; i++){
                    Liquid l = allL.get(i);
                    if(liquids.get(l) > 0.01f){
                        Draw.z(Layer.blockOver + 0.02f);
                        Draw.rect(l.uiIcon, endX, endY, itemFanSize, itemFanSize);
                        break;
                    }
                }
            }
        }

        @Override
        public void updateTile(){
            super.updateTile();
            noSleep();

            if(grabCd > 0f) grabCd--;
            if(dumpCd > 0f) dumpCd--;
            if(lingerCd > 0f){
                lingerCd--;
                if(lingerTarget != null){
                    applyIK(new ArmTarget(lingerTarget, false, false));
                }
                return;
            }

            tickC++;
            if(tickC < tickInterval) return;
            tickC = 0;

            if(!active){
                applyIK(null);
                return;
            }
            if(sourcePos == -1 || targets.size == 0){
                applyIK(null);
                return;
            }

            maintain();

            if(scanTimer <= 0f){
                scanTimer = autoScanInterval;
                autoScan();
            }else{
                scanTimer--;
            }

            ArmTarget target = pickTarget();
            applyIK(target);
            doTransfer(target);
        }

        public void maintain(){
            if(sourcePos != -1 && world.build(sourcePos) == null){
                sourcePos = -1;
            }
            for(int i = targets.size - 1; i >= 0; i--){
                if(world.build(targets.get(i)) == null){
                    targets.removeIndex(i);
                }
            }
        }

        public void autoScan(){
            autoSource = null;
            autoTarget = null;
            autoLiquidSource = null;
            autoLiquidTarget = null;

            int rt = (int)Math.floor(range);
            float maxD = range * tilesize;
            float bestDS = 1e9f, bestDT = 1e9f, bestLS = 1e9f, bestLT = 1e9f;
            int tx = tile.x, ty = tile.y;

            for(int dx = -rt; dx <= rt; dx++){
                for(int dy = -rt; dy <= rt; dy++){
                    Building b = world.build(tx + dx, ty + dy);
                    if(b == null || b == this || b.team != team) continue;

                    float ddx = b.x - x, ddy = b.y - y;
                    float dist = Mathf.sqrt(ddx * ddx + ddy * ddy);
                    if(dist > maxD) continue;

                    if(b.items != null){
                        if(b.items.total() > 0 && dist < bestDS){
                            bestDS = dist;
                            autoSource = b;
                        }
                        if(dist < bestDT && canGive(b)){
                            bestDT = dist;
                            autoTarget = b;
                        }
                    }
                    if(b.liquids != null){
                        if(b.liquids.currentAmount() > 0.01f && dist < bestLS){
                            bestLS = dist;
                            autoLiquidSource = b;
                        }
                        if(dist < bestLT && canGiveLiquid(b, null)){
                            bestLT = dist;
                            autoLiquidTarget = b;
                        }
                    }
                }
            }
        }

        public boolean canGive(Building b){
            if(b.items == null) return false;
            Seq<Item> all = content.items();
            for(int i = 0; i < all.size; i++){
                if(b.items.get(all.get(i)) < b.block.itemCapacity) return true;
            }
            return false;
        }

        public boolean canGiveLiquid(Building b, Liquid liq){
            if(b == null || b.liquids == null || !b.block.hasLiquids) return false;
            if(liq == null){
                return b.liquids.currentAmount() < b.block.liquidCapacity - 0.01f;
            }
            float free = b.block.liquidCapacity - b.liquids.get(liq);
            if(free <= 0.01f) return false;
            if(b.liquids.currentAmount() > 0.2f && b.liquids.current() != liq){
                return false;
            }
            return true;
        }

        public int freeSlots(Building b){
            if(b.items == null) return 0;
            int free = 0;
            Seq<Item> all = content.items();
            for(int i = 0; i < all.size; i++){
                int f = b.block.itemCapacity - b.items.get(all.get(i));
                if(f > 0) free += f;
            }
            return free;
        }

        public ArmTarget pickTarget(){
            if(items.total() > 0){
                return pickDeliverTarget();
            }
            if(liquids.currentAmount() > 0.01f){
                return pickDeliverLiquidTarget();
            }

            batchActive = false;

            if(mode == 1){
                if(pickDeliverTarget() == null) return null;
            }else if(mode == 2){
                if(pickDeliverLiquidTarget() == null) return null;
            }else{
                if(pickDeliverTarget() == null && pickDeliverLiquidTarget() == null) return null;
            }

            Building s = null;
            if(sourcePos != -1) s = world.build(sourcePos);
            if(s == null){
                if(mode == 2){
                    s = autoLiquidSource;
                }else if(mode == 1){
                    s = autoSource;
                }else{
                    s = autoSource;
                    if(s == null) s = autoLiquidSource;
                }
            }
            if(s == null || !validBuild(s)) return null;

            boolean liq = false;
            if(mode == 2){
                liq = true;
            }else if(mode == 1){
                liq = false;
            }else{
                boolean hasItems = s.items != null && s.items.total() > 0;
                boolean hasLiquid = s.liquids != null && s.liquids.currentAmount() > 0.01f;
                if(hasLiquid && !hasItems){
                    liq = true;
                }else if(hasItems && !hasLiquid){
                    liq = false;
                }else if(hasItems && hasLiquid){
                    liq = grabFlip;
                }
            }

            //源里没有可抓的东西时直接收臂，避免臂反复跑向空源而"乱甩"
            boolean hasContent = liq
                ? (s.liquids != null && s.liquids.currentAmount() > 0.01f)
                : (s.items != null && s.items.total() > 0);
            if(!hasContent) return null;

            batchActive = true;
            return new ArmTarget(s, true, liq);
        }

        public ArmTarget pickDeliverTarget(){
            int n = targets.size;
            for(int i = 0; i < n; i++){
                int idx = (deliverIndex + i) % n;
                Building t = world.build(targets.get(idx));
                if(t == null || !validBuild(t)) continue;
                if(freeSlots(t) <= 0) continue;
                return new ArmTarget(t, false, false);
            }
            if(autoTarget != null && validBuild(autoTarget) && freeSlots(autoTarget) > 0){
                return new ArmTarget(autoTarget, false, false);
            }
            return null;
        }

        public ArmTarget pickDeliverLiquidTarget(){
            int n = targets.size;
            Liquid liq = liquids.currentAmount() > 0.01f ? liquids.current() : null;
            for(int i = 0; i < n; i++){
                int idx = (deliverIndex + i) % n;
                Building t = world.build(targets.get(idx));
                if(t == null || !validBuild(t)) continue;
                if(!canGiveLiquid(t, liq)) continue;
                return new ArmTarget(t, false, true);
            }
            if(autoLiquidTarget != null && validBuild(autoLiquidTarget) && canGiveLiquid(autoLiquidTarget, liq)){
                return new ArmTarget(autoLiquidTarget, false, true);
            }
            return null;
        }

        public void advanceDeliver(Building b){
            if(b == null) return;
            int idx = targets.indexOf(b.pos());
            if(idx != -1){
                deliverIndex = (idx + 1) % targets.size;
            }
        }

        public boolean validBuild(Building b){
            if(b == null || b.tile == null) return false;
            return world.build(b.tile.x, b.tile.y) == b;
        }

        public void applyIK(ArmTarget target){
            int n = segments.length;
            float tx, ty;
            if(target != null){
                tx = target.building.x;
                ty = target.building.y;
            }else{
                tx = x;
                ty = y + armThick * 4f;
            }

            mx += (tx - mx) * moveLerp;
            my += (ty - my) * moveLerp;

            Vec2[] pts = segPts;

            for(int iter = 0; iter < ikIterations; iter++){
                for(int i = n - 1; i >= 0; i--){
                    float jx = pts[i].x, jy = pts[i].y;
                    float ex = pts[n].x - jx, ey = pts[n].y - jy;
                    float bx = mx - jx, by = my - jy;
                    float l1 = Mathf.sqrt(ex * ex + ey * ey);
                    float l2 = Mathf.sqrt(bx * bx + by * by);
                    if(l1 < 0.01f || l2 < 0.01f) continue;

                    float cross = ex * by - ey * bx;
                    float dot = ex * bx + ey * by;
                    float delta = Mathf.atan2(cross, dot) * ikDamping;
                    float cosd = Mathf.cos(delta), sind = Mathf.sin(delta);

                    for(int k = i + 1; k <= n; k++){
                        float rx = pts[k].x - jx, ry = pts[k].y - jy;
                        pts[k].x = jx + rx * cosd - ry * sind;
                        pts[k].y = jy + rx * sind + ry * cosd;
                    }
                }
            }

            for(int i = 0; i < n; i++){
                float ax = pts[i + 1].x - pts[i].x, ay = pts[i + 1].y - pts[i].y;
                segAngles[i] = Mathf.atan2(ay, ax);
            }
            endX = pts[n].x;
            endY = pts[n].y;
        }

        public void doTransfer(ArmTarget target){
            if(target == null) return;

            float ddx = endX - target.building.x;
            float ddy = endY - target.building.y;
            float dist = Mathf.sqrt(ddx * ddx + ddy * ddy) / tilesize;

            if(target.grab){
                boolean canGrab = target.liquid
                    ? liquids.currentAmount() < liquidCarry
                    : items.total() < maxCarry;
                if(dist <= hoverRange && grabCd <= 0f && canGrab){
                    int key = target.building.pos() * 2;
                    if(hoverTarget != key){
                        hoverTarget = key;
                        hoverCd = 0f;
                    }
                    hoverCd += tickInterval;
                    if(hoverCd >= hoverTime){
                        if(target.liquid){
                            grabLiquidFrom(target.building);
                        }else{
                            grabFrom(target.building);
                        }
                        grabCd = grabCooldown;
                        hoverCd = 0f;
                        hoverTarget = -1;
                    }
                }else{
                    hoverCd = 0f;
                    hoverTarget = -1;
                }
            }else{
                boolean canDump = target.liquid
                    ? liquids.currentAmount() > 0.01f
                    : items.total() > 0;
                if(dist <= hoverRange && canDump && dumpCd <= 0f){
                    int key = target.building.pos() * 2 + 1;
                    if(hoverTarget != key){
                        hoverTarget = key;
                        hoverCd = 0f;
                    }
                    hoverCd += tickInterval;
                    if(hoverCd >= hoverTime){
                        if(target.liquid){
                            dumpLiquidTo(target.building, liquids.currentAmount());
                        }else{
                            dumpTo(target.building, items.total());
                        }
                        dumpCd = dumpCooldown;
                        hoverCd = 0f;
                        hoverTarget = -1;
                    }
                }else{
                    hoverCd = 0f;
                    hoverTarget = -1;
                }
            }
        }

        public void grabFrom(Building source){
            if(source == null || source.items == null) return;
            int space = maxCarry - items.total();
            if(space <= 0) return;
            int take = Math.min(grabMax, space);

            Seq<Item> all = content.items();
            for(int i = 0; i < all.size; i++){
                Item it = all.get(i);
                int stock = source.items.get(it);
                if(stock > 0){
                    int amt = Math.min(stock, take);
                    source.removeStack(it, amt);
                    items.add(it, amt);
                    batchActive = true;
                    if(source.liquids != null && source.liquids.currentAmount() > 0.01f) grabFlip = true;
                    Fx.steam.at(endX, endY);
                    return;
                }
            }
        }

        public void grabLiquidFrom(Building source){
            if(source == null || source.liquids == null) return;
            float space = liquidCarry - liquids.currentAmount();
            if(space <= 0.01f) return;

            Seq<Liquid> all = content.liquids();
            for(int i = 0; i < all.size; i++){
                Liquid l = all.get(i);
                float stock = source.liquids.get(l);
                if(stock > 0.01f){
                    float amt = Math.min(space, Math.min(stock, grabLiquidMax));
                    if(amt <= 0.01f) return;
                    source.liquids.remove(l, amt);
                    liquids.add(l, amt);
                    batchActive = true;
                    if(source.items != null && source.items.total() > 0) grabFlip = false;
                    Fx.steam.at(endX, endY);
                    return;
                }
            }
        }

        public void dumpTo(Building target, float maxGive){
            Seq<Item> all = content.items();
            for(int i = 0; i < all.size; i++){
                Item it = all.get(i);
                int amt = items.get(it);
                if(amt <= 0) continue;
                if(!target.acceptItem(this, it)) continue;
                int free = target.block.itemCapacity - target.items.get(it);
                if(free <= 0) continue;
                int give = (int)Math.min(amt, Math.min(free, maxGive));
                for(int k = 0; k < give; k++){
                    target.handleItem(this, it);
                }
                items.remove(it, give);
                Fx.steam.at(endX, endY);
                lingerCd = dumpLinger;
                lingerTarget = target;
                if(items.total() <= 0){
                    batchActive = false;
                    advanceDeliver(target);
                }
            }
        }

        public void dumpLiquidTo(Building target, float maxGive){
            Seq<Liquid> all = content.liquids();
            for(int i = 0; i < all.size; i++){
                Liquid l = all.get(i);
                float amt = liquids.get(l);
                if(amt <= 0.01f) continue;
                if(!target.acceptLiquid(this, l)) continue;
                float free = target.block.liquidCapacity - target.liquids.get(l);
                if(free <= 0.01f) continue;
                float give = Math.min(amt, Math.min(free, maxGive));
                target.liquids.add(l, give);
                liquids.remove(l, give);
                Fx.steam.at(endX, endY);
                lingerCd = dumpLinger;
                lingerTarget = target;
                if(liquids.currentAmount() <= 0.01f){
                    batchActive = false;
                    advanceDeliver(target);
                }
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public boolean canUnload(){
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return false;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(sourcePos);
            write.i(targets.size);
            for(int i = 0; i < targets.size; i++){
                write.i(targets.get(i));
            }
            write.bool(active);
            write.i(mode);
            write.i(grabMax);
            write.f(grabLiquidMax);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            sourcePos = read.i();
            int n = read.i();
            targets.clear();
            for(int i = 0; i < n; i++){
                targets.add(read.i());
            }
            active = read.bool();
            mode = read.i();
            grabMax = read.i();
            grabLiquidMax = read.f();
        }
    }
}
