package solaris2.turret;

import mindustry.content.Items;
import mindustry.gen.*;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.meta.BuildVisibility;
import solaris2.SolarisContent;

/**
 * 由 scripts/建筑/炮台 下的 2 个 JS 炮台移植而来。
 */
public class TurretBlocks{

    public static MagneticCoil magneticCoil;
    public static SpikeReleaser spikeReleaser;

    public static void load(){
        magneticCoil = new MagneticCoil("磁暴线圈");
        magneticCoil.description = "Let the Joules flow";
        magneticCoil.configurable = true;
        magneticCoil.health = 700;
        magneticCoil.size = 3;
        magneticCoil.rotate = false;
        magneticCoil.recoil = 0f;
        magneticCoil.customShadow = false;
        magneticCoil.buildVisibility = BuildVisibility.shown;
        magneticCoil.requirements(Category.turret,
                ItemStack.with(SolarisContent.item("磁钢"), 95, SolarisContent.item("铝"), 150,
                        Items.silicon, 100, SolarisContent.item("银"), 50, SolarisContent.item("硅铁"), 50));
        magneticCoil.consumePowerDynamic((Building build) -> {
            Building tail = MagneticCoil.getTail(build, magneticCoil);
            if(tail != build) return 0f;
            return build.shouldConsume() ? 6.5f * MagneticCoil.getChainLength(build, magneticCoil) : 0f;
        });

        spikeReleaser = new SpikeReleaser("地刺释放器");
        spikeReleaser.description = "扇形定向抛射地刺。\n选中建筑后点击地图即可设置扇形中心方向。\n一次抛出 5 颗，一颗一颗均匀铺开，20 颗为一轮。";
        spikeReleaser.configurable = true;
        spikeReleaser.saveConfig = true;
        spikeReleaser.health = 900;
        spikeReleaser.size = 2;
        spikeReleaser.buildVisibility = BuildVisibility.shown;
        spikeReleaser.requirements(Category.turret,
                ItemStack.with(Items.silicon, 500, Items.thorium, 180,
                        Items.graphite, 250, Items.titanium, 200));
    }
}
