package solaris2.storage;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.storage.StorageBlock;
import solaris2.SolarisContent;

/**
 * 存储建筑注册。
 * 原 assets/content/blocks/存储 下 json 已迁移为 Java。
 */
public class StorageBlocks{

    public static StorageBlock vault, container, box;

    public static void load(){
        vault = new StorageBlock("仓库");
        vault.description = "可以存储大量物品";
        vault.health = 515;
        vault.size = 3;
        vault.itemCapacity = 1000;
        vault.requirements(Category.effect,
                ItemStack.with(SolarisContent.item("铁"), 120, Items.copper, 50));

        container = new StorageBlock("容器");
        container.description = "可以存储少量物品";
        container.health = 130;
        container.size = 2;
        container.itemCapacity = 100;
        container.requirements(Category.effect, ItemStack.with(SolarisContent.item("铁"), 50));

        box = new StorageBlock("箱子");
        box.description = "mini容器";
        box.health = 50;
        box.size = 1;
        box.itemCapacity = 400;
        box.requirements(Category.effect,
                ItemStack.with(SolarisContent.item("铁"), 80, Items.copper, 20));
    }
}
