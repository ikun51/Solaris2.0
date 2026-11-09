package solaris2.turret;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import solaris2.*;

import static mindustry.Vars.*;

public class SpikeReleaser extends Block{

    public static final int FRAMES = 5;

    private static final IntFloatMap loadTable = new IntFloatMap();

    public Item iron;
    public ItemStack[] consumption;

    public float range = 72f;
    public float fanAngle = 120f;
    public int frames = FRAMES;

    public final MineBullet mineBullet = new MineBullet();

    public SpikeReleaser(String name){
        super(name);

        solid = true;
        update = true;
        destructible = true;

        iron = SolarisContent.item("铁");
        consumption = new ItemStack[]{new ItemStack(iron, 1)};

        buildType = SpikeReleaserBuild::new;
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{
            Core.atlas.find(name + "-bottom"),
            Core.atlas.find(name + "-f0"),
            Core.atlas.find(name)
        };
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.accent);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.reload, 60f / 150f, StatUnit.perSecond);

        ObjectMap<Block, BulletType> ammo = new ObjectMap<>();
        ammo.put(this, mineBullet);
        stats.add(Stat.ammo, StatValues.ammo(ammo));

        stats.add(Stat.input, table -> {
            table.defaults().left();
            table.row();
            table.table(t -> t.add("[accent]自动从核心获取[]"));
            table.row();
            table.table(Styles.grayPanel, t -> t.add(itemIcon(iron.uiIcon, 1)).pad(4));
        });
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("heat", (SpikeReleaserBuild entity) -> new Bar(
            () -> "reload",
            () -> Pal.lightOrange,
            () -> loadTable.get(entity.id()) / 150f
        ));
    }

    private static Stack itemIcon(TextureRegion region, int amount){
        Stack stack = new Stack();
        stack.add(new Image(region));
        if(amount > 1){
            Table t = new Table();
            t.bottom().right();
            t.add("" + amount).style(Styles.outlineLabel);
            t.pack();
            stack.add(t);
        }
        return stack;
    }

    private static void dashedLine(float x1, float y1, float x2, float y2){
        float dx = x2 - x1, dy = y2 - y1;
        float len = Mathf.sqrt(dx * dx + dy * dy);
        if(len <= 0.001f) return;
        dx /= len;
        dy /= len;
        for(float d = 0f; d < len; d += 11f){
            float e = Math.min(d + 6f, len);
            Lines.line(x1 + dx * d, y1 + dy * d, x1 + dx * e, y1 + dy * e);
        }
    }

    private static void dashedArc(float x, float y, float radius, float start, float end){
        for(float a = start; a < end; a += 10f){
            float b = Math.min(a + 6f, end);
            Lines.line(
                x + Angles.trnsx(a, radius), y + Angles.trnsy(a, radius),
                x + Angles.trnsx(b, radius), y + Angles.trnsy(b, radius)
            );
        }
    }

    public static class MineBullet extends BulletType{

        public MineBullet(){
            lifetime = 1800f;
            damage = 20f;
            pierceArmor = true;
            collidesTiles = false;
            collidesAir = false;
            hitEffect = Fx.none;
            despawnEffect = Fx.none;
            hitSize = 6f;
            ammoMultiplier = 5f;
        }

        @Override
        public void draw(Bullet b){
            super.draw(b);

            float f = b.data == null ? 0f : (b.data instanceof Number num ? num.floatValue() : b.rotation());
            float z = Draw.z();
            Draw.z(36f);
            for(int i = 0; i < 3; i++){
                Draw.color(Pal.gray);
                Drawf.tri(b.x, b.y, 1.6f, 4f, f + 120f * i);
                Draw.color(b.team.color);
                Drawf.tri(b.x, b.y, 1.2f, 3f, f + 120f * i);
            }
            Draw.reset();
            Draw.z(z);
        }
    }

    public class SpikeReleaserBuild extends Building{

        public float timeMine;
        public boolean open;
        public float fm;
        public float direction;
        public boolean aiming;

        public float volleyTimer;
        public float reloadTimer;

        public int roundFired;
        public int volleyLeft;

        @Override
        public void updateTile(){
            loadTable.put(id(), timeMine);

            if(open){
                fm = Mathf.lerpDelta(fm, 1f, 0.3f);
            }else{
                fm = Mathf.lerpDelta(fm, 0f, 0.15f);
            }
            if(fm >= 0.99f) open = false;

            if(reloadTimer > 0f){
                reloadTimer -= delta();
                if(reloadTimer <= 0f) roundFired = 0;
                return;
            }

            if(volleyLeft > 0){
                volleyTimer -= delta();
                if(volleyTimer <= 0f){
                    launchMine(roundFired);
                    volleyLeft--;
                    volleyTimer = 4f;
                }
                return;
            }

            if(coreCanConsume()){
                timeMine += delta();
                if(timeMine >= 150f){
                    timeMine = 0f;
                    coreConsume();
                    open = true;
                    volleyLeft = 5;
                    volleyTimer = 0f;
                }
            }
        }

        public void launchMine(int index){
            float offset = (index / 19f - 0.5f) * 120f;
            float angle = direction + offset;
            float dst = Mathf.random(range * 0.2f, range * 1.2f);

            mineBullet.create(
                this, team,
                x + Mathf.random(-4f, 4f),
                y + Mathf.random(-4f, 4f),
                angle, -1f, 1f, 1f, Mathf.random(120f),
                b -> b.initVel(b.rotation(), dst / 30f * 2f * Math.max(0f, 1f - b.time / 30f))
            );

            roundFired++;
            if(roundFired >= 20){
                reloadTimer = 240f;
            }
        }

        public boolean coreCanConsume(){
            var core = team.core();
            if(core == null) return false;
            return core.items.get(iron) >= 1;
        }

        public void coreConsume(){
            var core = team.core();
            if(core == null) return;
            core.items.remove(iron, 1);
        }

        @Override
        public void displayConsumption(Table table){
            super.displayConsumption(table);

            SpikeReleaserBuild self = this;
            table.table(c -> {
                int n = 0;
                for(int k = 0; k < consumption.length; k++){
                    ItemStack stack = consumption[k];
                    int need = Math.round(stack.amount);
                    c.add(new ReqImage(itemIcon(stack.item.uiIcon, need), () ->
                        state.rules.infiniteResources || self.cheating()
                            || (self.core() != null && self.core().items.has(stack.item, need))
                    )).padRight(8f);
                    n++;
                    if(n % 4 == 0) c.row();
                }
            }).left();
        }

        @Override
        public void draw(){
            String name = block.name;
            Draw.rect(Core.atlas.find(name + "-bottom"), x, y);
            int idx = Math.min(Mathf.floor(fm * frames), frames - 1);
            Draw.rect(Core.atlas.find(name + "-f" + idx), x, y);
            Draw.rect(Core.atlas.find(name), x, y);
        }

        @Override
        public void configured(Unit builder, Object value){
            super.configured(builder, value);
            aiming = false;
            if(value == null) return;
            if(value instanceof Number num){
                direction = num.floatValue();
            }
        }

        @Override
        public boolean onConfigureTapped(float x, float y){
            if(Mathf.dst(this.x, this.y, x, y) > range){
                aiming = false;
                deselect();
                return true;
            }
            if(!aiming){
                aiming = true;
                return true;
            }

            configure(Mathf.angle(x - this.x, y - this.y));
            aiming = false;
            return true;
        }

        @Override
        public void buildConfiguration(Table table){
            aiming = false;
            table.add("[accent]点击地图[] 显示方向，再点击一次重新确认方向").row();
        }

        @Override
        public void drawConfigure(){
            float dir = aiming ? control.input.mouseAngle(x, y) : direction;
            float half = fanAngle / 2f;
            float outer = range * 1.2f;
            float inner = range * 0.2f;

            Draw.color(Pal.accent);
            Lines.stroke(2f);

            dashedArc(x, y, outer, dir - half, dir + half);
            dashedArc(x, y, inner, dir - half, dir + half);
            dashedLine(
                x + Angles.trnsx(dir - half, inner), y + Angles.trnsy(dir - half, inner),
                x + Angles.trnsx(dir - half, outer), y + Angles.trnsy(dir - half, outer)
            );
            dashedLine(
                x + Angles.trnsx(dir + half, inner), y + Angles.trnsy(dir + half, inner),
                x + Angles.trnsx(dir + half, outer), y + Angles.trnsy(dir + half, outer)
            );

            Lines.stroke(1f);
            Draw.reset();
        }

        @Override
        public BlockStatus status(){
            if(!enabled) return BlockStatus.logicDisable;
            if(!shouldConsume()) return BlockStatus.noOutput;
            if(efficiency > 0f && productionValid() && coreCanConsume()){
                return (state.tick / 30f) % 1f < efficiency ? BlockStatus.active : BlockStatus.noInput;
            }
            return BlockStatus.noInput;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(direction);
            write.i(roundFired);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            direction = read.f();
            roundFired = read.i();
        }

        @Override
        public void remove(){
            super.remove();
            loadTable.remove(id(), loadTable.get(id()));
        }
    }
}
