package solaris2.content;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.weather.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import solaris2.power.WeatherShield;

import static mindustry.Vars.*;

/**
 * 天气内容：原 assets/content/weathers 下 json 与 特殊/*.js 的 Java 实现。
 */
public class SolarisWeathers{

    public static ParticleWeather firestorm, empPulse, radiationFog, thunderstorm;

    // 雷暴可调数值（与 特殊/雷暴.js 一致）
    private static final float BOLT_DAMAGE = 150f;
    private static final float BOLT_RADIUS = 48f;
    private static final float HUNT_RADIUS = 300f;
    private static final float PULL = 0.55f;
    private static final float CLOUD_MIN = 300f;
    private static final float CLOUD_SPAN = 120f;
    private static final int BRANCH_NUM = 6;

    private static final Color BOLT_COLOR = Color.valueOf("9fd8ff");
    private static Sound thunderSound;

    private static final Effect mainBolt = new Effect(24f, 500f, e -> {
        if(!(e.data instanceof Position p)) return;
        float dist = Mathf.dst(e.x, e.y, p.getX(), p.getY());
        int seg = Math.max(5, Mathf.ceil(dist / 9f));
        drawJagged(e, p, seg, 14f, 9f * e.fout(), Color.valueOf("a8dcff"), 0.55f);
        drawJagged(e, p, seg, 9f, 5f * e.fout(), BOLT_COLOR, 1f);
        drawJagged(e, p, seg, 5f, 2f * e.fout(), Color.white, e.fout());
    });

    private static final Effect strikeFlash = new Effect(20f, 500f, e -> {
        Draw.color(Color.white, e.color, e.fin());
        Draw.alpha(e.fout());
        Fill.circle(e.x, e.y, 1f + 2.5f * e.fin());
        Lines.stroke(1.3f * e.fout());
        Draw.alpha(e.fout() * 0.9f);
        Lines.circle(e.x, e.y, 2f + 6f * e.fin());
    });

    private static final Effect groundRoot = new Effect(16f, 500f, e -> {
        if(!(e.data instanceof float[] data) || data.length < 2) return;
        float base = data[0], height = data[1];
        float ox = Mathf.cosDeg(e.rotation) * height, oy = Mathf.sinDeg(e.rotation) * height;
        float px = -oy, py = ox;
        Draw.color(e.color);
        Draw.alpha(e.fout());
        Fill.tri(e.x, e.y,
                e.x + ox + px * base / 2f, e.y + oy + py * base / 2f,
                e.x + ox - px * base / 2f, e.y + oy - py * base / 2f);
    });

    static{
        mainBolt.followParent = false;
        strikeFlash.followParent = false;
        groundRoot.followParent = false;

        try{
            thunderSound = tree.loadSound("雷声");
        }catch(Throwable e){
            thunderSound = null;
        }

        firestorm = shielded("火焰风暴");
        firestorm.color = color("FFBB44");
        firestorm.noiseColor = color("5E1A00");
        firestorm.particleRegion = "particle";
        firestorm.sizeMax = 10f;
        firestorm.sizeMin = 1f;
        firestorm.density = 10000f;
        firestorm.useWindVector = true;
        firestorm.drawNoise = true;
        firestorm.baseSpeed = 6.1f;
        firestorm.force = 0.5f;
        firestorm.opacityMultiplier = 0.95f;
        firestorm.soundVol = 0.65f;
        firestorm.status = StatusEffects.burning;
        firestorm.attrs.set(Attribute.heat, 2f);
        firestorm.attrs.set(Attribute.water, -0.9f);
        firestorm.attrs.set(Attribute.spores, -0.9f);

        empPulse = shielded("磁爆脉冲");
        empPulse.noiseColor = color("FFFFFF");
        empPulse.particleRegion = "particle";
        empPulse.sizeMax = 0f;
        empPulse.sizeMin = 0f;
        empPulse.density = 1200f;
        empPulse.useWindVector = true;
        empPulse.drawNoise = true;
        empPulse.soundVol = 0f;
        empPulse.soundVolOscMag = 1.5f;
        empPulse.soundVolOscScl = 1100f;
        empPulse.soundVolMin = 0.02f;
        empPulse.status = SolarisStatuses.emp1;

        radiationFog = shielded("放射性雾霾");
        radiationFog.color = color("70D070");
        radiationFog.noiseColor = color("70D070");
        radiationFog.noiseLayers = 3;
        radiationFog.noiseLayerAlphaM = 0.9f;
        radiationFog.noiseLayerSpeedM = 2.5f;
        radiationFog.noiseLayerSclM = 0.7f;
        radiationFog.baseSpeed = 0.08f;
        radiationFog.noiseScale = 1300f;
        radiationFog.noisePath = "fog";
        radiationFog.drawParticles = false;
        radiationFog.drawNoise = true;
        radiationFog.useWindVector = false;
        radiationFog.xspeed = 1f;
        radiationFog.yspeed = 0.01f;
        radiationFog.status = SolarisStatuses.radiationDamage;
        radiationFog.statusDuration = 180f;
        radiationFog.opacityMultiplier = 0.43f;
        radiationFog.attrs.set(Attribute.light, -0.3f);
        radiationFog.attrs.set(Attribute.water, -0.1f);
        radiationFog.attrs.set(Attribute.heat, 0.1f);

        thunderstorm = new StormWeather("雷暴");
        thunderstorm.description = "大范围的闪电雷暴天气，落雷会劈中地图上任意一方的单位/建筑并造成范围伤害。";
        thunderstorm.color = color("7FB5E8");
        thunderstorm.noiseColor = color("565666");
        thunderstorm.useWindVector = true;
        thunderstorm.baseSpeed = 12f;
        thunderstorm.sizeMin = 10f;
        thunderstorm.sizeMax = 40f;
        thunderstorm.density = 1600f;
        thunderstorm.xspeed = 8f;
        thunderstorm.yspeed = 16f;
        thunderstorm.padding = 16f;
        thunderstorm.force = 1f;
        thunderstorm.drawNoise = true;
        thunderstorm.opacityMultiplier = 0.95f;
    }

    /** 由主类或 JS 调用，触发静态初始化。 */
    public static void load(){
    }

    private static ParticleWeather shielded(String name){
        return new ParticleWeather(name){
            @Override
            public void updateEffect(WeatherState state){
                super.updateEffect(state);
                if(status != null && status != StatusEffects.none){
                    removeStatusFromShields(status);
                }
            }
        };
    }

    /** 通电的气象屏蔽器清除范围内单位的指定状态。 */
    public static void removeStatusFromShields(StatusEffect status){
        for(Building build : Groups.build){
            if(!(build.block instanceof WeatherShield)) continue;
            if(build.power == null || build.power.status <= 0f) continue;
            float bx = build.x, by = build.y;
            Units.nearby(build.team, bx, by, WeatherShield.shieldRadius, unit -> {
                if(unit != null && !unit.dead) unit.unapply(status);
            });
        }
    }

    private static Color color(String hex){
        return Color.valueOf(hex.length() == 6 ? hex + "FF" : hex);
    }

    private static void drawJagged(Effect.EffectContainer e, Position p, int seg, float amp, float width, Color color, float alpha){
        Draw.color(color);
        Draw.alpha(alpha);
        Lines.stroke(width);
        float sx = p.getX(), sy = p.getY(), tx = e.x, ty = e.y;
        float dist = Mathf.dst(sx, sy, tx, ty);
        float nx = (tx - sx) / dist, ny = (ty - sy) / dist;
        Lines.beginLine();
        Lines.linePoint(sx, sy);
        for(int i = 1; i < seg; i++){
            float len = dist * i / seg;
            float jit = (Mathf.randomSeed(e.id * 131L + i * 17L) - 0.5f) * 2f * amp;
            Lines.linePoint(sx + nx * len - ny * jit, sy + ny * len + nx * jit);
        }
        Lines.linePoint(tx, ty);
        Lines.endLine();
    }

    /** 雷暴：雨幕 + 随机落雷，落点附近优先命中单位/建筑。 */
    public static class StormWeather extends ParticleWeather{
        private final TextureRegion[] splashes = new TextureRegion[12];

        public StormWeather(String name){
            super(name);
        }

        @Override
        public void load(){
            super.load();
            for(int i = 0; i < splashes.length; i++){
                splashes[i] = Core.atlas.find("splash-" + i);
            }
        }

        @Override
        public void drawOver(WeatherState state){
            drawRain(sizeMin, sizeMax, xspeed, yspeed, density * 1.5f, state.intensity, 0.85f, color);
            if(!Vars.state.isPlaying() || !Mathf.chanceDelta(0.04f * state.intensity / 2f)) return;

            float wx = Mathf.random(world.width()) * tilesize;
            float wy = Mathf.random(world.height()) * tilesize;
            Victim v = findVictim(wx, wy);
            float tx = v.found ? Mathf.lerp(wx, v.x, PULL) : wx;
            float ty = v.found ? Mathf.lerp(wy, v.y, PULL) : wy;

            spawnBolt(tx, ty);
            if(v.found){
                boltDamage(tx, ty, v.team, state.intensity);
            }
        }

        @Override
        public void drawUnder(WeatherState state){
            drawSplashes(splashes, sizeMax, density, state.intensity, state.opacity, 22f, 0.75f, color, Liquids.water);
        }
    }

    private static class Victim{
        boolean found;
        float x, y;
        Team team;
    }

    private static Victim findVictim(float wx, float wy){
        Victim v = new Victim();
        float best = HUNT_RADIUS * HUNT_RADIUS;

        for(Building b : Groups.build){
            if(b == null) continue;
            float d = Mathf.dst2(b.x, b.y, wx, wy);
            if(d < best){
                best = d;
                v.x = b.x;
                v.y = b.y;
                v.team = b.team;
                v.found = true;
            }
        }

        for(Unit u : Groups.unit){
            if(u == null || u.dead) continue;
            float d = Mathf.dst2(u.x, u.y, wx, wy);
            if(d < best){
                best = d;
                v.x = u.x;
                v.y = u.y;
                v.team = u.team;
                v.found = true;
            }
        }
        return v;
    }

    private static void spawnBolt(float x, float y){
        float cloudY = y + Mathf.random(CLOUD_MIN, CLOUD_MIN + CLOUD_SPAN);
        Vec2 cloud = new Vec2(x + Mathf.range(6f), cloudY + Mathf.range(10f));
        mainBolt.at(x, y, 9f, BOLT_COLOR, cloud);

        for(int i = 0; i < BRANCH_NUM; i++){
            Lightning.create(Team.derelict, Pal.lancerLaser, 0f, x, y, Mathf.random(360f), (int)Mathf.random(2f, 4f));
        }

        strikeFlash.at(x, y, Mathf.random(360f), BOLT_COLOR);

        int n = Mathf.random(2, 4);
        for(int i = 0; i < n; i++){
            groundRoot.at(x, y, i * (360f / n) + Mathf.random(45f), Pal.lancerLaser,
                    new float[]{Mathf.random(6f, 9f), Mathf.random(14f, 22f)});
        }

        if(thunderSound != null) thunderSound.at(x, y, 1f, 6f);
        Effect.shake(4f, 90f, x, y);
    }

    private static boolean weatherCovered(Team team, float x, float y){
        for(Building b : Groups.build){
            if(b == null || b.team != team || !(b.block instanceof WeatherShield)) continue;
            if(b.power == null || b.power.status <= 0f) continue;
            float dx = b.x - x, dy = b.y - y;
            if(dx * dx + dy * dy <= WeatherShield.shieldRadius * WeatherShield.shieldRadius) return true;
        }
        return false;
    }

    private static void boltDamage(float x, float y, Team team, float intensity){
        if(team == null || weatherCovered(team, x, y)) return;

        for(Building b : Groups.build){
            if(b == null || b.team != team) continue;
            float rad = BOLT_RADIUS + b.hitSize() / 2f;
            float d = Mathf.dst(b.x, b.y, x, y);
            if(d <= rad){
                b.damage(BOLT_DAMAGE * intensity * (1f - d / rad));
            }
        }

        for(Unit u : Groups.unit){
            if(u == null || u.team != team || u.dead) continue;
            float ur = BOLT_RADIUS + u.hitSize / 2f;
            float du = Mathf.dst(u.x, u.y, x, y);
            if(du <= ur){
                u.damage(BOLT_DAMAGE * intensity * 1.5f * (1f - du / ur));
            }
        }
    }
}
