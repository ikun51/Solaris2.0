package solaris2.orbit;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.meta.BuildVisibility;
import solaris2.SolarisContent;

/**
 * 由 scripts/建筑/轨道 下的 2 个 JS 建筑移植而来。
 */
public class OrbitBlocks{

    public static OrbitLauncher orbitLauncher;
    public static OrbitMarker orbitMarker;

    public static void load(){
        // 触发 OrbitFireEffects 类初始化：其中的静态子弹需要在内容初始化阶段之前完成注册，
        // 与 JS 版本在 scripts 加载时创建子弹的时机保持一致。
        OrbitFireEffects.orbiters = 0;

        orbitLauncher = new OrbitLauncher("轨道发射器");
        orbitLauncher.description = "把燃料、材料与低温液注入火箭，将轨道器或轨道弹送上轨道。";
        orbitLauncher.size = 4;
        orbitLauncher.health = 2000;
        orbitLauncher.armor = 10f;
        orbitLauncher.liquidCapacity = 2200f;
        orbitLauncher.itemCapacity = 5100;
        orbitLauncher.configurable = true;
        orbitLauncher.saveConfig = true;
        orbitLauncher.buildVisibility = BuildVisibility.shown;
        orbitLauncher.alwaysUnlocked = true;
        orbitLauncher.requirements(Category.effect,
                ItemStack.with(SolarisContent.item("磁钢"), 1600, Items.tungsten, 1070,
                        Items.graphite, 1080, SolarisContent.item("钢"), 1170,
                        Items.copper, 1100, SolarisContent.item("铝"), 1300));

        orbitMarker = new OrbitMarker("轨道标记");
        orbitMarker.description = "标记目标后引导轨道动能打击，每次攻击消耗一发轨道弹。";
        orbitMarker.size = 2;
        orbitMarker.health = 800;
        orbitMarker.armor = 5f;
        orbitMarker.reload = 720f;
        orbitMarker.range = 1600f;
        orbitMarker.inaccuracy = 0f;
        orbitMarker.recoil = 0f;
        orbitMarker.hasShadow = false;
        orbitMarker.buildVisibility = BuildVisibility.shown;
        orbitMarker.alwaysUnlocked = true;
    }
}
