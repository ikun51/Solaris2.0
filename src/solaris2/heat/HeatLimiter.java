package solaris2.heat;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;

/**
 * 由 scripts/建筑/热量/热量限制器.js 移植而来。
 */
public class HeatLimiter extends HeatConductor{

    private static final int[] DIR_X = {1, 0, -1, 0};
    private static final int[] DIR_Y = {0, 1, 0, -1};

    private static final ObjectMap<Class<?>, Field[]> heatFieldCache = new ObjectMap<>();

    public static TextureRegion topRegion, glowRegion;

    public float maxThreshold = 100f;

    public HeatLimiter(String name){
        super(name);

        size = 1;
        rotate = true;
        rotateDraw = false;
        drawArrow = true;
        configurable = true;
        visualMaxHeat = 100f;
        splitHeat = false;

        buildVisibility = BuildVisibility.shown;
        category = Category.crafting;
        requirements = ItemStack.with(Items.graphite, 30, Items.silicon, 20);

        config(String.class, (HeatLimiterBuild tile, String value) -> tile.configure(value));

        buildType = HeatLimiterBuild::new;
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("heat");
    }

    @Override
    public void load(){
        super.load();
        topRegion = Core.atlas.find(name + "-top");
        glowRegion = Core.atlas.find(name + "-glow");
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        if(topRegion != null && topRegion.found()){
            Draw.rect(topRegion, plan.drawx(), plan.drawy(), plan.rotation * 90f);
        }
    }

    private static Building neighbor(Building b, int d){
        return Vars.world.build(b.tile.x + DIR_X[d], b.tile.y + DIR_Y[d]);
    }

    private static Field[] fields(Building b){
        Class<?> c = b.getClass();
        Field[] cached = heatFieldCache.get(c);
        if(cached == null){
            Field heatField = null, lastUpdateField = null;
            try{
                heatField = c.getField("heat");
            }catch(Throwable ignored){
            }
            try{
                lastUpdateField = c.getField("lastHeatUpdate");
            }catch(Throwable ignored){
            }
            cached = new Field[]{heatField, lastUpdateField};
            heatFieldCache.put(c, cached);
        }
        return cached;
    }

    private static boolean hasHeat(Building b){
        return fields(b)[0] != null;
    }

    private static float readHeat(Building b){
        try{
            return fields(b)[0].getFloat(b);
        }catch(Throwable ignored){
            return 0f;
        }
    }

    private static void writeHeat(Building b, float amount){
        try{
            fields(b)[0].setFloat(b, amount);
        }catch(Throwable ignored){
        }
        Field lastUpdateField = fields(b)[1];
        if(lastUpdateField != null){
            try{
                lastUpdateField.setLong(b, Vars.state.updateId);
            }catch(Throwable ignored){
            }
        }
    }

    private static boolean isSplitter(Building o){
        return o != null && o.block.name.contains("分热器") && hasHeat(o);
    }

    private static boolean canWriteHeat(Building o, Building b){
        return o != null && o != b && o.team == b.team && hasHeat(o);
    }

    public class HeatLimiterBuild extends HeatConductorBuild{

        public float threshold = 20f;
        public boolean active = false;

        @Override
        public void configured(Unit builder, Object value){
            if(value == null) return;

            float v;
            try{
                v = Float.parseFloat(String.valueOf(value));
            }catch(NumberFormatException e){
                return;
            }

            threshold = Mathf.clamp(v, 0f, maxThreshold);
        }

        @Override
        public void updateTile(){
            Building src = neighbor(this, (rotation + 2) % 4);
            active = isSplitter(src);

            float input = active ? Math.max(readHeat(src), 0f) : 0f;
            heat = input;

            Building main = neighbor(this, rotation);
            Building left = neighbor(this, (rotation + 1) % 4);
            Building right = neighbor(this, (rotation + 3) % 4);

            boolean mainNeed = canWriteHeat(main, this);
            boolean leftNeed = canWriteHeat(left, this);
            boolean rightNeed = canWriteHeat(right, this);

            int needs = (mainNeed ? 1 : 0) + (leftNeed ? 1 : 0) + (rightNeed ? 1 : 0);

            if(input <= 0.0001f || needs == 0) return;

            if(needs == 1){
                if(mainNeed){
                    writeHeat(main, input);
                }else if(leftNeed){
                    writeHeat(left, input);
                }else{
                    writeHeat(right, input);
                }
                return;
            }

            float mainGive = 0f;
            if(mainNeed){
                mainGive = Math.min(input, threshold);
                writeHeat(main, mainGive);
            }

            float rest = input - mainGive;
            int others = (leftNeed ? 1 : 0) + (rightNeed ? 1 : 0);

            if(rest > 0.0001f && others > 0){
                float share = rest / others;
                if(leftNeed) writeHeat(left, share);
                if(rightNeed) writeHeat(right, share);
            }
        }

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            if(topRegion != null && topRegion.found()){
                Draw.rect(topRegion, x, y, rotdeg());
            }

            if(heat > 0 && glowRegion != null && glowRegion.found()){
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                Draw.color(1f, 0.22f, 0.22f, heatFrac() * (0.8f * (1f - 0.3f + Mathf.absin(10f, 0.3f))) * 1.2f);
                Draw.rect(glowRegion, x, y);
                Draw.blend();
                Draw.color();
            }
        }

        @Override
        public float heat(){
            return 0f;
        }

        @Override
        public void buildConfiguration(Table table){
            table.clear();

            table.defaults().left().pad(3f);

            table.add(active ? "状态：可用" : "状态：不可用").color(active ? Pal.accent : Pal.remove).row();

            if(!active){
                table.add("箭头背面的相邻格没有分热器").color(Pal.remove).width(300f).wrap().row();
            }

            TextField field = new TextField("" + threshold);
            field.setMessageText("输出阈值");
            field.changed(() -> configure(field.getText()));

            table.add("输出阈值").padTop(6f);
            table.add(field).width(120f);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(threshold);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            threshold = read.f();
        }
    }
}
