package solaris2.liquid;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 由 scripts/建筑/液体/液体驱动器.js 移植而来。
 */
public class LiquidDriver extends LiquidRouter{

    public static final float range = 45f * 8f;
    public static final float deliveryRate = 40f;
    public static final float minDelivery = 25f;
    public static final float shakeStrength = 3f;

    public static final float shotSpeed = 5.5f;
    public static final float hitDistance = 8f;
    public static final float muzzleOffset = 4f;
    public static final float reloadTime = 150f;
    public static final float rotateSpeed = 90f;
    public static final float recoilDistance = 4f;

    public static final int stateIdle = 0;
    public static final int stateAccept = 1;
    public static final int stateFire = 2;

    private static final IntFloatMap aimStates = new IntFloatMap();

    public TextureRegion baseTex, tankTex, liquidTex, topTex;

    public final LiquidShot shot = new LiquidShot();

    public LiquidDriver(String name){
        super(name);

        update = true;
        solid = true;
        destructible = true;
        hasLiquids = true;
        configurable = true;

        buildType = LiquidDriverBuild::new;

        config(Integer.class, (LiquidDriverBuild tile, Integer value) -> tile.setOneLink(value));
    }

    @Override
    public void load(){
        super.load();
        baseTex = Core.atlas.find(name + "-base");
        tankTex = Core.atlas.find(name);
        liquidTex = Core.atlas.find(name + "-liquid");
        topTex = Core.atlas.find(name + "-top");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.shootRange, range, StatUnit.blocks);
        stats.add(Stat.reload, 60f / reloadTime, StatUnit.perSecond);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.accent);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(Core.atlas.find(name + "-base"), plan.drawx(), plan.drawy());
        Draw.rect(Core.atlas.find(name), plan.drawx(), plan.drawy());
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(name + "-base"), Core.atlas.find(name)};
    }

    public static class Payload{
        public final Building to;
        public final Liquid liquid;
        public final float amount;
        public final Building from;

        public Payload(Building to, Liquid liquid, float amount, Building from){
            this.to = to;
            this.liquid = liquid;
            this.amount = amount;
            this.from = from;
        }
    }

    public static class LiquidShot extends BasicBulletType{

        public LiquidShot(){
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
            if(!(b.data instanceof Payload payload) || payload.to == null || payload.to.dead){
                b.remove();
                return;
            }
            float step = shotSpeed * Time.delta;
            if(b.dst(payload.to) <= Math.max(hitDistance, step)){
                b.x = payload.to.x;
                b.y = payload.to.y;
                if(payload.to instanceof LiquidDriverBuild build){
                    build.receiveLiquid(payload.liquid, payload.amount, payload.from);
                }
                b.remove();
                return;
            }
            b.x += Angles.trnsx(b.rotation(), step);
            b.y += Angles.trnsy(b.rotation(), step);
        }
    }

    public class LiquidDriverBuild extends LiquidRouterBuild{

        public int link = -1;
        public float beamRotation = 90f;
        public float beamProgress = 0f;
        public float resProgress = 0f;
        public float reloadCounter = 0f;
        public int driverState = stateIdle;

        public final Seq<LiquidDriverBuild> waiters = new Seq<>();

        public void setOneLink(int value){
            link = value;
        }

        public int getLink(){
            return link;
        }

        public float getBeamRotation(){
            return beamRotation;
        }

        public float getBeamProgress(){
            return beamProgress;
        }

        public float getResProgress(){
            return resProgress;
        }

        public int getDriverState(){
            return driverState;
        }

        public boolean linkValid(){
            if(link == -1) return false;
            Building other = world.build(link);
            if(other == null){
                link = -1;
                return false;
            }
            return other != this && other.block == block && other.team == team && within((Position)other, range);
        }

        public void queue(LiquidDriverBuild build){
            if(!waiters.contains(build)) waiters.add(build);
        }

        public void unqueue(LiquidDriverBuild build){
            waiters.remove(build);
        }

        public LiquidDriverBuild currentShooter(){
            return waiters.isEmpty() ? null : waiters.first();
        }

        public boolean shooterValid(LiquidDriverBuild other){
            return other != null && other.block == block && other.getLink() == pos() && other.efficiency > 0f && within((Position)other, range);
        }

        public void pump(){
            float cap = block.liquidCapacity;
            if(liquids.currentAmount() >= cap - 0.001f) return;
            float amount = deliveryRate * delta() * power.status;
            for(int i = 0; i < 4 && amount > 0.001f; i++){
                Building other = nearby(i);
                if(other == null || other.team != team || other.liquids == null) continue;
                Liquid liq = other.liquids.current();
                if(liq == null || other.liquids.get(liq) <= 0.001f) continue;
                if(liquids.current() != liq && liquids.currentAmount() > 0.001f) continue;
                float take = Math.min(amount, Math.min(other.liquids.get(liq), cap - liquids.currentAmount()));
                if(take <= 0.001f) continue;
                other.liquids.remove(liq, take);
                liquids.add(liq, take);
                amount -= take;
            }
        }

        public void fire(Building other){
            Liquid liq = liquids.current();
            float amount = liquids.currentAmount();
            if(liq == null || amount <= 0.001f) return;
            reloadCounter = 1f;
            liquids.clear();
            float angle = angleTo((Position)other);
            float muzzleX = x + Angles.trnsx(angle, muzzleOffset);
            float muzzleY = y + Angles.trnsy(angle, muzzleOffset);
            Sounds.massdriver.at(x, y, 1f + Mathf.range(0.2f), 0.5f);
            Fx.shootBig2.at(muzzleX, muzzleY, angle);
            Fx.shootBigSmoke2.at(muzzleX, muzzleY, angle);
            Effect.shake(shakeStrength, shakeStrength, this);
            shot.create(this, team, muzzleX, muzzleY, angle, 0f, 1f, 10f, new Payload(other, liq, amount, this));
        }

        public void receiveLiquid(Liquid liq, float amount, Building from){
            if(from instanceof LiquidDriverBuild build) unqueue(build);
            driverState = stateIdle;
            if(liquids.current() != liq && liquids.currentAmount() > 0.2f) return;
            float space = block.liquidCapacity - liquids.currentAmount();
            if(space <= 0.001f) return;
            reloadCounter = 1f;
            Effect.shake(shakeStrength, shakeStrength, this);
            Fx.mineBig.at(x, y);
            Sounds.massdriverReceive.at(x, y, 1f + Mathf.range(0.2f), 0.5f);
            liquids.add(liq, Math.min(amount, space));
        }

        @Override
        public void draw(){
            Draw.rect(baseTex, x, y);
            float recoil = reloadCounter * recoilDistance;
            float offsetX = Angles.trnsx(beamRotation + 180f, recoil);
            float offsetY = Angles.trnsy(beamRotation + 180f, recoil);
            float drawAngle = beamRotation - 90f;
            Draw.rect(tankTex, x + offsetX, y + offsetY, drawAngle);
            if(liquids.current() != null){
                Drawf.liquid(liquidTex, x + offsetX, y + offsetY, liquids.currentAmount() / block.liquidCapacity, liquids.current().color, drawAngle);
            }
            Draw.rect(topTex, x + offsetX, y + offsetY, drawAngle);
        }

        @Override
        public void updateTile(){
            noSleep();
            Building other = linkValid() ? world.build(link) : null;
            if(other != null) link = other.pos();
            if(reloadCounter > 0f) reloadCounter = Mathf.clamp(reloadCounter - edelta() / reloadTime);
            beamProgress = 1f - reloadCounter;
            if(other != null) pump();

            LiquidDriverBuild head = currentShooter();
            if(head != null && !shooterValid(head)) waiters.remove(head);

            if(driverState == stateIdle){
                if(waiters.size > 0 && block.liquidCapacity - liquids.currentAmount() >= minDelivery){
                    driverState = stateAccept;
                }else if(other != null){
                    driverState = stateFire;
                }
            }

            if((driverState == stateIdle || driverState == stateAccept) && !linkValid()){
                Liquid currentLiquid = liquids.current();
                if(currentLiquid != null) dumpLiquid(currentLiquid);
            }

            if(efficiency > 0f){
                if(driverState == stateAccept){
                    LiquidDriverBuild shooter = currentShooter();
                    if(shooter == null || block.liquidCapacity - liquids.currentAmount() < minDelivery){
                        driverState = stateIdle;
                    }else{
                        shooter.noSleep();
                        beamRotation = Angles.moveToward(beamRotation, angleTo((Position)shooter), rotateSpeed * efficiency * delta());
                    }
                }else if(driverState == stateFire){
                    boolean waiting = waiters.size > 0 && block.liquidCapacity - liquids.currentAmount() >= minDelivery;
                    if(other == null || waiting){
                        driverState = stateIdle;
                    }else if(other.liquids != null){
                        float targetAngle = angleTo((Position)other);
                        float space = other.block.liquidCapacity - other.liquids.currentAmount();
                        if(liquids.currentAmount() >= minDelivery && space >= minDelivery){
                            other.noSleep();
                            if(other instanceof LiquidDriverBuild driver) driver.queue(this);
                            if(reloadCounter <= 0.001f){
                                beamRotation = Angles.moveToward(beamRotation, targetAngle, rotateSpeed * efficiency * delta());
                                if(other instanceof LiquidDriverBuild driver){
                                    LiquidDriverBuild otherHead = driver.currentShooter();
                                    boolean hasOtherAim = aimStates.containsKey(other.pos());
                                    float otherAim = aimStates.get(other.pos());
                                    if(otherHead != null && otherHead.pos() == pos() && driver.getDriverState() == stateAccept
                                        && Angles.near(beamRotation, targetAngle, 2f) && hasOtherAim && Angles.near(otherAim, targetAngle + 180f, 2f)){
                                        fire(other);
                                        driverState = stateIdle;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            aimStates.put(pos(), beamRotation);
            float p = Math.min(liquids.currentAmount() / block.liquidCapacity, 1f);
            resProgress = Mathf.slerpDelta(resProgress, p, 0.05f);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            noSleep();
            return liquids.currentAmount() < block.liquidCapacity && linkValid();
        }

        @Override
        public void drawConfigure(){
            float sin = Mathf.absin(Time.time, 6f, 1f);
            Draw.color(Pal.accent);
            Lines.stroke(1f);
            Drawf.circles(x, y, (block.size / 2f + 1f) * tilesize + sin - 2f, Pal.accent);
            for(int i = 0; i < waiters.size; i++){
                LiquidDriverBuild shooter = waiters.get(i);
                Drawf.circles(shooter.x, shooter.y, (block.size / 2f + 1f) * tilesize + sin - 2f, Pal.place);
                Drawf.arrow(shooter.x, shooter.y, x, y, block.size * tilesize + sin, 4f + sin, Pal.place);
            }
            if(linkValid()){
                Building other = world.build(link);
                Drawf.circles(other.x, other.y, (block.size / 2f + 1f) * tilesize + sin - 2f, Pal.place);
                Drawf.arrow(x, y, other.x, other.y, block.size * tilesize + sin, 4f + sin);
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
                configure(-1);
                return false;
            }else if(other.block == this.block && dst((Position)other) <= range && other.team == team){
                configure(other.pos());
                return false;
            }

            return true;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(link);
            write.f(beamRotation);
            write.i(driverState);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            link = read.i();
            beamRotation = read.f();
            driverState = read.i();
        }
    }
}
