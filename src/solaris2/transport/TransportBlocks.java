package solaris2.transport;

import mindustry.content.Items;
import mindustry.content.TechTree;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.*;
import solaris2.SolarisContent;

import static mindustry.Vars.content;

/**
 * 运输建筑注册。
 * 原 assets/content/blocks/运输 下 json 已迁移为 Java。
 */
public class TransportBlocks{

    public static InvertedJunction invertedJunction;
    public static Conveyor basicConveyor, heavyConveyor;
    public static Junction basicJunction, heavyJunction;
    public static DuctBridge basicBridge, heavyBridge;
    public static Router basicRouter, heavyRouter;
    public static OverflowGate overflowGate, invertedOverflowGate;
    public static Sorter sorter, invertedSorter;

    public static void load(){
        // 交叉器
        basicJunction = new Junction("基础交叉器");
        basicJunction.requirements(Category.distribution, ItemStack.with(SolarisContent.item("铁"), 4));
        basicJunction.health = 50;
        basicJunction.capacity = 13;
        basicJunction.speed = 6f;
        basicJunction.crushDamageMultiplier = 0.05f;
        basicJunction.flammabilityScale = 0f;

        heavyJunction = new Junction("重型交叉器");
        heavyJunction.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("钢"), 4, Items.graphite, 1, Items.tungsten, 2));
        heavyJunction.health = 80;
        heavyJunction.capacity = 13;
        heavyJunction.speed = 6f;
        heavyJunction.crushDamageMultiplier = 0.05f;
        heavyJunction.flammabilityScale = 0f;

        // 桥
        basicBridge = new DuctBridge("基础桥");
        basicBridge.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("铁"), 8, Items.graphite, 8));
        basicBridge.health = 80;
        basicBridge.range = 3;
        basicBridge.itemCapacity = 10;
        basicBridge.crushFragile = true;
        basicBridge.crushDamageMultiplier = 0.05f;
        basicBridge.flammabilityScale = 0f;

        heavyBridge = new DuctBridge("重型桥");
        heavyBridge.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("铁"), 8, Items.graphite, 8, Items.tungsten, 3));
        heavyBridge.health = 80;
        heavyBridge.range = 6;
        heavyBridge.itemCapacity = 10;
        heavyBridge.crushFragile = true;
        heavyBridge.crushDamageMultiplier = 0.05f;
        heavyBridge.flammabilityScale = 0f;

        // 传送带
        basicConveyor = new Conveyor("基础传送带");
        basicConveyor.requirements(Category.distribution, ItemStack.with(SolarisContent.item("铁"), 1));
        basicConveyor.health = 45;
        basicConveyor.speed = 0.033f;
        basicConveyor.displayedSpeed = 4.62f;
        basicConveyor.junctionReplacement = basicJunction;
        basicConveyor.bridgeReplacement = basicBridge;
        basicConveyor.crushDamageMultiplier = 0.05f;
        basicConveyor.flammabilityScale = 0f;
        research(basicConveyor, "router");

        heavyConveyor = new Conveyor("重型传送带");
        heavyConveyor.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("钢"), 1, Items.tungsten, 1));
        heavyConveyor.health = 85;
        heavyConveyor.speed = 0.0594f;
        heavyConveyor.displayedSpeed = 8.316f;
        heavyConveyor.junctionReplacement = heavyJunction;
        heavyConveyor.bridgeReplacement = heavyBridge;
        heavyConveyor.crushDamageMultiplier = 0.05f;
        heavyConveyor.flammabilityScale = 0f;
        research(heavyConveyor, "router");

        // 路由器
        basicRouter = new Router("基础路由器");
        basicRouter.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("铁"), 6, Items.graphite, 3));
        basicRouter.health = 40;
        basicRouter.speed = 4f;
        basicRouter.crushDamageMultiplier = 0.05f;
        basicRouter.flammabilityScale = 0f;

        heavyRouter = new Router("重型路由器");
        heavyRouter.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("钢"), 6, Items.graphite, 3, Items.tungsten, 2));
        heavyRouter.health = 80;
        heavyRouter.speed = 6f;
        heavyRouter.crushDamageMultiplier = 0.05f;
        heavyRouter.flammabilityScale = 0f;

        // 溢流门
        overflowGate = new OverflowGate("溢流门");
        overflowGate.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("铁"), 4, Items.graphite, 2));
        overflowGate.health = 90;
        overflowGate.buildCostMultiplier = 3f;

        invertedOverflowGate = new OverflowGate("反向溢流门");
        invertedOverflowGate.invert = true;
        invertedOverflowGate.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("铁"), 4, Items.graphite, 2));
        invertedOverflowGate.health = 90;
        invertedOverflowGate.buildCostMultiplier = 3f;

        // 分类器
        sorter = new Sorter("分类器");
        sorter.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("铁"), 4, Items.graphite, 2, Items.tungsten, 1));
        sorter.health = 90;
        sorter.buildCostMultiplier = 3f;

        invertedSorter = new Sorter("反向分类器");
        invertedSorter.invert = true;
        invertedSorter.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("铁"), 4, Items.graphite, 2, Items.tungsten, 1));
        invertedSorter.health = 90;
        invertedSorter.buildCostMultiplier = 3f;

        // 反向交叉器
        invertedJunction = new InvertedJunction("反向交叉器");
        invertedJunction.requirements(Category.distribution, ItemStack.with(SolarisContent.item("铁"), 4));
        invertedJunction.health = 50;
        invertedJunction.capacity = 13;
        invertedJunction.speed = 6f;
        invertedJunction.crushDamageMultiplier = 0.05f;
        invertedJunction.flammabilityScale = 0f;
    }

    private static void research(Block block, String parentName){
        Block parent = content.block(parentName);
        if(parent == null) return;
        TechTree.TechNode parentNode = TechTree.all.find(t -> t.content == parent);
        if(parentNode == null) return;
        TechTree.TechNode node = new TechTree.TechNode(null, block, block.researchRequirements());
        node.parent = parentNode;
        parentNode.children.add(node);
    }
}
