package solaris2.defense;

import mindustry.content.Items;
import mindustry.content.TechTree;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import solaris2.SolarisContent;

import static mindustry.Vars.content;

/**
 * 防御建筑注册。
 * 原 assets/content/blocks/墙 下 json 已迁移为 Java。
 */
public class WallBlocks{

    public static Wall ironWall, largeIronWall, hugeIronWall;
    public static MagneticWall magneticWall;
    public static SiliconAlloyWall siliconAlloyWall;
    public static GelWall gelWall;
    public static LargeMagneticWall largeMagneticWall;

    public static void load(){
        // 铁墙
        ironWall = new Wall("铁墙");
        ironWall.description = "前期的优良防御。";
        ironWall.health = 590;
        ironWall.size = 1;
        ironWall.requirements(Category.defense, ItemStack.with(SolarisContent.item("铁"), 5));
        research(ironWall, "copper-wall");

        largeIronWall = new Wall("大型铁墙");
        largeIronWall.description = "铁墙pro。";
        largeIronWall.health = 2470;
        largeIronWall.size = 2;
        largeIronWall.requirements(Category.defense,
                ItemStack.with(SolarisContent.item("铁"), 5, Items.copper, 2));
        research(largeIronWall, "copper-wall");

        hugeIronWall = new Wall("巨型铁墙");
        hugeIronWall.description = "铁墙promax。";
        hugeIronWall.health = 5490;
        hugeIronWall.size = 3;
        hugeIronWall.requirements(Category.defense,
                ItemStack.with(SolarisContent.item("铁"), 8, Items.copper, 3));
        research(hugeIronWall, "copper-wall");

        magneticWall = new MagneticWall();
        magneticWall.requirements(Category.defense,
                ItemStack.with(SolarisContent.item("磁钢"), 5, Items.tungsten, 8, SolarisContent.item("硅铁"), 4));

        siliconAlloyWall = new SiliconAlloyWall();
        siliconAlloyWall.requirements(Category.defense,
                ItemStack.with(SolarisContent.item("氮化硅"), 80, SolarisContent.item("铁"), 60));

        gelWall = new GelWall();
        gelWall.requirements(Category.defense,
                ItemStack.with(SolarisContent.item("氮化硅"), 80, SolarisContent.item("气凝胶"), 60));

        largeMagneticWall = new LargeMagneticWall();
        largeMagneticWall.requirements(Category.defense,
                ItemStack.with(SolarisContent.item("磁钢"), 8, Items.tungsten, 11, SolarisContent.item("硅铁"), 5));
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
