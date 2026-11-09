package solaris2.liquid;

import mindustry.content.Items;
import mindustry.content.TechTree;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.DirectionLiquidBridge;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.production.Pump;
import solaris2.SolarisContent;

/**
 * 液体建筑注册。
 * 原 assets/content/blocks/液体 下 json 已迁移为 Java。
 */
public class LiquidBlocks{

    public static Block genericTank, pulsePump, liquidDriver;
    public static Conduit liquidPipe;
    public static LiquidJunction liquidJunction;
    public static LiquidRouter liquidRouter, liquidTank, giantTank;
    public static DirectionLiquidBridge liquidBridge;
    public static Pump largePump, mechPump;
    public static Gasifier gasifier;
    public static Liquefier liquefier;
    public static LiquidFilter liquidFilter;
    public static LiquidFlowLimiter liquidFlowLimiter;

    public static void load(){
        genericTank = new GenericTank("通用储罐");
        genericTank.description = "可同时存储物品与液体的通用仓库，可作为机械臂的提取/放置端";
        genericTank.health = 1200;
        genericTank.size = 4;
        genericTank.squareSprite = false;
        genericTank.itemCapacity = 800;
        genericTank.liquidCapacity = 1200;
        genericTank.outputsLiquid = true;
        ((LiquidRouter)genericTank).liquidPadding = 2f;
        genericTank.requirements(Category.liquid,
                ItemStack.with(SolarisContent.item("钢"), 120, Items.metaglass, 60,
                        SolarisContent.item("铝"), 95, Items.tungsten, 50));

        pulsePump = new PulsePump("脉冲泵");
        pulsePump.description = "从地面液体抽取。消耗电力与氢气进行蓄压，蓄压满后瞬间一次性抽入大量液体。抽水速度条满即完成一次脉冲，只能放置在有液体的地面上。";
        pulsePump.liquidCapacity = 500f;
        pulsePump.requirements(Category.liquid,
                ItemStack.with(SolarisContent.item("铝"), 80, SolarisContent.item("钢"), 60, Items.metaglass, 40));
        research(pulsePump, "mechanical-pump");

        liquidDriver = new LiquidDriver("液体驱动器");
        liquidDriver.description = "消耗电力，从相邻建筑抽取液体并远程输送至链接的液体驱动器。";
        liquidDriver.size = 2;
        liquidDriver.buildVisibility = mindustry.world.meta.BuildVisibility.shown;
        liquidDriver.liquidCapacity = 60f;
        liquidDriver.consumePower(4f);
        liquidDriver.requirements(Category.liquid,
                ItemStack.with(Items.metaglass, 60, SolarisContent.item("硅铁"), 80, SolarisContent.item("钢"), 125));

        // 液体管道
        liquidPipe = new Conduit("液体管道");
        liquidPipe.localizedName = "液态管道";
        liquidPipe.description = "能够运输液体";
        liquidPipe.health = 20;
        liquidPipe.liquidCapacity = 32f;
        liquidPipe.requirements(Category.liquid,
                ItemStack.with(SolarisContent.item("铁"), 2, Items.metaglass, 2));

        // 液体交叉器
        liquidJunction = new LiquidJunction("液体交叉器");
        liquidJunction.description = "两条交叉管道的节点";
        liquidJunction.health = 400;
        liquidJunction.buildCostMultiplier = 3f;
        liquidJunction.solid = false;
        liquidJunction.underBullets = true;
        liquidJunction.requirements(Category.liquid,
                ItemStack.with(SolarisContent.item("铁"), 4, Items.graphite, 2));

        // 液体管道桥
        liquidBridge = new DirectionLiquidBridge("液体管道桥");
        liquidBridge.description = "能够跨过建筑与地形输送液体的管道桥";
        liquidBridge.size = 1;
        liquidBridge.health = 60;
        liquidBridge.liquidCapacity = 20f;
        liquidBridge.range = 4;
        liquidBridge.requirements(Category.liquid,
                ItemStack.with(SolarisContent.item("铁"), 8, Items.metaglass, 8));
        research(liquidBridge, "液体管道");

        // 液体路由器
        liquidRouter = new LiquidRouter("液体路由器");
        liquidRouter.liquidCapacity = 30f;
        liquidRouter.underBullets = true;
        liquidRouter.conductivePower = true;
        liquidRouter.hasPower = false;
        liquidRouter.outputsPower = false;
        liquidRouter.solid = false;
        liquidRouter.requirements(Category.liquid,
                ItemStack.with(SolarisContent.item("铁"), 3, Items.metaglass, 2));
        research(liquidRouter, "液体管道");

        // 流体罐
        liquidTank = new LiquidRouter("流体罐");
        liquidTank.description = "用于存储液体";
        liquidTank.size = 2;
        liquidTank.liquidCapacity = 800f;
        liquidTank.liquidPadding = 2f;
        liquidTank.solid = true;
        liquidTank.armor = 6f;
        liquidTank.requirements(Category.liquid,
                ItemStack.with(Items.metaglass, 10, SolarisContent.item("铝"), 15));

        // 巨型流体罐
        giantTank = new LiquidRouter("巨型流体罐");
        giantTank.description = "用于存储液体";
        giantTank.size = 3;
        giantTank.liquidCapacity = 2200f;
        giantTank.liquidPadding = 2f;
        giantTank.solid = true;
        giantTank.armor = 6f;
        giantTank.squareSprite = false;
        giantTank.requirements(Category.liquid,
                ItemStack.with(Items.metaglass, 20, SolarisContent.item("铝"), 50));
        research(giantTank, "can");

        // 大型电力泵
        largePump = new Pump("大型电力泵");
        largePump.description = "消耗电力泵出更多液体";
        largePump.size = 2;
        largePump.health = 680;
        largePump.pumpAmount = 0.125f;
        largePump.liquidCapacity = 30f;
        largePump.squareSprite = false;
        largePump.consumePower(0.5f);
        largePump.requirements(Category.liquid,
                ItemStack.with(Items.copper, 60, SolarisContent.item("铁"), 20, Items.metaglass, 30));

        // 小型电力泵
        mechPump = new Pump("机械泵");
        mechPump.localizedName = "小型电力泵";
        mechPump.description = "消耗电力泵出液体";
        mechPump.pumpAmount = 0.13333333f;
        mechPump.warmupSpeed = 0.015f;
        mechPump.researchCostMultiplier = 0.1f;
        mechPump.hasPower = true;
        mechPump.consumePower(0.1f);
        mechPump.requirements(Category.liquid, ItemStack.with(SolarisContent.item("铁"), 12));

        // 气化器
        gasifier = new Gasifier("气化器");
        research(gasifier, "steam-generator");

        // 液化器
        liquefier = new Liquefier("液化器");
        research(liquefier, "steam-generator");

        // 液体过滤阀
        liquidFilter = new LiquidFilter("液体过滤阀");

        // 液体限流阀
        liquidFlowLimiter = new LiquidFlowLimiter("液体限流阀");

        // 热能泵 json 覆盖字段
        Block thermal = solaris2.heat.HeatBlocks.thermalPump;
        if(thermal != null){
            thermal.description = "消耗热量泵出液体";
            thermal.size = 2;
            thermal.armor = 5f;
            thermal.liquidCapacity = 150f;
            thermal.crushDamageMultiplier = 0.2f;
            thermal.squareSprite = false;
            if(thermal instanceof Pump p){
                p.pumpAmount = 0.4f;
                p.warmupSpeed = 0.02f;
            }
            thermal.researchCostMultiplier = 0.3f;
            thermal.requirements(Category.liquid,
                    ItemStack.with(SolarisContent.item("铝"), 40, SolarisContent.item("铬"), 30,
                            SolarisContent.item("氮化硅"), 5));
        }
    }

    private static void research(Block block, String parentName){
        Block parent = SolarisContent.block(parentName);
        if(parent == null) return;
        TechTree.TechNode parentNode = TechTree.all.find(t -> t.content == parent);
        if(parentNode == null) return;
        TechTree.TechNode node = new TechTree.TechNode(null, block, block.researchRequirements());
        node.parent = parentNode;
        parentNode.children.add(node);
    }
}
