package solaris2.industry;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 由 scripts/建筑/时间迟滞器.js 移植而来。
 */
public class TimeDilator extends Block{

    public static final int cooldownWaves = 4;
    public static final Color black = Color.valueOf("08080d");
    public static final Color blackLightning = Color.valueOf("101018");
    public static final Color offsetRed = Color.valueOf("ff3b3b");
    public static final Color offsetGreen = Color.valueOf("3bff6b");
    public static final Color offsetBlue = Color.valueOf("3b7bff");

    public static int lastUseWave = -cooldownWaves;

    public static boolean clockActive = false;
    public static float clockTime = 0f;
    public static float clockX = 0f, clockY = 0f;
    public static long clockSeed = 0;

    public static final Seq<Warmup> warmupList = new Seq<>();
    public static final Seq<Fog> fogList = new Seq<>();

    public Sound freezeSound;

    public TimeDilator(String name){
        super(name);

        configurable = true;
        update = true;
        solid = true;
        destructible = true;

        buildType = TimeDilatorBuild::new;

        registerEvents();
    }

    @Override
    public void load(){
        super.load();
        freezeSound = tree.loadSound("时停");
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return state.teams.get(team).getCount(this) < 1;
    }

    private void registerEvents(){
        Events.on(WaveEvent.class, e -> {
            Groups.build.each(b -> {
                if(b != null && b.block instanceof TimeDilator){
                    ((TimeDilatorBuild)b).releaseDelayed();
                }
            });
        });

        Events.run(Trigger.update, () -> {
            for(int i = fogList.size - 1; i >= 0; i--){
                Fog fog = fogList.get(i);
                fog.timer += Time.delta / 60f;
                if(fog.timer >= 2f){
                    fogList.remove(i);
                }
            }

            if(!clockActive) return;
            clockTime += Time.delta / 60f;
            if(clockTime >= 2f){
                clockActive = false;
                clockTime = 0f;
            }
        });

        Events.run(Trigger.draw, () -> {
            if(warmupList.size > 0){
                Draw.draw(Layer.endPixeled, () -> {
                    for(int i = warmupList.size - 1; i >= 0; i--){
                        Warmup item = warmupList.get(i);
                        if(item.building == null || !item.building.isValid()){
                            warmupList.remove(i);
                            continue;
                        }
                        drawWarmup(item);
                    }
                });
            }

            if(fogList.size > 0){
                Draw.draw(Layer.shields + 2, TimeDilator::drawFog);
            }

            if(!clockActive) return;

            Draw.draw(Layer.endPixeled - 1, TimeDilator::drawRgbOffset);

            Draw.draw(Layer.endPixeled, () -> {
                drawClock();
                drawShockwave(clockX, clockY);
            });
        });

        Events.on(WorldLoadEvent.class, e -> {
            for(int i = 0; i < warmupList.size; i++){
                Warmup item = warmupList.get(i);
                if(item.building != null) item.building.stopWarmupSound();
            }
            clockActive = false;
            clockTime = 0f;
            lastUseWave = -cooldownWaves;
            fogList.clear();
            warmupList.clear();
        });
    }

    public static class Warmup{
        public final TimeDilatorBuild building;
        public float timer = 0f;

        public Warmup(TimeDilatorBuild building){
            this.building = building;
        }
    }

    public static class Fog{
        public float x, y, phase, timer = 0f;

        public Fog(float x, float y){
            this.x = x;
            this.y = y;
            this.phase = x * 0.05f + y * 0.09f;
        }
    }

    public static class DelayUnit{
        public final UnitType type;
        public final float health;

        public DelayUnit(UnitType type, float health){
            this.type = type;
            this.health = health;
        }
    }

    public class TimeDilatorBuild extends Building{

        public final Seq<DelayUnit> delayedUnits = new Seq<>();
        public boolean warming = false;
        public Warmup warmupItem = null;
        public float warmLightningTimer = 0f;
        public int warmSoundId = -1;

        public int remainingCooldown(){
            return Math.max(0, cooldownWaves - (state.wave - lastUseWave));
        }

        public void stopWarmupSound(){
            if(warmSoundId != -1){
                Core.audio.stop(warmSoundId);
                warmSoundId = -1;
            }
        }

        public void startWarmupSound(){
            warmSoundId = Sounds.chargeVela.loop(Sounds.chargeVela.calcVolume(x, y) * 0.8f, 1f, Sounds.chargeVela.calcPan(x, y));
        }

        @Override
        public void remove(){
            super.remove();
            stopWarmupSound();
        }

        @Override
        public void updateTile(){
            if(!warming) return;
            Warmup item = warmupItem;
            if(item == null) return;

            float deltaTime = Time.delta / 60f;
            item.timer += deltaTime;
            warmLightningTimer += deltaTime;
            if(warmLightningTimer >= 0.8f){
                warmLightningTimer = 0f;
                strikeBlackLightning();
            }

            if(warmSoundId == -1){
                startWarmupSound();
            }else{
                Core.audio.set(warmSoundId, Sounds.chargeVela.calcPan(x, y), Sounds.chargeVela.calcVolume(x, y) * 0.8f);
            }

            if(item.timer >= 6f){
                warming = false;
                warmupItem = null;
                warmupList.remove(item);
                stopWarmupSound();
                doDilate();
            }
        }

        public void strikeBlackLightning(){
            int count = Mathf.random(1, 2);
            for(int i = 0; i < count; i++){
                float dir = Mathf.random(360f);
                float dist = Mathf.random(60f, 120f);
                float ox = x + Angles.trnsx(dir, 4f);
                float oy = y + Angles.trnsy(dir, 4f);
                Lightning.create(Team.derelict, blackLightning, 0f, ox, oy, dir, Math.round(dist / 7.5f));
            }
        }

        public void activate(){
            if(warming) return;

            int remaining = remainingCooldown();
            if(remaining > 0){
                Call.label("[red]冷却中！还需 " + remaining + " 波", 2f, x, y);
                return;
            }

            final int[] enemyCount = {0};
            Groups.unit.each(u -> {
                if(u != null && u.isValid() && u.team != team){
                    enemyCount[0]++;
                }
            });

            if(enemyCount[0] == 0){
                Call.label("[yellow]当前没有敌人", 2f, x, y);
                return;
            }

            warming = true;
            warmLightningTimer = 0f;
            warmupItem = new Warmup(this);
            warmupList.add(warmupItem);
            strikeBlackLightning();
            stopWarmupSound();
            startWarmupSound();
        }

        public void doDilate(){
            Seq<Unit> enemies = new Seq<>();
            Groups.unit.each(u -> {
                if(u != null && u.isValid() && u.team != team){
                    enemies.add(u);
                }
            });

            if(enemies.size == 0){
                Call.label("[yellow]预热结束时没有敌人", 2f, x, y);
                return;
            }

            delayedUnits.clear();
            for(int i = 0; i < enemies.size; i++){
                Unit enemy = enemies.get(i);
                fogList.add(new Fog(enemy.x, enemy.y));
                delayedUnits.add(new DelayUnit(enemy.type, enemy.health));
                enemy.remove();
            }

            lastUseWave = state.wave;
            clockActive = true;
            clockTime = 0f;
            clockX = x;
            clockY = y;
            clockSeed = Mathf.random(100000);

            Call.label("[green]已迟滞 " + enemies.size + " 个敌人", 3f, x, y);
            if(freezeSound != null) freezeSound.at(x, y, 1f, 1.5f);
        }

        public void releaseDelayed(){
            if(delayedUnits.isEmpty()) return;

            Seq<DelayUnit> list = new Seq<>(delayedUnits);
            delayedUnits.clear();

            Seq<Tile> spawns = spawner.getSpawns();
            Team waveTeam = state.rules.waveTeam;
            float cx = world.width() * tilesize * 0.5f;
            float cy = world.height() * tilesize * 0.5f;

            for(int i = 0; i < list.size; i++){
                DelayUnit data = list.get(i);
                if(data.type == null) continue;

                float sx, sy;
                if(spawns.size > 0){
                    Tile t = spawns.get(Mathf.random(0, spawns.size - 1));
                    sx = t.worldx() + Mathf.range(tilesize * 2f);
                    sy = t.worldy() + Mathf.range(tilesize * 2f);
                }else{
                    sx = cx;
                    sy = cy;
                }

                Unit u = data.type.spawn(waveTeam, sx, sy, Angles.angle(sx, sy, cx, cy));
                u.health = Math.min(data.health, u.maxHealth);
            }
        }

        @Override
        public void buildConfiguration(Table table){
            Table t = table.table().left().get();
            t.label(() -> "[#8fd0ff]已迟滞：" + delayedUnits.size + " 个敌人").left();
            t.row();
            t.button("启动迟滞", this::activate).size(220f, 46f).padTop(6).update(b -> {
                if(warming){
                    b.setDisabled(true);
                    return;
                }
                int cd = remainingCooldown();
                b.setDisabled(cd > 0);
                b.setText(cd > 0 ? "冷却中（还剩 " + cd + " 波）" : "启动迟滞");
            });
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.i(delayedUnits.size);
            for(int i = 0; i < delayedUnits.size; i++){
                write.i(delayedUnits.get(i).type.id);
                write.f(delayedUnits.get(i).health);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            int n = read.i();
            delayedUnits.clear();
            for(int i = 0; i < n; i++){
                int id = read.i();
                float health = read.f();
                Content type = content.getByID(ContentType.unit, id);
                if(type instanceof UnitType unitType){
                    delayedUnits.add(new DelayUnit(unitType, health));
                }
            }
        }
    }

    public static void drawWarmup(Warmup item){
        TimeDilatorBuild b = item.building;
        float progress = Math.min(1f, item.timer / 6f);
        float fadeIn = Math.min(1f, progress * 4f);
        float fadeOut = progress > 0.92f ? (1f - progress) / 0.08f : 1f;
        float a = fadeIn * fadeOut;
        if(!(a > 0f)) return;

        float cx = b.x, cy = b.y;

        Draw.blend(Blending.normal);

        drawShrinkWave(cx, cy, item.timer, a);

        float r0 = 4f * (1f + Mathf.absin(20f, 0.08f));
        Draw.color(black, a * 0.95f);
        Fill.circle(cx, cy, r0);
        Draw.color(Pal.darkMetal, a * 0.9f);
        Fill.circle(cx, cy, r0 / 2f);

        Lines.stroke((0.7f + Mathf.absin(20f, 0.7f)) * Math.max(1f, r0 / 5f), black);
        for(int i = 0; i < 5; i++){
            float rot = i * 360f / 5f - Time.time * 0.5f;
            Lines.arc(cx, cy, r0 + 3f, 0.14f, rot);
        }

        Lines.stroke((0.7f + Mathf.absin(20f, 0.7f)) * a, black);
        for(int i = 0; i < 5; i++){
            float rot = i * 360f / 5f + Time.time * 0.5f;
            Lines.arc(cx, cy, 180f, 0.14f, rot);
        }

        Draw.reset();
    }

    private static void drawShrinkWave(float cx, float cy, float timer, float a){
        float frame = (timer % 1.5f) * 60f;

        drawWave(cx, cy, frame, 80f, 0f, Interp.linear, a);
        drawWave(cx, cy, frame, 60f, 10f, Interp.circleIn, a);

        float particleProgress = frame / 80f;
        if(particleProgress < 0f || particleProgress > 1f) return;
        float lineLen = Interp.pow5Out.apply(0f, 10f, particleProgress);
        float lineWidth = Interp.pow5Out.apply(0f, 1.2f, particleProgress);
        if(lineLen <= 0.1f || lineWidth <= 0.05f) return;

        float posFrac = Interp.pow5In.apply(particleProgress);
        Lines.stroke(lineWidth, black);
        for(int i = 0; i < 13; i++){
            float ang = Mathf.randomSeed(i * 131 + 17, 0, 360);
            float dist = 30f * (1f - posFrac) * Mathf.randomSeed(i * 977 + 41, 0.35f, 1f);
            Lines.lineAngle(cx + Angles.trnsx(ang, dist), cy + Angles.trnsy(ang, dist), ang, lineLen);
        }
    }

    private static void drawWave(float cx, float cy, float frame, float lifetime, float delay, Interp interp, float a){
        float f = (frame - delay) / lifetime;
        if(f < 0f || f > 1f) return;
        float width = interp.apply(0f, 4f, f);
        if(width <= 0.05f) return;
        Draw.color(black, a);
        Lines.stroke(width);
        Lines.circle(cx, cy, interp.apply(50f, 0f, f));
    }

    public static void drawFog(){
        Draw.color(black, 0.85f);

        for(int i = 0; i < fogList.size; i++){
            Fog fog = fogList.get(i);
            float f = fog.timer / 2f;
            float seed = fog.phase * 1000f + i * 137f;

            Draw.alpha(0.85f * (1f - f));
            for(int j = 0; j < 7; j++){
                float blockAngle = Mathf.randomSeed((long)(seed + j * 131f), 0, 360) + f * Mathf.randomSeed((long)(seed + j * 271f), -90, 90);
                float blockDist = Mathf.randomSeed((long)(seed + j * 419f), 2f, 15f);
                float blockRadius = Mathf.randomSeed((long)(seed + j * 613f), 5f, 12f);
                float blockSpread = Mathf.randomSeed((long)(seed + j * 857f), 1.2f, 3.4f);
                float radius = blockDist + f * 22f * blockSpread;
                Fill.circle(fog.x + Angles.trnsx(blockAngle, radius), fog.y + Angles.trnsy(blockAngle, radius), blockRadius * (1f - 0.6f * f) + 2f);
            }
            Fill.circle(fog.x, fog.y, (1f - f) * 18f + 6f);
        }

        Draw.reset();
    }

    public static void drawRgbOffset(){
        float p = Math.min(1f, clockTime / 2f);
        float strength = p < 0.65f ? p / 0.65f : (1f - p) / 0.35f;
        if(strength <= 0f) return;
        float a = strength * 0.45f;

        Camera cam = Core.camera;
        float w = cam.width, h = cam.height;
        float cx = cam.position.x, cy = cam.position.y;
        float x0 = cx - w * 0.5f, y0 = cy - h * 0.5f;

        Draw.blend(Blending.additive);

        float spacing = 46f;
        float phase = (p * 1872f) % spacing;
        float diagonal = (float)Math.sqrt(w * w + h * h) + spacing * 2f;
        int count = Mathf.ceil((w + h) / spacing) + 4;

        float offset = 7f * strength;

        Lines.stroke(spacing * 0.30f);
        for(int i = -2; i <= count; i++){
            float ox = -h + i * spacing + phase;
            float x1 = x0 + ox, y1 = y0;
            float x2 = x0 + ox + diagonal * 0.7071f, y2 = y0 + diagonal * 0.7071f;
            float shim = 0.5f + 0.5f * Mathf.sin(i * 0.8f + p * 86f);

            Draw.color(offsetRed, a * shim);
            Lines.line(x1 - offset, y1 - offset * 0.7f, x2 - offset, y2 - offset * 0.7f);
            Draw.color(offsetGreen, a * shim);
            Lines.line(x1, y1, x2, y2);
            Draw.color(offsetBlue, a * shim);
            Lines.line(x1 + offset, y1 + offset * 0.7f, x2 + offset, y2 + offset * 0.7f);
        }

        Draw.blend();
        Draw.reset();
    }

    public static void drawClock(){
        float p = Math.min(1f, clockTime / 2f);
        if(p <= 0f) return;

        float scale = Interp.pow3Out.apply(Math.min(1f, p / 0.5f));
        float alpha = p < 0.78f ? 1f : (1f - (p - 0.78f) / 0.22f);
        float r = 34f * scale;
        if(r <= 0.5f || alpha <= 0f) return;

        float cx = clockX, cy = clockY;

        for(int i = 4; i >= 0; i--){
            boolean latest = i == 0;
            float offsetAmt = i * 6f;
            float dir = Mathf.randomSeed((long)(clockSeed + i * 977), 0, 360);
            float ox = Angles.trnsx(dir, offsetAmt);
            float oy = Angles.trnsy(dir, offsetAmt);
            float a = latest ? alpha : alpha * 0.5f;
            float rr = r * (1f + 0.02f * i);
            drawDial(cx + ox, cy + oy, rr, a, p, i * 40f);
        }

        Draw.reset();
    }

    public static void drawShockwave(float cx, float cy){
        float f = clockTime / 0.5f;
        if(f < 0f || f > 1f) return;

        Draw.blend(Blending.normal);
        for(int i = 0; i < 4; i++){
            float fi = f - i * 0.12f;
            if(fi <= 0f || fi > 1f) continue;
            Draw.color(black, 0.9f * (1f - fi) * (1f - i * 0.22f));
            Lines.stroke(8.5f * (1f - fi) * (1f - i * 0.18f) + 0.6f);
            Lines.circle(cx, cy, 250f * Interp.pow3Out.apply(fi));
        }
        Draw.reset();
    }

    public static void drawDial(float cx, float cy, float r, float a, float p, float retreat){
        Draw.color(black, a * 0.72f);
        Fill.circle(cx, cy, r);

        Draw.color(black, a);
        Lines.stroke(Math.max(1.2f, r * 0.09f));
        Lines.circle(cx, cy, r);

        Lines.stroke(Math.max(1f, r * 0.05f));
        for(int i = 0; i < 12; i++){
            float ang = i * 30f;
            float len = i % 3 == 0 ? r * 0.16f : r * 0.09f;
            float x1 = cx + Angles.trnsx(ang, r * 0.82f);
            float y1 = cy + Angles.trnsy(ang, r * 0.82f);
            float x2 = cx + Angles.trnsx(ang, r * 0.82f - len);
            float y2 = cy + Angles.trnsy(ang, r * 0.82f - len);
            Lines.line(x1, y1, x2, y2);
        }

        float minuteAngle = 90f + p * 3600f - retreat;
        float hourAngle = 90f + p * 648f - retreat * 0.18f;

        Lines.stroke(Math.max(1.4f, r * 0.11f));
        Lines.line(cx, cy, cx + Angles.trnsx(minuteAngle, r * 0.78f), cy + Angles.trnsy(minuteAngle, r * 0.78f));
        Lines.stroke(Math.max(1.8f, r * 0.16f));
        Lines.line(cx, cy, cx + Angles.trnsx(hourAngle, r * 0.52f), cy + Angles.trnsy(hourAngle, r * 0.52f));

        Draw.color(black, a);
        Fill.circle(cx, cy, Math.max(1f, r * 0.09f));
    }
}
