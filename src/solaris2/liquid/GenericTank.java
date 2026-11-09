package solaris2.liquid;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.liquid.*;

/**
 * 由 scripts/建筑/液体/通用储罐.js 移植而来。
 */
public class GenericTank extends LiquidRouter{

    public GenericTank(String name){
        super(name);

        hasItems = true;
        itemCapacity = 500;
        separateItemCapacity = true;
        liquidCapacity = 500;

        buildType = GenericTankBuild::new;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public boolean consumesItem(Item item){
        return true;
    }

    @Override
    public boolean consumesLiquid(Liquid liquid){
        return true;
    }

    public class GenericTankBuild extends LiquidRouterBuild{

        @Override
        public void draw(){
            TextureRegion bottom = ((LiquidRouter)block).bottomRegion;
            if(bottom != null && Core.atlas.isFound(bottom)){
                Draw.rect(bottom, x, y);
            }
            if(liquids != null && liquids.currentAmount() > 0.001f){
                LiquidBlock.drawTiledFrames(block.size, x, y, ((LiquidRouter)block).liquidPadding, liquids.current(), liquids.currentAmount() / block.liquidCapacity);
            }
            Draw.rect(block.region, x, y);
            drawTeamTop();
        }
    }
}
