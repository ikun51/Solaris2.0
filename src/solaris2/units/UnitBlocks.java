package solaris2.units;

import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.TechTree;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.*;
import solaris2.SolarisContent;

import static mindustry.Vars.content;

/**
 * 单位建筑注册。
 */
public class UnitBlocks{

    public static DroneCarrier droneCarrier;
    public static GenericCrafter bandwidthNode;

    public static void load(){
        DroneCarrier.configureTypes();
        droneCarrier = new DroneCarrier();
        droneCarrier.health = 1200;
        droneCarrier.size = 3;
        droneCarrier.hasItems = true;
        droneCarrier.itemCapacity = 50;
        droneCarrier.hasPower = true;
        droneCarrier.update = true;
        droneCarrier.solid = true;
        droneCarrier.destructible = true;
        droneCarrier.requirements(Category.turret,
                ItemStack.with(Items.copper, 150, Items.lead, 120,
                        Items.silicon, 100, Items.titanium, 80));
        research(droneCarrier, "duo");

        // 带宽节点
        bandwidthNode = new GenericCrafter("带宽");
        bandwidthNode.localizedName = "带宽节点";
        bandwidthNode.description = "通过扩展核心带宽，增加单位容量。";
        bandwidthNode.health = 500;
        bandwidthNode.size = 2;
        bandwidthNode.squareSprite = true;
        bandwidthNode.hasPower = true;
        bandwidthNode.hasItems = false;
        bandwidthNode.craftEffect = Fx.none;
        bandwidthNode.unitCapModifier = 4;
        bandwidthNode.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawRegion("-1"){{
                    spinSprite = true;
                    rotateSpeed = 1f;
                }},
                new DrawRegion("-2"){{
                    rotateSpeed = -1f;
                }},
                new DrawRegion("-top"));
        bandwidthNode.requirements(Category.effect,
                ItemStack.with(Items.silicon, 90, Items.copper, 120, SolarisContent.item("铁"), 90));
        research(bandwidthNode, "core-shard");
    }

    private static void research(Block block, String parentName){
        Block found = SolarisContent.block(parentName);
        if(found == null) found = content.block(parentName);
        if(found == null) return;
        final Block parent = found;
        TechTree.TechNode parentNode = TechTree.all.find(t -> t.content == parent);
        if(parentNode == null) return;
        TechTree.TechNode node = new TechTree.TechNode(null, block, block.researchRequirements());
        node.parent = parentNode;
        parentNode.children.add(node);
    }
}
