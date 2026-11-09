package solaris2.turret;

import arc.*;
import arc.func.*;
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
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import solaris2.daynight.DayNight;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import solaris2.*;
import solaris2.effects.*;

import static mindustry.Vars.*;

public class MagneticCoil extends PowerTurret{

    public static final Color LIGHTNING_COLOR = Color.valueOf("c0ecffff");
    public static final Color CHAIN_COLOR = Color.valueOf("ffd37fff");

    public static final int MAX_CHAIN_COUNT = 5;
    public static final int BASE_DAMAGE = 90;
    public static final float CHAIN_DAMAGE_MULTIPLIER = 1.8f;
    public static final float THUNDER_BOOST = 1.3f;

    public static final Effect coilWaveFx = new Effect(20f, 40f, e -> {
        Color from = Color.valueOf("c0ecff00");
        Color to = Color.valueOf("c0ecffff");
        Draw.color(from, to, e.fin());
        Lines.stroke(3f * (1f - e.fin()));
        Lines.poly(e.x, e.y, 48, 10.5f);
        Draw.reset();
    });

    public static final Effect coilLightningFx = new Effect(20f, 500f, e -> {
        if(!(e.data instanceof Position pos)) return;

        float tx = e.x, ty = e.y;
        float dst = Mathf.dst(pos.getX(), pos.getY(), tx, ty);
        Tmp.v1.set(e.x, e.y).sub(pos.getX(), pos.getY()).nor();
        float normx = Tmp.v1.x;
        float normy = Tmp.v1.y;
        float range = e.rotation;
        int links = Mathf.ceil(dst / range);
        float spacing = dst / links;

        Lines.stroke(2.5f * e.fout());
        Draw.color(e.color);

        Lines.beginLine();
        Lines.linePoint(pos.getX(), pos.getY());
        Fx.rand.setSeed(e.id);
        for(int i = 0; i < links; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(Fx.rand).scl(range / 2f);
                nx = pos.getX() + normx * len + Tmp.v1.x;
                ny = pos.getY() + normy * len + Tmp.v1.y;
            }
            Lines.linePoint(nx, ny);
        }
        Lines.endLine();
    });

    private static final IntMap<ChainData> turretData = new IntMap<>();

    public static Building connectingFrom;

    public Weather thunderstorm;

    public MagneticCoil(String name){
        super(name);

        shootCone = 360f;
        shootSound = Sounds.shootArc;
        shootEffect = Fx.none;
        smokeEffect = Fx.none;
        rotateSpeed = 0f;
        reload = 60f;
        shootX = 0f;
        shootY = 0f;
        range = 200f;
        targetAir = true;
        targetGround = true;
        hasLiquids = false;

        shootType = makeShootType();

        buildType = MagneticCoilBuild::new;
        thunderstorm = SolarisContent.weather("雷暴");
    }

    private static BulletType makeShootType(){
        EmptyBulletType type = new EmptyBulletType();
        type.damage = 0f;
        type.hitEffect = Fx.none;
        type.shootEffect = Fx.none;
        type.lifetime = 1f;
        type.speed = 0f;
        return type;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.abilities, Core.bundle.format("ability.chainlightning", BASE_DAMAGE, MAX_CHAIN_COUNT, CHAIN_DAMAGE_MULTIPLIER));

        if(coolant != null){
            stats.replace(Stat.booster, StatValues.boosters(reload, coolant.amount, coolantMultiplier, true, coolant::consumes));
        }
    }

    static{
        coilLightningFx.followParent = false;

        Events.on(EventType.TapEvent.class, event -> {
            if(connectingFrom == null || !connectingFrom.isValid()) return;

            Tile tile = event.tile;
            if(tile != null && tile.build != null){
                Building target = tile.build;
                Block block = connectingFrom.block;

                if(target.block.name.equals(block.name) && target != connectingFrom){
                    ChainData fromData = getData(connectingFrom.pos());
                    ChainData targetData = getData(target.pos());

                    if(!targetData.chainMode){
                        Call.label(Core.bundle.get("msg.target-not-chain"), 2f, target.x, target.y);
                    }else if(fromData.nextPos != 0){
                        Call.label(Core.bundle.get("msg.has-next-connection"), 2f, connectingFrom.x, connectingFrom.y);
                    }else if(targetData.prevPos != 0){
                        Call.label(Core.bundle.get("msg.has-prev-connection"), 2f, target.x, target.y);
                    }else if(Mathf.dst(connectingFrom.x, connectingFrom.y, target.x, target.y) > ((MagneticCoil)block).range){
                        Call.label(Core.bundle.format("msg.chain-too-far", Math.round(((MagneticCoil)block).range / tilesize)), 2f, target.x, target.y);
                    }else{
                        Building head = getHead(connectingFrom, block);
                        Building tail = getTail(target, block);
                        int newLength = getChainLength(head, block) + getChainLength(tail, block);

                        if(newLength > MAX_CHAIN_COUNT){
                            Call.label(Core.bundle.format("msg.chain-too-long", MAX_CHAIN_COUNT), 2f, target.x, target.y);
                        }else{
                            fromData.nextPos = target.pos();
                            targetData.prevPos = connectingFrom.pos();
                        }
                    }
                }
            }

            connectingFrom = null;
        });
    }

    public static ChainData getData(int pos){
        ChainData data = turretData.get(pos);
        if(data == null){
            data = new ChainData();
            turretData.put(pos, data);
        }
        return data;
    }

    public static void removeData(int pos){
        turretData.remove(pos);
    }

    public static Building getBuildByPos(int pos){
        Tile tile = world.tile(pos);
        return tile == null ? null : tile.build;
    }

    public static Building getHead(Building build, Block block){
        Building current = build;
        ChainData data = getData(current.pos());
        while(data.prevPos != 0){
            Building prev = getBuildByPos(data.prevPos);
            if(prev == null || !prev.block.name.equals(block.name)) break;
            current = prev;
            data = getData(current.pos());
        }
        return current;
    }

    public static Building getTail(Building build, Block block){
        Building current = build;
        ChainData data = getData(current.pos());
        while(data.nextPos != 0){
            Building next = getBuildByPos(data.nextPos);
            if(next == null || !next.block.name.equals(block.name)) break;
            current = next;
            data = getData(current.pos());
        }
        return current;
    }

    public static Seq<Building> getChainList(Building build, Block block){
        Seq<Building> list = new Seq<>();
        Building head = getHead(build, block);
        Building current = head;
        while(current != null){
            list.add(current);
            ChainData data = getData(current.pos());
            if(data.nextPos == 0) break;
            Building next = getBuildByPos(data.nextPos);
            if(next == null || !next.block.name.equals(block.name)) break;
            current = next;
        }
        return list;
    }

    public static int getChainLength(Building build, Block block){
        return getChainList(build, block).size;
    }

    private static void spawnMainLightning(float startX, float startY, float endX, float endY, float length, Color color, float damage, Team team){
        coilLightningFx.at(startX, startY, length, color, new Vec2(endX, endY));

        Damage.damage(team, endX, endY, 8f, damage);
        Damage.damage(team, endX, endY, 30f, damage / 3f);

        Effect.shake(2f, 30f, new Vec2(endX, endY));
        FightFx.smallRectBurst.at(endX, endY, color);
    }

    private static void spawnBranchLightning(float centerX, float centerY, int count, float spread, float minLen, float maxLen, Color color, float damage, Team team){
        Seq<Unit> enemies = new Seq<>();
        Units.nearby(null, centerX, centerY, spread, unit -> {
            if(unit.team != team && unit.hittable()){
                enemies.add(unit);
            }
        });

        for(int i = 0; i < count; i++){
            Vec2 end;
            if(enemies.size > 0){
                Unit enemy = enemies.get(i % enemies.size);
                end = new Vec2(enemy.x, enemy.y);
            }else{
                end = new Vec2(Mathf.range(spread) + centerX, Mathf.range(spread) + centerY);
            }

            coilLightningFx.at(centerX, centerY, Mathf.random(minLen, maxLen), color, end);

            Damage.damage(team, end.x, end.y, 8f, damage);
            Effect.shake(0.6f, 20f, end);
            FightFx.smallRectBurst.at(end.x, end.y, color);
        }
    }

    public static class ChainData{
        public boolean chainMode = true;
        public int nextPos = 0;
        public int prevPos = 0;
    }

    public class MagneticCoilBuild extends PowerTurretBuild{

        // 夜晚未在探照灯范围内时降低射程
        @Override
        public float range(){
            return super.range() * DayNight.rangeMultiplier(x, y);
        }

        @Override
        public void buildConfiguration(Table table){
            ChainData data = getData(pos());
            MagneticCoilBuild self = this;

            table.table(t -> {
                t.left();
                t.add("连锁模式").padRight(8f);

                CheckBox checkbox = new CheckBox("", Styles.defaultCheck);
                checkbox.setChecked(data.chainMode);
                checkbox.setDisabled(data.nextPos != 0 || data.prevPos != 0);
                checkbox.changed(() -> {
                    if(data.nextPos != 0 || data.prevPos != 0){
                        checkbox.setChecked(true);
                        data.chainMode = true;
                    }else{
                        data.chainMode = checkbox.isChecked();
                    }
                });

                t.add(checkbox);
            }).left().padBottom(4f).row();

            table.table(t -> {
                t.left();
                t.add("闪电链长度: " + getChainLength(self, block)).padRight(8f);
            }).left().padBottom(4f).row();

            table.table(t -> {
                TextButton connBtn = new TextButton("开始连接", Styles.defaultt);
                connBtn.clicked(() -> connectingFrom = self);
                t.add(connBtn).padRight(8f);

                TextButton clearBtn = new TextButton("断开连接", Styles.defaultt);
                clearBtn.clicked(self::breakConnection);
                t.add(clearBtn);
            }).left().row();
        }

        public void breakConnection(){
            ChainData data = getData(pos());

            if(data.nextPos != 0){
                Building nextBuild = getBuildByPos(data.nextPos);
                if(nextBuild != null){
                    getData(nextBuild.pos()).prevPos = 0;
                }
                data.nextPos = 0;
            }

            if(data.prevPos != 0){
                Building prevBuild = getBuildByPos(data.prevPos);
                if(prevBuild != null){
                    getData(prevBuild.pos()).nextPos = 0;
                }
                data.prevPos = 0;
            }
        }

        @Override
        protected void shoot(BulletType type){
            ChainData data = getData(pos());
            Seq<Building> chain = getChainList(this, block);
            Building tail = getTail(this, block);
            boolean isThisTail = tail == this;

            if(!isThisTail) return;

            if(data.chainMode){
                for(Building cb : chain){
                    if(cb.power == null || cb.power.status <= 0f) return;
                }
            }else{
                if(power == null || power.status <= 0f) return;
            }

            if(data.chainMode){
                for(int i = 0; i < chain.size - 1; i++){
                    Building from = chain.get(i), to = chain.get(i + 1);
                    coilLightningFx.at(from.x, from.y, 6f, LIGHTNING_COLOR, new Vec2(to.x, to.y));
                }
            }

            if(target != null || isControlled()){
                float targetX = targetPos.x, targetY = targetPos.y;
                float dst = Mathf.dst(x, y, targetX, targetY);
                if(dst > range){
                    float scl = range / dst;
                    targetX = x + (targetX - x) * scl;
                    targetY = y + (targetY - y) * scl;
                }

                float damageMultiplier = data.chainMode ? (float)Math.pow(CHAIN_DAMAGE_MULTIPLIER, chain.size - 1) : 1f;
                float boost = thunderstorm != null && thunderstorm.isActive() ? THUNDER_BOOST : 1f;

                spawnMainLightning(x, y, targetX, targetY, Mathf.random(15f, 35f), LIGHTNING_COLOR, BASE_DAMAGE * damageMultiplier * boost, team);

                float branchChance = 0f;
                if(chain.size == 3) branchChance = 0.2f;
                else if(chain.size == 4) branchChance = 0.4f;
                else if(chain.size >= 5) branchChance = 0.6f;

                if(chain.size >= 3 && Mathf.chance(branchChance)){
                    spawnBranchLightning(targetX, targetY, 3, 80f, 4f, 17f, LIGHTNING_COLOR, BASE_DAMAGE * damageMultiplier * 0.7f * boost, team);
                }

                Damage.status(team, targetX, targetY, 30f, StatusEffects.shocked, 60f * damageMultiplier, true, true);
            }

            super.shoot(type);
        }

        @Override
        public boolean shouldConsume(){
            ChainData data = getData(pos());

            if(data.chainMode){
                for(Building cb : getChainList(this, block)){
                    if(cb.power == null || cb.power.status <= 0f) return false;
                }

                Building tail = getTail(this, block);
                if(tail == null || tail == this){
                    return super.shouldConsume();
                }

                MagneticCoilBuild tailBuild = (MagneticCoilBuild)tail;
                return tailBuild.isShooting || tailBuild.reloadCounter < reload;
            }

            return super.shouldConsume();
        }

        public boolean shouldActiveTurn(){
            return getTail(this, block) == this;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(power != null && power.status > 0f && Mathf.chance(0.04)){
                coilWaveFx.at(x, y);
            }

            ChainData data = getData(pos());
            Building tail = getTail(this, block);
            boolean isTail = tail == this;

            if(data.chainMode && !isTail){
                if(coolant != null && coolant.efficiency(this) > 0f && efficiency > 0f && tail != null && ((MagneticCoilBuild)tail).wasShooting){
                    float heatCap = liquids.current() != null ? liquids.current().heatCapacity : 0.4f;
                    float amount = coolant.amount * coolant.efficiency(this);
                    Liquid liq = coolant instanceof ConsumeLiquidFilter filter ? filter.getConsumed(this) : null;

                    if(liq != null){
                        liquids.remove(liq, coolant.amount * edelta());
                    }

                    ((MagneticCoilBuild)tail).reloadCounter += amount * edelta() * heatCap * coolantMultiplier;

                    if(Mathf.chance(0.06 * amount)){
                        coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                    }
                }
            }else if(isTail){
                if(coolant != null && coolant.efficiency(this) > 0f && efficiency > 0f && wasShooting){
                    float heatCap = liquids.current() != null ? liquids.current().heatCapacity : 0.4f;
                    float amount = coolant.amount * coolant.efficiency(this);
                    Liquid liq = coolant instanceof ConsumeLiquidFilter filter ? filter.getConsumed(this) : null;

                    if(liq != null){
                        liquids.remove(liq, coolant.amount * edelta());
                    }

                    reloadCounter += amount * edelta() * heatCap * coolantMultiplier;

                    if(Mathf.chance(0.06 * amount)){
                        coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                    }
                }
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(coolant != null && coolant.consumes(liquid)){
                return super.acceptLiquid(source, liquid) || (liquids.current() == liquid || liquids.currentAmount() < 0.2f);
            }
            return false;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            if(coolant != null && liquids.currentAmount() <= 0.001f){
                Events.fire(EventType.Trigger.turretCool);
            }
            super.handleLiquid(source, liquid, amount);
        }

        @Override
        public void draw(){
            super.draw();

            float heat = warmup();
            if(heat > 0.001f && power != null && power.status > 0f){
                Draw.z(Layer.effect);
                float f = 1f - (Time.time / 100f) % 1f;
                Draw.color(LIGHTNING_COLOR);
                Lines.stroke((4f * f + 0.4f) * heat * 0.2f);
                Lines.square(x, y, Math.min(1f + (1f - f) * 12f, 12f));
                Draw.reset();
            }

            if(connectingFrom == this){
                Draw.z(Layer.effect);
                Draw.color(CHAIN_COLOR);
                Lines.stroke(2f);
                Lines.circle(x, y, range);

                Vec2 mouse = Core.input.mouseWorld();
                float dst = Mathf.dst(x, y, mouse.x, mouse.y);
                if(dst <= range){
                    Lines.line(x, y, mouse.x, mouse.y);
                }else{
                    Draw.color(Color.scarlet);
                    Lines.line(x, y, mouse.x, mouse.y);
                }
                Draw.reset();
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            Seq<Building> chain = getChainList(this, block);

            Draw.color(CHAIN_COLOR);
            Lines.stroke(2f);

            for(int i = 0; i < chain.size - 1; i++){
                Building from = chain.get(i), to = chain.get(i + 1);
                Lines.line(from.x, from.y, to.x, to.y);

                float angle = Angles.angle(from.x, from.y, to.x, to.y);
                float len = 8f, w = 4f;
                float midX = (from.x + to.x) / 2f, midY = (from.y + to.y) / 2f;
                float cos = Mathf.cosDeg(angle), sin = Mathf.sinDeg(angle);

                Fill.tri(
                    midX + cos * len / 2f, midY + sin * len / 2f,
                    midX - cos * len + sin * w, midY - sin * len - cos * w,
                    midX - cos * len - sin * w, midY - sin * len + cos * w
                );
            }

            Draw.reset();
        }

        @Override
        public void onRemoved(){
            breakConnection();
            if(connectingFrom == this) connectingFrom = null;
            removeData(pos());
        }

        @Override
        public void write(Writes write){
            super.write(write);
            ChainData data = getData(pos());
            write.bool(data.chainMode);
            write.i(data.nextPos);
            write.i(data.prevPos);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            ChainData data = getData(pos());
            data.chainMode = read.bool();
            data.nextPos = read.i();
            data.prevPos = read.i();
        }
    }
}
