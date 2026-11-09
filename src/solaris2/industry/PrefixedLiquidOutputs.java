package solaris2.industry;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Eachable;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.type.LiquidStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.DrawBlock;

/**
 * 由 scripts/建筑/电解机.js 移植。
 * 与 DrawLiquidOutputs 行为一致，区别在于贴图名先去掉了模组前缀，按
 * “模组名-方块纯名-液体纯名-output1/2” 查找，避免出现双重前缀。
 */
public class PrefixedLiquidOutputs extends DrawBlock{
    // [液体index][0=output1, 1=output2]
    public TextureRegion[][] outputRegions;

    @Override
    public void load(Block block){
        String modName = (block.minfo != null && block.minfo.mod != null) ? block.minfo.mod.name : null;
        String pureBlock = stripModPrefix(block.name, modName);
        if(!(block instanceof GenericCrafter crafter) || crafter.outputLiquids == null){
            outputRegions = null;
            return;
        }

        outputRegions = new TextureRegion[crafter.outputLiquids.length][];
        for(int i = 0; i < crafter.outputLiquids.length; i++){
            LiquidStack stack = crafter.outputLiquids[i];
            if(stack == null || stack.liquid == null) continue;
            String pureLiquid = stripModPrefix(stack.liquid.name, modName);
            TextureRegion[] row = new TextureRegion[2];
            for(int j = 1; j <= 2; j++){
                row[j - 1] = Core.atlas.find(modName + "-" + pureBlock + "-" + pureLiquid + "-output" + j);
            }
            outputRegions[i] = row;
        }
    }

    @Override
    public void draw(Building build){
        if(!(build.block instanceof GenericCrafter crafter) || crafter.outputLiquids == null || outputRegions == null) return;
        for(int i = 0; i < crafter.outputLiquids.length; i++){
            int side = i < crafter.liquidOutputDirections.length ? crafter.liquidOutputDirections[i] : -1;
            if(side == -1 || outputRegions[i] == null) continue;
            int realRot = (side + build.rotation) % 4;
            Draw.rect(outputRegions[i][realRot > 1 ? 1 : 0], build.x, build.y, realRot * 90f);
        }
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        if(!(block instanceof GenericCrafter crafter) || crafter.outputLiquids == null || outputRegions == null) return;
        for(int i = 0; i < crafter.outputLiquids.length; i++){
            int side = i < crafter.liquidOutputDirections.length ? crafter.liquidOutputDirections[i] : -1;
            if(side == -1 || outputRegions[i] == null) continue;
            int realRot = (side + plan.rotation) % 4;
            Draw.rect(outputRegions[i][realRot > 1 ? 1 : 0], plan.drawx(), plan.drawy(), realRot * 90f);
        }
    }

    private static String stripModPrefix(String name, String modName){
        if(name != null && modName != null && name.startsWith(modName + "-")){
            return name.substring(modName.length() + 1);
        }
        return name;
    }
}
