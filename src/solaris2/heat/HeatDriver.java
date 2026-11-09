package solaris2.heat;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

/**
 * 由 scripts/建筑/热量/热量驱动器.js 移植而来。
 * 原脚本引用的 drawer/DrawHeatDriver 并不存在，因此使用 else 分支的 DrawMulti 后备方案。
 */
public class HeatDriver extends HeatConductor{

    public static final float range = 25f * 8f;

    private static final float maxHeat = 120f;
    private static final float decayInterval = 14f;
    private static final float decayAmount = 2f;
    private static final float bulletSpeed = 5.5f;
    private static final float hitRange = 8f;
    private static final float fireOffset = 4f;
    private static final float reloadTime = 150f;
    private static final float rotateSpeed = 90f;
    private static final float recoilDistance = 4f;
    private static final float shakeStrength = 3f;

    public static TextureRegion baseRegion, bodyRegion;

    public static final HeatDriverBulletType heatBullet = new HeatDriverBulletType();

    public HeatDriver(String name){
        super(name);

        drawer = new DrawMulti(new DrawRegion("-base"), new DrawDefault(), new DrawHeatInput("-heat"));

        config(Integer.class, (HeatDriverBuild tile, Integer v) -> tile.setOneLink(v));

        buildType = HeatDriverBuild::new;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * Vars.tilesize + offset, y * Vars.tilesize + offset, range, Pal.accent);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.shootRange, range, StatUnit.blocks);
        stats.add(Stat.reload, 60f / reloadTime, StatUnit.perSecond);
    }

    @Override
    public void load(){
        super.load();
        baseRegion = Core.atlas.find(name + "-base");
        bodyRegion = Core.atlas.find(name);
        heatBullet.load();
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        if(baseRegion != null && baseRegion.found()) Draw.rect(baseRegion, plan.drawx(), plan.drawy());
        if(bodyRegion != null && bodyRegion.found()) Draw.rect(bodyRegion, plan.drawx(), plan.drawy());
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(name + "-base"), Core.atlas.find(name)};
    }

    public static class HeatDriverBulletData{
        public HeatDriverBuild to;
        public float amount;

        public HeatDriverBulletData(HeatDriverBuild to, float amount){
            this.to = to;
            this.amount = amount;
        }
    }

    public static class HeatDriverBulletType extends BasicBulletType{
        public HeatDriverBulletType(){
            speed = 0f;
            lifetime = 60f;
            collides = false;
            collidesTiles = false;
            collidesAir = false;
            collidesGround = false;
            hittable = false;
            sprite = "shell";
            width = 10f;
            height = 12f;
            shrinkY = 0f;
            hitEffect = Fx.hitBulletBig;
            despawnEffect = Fx.smeltsmoke;
        }

        @Override
        public void update(Bullet b){
            if(!(b.data instanceof HeatDriverBulletData data) || data.to == null || data.to.dead){
                b.remove();
                return;
            }
            float step = bulletSpeed * Time.delta;
            if(b.dst(data.to) <= Math.max(hitRange, step)){
                b.x = data.to.x;
                b.y = data.to.y;
                data.to.receiveHeat(data.amount);
                b.remove();
                return;
            }
            b.x += Angles.trnsx(b.rotation(), step);
            b.y += Angles.trnsy(b.rotation(), step);
        }
    }

    public class HeatDriverBuild extends HeatConductorBuild{

        public int link = -1;
        public Seq<Building> owner = new Seq<>();
        public float aimRotation = 90f;
        public float progress = 0f;
        public float resProgress = 0f;
        public float reloadCounter = 0f;
        public float decayTimer = 0f;
        public float receiveAngle = -1f;

        public void setOneLink(Integer v){
            link = v;
        }

        public void setLink2(int v){
            link = v;
        }

        public void setOwner(Building build){
            owner.add(build);
        }

        public void removeOwner(Building build){
            owner.remove(build);
        }

        public Seq<Building> getOwner(){
            return owner;
        }

        public int getLink(){
            return link;
        }

        public float getRotation(){
            return aimRotation;
        }

        public float getProgress(){
            return progress;
        }

        public float getResProgress(){
            return resProgress;
        }

        public boolean linkValid(){
            if(link == -1) return false;

            Building other = Vars.world.build(link);
            if(other == null){
                link = -1;
                return false;
            }

            return other.block == block && other.team == team && within(other, range);
        }

        public void setHeat(float v){
            heat = v;
        }

        @Override
        public void updateTile(){
            checkOwner();

            if(owner.size == 0 && link == -1) heat = 0f;

            if(reloadCounter > 0f){
                reloadCounter = Mathf.clamp(reloadCounter - edelta() / reloadTime);
            }

            Building other = Vars.world.build(link);
            boolean hasLink = linkValid();

            if(hasLink){
                HeatDriverBuild target = (HeatDriverBuild)other;

                if(target.checkOneOwner(this)) target.setOwner(this);

                float toRotation = angleTo(other);
                aimRotation = Angles.moveToward(aimRotation, toRotation, rotateSpeed * efficiency * delta());

                if(Angles.near(aimRotation, toRotation, 2f)){
                    other.noSleep();
                    target.prepareReceive(other.angleTo(this));
                    updateTransfer();

                    if(reloadCounter <= 0.001f && aligned(target, toRotation)){
                        fire(target);
                    }

                    progress = Mathf.slerpDelta(progress, 1f, 0.02f * power.status);
                }else{
                    progress = Mathf.slerpDelta(progress, 0f, 0.04f);
                }
            }else{
                if(receiveAngle >= 0f){
                    aimRotation = Angles.moveToward(aimRotation, receiveAngle, rotateSpeed * efficiency * delta());
                }
                progress = Mathf.slerpDelta(progress, 0f, 0.04f);
            }

            if(owner.size > 0 && heat > 0f){
                decayTimer += delta();
                if(decayTimer >= decayInterval){
                    decayTimer -= decayInterval;
                    heat = Math.max(0f, heat - decayAmount);
                    if(heat <= 0f){
                        heat = 0f;
                        decayTimer = 0f;
                    }
                }
            }else{
                decayTimer = 0f;
            }

            float p = Math.min((heat / ((HeatConductor)block).visualMaxHeat), 1f);
            if(owner.size > 0 && p > 0f){
                resProgress = Mathf.slerpDelta(resProgress, 1f, 0.02f * p);
            }else{
                resProgress = Mathf.slerpDelta(resProgress, 0f, 0.05f);
            }
        }

        public void updateTransfer(){
            if(owner.size == 0) super.updateTile();
        }

        public boolean aligned(HeatDriverBuild other, float targetAngle){
            if(other.getLink() != -1) return true;
            return Angles.near(other.getRotation(), targetAngle + 180f, 2f);
        }

        public void fire(HeatDriverBuild other){
            if(heat <= 0.01f) return;

            float amount = Math.min(heat, maxHeat);
            heat = Math.max(0f, heat - amount);
            reloadCounter = 1f;
            decayTimer = 0f;

            float angle = angleTo(other);
            float ox = x + Angles.trnsx(angle, fireOffset);
            float oy = y + Angles.trnsy(angle, fireOffset);

            Sounds.massdriver.at(x, y, 1f + Mathf.range(0.2f), 0.5f);
            Fx.shootBig2.at(ox, oy, angle);
            Fx.shootBigSmoke2.at(ox, oy, angle);
            Effect.shake(shakeStrength, shakeStrength, this);

            heatBullet.create(this, team, ox, oy, angle, 0f, 1f, 10f, new HeatDriverBulletData(other, amount));
        }

        public void receiveHeat(float amount){
            receiveAngle = -1f;
            if(amount <= 0f) return;

            heat = Math.min(heat + amount, maxHeat);
            decayTimer = 0f;

            Sounds.massdriverReceive.at(x, y, 1f + Mathf.range(0.2f), 0.5f);
        }

        public void prepareReceive(float angle){
            receiveAngle = angle;
        }

        @Override
        public void draw(){
            float recoil = reloadCounter * recoilDistance;
            float ox = Angles.trnsx(aimRotation + 180f, recoil);
            float oy = Angles.trnsy(aimRotation + 180f, recoil);

            if(baseRegion != null && baseRegion.found()) Draw.rect(baseRegion, x, y);
            if(bodyRegion != null && bodyRegion.found()) Draw.rect(bodyRegion, x + ox, y + oy, aimRotation - 90f);
        }

        @Override
        public float heat(){
            return owner.size > 0 && link == -1 ? heat : 0f;
        }

        @Override
        public void drawConfigure(){
            float sin = Mathf.absin(Time.time, 6f, 1f);

            Draw.color(Pal.accent);
            Lines.stroke(1f);

            Drawf.circles(x, y, (block.size / 2f + 1f) * Vars.tilesize + sin - 2f, Pal.accent);

            if(linkValid()){
                Building other = Vars.world.build(link);
                Drawf.circles(other.x, other.y, (block.size / 2f + 1f) * Vars.tilesize + sin - 2f, Pal.place);
                Drawf.arrow(x, y, other.x, other.y, block.size * Vars.tilesize + sin, 4f + sin);
            }

            for(int i = 0; owner.size > 0 && i < owner.size; i++){
                Building o = owner.get(i);
                Drawf.circles(o.x, o.y, (block.size / 2f + 1f) * Vars.tilesize + sin - 2f, Pal.place);
                Drawf.arrow(o.x, o.y, x, y, block.size * Vars.tilesize + sin, 4f + sin, Pal.place);
            }

            Drawf.dashCircle(x, y, range, Pal.accent);
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other){
                if(link == -1) deselect();
                configure(-1);
                return false;
            }

            if(link == other.pos()){
                if(other instanceof HeatDriverBuild h) h.removeOwner(this);
                configure(-1);
                return false;
            }else if(other.block == block && dst(other) <= range && other.team == team && checkOneOwner(other)){
                Building lastLink = Vars.world.build(link);
                if(lastLink instanceof HeatDriverBuild h) h.removeOwner(this);

                ((HeatDriverBuild)other).setOwner(this);
                configure(other.pos());
                return false;
            }

            return true;
        }

        @Override
        public void configure(Object value){
            link = value instanceof Number n ? n.intValue() : -1;
        }

        public void checkOwner(){
            for(int i = 0; i < owner.size; i++){
                int pos = owner.get(i).pos();
                Building build = Vars.world.build(pos);
                if(build == null || build.block != block || ((HeatDriverBuild)build).getLink() != pos()){
                    owner.remove(i);
                }
            }
        }

        public boolean checkOneOwner(Building other){
            if(owner.size == 0) return true;

            int i = 0;
            for(; i < owner.size; i++){
                int pos = owner.get(i).pos();
                if(Vars.world.build(pos) == other) break;
            }

            return i == owner.size;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(link);
            write.f(aimRotation);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            link = read.i();
            aimRotation = read.f();
            progress = read.f();
        }
    }
}
