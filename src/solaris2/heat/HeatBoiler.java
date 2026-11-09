package solaris2.heat;

import mindustry.content.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import solaris2.SolarisContent;

/**
 * 由 scripts/建筑/热量/热量锅炉.js 移植而来。
 */
public class HeatBoiler extends HeatCrafter{

    public HeatBoiler(String name){
        super(name);

        hasItems = false;
        hasLiquids = true;
        liquidCapacity = 200f;
        craftTime = 120f;
        heatRequirement = 24f;
        maxEfficiency = 2f;

        consume(new ConsumeLiquid(Liquids.water, 0.2f));
        outputLiquid = new LiquidStack(SolarisContent.liquid("蒸汽"), 0.4f);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("效率", (HeatCrafterBuild e) -> new Bar(
            () -> "效率 " + (int)Math.floor(e.efficiency * 100) + "%",
            () -> Pal.accent,
            () -> Math.max(0f, Math.min(e.efficiency, 1f))
        ));
    }
}
