package solaris2.content;

import arc.graphics.Color;
import arc.math.Interp;
import arc.math.geom.Rect;
import mindustry.entities.units.AIController;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.entities.effect.ExplosionEffect;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.ParticleEffect;
import mindustry.entities.effect.WaveEffect;
import mindustry.entities.part.DrawPart;
import mindustry.entities.part.RegionPart;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.Sounds;
import mindustry.gen.TankUnit;
import mindustry.gen.UnitEntity;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.unit.TankUnitType;

/**
 * 单位内容：原 assets/content/units 下 json 与 建筑/无人机/无人机.js 的 Java 实现。
 */
public class SolarisUnits{

    public static UnitType droneCannon, droneMissile, miningDrone, bomberTank;

    private static final ExplosionEffect cannonHitFx = new ExplosionEffect(){{
        waveRad = 12f;
        waveRadBase = 0f;
        waveColor = Color.valueOf("ffaa66");
        waveStroke = 1.5f;
        waveLife = 12f;
        smokes = 2;
        sparks = 5;
        sparkLen = 3f;
        sparkStroke = 1.2f;
        sparkColor = Color.valueOf("ffaa66");
        sparkRad = 12f;
    }};

    static{
        // ===== 机炮无人机 =====
        droneCannon = new UnitType("无人机-机炮");
        droneCannon.constructor = UnitEntity::create;
        droneCannon.weapons.add(new Weapon(){{
            name = "无人机机炮";
            x = 2f;
            y = 0f;
            mirror = true;
            rotate = true;
            reload = 15f;
            shootCone = 90f;
            shootSound = mindustry.gen.Sounds.shoot;
            inaccuracy = 0f;
            bullet = new BasicBulletType(){{
                damage = 24f;
                width = 7f;
                height = 9f;
                speed = 6.5f;
                lifetime = 13.8f;
                inaccuracy = 4f;
                backColor = Color.valueOf("ffaa66");
                frontColor = Color.valueOf("ffcc88");
                collidesAir = true;
                trailLength = 5;
                trailWidth = 0.8f;
                trailColor = Color.valueOf("ffaa66");
                shootEffect = Fx.shootSmall;
                smokeEffect = Fx.shootSmallSmoke;
                hitEffect = cannonHitFx;
                despawnEffect = cannonHitFx;
            }};
        }});
        droneCannon.health = 800f;
        droneCannon.speed = 3.2f;
        droneCannon.hitSize = 8f;
        droneCannon.rotateSpeed = 10f;
        droneCannon.engineOffset = 3f;
        droneCannon.engineSize = 1.8f;
        droneCannon.flying = true;
        droneCannon.targetAir = true;
        droneCannon.targetGround = true;
        droneCannon.range = 90f;
        droneCannon.playerControllable = false;
        droneCannon.useUnitCap = false;
        droneCannon.controller = u -> new AIController(){
            @Override
            public void updateUnit(){
            }
        };

        // ===== 导弹无人机 =====
        droneMissile = new UnitType("无人机-导弹");
        droneMissile.constructor = UnitEntity::create;
        droneMissile.weapons.add(new Weapon(){{
            name = "无人机导弹发射器";
            x = 3f;
            y = 0f;
            mirror = true;
            rotate = true;
            reload = 60f;
            shootSound = mindustry.gen.Sounds.shoot;
            inaccuracy = 0.5f;
            bullet = new MissileBulletType(){{
                damage = 20f;
                speed = 5f;
                lifetime = 50f;
                splashDamage = 60f;
                splashDamageRadius = 25f;
                homingPower = 0.12f;
                homingRange = 100f;
                width = 8f;
                height = 8f;
                shrinkY = 0f;
                drag = -0.003f;
                keepVelocity = false;
                weaveScale = 6f;
                weaveMag = 1f;
                backColor = Pal.unitBack;
                frontColor = Pal.unitFront;
                trailColor = Pal.unitBack;
                hitEffect = Fx.blastExplosion;
                despawnEffect = Fx.blastExplosion;
                trailLength = 8;
                trailWidth = 1f;
                shootEffect = Fx.shootBig;
                collidesAir = true;
                collidesGround = true;
            }};
        }});
        droneMissile.health = 700f;
        droneMissile.speed = 2.8f;
        droneMissile.hitSize = 8f;
        droneMissile.rotateSpeed = 9f;
        droneMissile.engineOffset = 3f;
        droneMissile.engineSize = 1.8f;
        droneMissile.flying = true;
        droneMissile.targetAir = true;
        droneMissile.targetGround = true;
        droneMissile.range = 150f;
        droneMissile.playerControllable = false;
        droneMissile.useUnitCap = false;
        droneMissile.controller = u -> new AIController(){
            @Override
            public void updateUnit(){
            }
        };

        // ===== 采集矿机 =====
        miningDrone = new UnitType("采集矿机");
        miningDrone.constructor = UnitEntity::create;
        miningDrone.flying = true;
        miningDrone.lowAltitude = true;
        miningDrone.omniMovement = true;
        miningDrone.physics = false;
        miningDrone.hittable = false;
        miningDrone.killable = false;
        miningDrone.targetable = false;
        miningDrone.useUnitCap = false;
        miningDrone.playerControllable = false;
        miningDrone.logicControllable = false;
        miningDrone.allowedInPayloads = false;
        miningDrone.createWreck = false;
        miningDrone.createScorch = false;
        miningDrone.drawCell = false;
        miningDrone.drawMinimap = false;
        miningDrone.isEnemy = false;
        miningDrone.speed = 2.2f;
        miningDrone.drag = 0.35f;
        miningDrone.accel = 0.55f;
        miningDrone.hitSize = 5f;
        miningDrone.rotateSpeed = 12f;
        miningDrone.health = 150f;
        miningDrone.armor = 0f;
        miningDrone.drownTimeMultiplier = 100f;
        miningDrone.clipSize = 24;
        miningDrone.outlineRadius = 2;
        miningDrone.outlineColor = Color.valueOf("434343FF");
        miningDrone.envEnabled = -1;
        miningDrone.envDisabled = 0;
        miningDrone.mineTier = 3;
        miningDrone.mineSpeed = 1.8f;
        miningDrone.mineRange = 22f;
        miningDrone.mineWalls = true;
        miningDrone.mineFloor = true;
        miningDrone.mineHardnessScaling = true;
        miningDrone.itemCapacity = 25;
        miningDrone.payloadCapacity = 0f;
        miningDrone.buildSpeed = -1f;
        miningDrone.canBoost = false;
        miningDrone.fogRadius = 0f;
        miningDrone.hidden = false;
        miningDrone.alwaysUnlocked = true;

        // ===== 轰炸坦克 =====
        bomberTank = new TankUnitType("陆3");
        bomberTank.constructor = TankUnit::create;
        bomberTank.localizedName = "轰炸";
        bomberTank.description = "向远程地面目标发射50枚温压弹，对建筑破拆效果极佳。";
        bomberTank.details = "发射前需要架起，需要提防敌人的远程打击与空中力量";
        bomberTank.treadFrames = 8;
        bomberTank.treadPullOffset = 8;
        bomberTank.treadRects = new Rect[]{
            new Rect(-45f, -45f, 24f, 88f)
        };
        bomberTank.speed = 1.3f;
        bomberTank.hitSize = 20f;
        bomberTank.ammoCapacity = 16;
        bomberTank.targetAir = false;
        bomberTank.squareShape = true;
        bomberTank.omniMovement = false;
        bomberTank.rotateMoveFirst = false;
        bomberTank.health = 1850f;
        bomberTank.armor = 11f;
        bomberTank.rotateSpeed = 2f;
        bomberTank.itemCapacity = 45;
        bomberTank.faceTarget = false;

        bomberTank.weapons.add(new Weapon(){{
            name = "轰炸1";
            reload = 250f;
            x = 0f;
            y = 0f;
            rotate = true;
            mirror = true;
            alternate = false;
            rotateSpeed = 1.3f;
            minWarmup = 0.93f;
            shootWarmupSpeed = 0.05f;
            xRand = 4f;
            inaccuracy = 6f;
            shootStatus = StatusEffects.unmoving;
            shootStatusDuration = 200f;
            velocityRnd = 0.1f;
            shoot = new ShootPattern(){{
                shots = 25;
                shotDelay = 6f;
            }};
            parts.add(new RegionPart(){{
                mirror = true;
                suffix = "-前";
                under = true;
                moveY = -4f;
                progress = DrawPart.PartProgress.warmup;
            }});
            bullet = new ArtilleryBulletType(){{
                sprite = "索拉里斯2.0-导弹";
                backColor = Color.valueOf("FF7055");
                frontColor = Color.valueOf("E3E3E3");
                width = 8f;
                height = 45f;
                trailChance = 1f;
                trailInterval = 1f;
                trailEffect = new ParticleEffect(){{
                    particles = 2;
                    length = 6f;
                    baseLength = 0f;
                    sizeInterp = Interp.pow5In;
                    lifetime = 35f;
                    colorFrom = Color.valueOf("E3E3E3");
                    colorTo = Color.valueOf("E3E3E300");
                    sizeFrom = 3f;
                    sizeTo = 0f;
                }};
                splashDamage = 55f;
                splashDamageRadius = 45f;
                buildingDamageMultiplier = 2.33f;
                collides = false;
                status = StatusEffects.blasted;
                statusDuration = 60f;
                shootEffect = Fx.shootSmallFlame;
                smokeEffect = new ParticleEffect(){{
                    particles = 9;
                    interp = Interp.pow10Out;
                    sizeInterp = Interp.pow10In;
                    sizeFrom = 6f;
                    sizeTo = 0f;
                    length = -58f;
                    baseLength = -20f;
                    lifetime = 42f;
                    colorFrom = Color.valueOf("73737390");
                    colorTo = Color.valueOf("73737390");
                    cone = 40f;
                    layer = 49f;
                }};
                speed = 12f;
                lifetime = 41.6f;
                hitShake = 2f;
                hitSound = Sounds.explosion;
                hitEffect = new MultiEffect(
                    new ParticleEffect(){{
                        particles = 8;
                        sizeFrom = 10f;
                        sizeTo = 0f;
                        length = 35f;
                        baseLength = 33f;
                        lifetime = 35f;
                        colorFrom = Color.valueOf("737373FF");
                        colorTo = Color.valueOf("737373FF");
                        cone = 360f;
                    }},
                    new ParticleEffect(){{
                        particles = 12;
                        line = true;
                        strokeFrom = 2f;
                        strokeTo = 0f;
                        lenFrom = 16f;
                        lenTo = 8f;
                        length = 66f;
                        baseLength = 3f;
                        lifetime = 12f;
                        colorFrom = Color.valueOf("FFFFFF");
                        colorTo = Color.valueOf("FFE176FF");
                        cone = 360f;
                    }},
                    new ParticleEffect(){{
                        particles = 1;
                        sizeFrom = 0f;
                        sizeTo = 45f;
                        length = 0f;
                        baseLength = 0f;
                        lifetime = 10f;
                        colorFrom = Color.valueOf("FFFFFF80");
                        colorTo = Color.valueOf("FFFFFF00");
                        cone = 360f;
                    }},
                    new WaveEffect(){{
                        lifetime = 10f;
                        sizeFrom = 0f;
                        sizeTo = 45f;
                        strokeFrom = 3f;
                        strokeTo = 0f;
                        colorFrom = Color.valueOf("FFFFFFFF");
                        colorTo = Color.valueOf("FFFFFF00");
                    }}
                );
                despawnEffect = Fx.flakExplosionBig;
            }};
        }});

        bomberTank.parts.add(new RegionPart(){{
            mirror = true;
            y = -4.75f;
            suffix = "-支架";
            layer = 59f;
            moveX = 5.55f;
            moveY = -8f;
            moveRot = -30f;
            progress = DrawPart.PartProgress.warmup;
        }});
    }

    /** 由主类或 JS 调用，触发静态初始化。 */
    public static void load(){
    }
}
