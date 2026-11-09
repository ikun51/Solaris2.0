package solaris2.liquid;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.type.*;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.BuildVisibility;
import solaris2.SolarisContent;

/**
 * 液体过滤阀：可配置只让指定种类的液体通过，沿旋转方向输送。
 * 由 scripts/建筑/液体/液体过滤阀.js 与 content/blocks/液体/液体过滤阀.json 移植而来。
 */
public class LiquidFilter extends Conduit{

    public TextureRegion centerRegion, arrowRegion, topRegion;

    public LiquidFilter(String name){
        super(name);

        description = "可让指定种类的液体通过";
        health = 60;
        size = 1;
        liquidCapacity = 20f;
        buildVisibility = BuildVisibility.shown;
        configurable = true;
        saveConfig = true;
        clearOnDoubleTap = true;
        requirements(Category.liquid, ItemStack.with(SolarisContent.item("铁"), 30));

        config(Liquid.class, (LiquidFilterBuild build, Liquid liquid) -> build.setLiquid(liquid));
        configClear((LiquidFilterBuild build) -> build.setLiquid(null));

        buildType = LiquidFilterBuild::new;
    }

    @Override
    public void load(){
        super.load();
        centerRegion = Core.atlas.find(name + "-center");
        arrowRegion = Core.atlas.find(name + "-arrow");
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        if(plan.config != null){
            drawPlanConfig(plan, list);
        }
        if(topRegion != null && topRegion.found()){
            Draw.rect(topRegion, plan.drawx(), plan.drawy(), plan.rotation * 90f);
        }
    }

    @Override
    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list){
        drawPlanConfigCenter(plan, plan.config, name + "-center");
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid");
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, arrowRegion};
    }

    public class LiquidFilterBuild extends ConduitBuild{

        public Liquid filterLiquid;

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            noSleep();
            if(filterLiquid == null){
                return super.acceptLiquid(source, liquid);
            }
            if(liquid != filterLiquid) return false;
            if(liquids.current() != liquid && liquids.currentAmount() >= 0.2f) return false;
            return tile == null || source == this || (source.relativeTo(tile.x, tile.y) + 2) % 4 != rotation;
        }

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            if(filterLiquid != null){
                Draw.color(filterLiquid.color);
                Draw.rect(centerRegion, x, y);
                Draw.color();
            }else{
                Draw.rect(arrowRegion, x, y, rotdeg());
            }
            if(topRegion != null && topRegion.found()){
                Draw.rect(topRegion, x, y, rotdeg());
            }
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(block, table, Vars.content.liquids(), () -> filterLiquid, value -> configure(value));
        }

        public void setLiquid(Liquid liquid){
            filterLiquid = liquid;
            noSleep();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(filterLiquid == null ? -1 : filterLiquid.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            short id = read.s();
            filterLiquid = id == -1 ? null : Vars.content.liquids().get(id);
        }
    }
}
