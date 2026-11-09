package solaris2.daynight;

import arc.Core;
import arc.Events;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.blocks.power.PowerGenerator;

import static mindustry.Vars.*;

/**
 * 昼夜循环：一局 24 分钟，白天/黄昏/夜晚/黎明。
 * 夜晚降低炮台射程、太阳能归零，并绘制全屏变暗与探照灯光照。
 */
public class DayNight{
    /** 一整轮昼夜的时长（秒），24 分钟。 */
    public static final float cycleTime = 60f * 24f;
    /** 时间比例：0~0.4 白天，0.4~0.5 黄昏，0.5~0.9 夜晚，0.9~1 黎明。 */
    public static final float dayEnd = 0.4f, duskEnd = 0.5f, nightEnd = 0.9f;
    /** 夜晚最深遮罩透明度。 */
    public static final float maxDarkness = 0.8f;
    /** 夜晚未被照亮的炮台射程倍率。 */
    public static final float nightRangeScale = 0.5f;
    /** 探照灯判定为点亮的效率阈值。 */
    public static final float litThreshold = 0.35f;

    public static float elapsed;
    public static float dayFactor = 1f;
    public static float darkness;

    private static boolean inited, solarResolved;
    private static PowerGenerator solar;
    private static float solarBase;
    private static TextureRegion lightRegion;
    private static float hudTimer;

    public static void init(){
        if(inited) return;
        inited = true;
        Events.run(Trigger.update, DayNight::update);
        Events.run(Trigger.draw, DayNight::drawOverlay);
        Events.on(WorldLoadEvent.class, e -> reset());
    }

    public static void reset(){
        elapsed = 0f;
        dayFactor = 1f;
        darkness = 0f;
        Searchlight.lights.clear();
    }

    private static void update(){
        if(!state.isPlaying()) return;
        //Time.delta 是 tick 数（每秒约 60），这里换算成秒，保证 cycleTime 的 24 分钟真的是一轮 24 分钟
        elapsed += Time.delta / 60f;
        dayFactor = computeDayFactor(elapsed / cycleTime);
        darkness = (1f - dayFactor) * maxDarkness;
        updateSolar();

        hudTimer += Time.delta;
        if(hudTimer >= 60f){
            hudTimer = 0f;
            updateHud();
        }
    }

    /** 在波次框（name = "waves"）下面追加一行 24 分钟制的时间显示。 */
    private static void updateHud(){
        if(headless || Core.scene == null) return;

        Table waves = (Table)Core.scene.find("waves");
        if(waves == null) return;
        Element existing = waves.find("daynight-time");
        if(existing != null) return;

        waves.row();
        Table row = new Table();
        row.name = "daynight-time";
        row.left();
        row.label(() -> "时间 " + timeText()).color(Color.lightGray).left().padLeft(6f).padBottom(2f);
        waves.add(row).left();
    }

    /** 当前循环内的 24 分钟制时间，格式 mm:ss。 */
    public static String timeText(){
        int total = (int)(elapsed % cycleTime);
        int m = total / 60, s = total % 60;
        return (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }

    /** 传入 0~1 的循环进度，返回 1 表示全白天、0 表示全黑夜。 */
    public static float computeDayFactor(float progress){
        progress -= Mathf.floor(progress);
        if(progress < dayEnd) return 1f;
        if(progress < duskEnd) return 1f - (progress - dayEnd) / (duskEnd - dayEnd);
        if(progress < nightEnd) return 0f;
        return (progress - nightEnd) / (1f - nightEnd);
    }

    /** 炮台射程倍率：夜晚未被探照灯照到则降低，白天或照亮则正常。 */
    public static float rangeMultiplier(float x, float y){
        if(!state.isPlaying() || dayFactor >= 0.999f) return 1f;
        if(isLit(x, y)) return 1f;     
        return Mathf.lerp(1f, nightRangeScale, 1f - dayFactor);
    }

    /** 坐标是否在任意一座已通电探照灯的光圈内。 */
    public static boolean isLit(float x, float y){
        for(Building build : Searchlight.lights){
            if(build.efficiency < litThreshold || !(build.block instanceof Searchlight light)) continue;
            float radius = light.radius;
            if(Mathf.dst2(build.x - x, build.y - y) <= radius * radius) return true;
        }
        return false;  
    }

    /** 太阳能发电随昼夜变化，夜晚归零。 */
    private static void updateSolar(){
        if(!solarResolved){
            solarResolved = true;
            for(Block block : content.blocks()){
                if(block instanceof PowerGenerator generator && block.name.endsWith("太阳能光伏板")){
                    solar = generator;
                    solarBase = generator.powerProduction;
                    break;
                }
            }
        }
        if(solar != null){
            solar.powerProduction = solarBase * dayFactor;
        }
    }

    private static void drawOverlay(){
        if(headless || !state.isPlaying() || darkness <= 0.02f) return;
     
        Draw.draw(Layer.darkness, DayNight::drawDark);
    }

    private static void drawDark(){
        Draw.reset();
        Draw.color(0f, 0.01f, 0.06f, darkness);
        Draw.rect(Core.atlas.white(), Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height);

        TextureRegion light = lightRegion();
        if(light != null){
            Draw.blend(Blending.additive);
            Draw.color(1f, 0.96f, 0.78f, 1f);
            for(Building build : Searchlight.lights){
                if(build.efficiency < litThreshold || !(build.block instanceof Searchlight searchlight)) continue;
                float radius = searchlight.radius;
                Draw.rect(light, build.x, build.y, radius * 2f, radius * 2f);
            }
            Draw.blend();
        }
        Draw.reset();
    }

    /** 运行时生成一张中心亮、边缘透明的柔光贴图。 */
    private static TextureRegion lightRegion(){
        if(lightRegion == null){
            int size = 256;
            Pixmap pixmap = new Pixmap(size, size);
            Color color = new Color(1f, 1f, 1f, 0f);
            for(int y = 0; y < size; y++){
                for(int x = 0; x < size; x++){
                    float dx = (x + 0.5f - size / 2f) / (size / 2f);
                    float dy = (y + 0.5f - size / 2f) / (size / 2f);
                    float dist = Mathf.sqrt(dx * dx + dy * dy);
                    float alpha = Mathf.clamp(1f - dist);
                    color.a = alpha * alpha;
                    pixmap.set(x, y, color);
                }
            }
            lightRegion = new TextureRegion(new Texture(pixmap));
            pixmap.dispose();
        }
        return lightRegion;
    }
}
