package solaris2.orbit;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import solaris2.*;

/**
 * 由 scripts/建筑/轨道/轨道射击特效.js 移植而来。
 * 这里保存全部特效、子弹与轨道打击全局状态。
 */
public class OrbitFireEffects{

    // ========== 轨道打击全局状态 ==========
    /** 已发射的轨道器数量。 */
    public static int orbiters = 0;
    /** 已储备的轨道弹数量。 */
    public static int shells = 0;
    /** 已绑定轨道器的标记数量。 */
    public static int bound = 0;
    /** 已注册的标记建筑数量。 */
    public static int markers = 0;

    /** 打击附加状态：碎甲。 */
    public static final StatusEffect armorShatter = SolarisContent.status("碎甲");

    // ========== 颜色 ==========
    public static final Color primary = Color.valueOf("FF3B3BFF");
    public static final Color secondary = Color.valueOf("FF6B6BFF");
    public static final Color bright = Color.valueOf("FFB3B3FF");
    public static final Color dark = Color.valueOf("CC2B2BFF");
    public static final Color whiteHot = Color.valueOf("FFFFFFFF");
    public static final Color steelGray = Color.valueOf("FF3B3BFF");
    public static final Color steelBlue = Color.valueOf("FF6B6BFF");
    public static final Color silverGray = Color.valueOf("FFB3B3FF");
    public static final Color rustRed = Color.valueOf("CC2B2BFF");

    // ========== 主打击参数 ==========
    public static final float CHARGE_FRAMES = 26f;
    public static final float HOLD_FRAMES = 22f;
    /** 落地后激光继续驻留的帧数。 */
    public static final float FRAG_DELAY = 14f;
    public static final float IMPACT_RADIUS = 240f;
    public static final float BEAM_LENGTH = IMPACT_RADIUS * 4.5f;
    /** 从高空天顶偏左斜落（108°≈垂直向上偏 18°）。 */
    public static final float BEAM_ANGLE = 108f;

    // ========== 浩劫特效 ==========
    public static final Effect havocEffect1 = new ExplosionEffect(){{
        waveColor = steelGray;
        waveRad = 0f;
        waveRadBase = 8f;
        waveStroke = 16f;
        waveLife = 180f;
        lifetime = 180f;
        sparks = 0;
        smokes = 0;
    }};

    public static final Effect havocEffect2 = new ParticleEffect(){{
        particles = 5;
        sizeFrom = 3f;
        sizeTo = 0f;
        lifetime = 30f;
        length = 16f;
        colorFrom = silverGray;
        colorTo = steelBlue;
    }};

    public static final Effect havocEffect3 = new WaveEffect(){{
        colorFrom = steelGray;
        colorTo = steelBlue;
        sizeFrom = 180f;
        sizeTo = 180f;
        lifetime = 100f;
        strokeFrom = 20f;
        strokeTo = 0f;
    }};

    public static final Effect havocEffect13 = new MultiEffect(havocEffect1, havocEffect3);

    // ========== 动能飞行尾迹 ==========
    public static final Effect machTrailCore = new ParticleEffect(){{
        particles = 50;
        sizeFrom = 5f;
        sizeTo = 1f;
        lifetime = 70f;
        length = 320f;
        cone = 5f;
        colorFrom = whiteHot;
        colorTo = silverGray;
        interp = Interp.pow3Out;
    }};

    public static final Effect machTrailIon = new ParticleEffect(){{
        particles = 35;
        sizeFrom = 8f;
        sizeTo = 2f;
        lifetime = 85f;
        length = 280f;
        cone = 10f;
        colorFrom = steelBlue;
        colorTo = silverGray;
        interp = Interp.circleOut;
    }};

    public static final Effect machShock1 = new WaveEffect(){{
        colorFrom = whiteHot;
        colorTo = steelBlue;
        sizeFrom = 4f;
        sizeTo = 55f;
        lifetime = 22f;
        strokeFrom = 5f;
        strokeTo = 0f;
        interp = Interp.pow3Out;
    }};

    public static final Effect machShock2 = new WaveEffect(){{
        colorFrom = silverGray;
        colorTo = steelGray;
        sizeFrom = 6f;
        sizeTo = 75f;
        lifetime = 30f;
        strokeFrom = 3f;
        strokeTo = 0f;
        interp = Interp.pow3Out;
    }};

    public static final Effect noseFlash = new ParticleEffect(){{
        particles = 3;
        sizeFrom = 14f;
        sizeTo = 0f;
        lifetime = 10f;
        length = 0f;
        baseLength = 0f;
        cone = 10f;
        colorFrom = whiteHot;
        colorTo = silverGray;
        interp = Interp.circleOut;
    }};

    public static final Effect supersonicStreak = new ParticleEffect(){{
        particles = 12;
        sizeFrom = 2f;
        sizeTo = 0f;
        lifetime = 45f;
        length = 130f;
        cone = 15f;
        colorFrom = steelBlue;
        colorTo = bright;
        interp = Interp.fastSlow;
    }};

    public static final Effect kineticTrail = new MultiEffect(machTrailCore, machTrailIon, noseFlash, supersonicStreak);

    public static final Effect machRingComposite = new MultiEffect(machShock1, machShock2);

    // ========== 瞄准锁定标记 ==========
    public static final Effect aimOuterRing = new WaveEffect(){{
        colorFrom = steelGray;
        colorTo = steelBlue;
        sizeFrom = 2f;
        sizeTo = 2f;
        lifetime = 40f;
        strokeFrom = 3f;
        strokeTo = 3f;
        interp = Interp.linear;
    }};

    public static final Effect aimCrossPulse = new WaveEffect(){{
        colorFrom = whiteHot;
        colorTo = steelBlue;
        sizeFrom = 3f;
        sizeTo = 40f;
        lifetime = 18f;
        strokeFrom = 4f;
        strokeTo = 0f;
        interp = Interp.pow3Out;
    }};

    public static final Effect aimLockMark = new MultiEffect(aimOuterRing, aimCrossPulse);

    // ========== 轨道打击落地：冲击波与闪光 ==========
    public static final Effect impactPrimeWave = new WaveEffect(){{
        colorFrom = whiteHot;
        colorTo = silverGray;
        sizeFrom = 50f;
        sizeTo = 440f;
        lifetime = 24f;
        strokeFrom = 32f;
        strokeTo = 0f;
        interp = Interp.pow4Out;
    }};

    public static final Effect impactSubWave = new WaveEffect(){{
        colorFrom = steelGray;
        colorTo = rustRed;
        sizeFrom = 40f;
        sizeTo = 330f;
        lifetime = 28f;
        strokeFrom = 22f;
        strokeTo = 0f;
        interp = Interp.pow4Out;
    }};

    public static final Effect impactInnerPulse = new WaveEffect(){{
        colorFrom = whiteHot;
        colorTo = primary;
        sizeFrom = 25f;
        sizeTo = 200f;
        lifetime = 20f;
        strokeFrom = 16f;
        strokeTo = 0f;
        interp = Interp.pow4Out;
    }};

    public static final Effect impactCoreFlash = new ParticleEffect(){{
        particles = 1;
        sizeFrom = 15f;
        sizeTo = 180f;
        lifetime = 12f;
        length = 0f;
        baseLength = 0f;
        colorFrom = whiteHot;
        colorTo = whiteHot;
        interp = Interp.circleOut;
    }};

    public static final Effect impactMidFlash = new ParticleEffect(){{
        particles = 1;
        sizeFrom = 10f;
        sizeTo = 220f;
        lifetime = 18f;
        length = 0f;
        baseLength = 0f;
        colorFrom = silverGray;
        colorTo = primary;
        interp = Interp.circleOut;
    }};

    public static final Effect impactOuterFlash = new ParticleEffect(){{
        particles = 1;
        sizeFrom = 5f;
        sizeTo = 260f;
        lifetime = 25f;
        length = 0f;
        baseLength = 0f;
        colorFrom = steelBlue;
        colorTo = secondary;
        interp = Interp.circleOut;
    }};

    // ========== 破片层 ==========
    public static final Effect hyperFragLayer = new ParticleEffect(){{
        particles = 90;
        sizeFrom = 4f;
        sizeTo = 0f;
        lifetime = 35f;
        length = 210f;
        cone = 360f;
        colorFrom = silverGray;
        colorTo = steelGray;
        interp = Interp.pow3Out;
    }};

    public static final Effect midFragLayer = new ParticleEffect(){{
        particles = 75;
        sizeFrom = 5f;
        sizeTo = 1f;
        lifetime = 45f;
        length = 140f;
        cone = 360f;
        colorFrom = primary;
        colorTo = rustRed;
        interp = Interp.circleOut;
    }};

    public static final Effect incomingSpray = new ParticleEffect(){{
        particles = 60;
        sizeFrom = 5f;
        sizeTo = 0f;
        lifetime = 40f;
        length = 170f;
        cone = 100f;
        colorFrom = whiteHot;
        colorTo = primary;
        interp = Interp.pow3Out;
    }};

    // ========== 地面冲击 ==========
    public static final Effect groundDustColumn = new ParticleEffect(){{
        particles = 35;
        sizeFrom = 14f;
        sizeTo = 30f;
        lifetime = 65f;
        length = 40f;
        cone = 80f;
        colorFrom = steelGray;
        colorTo = rustRed;
        interp = Interp.pow2In;
    }};

    public static final Effect groundHeaveRing = new ParticleEffect(){{
        particles = 50;
        sizeFrom = 7f;
        sizeTo = 3f;
        lifetime = 50f;
        length = 0f;
        cone = 360f;
        baseLength = 260f;
        colorFrom = steelGray;
        colorTo = dark;
        interp = Interp.pow2Out;
    }};

    public static final Effect groundEchoWave = new WaveEffect(){{
        colorFrom = steelBlue;
        colorTo = secondary;
        sizeFrom = 50f;
        sizeTo = 380f;
        lifetime = 20f;
        strokeFrom = 12f;
        strokeTo = 0f;
        interp = Interp.pow3Out;
    }};

    public static final Effect ricochetShards = new ParticleEffect(){{
        particles = 45;
        sizeFrom = 3f;
        sizeTo = 0f;
        lifetime = 70f;
        length = 0f;
        cone = 360f;
        baseLength = 300f;
        colorFrom = silverGray;
        colorTo = steelBlue;
        interp = Interp.linear;
    }};

    public static final Effect orbitalFlameRing = new ParticleEffect(){{
        particles = 50;
        sizeFrom = 6f;
        sizeTo = 0f;
        lifetime = 35f;
        length = 90f;
        cone = 360f;
        colorFrom = primary;
        colorTo = silverGray;
        interp = Interp.fastSlow;
    }};

    public static final Effect orbitalFlameColumn = new ParticleEffect(){{
        particles = 20;
        sizeFrom = 8f;
        sizeTo = 0f;
        lifetime = 45f;
        length = 90f;
        cone = 20f;
        colorFrom = whiteHot;
        colorTo = primary;
        interp = Interp.pow2Out;
    }};

    public static final Effect orbitalImpactEffect = new MultiEffect(
        impactPrimeWave,
        impactSubWave,
        impactInnerPulse,
        impactCoreFlash,
        impactMidFlash,
        impactOuterFlash,
        hyperFragLayer,
        midFragLayer,
        orbitalFlameRing,
        incomingSpray,
        groundDustColumn,
        orbitalFlameColumn,
        groundHeaveRing,
        groundEchoWave,
        ricochetShards
    );

    // ========== 命中：动能击穿电弧 + 破片飞溅 ==========
    public static final Effect hitArcParticles = new ParticleEffect(){{
        particles = 20;
        sizeFrom = 7f;
        sizeTo = 0f;
        lifetime = 18f;
        length = 100f;
        cone = 360f;
        colorFrom = whiteHot;
        colorTo = steelBlue;
        interp = Interp.fastSlow;
    }};

    public static final Effect hitEnergyPulse = new WaveEffect(){{
        colorFrom = whiteHot;
        colorTo = steelGray;
        sizeFrom = 4f;
        sizeTo = 110f;
        lifetime = 18f;
        strokeFrom = 14f;
        strokeTo = 0f;
        interp = Interp.pow4Out;
    }};

    public static final Effect hitCoreFlash = new ParticleEffect(){{
        particles = 1;
        sizeFrom = 80f;
        sizeTo = 0f;
        lifetime = 10f;
        length = 0f;
        baseLength = 0f;
        colorFrom = whiteHot;
        colorTo = silverGray;
        interp = Interp.circleOut;
    }};

    public static final Effect hitShardSpray = new ParticleEffect(){{
        particles = 28;
        sizeFrom = 4f;
        sizeTo = 0f;
        lifetime = 26f;
        length = 85f;
        cone = 360f;
        colorFrom = silverGray;
        colorTo = rustRed;
        interp = Interp.circleOut;
    }};

    // ========== 地面冲击坑残留 ==========
    public static final Effect scorchSpots = new ParticleEffect(){{
        particles = 16;
        sizeFrom = 18f;
        sizeTo = 12f;
        lifetime = 200f;
        length = 0f;
        baseLength = 45f;
        cone = 360f;
        colorFrom = rustRed;
        colorTo = rustRed;
        interp = Interp.pow2In;
    }};

    public static final Effect scatteredDust = new ParticleEffect(){{
        particles = 45;
        sizeFrom = 7f;
        sizeTo = 3f;
        lifetime = 170f;
        length = 0f;
        baseLength = 210f;
        cone = 360f;
        colorFrom = steelGray;
        colorTo = steelGray;
        interp = Interp.pow2In;
    }};

    public static final Effect radialCracks = new ParticleEffect(){{
        particles = 10;
        sizeFrom = 4f;
        sizeTo = 1f;
        lifetime = 230f;
        length = 0f;
        baseLength = 160f;
        cone = 360f;
        region = "索拉里斯2.0-六边形";
        colorFrom = dark;
        colorTo = dark;
        interp = Interp.pow2In;
    }};

    public static final Effect groundShockAftershock = new WaveEffect(){{
        colorFrom = steelGray;
        colorTo = rustRed;
        sizeFrom = 30f;
        sizeTo = 280f;
        lifetime = 95f;
        strokeFrom = 4f;
        strokeTo = 0f;
        interp = Interp.pow2Out;
    }};

    public static final Effect secondaryDebris = new ParticleEffect(){{
        particles = 32;
        sizeFrom = 3f;
        sizeTo = 0f;
        lifetime = 65f;
        length = 0f;
        baseLength = 95f;
        cone = 360f;
        colorFrom = rustRed;
        colorTo = dark;
        interp = Interp.circleOut;
    }};

    public static final Effect groundEmbers = new ParticleEffect(){{
        particles = 50;
        sizeFrom = 2f;
        sizeTo = 0f;
        lifetime = 140f;
        length = 12f;
        cone = 360f;
        baseLength = 195f;
        colorFrom = silverGray;
        colorTo = rustRed;
        interp = Interp.pow2In;
    }};

    public static final Effect burntGroundResidue = new MultiEffect(
        scorchSpots,
        scatteredDust,
        radialCracks,
        groundShockAftershock,
        secondaryDebris,
        groundEmbers
    );

    // ========== 分裂：金属破片分裂 ==========
    public static final Effect splitParticles = new ParticleEffect(){{
        particles = 14;
        sizeFrom = 5f;
        sizeTo = 0f;
        lifetime = 18f;
        length = 30f;
        cone = 360f;
        colorFrom = silverGray;
        colorTo = steelGray;
        interp = Interp.circleOut;
    }};

    public static final Effect splitWave = new WaveEffect(){{
        colorFrom = steelGray;
        colorTo = steelBlue;
        sizeFrom = 8f;
        sizeTo = 55f;
        lifetime = 16f;
        strokeFrom = 8f;
        strokeTo = 0f;
        interp = Interp.pow3Out;
    }};

    public static final Effect splitArcSparks = new ParticleEffect(){{
        particles = 18;
        sizeFrom = 4f;
        sizeTo = 0f;
        lifetime = 16f;
        length = 45f;
        cone = 360f;
        colorFrom = whiteHot;
        colorTo = steelBlue;
        interp = Interp.fastSlow;
    }};

    public static final Effect splitComposite = new MultiEffect(splitParticles, splitWave, splitArcSparks);

    // ========== 主特效 ==========
    public static final Effect havocOrbital1 = new ParticleEffect(){{
        particles = 90;
        sizeFrom = 15f;
        sizeTo = 0f;
        lifetime = 180f;
        length = 380f;
        strokeFrom = 40f;
        colorFrom = steelBlue;
        colorTo = silverGray;
        interp = Interp.circleOut;
    }};

    public static final Effect havocOrbital2 = new WaveEffect(){{
        colorFrom = steelGray;
        colorTo = primary;
        sizeFrom = 30f;
        sizeTo = 320f;
        lifetime = 40f;
        strokeFrom = 55f;
        strokeTo = 0f;
        interp = Interp.pow4Out;
    }};

    public static final Effect havocOrbital = new MultiEffect(havocOrbital1, havocOrbital2);

    public static final Effect newHitEffect = new MultiEffect(
        hitCoreFlash,
        hitEnergyPulse,
        hitArcParticles,
        hitShardSpray,
        new ParticleEffect(){{
            particles = 15;
            interp = Interp.fastSlow;
            sizeFrom = 12f;
            sizeTo = 0f;
            length = 60f;
            baseLength = 39f;
            lifetime = 32f;
            colorFrom = silverGray;
            colorTo = whiteHot;
            cone = 360f;
        }},
        new ParticleEffect(){{
            particles = 1;
            baseLength = 0f;
            length = 0f;
            lifetime = 13f;
            region = "索拉里斯2.0-六边形";
            sizeFrom = 100f;
            sizeTo = 0f;
            colorFrom = steelGray;
            colorTo = silverGray;
            cone = 360f;
        }},
        new WaveEffect(){{
            lifetime = 9f;
            sizeFrom = 5f;
            sizeTo = 95f;
            strokeFrom = 7f;
            strokeTo = 0f;
            colorFrom = steelBlue;
            colorTo = steelBlue;
            interp = Interp.pow4Out;
        }}
    );

    // ========== 破盾子弹（电离金属流） ==========
    public static final Effect laserBreachEffect = new ExplosionEffect(){{
        waveRad = 0f;
        waveRadBase = 0f;
        sparks = 0;
        smokes = 0;
    }};

    public static final BulletType havocShieldBreaker = new LaserBulletType(){{
        lifetime = 0f;
        width = 0f;
        length = 220f;
        largeHit = true;
        laserEffect = laserBreachEffect;
        colors = new Color[]{steelGray, steelBlue, dark};
        collidesTiles = false;
        damage = 0f;
        hittable = false;
        reflectable = false;
        absorbable = false;
    }
        @Override
        public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
        }

        @Override
        public void hitEntity(Bullet b, Hitboxc entity, float health){
            if(entity instanceof Unit unit && unit.shield > 0){
                unit.shield -= 150f;
                Fx.hitLaser.at(unit.x, unit.y, 0f, steelBlue);
                hitArcParticles.at(unit.x, unit.y);
            }
        }
    };

    // ========== 第三代分裂子弹（动能破片） ==========
    public static final BulletType havocFrag3 = new BasicBulletType(){{
        speed = 5f;
        lifetime = 25f;
        damage = 800f;
        splashDamage = 2000f;
        splashDamageRadius = 30f;
        collides = true;
        width = 12f;
        height = 12f;
        backColor = steelGray;
        frontColor = silverGray;
        trailLength = 12;
        trailWidth = 3f;
        trailColor = steelBlue;
        hitEffect = newHitEffect;
        despawnEffect = splitComposite;
        despawnSound = Sounds.explosionReactor;
        hitShake = 14f;
    }};

    // ========== 第二代分裂子弹（动能破片） ==========
    public static final BulletType havocFrag2 = new BasicBulletType(){{
        speed = 5f;
        lifetime = 35f;
        damage = 1800f;
        splashDamage = 900f;
        splashDamageRadius = 60f;
        collides = true;
        width = 22f;
        height = 22f;
        backColor = steelGray;
        frontColor = whiteHot;
        trailLength = 16;
        trailWidth = 4f;
        trailColor = steelBlue;
        hitEffect = newHitEffect;
        despawnSound = Sounds.explosionReactor;
        despawnEffect = splitComposite;
        hitShake = 14f;

        fragBullets = 2;
        fragBullet = havocFrag3;
        fragVelocityMin = 1f;
        fragVelocityMax = 1f;
        fragOffsetMin = 30f;
        fragOffsetMax = 60f;
    }
        @Override
        public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
            super.hitTile(b, build, x, y, initialHealth, direct);
            if(direct){
                createFrags(b, x, y);
            }
        }
    };

    // ========== 落点白闪与星芒 ==========
    public static final Effect impactWhiteFlash = new ExplosionEffect(){{
        lifetime = 26f;
        waveColor = whiteHot;
        waveLife = 26f;
        waveStroke = 16f;
        waveRad = IMPACT_RADIUS * 0.9f;
        waveRadBase = 12f;
        sparkColor = whiteHot;
        sparkStroke = 6f;
        sparkRad = IMPACT_RADIUS * 0.8f;
        sparkLen = 70f;
        smokeColor = primary;
        smokes = 12;
        smokeSize = 18f;
        smokeSizeBase = 2f;
        smokeRad = IMPACT_RADIUS * 0.5f;
    }};

    public static final Effect impactStarburst = new ParticleEffect(){{
        line = true;
        particles = 24;
        lifetime = 20f;
        length = IMPACT_RADIUS;
        cone = 360f;
        strokeFrom = 8f;
        strokeTo = 0f;
        lenFrom = IMPACT_RADIUS * 0.5f;
        lenTo = 0f;
        colorFrom = whiteHot;
        colorTo = primary;
        interp = Interp.pow3Out;
    }};

    // ========== 主轨道动能打击子弹（楔形光束射入 + 落点同心圆环扩散） ==========
    public static final BulletType havocOrbitalStrike = new HavocOrbitalStrikeType(){{
        speed = 0f;
        lifetime = CHARGE_FRAMES + HOLD_FRAMES;
        collides = false;
        hittable = false;
        reflectable = false;
        absorbable = false;
        width = 1f;
        height = 1f;
        despawnSound = Sounds.explosionReactor;
        despawnShake = 95f;
        despawnEffect = havocOrbital;
    }};

    public static class HavocOrbitalStrikeType extends BasicBulletType{

        @Override
        public void update(Bullet b){
            super.update(b);

            // 按扩张中的圆环范围持续造成伤害（仅蓄力阶段）
            if(b.time > 0 && b.time < CHARGE_FRAMES && b.time % 10 == 0){
                Damage.damage(b.team, b.x, b.y, IMPACT_RADIUS * Interp.pow2Out.apply(Mathf.clamp(b.time / CHARGE_FRAMES)), 3000f, true, true);
            }

            // 光束压到地面的瞬间结算打击（之后激光继续驻留）
            if(b.time >= CHARGE_FRAMES && b.data == null){
                b.data = true;
                impact(b);
            }

            // 光束斜向压向目标时，前端留下浩劫子弹同款红色拖尾
            if(b.time < CHARGE_FRAMES && b.time % 3 == 0){
                float dist = beamDistance(b);
                kineticTrail.at(b.x + Mathf.cosDeg(BEAM_ANGLE) * dist, b.y + Mathf.sinDeg(BEAM_ANGLE) * dist, BEAM_ANGLE);
            }
        }

        public float beamDistance(Bullet b){
            return BEAM_LENGTH * (1 - Interp.pow2In.apply(Mathf.clamp(b.time / CHARGE_FRAMES)));
        }

        @Override
        public void draw(Bullet b){
            float progress = Mathf.clamp(b.time / CHARGE_FRAMES);
            float dist = beamDistance(b);
            float nearX = b.x + Mathf.cosDeg(BEAM_ANGLE) * dist;
            float nearY = b.y + Mathf.sinDeg(BEAM_ANGLE) * dist;
            float farX = nearX + Mathf.cosDeg(BEAM_ANGLE) * BEAM_LENGTH;
            float farY = nearY + Mathf.sinDeg(BEAM_ANGLE) * BEAM_LENGTH;
            float perpX = Mathf.cosDeg(BEAM_ANGLE + 90), perpY = Mathf.sinDeg(BEAM_ANGLE + 90);
            float shrink = b.time > CHARGE_FRAMES ? 1 - 0.15f * Interp.smooth.apply(Mathf.clamp((b.time - CHARGE_FRAMES) / HOLD_FRAMES)) : 1f;
            float wide = IMPACT_RADIUS * (0.25f + 1.0f * progress) * shrink;
            float fade = b.time > CHARGE_FRAMES ? 1 - Mathf.clamp((b.time - CHARGE_FRAMES) / HOLD_FRAMES) * 0.5f : 1f;

            Draw.z(Layer.effect);
            Draw.blend(Blending.additive);

            // 落点处多重同心圆环，由内向外扩散
            for(int i = 0; i < 4; i++){
                float ringIn = Mathf.clamp((progress - i * 0.16f) / 0.5f);
                if(ringIn > 0){
                    drawCircleRing(b.x, b.y, IMPACT_RADIUS * ringIn * (1 - i * 0.1f), i % 2 == 0 ? primary : whiteHot, 7 - i);
                }
            }

            // 光束放在最高渲染层
            Draw.z(Layer.overlayUI);

            float farLeftX = farX - perpX * wide, farLeftY = farY - perpY * wide;
            float farRightX = farX + perpX * wide, farRightY = farY + perpY * wide;

            // 两条红色拖尾贴着楔形边缘，从尖端拖向远端
            drawEdgeTrail(nearX + perpX * 5, nearY + perpY * 5, farLeftX + perpX * 9, farLeftY + perpY * 9, wide * 0.1f + 2, progress);
            drawEdgeTrail(nearX - perpX * 5, nearY - perpY * 5, farRightX - perpX * 9, farRightY - perpY * 9, wide * 0.1f + 2, progress);

            // 外圈红色楔形：尖端在落点，向远端张开
            Draw.color(primary, (0.42f + 0.2f * progress) * fade);
            Fill.tri(nearX, nearY, farRightX, farRightY, farLeftX, farLeftY);

            // 内芯白热楔形
            Draw.color(whiteHot, 0.85f * fade);
            Fill.tri(nearX, nearY,
                farX + perpX * (wide * 0.5f), farY + perpY * (wide * 0.5f),
                farX - perpX * (wide * 0.5f), farY - perpY * (wide * 0.5f));

            // 伪3D 环绕环：沿光束方向流动的椭圆环
            for(int k = 0; k < 4; k++){
                float ringT = (b.time / 9f + k * 0.25f) % 1;
                float ringX = nearX + Mathf.cosDeg(BEAM_ANGLE) * BEAM_LENGTH * ringT * 0.75f;
                float ringY = nearY + Mathf.sinDeg(BEAM_ANGLE) * BEAM_LENGTH * ringT * 0.75f;
                drawOrbitRing(ringX, ringY, wide * (0.55f + 0.6f * ringT), BEAM_ANGLE,
                    k % 2 == 0 ? whiteHot : primary, 4 - k * 0.6f, 0.7f - 0.5f * ringT);
            }

            Draw.blend();
            Draw.reset();
        }

        public void drawCircleRing(float cx, float cy, float radius, Color color, float stroke){
            Lines.stroke(stroke, color);
            Lines.circle(cx, cy, radius);
        }

        public void drawOrbitRing(float cx, float cy, float radius, float angle, Color color, float stroke, float alpha){
            float alongX = Mathf.cosDeg(angle), alongY = Mathf.sinDeg(angle);
            float acrossX = Mathf.cosDeg(angle + 90), acrossY = Mathf.sinDeg(angle + 90);
            int segments = 36;

            // 分前后半圈绘制：后半圈更暗、前半圈更亮，形成伪3D立体感
            for(int k = 0; k < 2; k++){
                Tmp.c1.set(color);
                Tmp.c1.a = k == 0 ? alpha * 0.35f : alpha;
                Lines.stroke(stroke, Tmp.c1);
                Lines.beginLine();
                for(int i = 0; i <= segments / 2; i++){
                    float a = k * Mathf.PI + i / (segments / 2f) * Mathf.PI;
                    Lines.linePoint(cx + acrossX * Mathf.cos(a) * radius + alongX * Mathf.sin(a) * radius * 0.35f,
                        cy + acrossY * Mathf.cos(a) * radius + alongY * Mathf.sin(a) * radius * 0.35f);
                }
                Lines.endLine();
            }
        }

        public void drawEdgeTrail(float startX, float startY, float endX, float endY, float width, float strength){
            int steps = 8;
            float stepX = (endX - startX) / steps, stepY = (endY - startY) / steps;
            for(int i = 0; i < steps; i++){
                float fade = 1 - i / (float)steps;
                Tmp.c1.set(primary);
                Tmp.c1.a = 0.55f * strength * fade;
                Lines.stroke(width * (0.4f + 0.6f * fade), Tmp.c1);
                Lines.line(startX + stepX * i, startY + stepY * i, startX + stepX * (i + 1), startY + stepY * (i + 1));
            }
        }

        public void impact(Bullet b){
            // 击中瞬间的白闪与放射星芒
            impactWhiteFlash.at(b.x, b.y);
            impactStarburst.at(b.x, b.y);

            orbitalImpactEffect.at(b.x, b.y);
            incomingSpray.at(b.x, b.y, 240f);
            orbitalImpactEffect.at(b.x, b.y, 240f);
            burntGroundResidue.at(b.x, b.y);

            // 落地多重圆环与震动
            impactPrimeWave.at(b.x, b.y);
            havocEffect3.at(b.x, b.y);
            Effect.shake(10f, 20f, b.x, b.y);

            for(int i = 0; i < 64; i++){
                havocShieldBreaker.create(b, b.x, b.y, 5.625f * i, 1f, 1f);
            }

            Damage.damage(b.team, b.x, b.y, IMPACT_RADIUS, 30000f, true, true);
            Damage.status(b.team, b.x, b.y, IMPACT_RADIUS, armorShatter, 900f, true, true);
        }

        @Override
        public void despawned(Bullet b){
            super.despawned(b);

            // 分裂子弹延迟到激光完全消失之后再出现
            float impactX = b.x;
            float impactY = b.y;
            Time.run(FRAG_DELAY, () -> {
                for(int j = 0; j < 15; j++){
                    havocFrag2.create(b, impactX, impactY, Mathf.random(360f), 1f);
                }
            });
        }
    }

    // ========== 浩劫强化攻击（标记器发射的轨道打击） ==========
    public static class HavocBoostAttack1Type extends BasicBulletType{

        public HavocBoostAttack1Type(){
            lifetime = 0f;
            despawnEffect = Fx.none;
            hitEffect = Fx.none;
            hittable = false;
            reflectable = false;
            absorbable = false;
            collides = false;
        }

        @Override
        public void despawned(Bullet b){
            // 直接在目标点生成打击：先光柱圆环蓄力，再圆球坠落
            havocOrbitalStrike.create(b, b.x, b.y, 0f, 1f, 1f);
        }
    }

    public static final BulletType havocBoostAttack1 = new HavocBoostAttack1Type();

    public static final BulletType havocBoostAttack = new PointBulletType(){{
        speed = 100f;
        trailSpacing = 4f;
        ammoMultiplier = 1f;
        trailEffect = havocEffect2;
        despawnEffect = havocEffect13;
        hitEffect = havocEffect13;
        fragBullets = 1;
        fragRandomSpread = 0f;
        fragSpread = 0f;
        fragAngle = 0f;
        fragVelocityMin = 1f;
        fragBullet = havocBoostAttack1;
    }};

    // ============================================================
    // 对外接口
    // ============================================================
    static{
        // 每次载入地图时清空轨道打击状态
        Events.on(EventType.WorldLoadBeginEvent.class, e -> {
            orbiters = 0;
            shells = 0;
            bound = 0;
            markers = 0;
        });
    }

    public static void launchOrbiter(){
        orbiters += 1;
    }

    public static void launchShell(){
        shells += 2;
    }

    public static void consumeShell(){
        if(shells > 0) shells -= 1;
    }
}
