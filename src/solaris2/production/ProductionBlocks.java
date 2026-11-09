package solaris2.production;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.TechTree;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.ParticleEffect;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.BurstDrill;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.SolidPump;
import mindustry.world.draw.DrawDefault;
import mindustry.world.draw.DrawLiquidTile;
import mindustry.world.draw.DrawMulti;
import mindustry.world.draw.DrawRegion;
import mindustry.world.meta.Attribute;
import solaris2.SolarisContent;

import static mindustry.Vars.content;

/**
 * 生产建筑注册。
 * 原 assets/content/blocks/钻头 下 json 已迁移为 Java。
 */
public class ProductionBlocks{

    public static MechanicalDrill mechanicalDrill;
    public static Drill steamDrill, smallPowerDrill, largePowerDrill, giantPowerDrill, plasmaDrill;
    public static BurstDrill groundBreaker;
    public static SolidPump oilDerrick;
    public static BeamDrill wallCrusher;

    public static ComprehensiveDrill comprehensiveDrill;
    public static DrillModule explorationModule, pressurizeModule, reinforcedPressurizeModule,
            compositeOreModule, blastingModule, acidLeachingModule;

    public static void load(){
        mechanicalDrill = new MechanicalDrill();
        mechanicalDrill.description = "基础钻头，通过应力驱动，会随着时间不断降低速度。";
        mechanicalDrill.canOverdrive = false;
        mechanicalDrill.hasLiquids = false;
        mechanicalDrill.size = 2;
        mechanicalDrill.tier = 2;
        mechanicalDrill.drillTime = 450f;
        mechanicalDrill.itemCapacity = 10;
        mechanicalDrill.requirements(Category.production, ItemStack.with(SolarisContent.item("铁"), 15));

        // 蒸汽钻头
        steamDrill = new Drill("蒸汽钻头");
        steamDrill.description = "通过高压蒸汽驱动。简单皮实，开采速度较慢";
        steamDrill.health = 90;
        steamDrill.size = 3;
        steamDrill.tier = 3;
        steamDrill.drillTime = 400f;
        steamDrill.drillEffect = Fx.none;
        steamDrill.warmupSpeed = 0.01f;
        steamDrill.rotateSpeed = 1.6f;
        steamDrill.itemCapacity = 20;
        steamDrill.liquidCapacity = 30;
        steamDrill.updateEffect = new ParticleEffect(){{
            particles = 5;
            sizeFrom = 3f;
            sizeTo = 0f;
            length = 5f;
            baseLength = 5f;
            lifetime = 50f;
            colorFrom = Color.valueOf("ffffff80");
            colorTo = Color.valueOf("ffffff80");
        }};
        steamDrill.consumeLiquid(SolarisContent.liquid("蒸汽"), 0.08f);
        steamDrill.requirements(Category.production,
                ItemStack.with(SolarisContent.item("铁"), 35, SolarisContent.item("铝"), 20, Items.graphite, 15));

        // 小型电力钻头
        smallPowerDrill = new Drill("小型电力钻头");
        smallPowerDrill.description = "基础钻头，通过电力驱动，开采速度较快。";
        smallPowerDrill.size = 2;
        smallPowerDrill.tier = 2;
        smallPowerDrill.drillTime = 320f;
        smallPowerDrill.itemCapacity = 10;
        smallPowerDrill.rotateSpeed = 5f;
        smallPowerDrill.liquidBoostIntensity = 1.8f;
        smallPowerDrill.warmupSpeed = 0.03f;
        smallPowerDrill.hasPower = true;
        smallPowerDrill.conductivePower = true;
        smallPowerDrill.consumeLiquid(Liquids.water, 0.025f).boost().optional(true, false);
        smallPowerDrill.consumePower(0.583333333f);
        smallPowerDrill.requirements(Category.production,
                ItemStack.with(SolarisContent.item("铁"), 20, SolarisContent.item("银"), 16, Items.silicon, 5));

        // 大型电力钻头
        largePowerDrill = new Drill("大型电力钻头");
        largePowerDrill.description = "基础钻头，通过电力驱动，开采速度较快。";
        largePowerDrill.size = 3;
        largePowerDrill.tier = 4;
        largePowerDrill.drillTime = 320f;
        largePowerDrill.itemCapacity = 10;
        largePowerDrill.rotateSpeed = 7f;
        largePowerDrill.liquidBoostIntensity = 1.8f;
        largePowerDrill.warmupSpeed = 0.03f;
        largePowerDrill.hasPower = true;
        largePowerDrill.conductivePower = true;
        largePowerDrill.updateEffect = new ParticleEffect(){{
            particles = 4;
            line = true;
            strokeFrom = 3f;
            strokeTo = 0f;
            lenFrom = 10f;
            lenTo = 0f;
            length = 20f;
            baseLength = 0f;
            lifetime = 10f;
            colorFrom = Color.valueOf("ffffff");
            colorTo = Color.valueOf("ffdaa9");
        }};
        largePowerDrill.consumeLiquid(Liquids.water, 0.05f).boost().optional(true, false);
        largePowerDrill.consumePower(1.2f);
        largePowerDrill.requirements(Category.production,
                ItemStack.with(SolarisContent.item("铁"), 20, SolarisContent.item("银"), 16, Items.silicon, 5));

        // 燃油巨型动力钻
        giantPowerDrill = new Drill("巨型动力钻");
        giantPowerDrill.localizedName = "燃油巨型动力钻";
        giantPowerDrill.description = "十分沉重的钻头，可钻取大部分矿物。";
        giantPowerDrill.size = 4;
        giantPowerDrill.tier = 6;
        giantPowerDrill.drillTime = 300f;
        giantPowerDrill.hardnessDrillMultiplier = 15f;
        giantPowerDrill.researchCostMultiplier = 0.1f;
        giantPowerDrill.itemCapacity = 20;
        giantPowerDrill.liquidCapacity = 20;
        giantPowerDrill.rotateSpeed = 2f;
        giantPowerDrill.liquidBoostIntensity = 1.6f;
        giantPowerDrill.warmupSpeed = 0.001f;
        giantPowerDrill.updateEffect = Fx.smoke;
        giantPowerDrill.updateEffectChance = 0.05f;
        giantPowerDrill.drillEffect = Fx.mineHuge;
        giantPowerDrill.drawRim = true;
        giantPowerDrill.squareSprite = false;
        giantPowerDrill.hasPower = true;
        giantPowerDrill.flammabilityScale = 0f;
        giantPowerDrill.explosivenessScale = 1.5f;
        giantPowerDrill.consumeLiquid(Liquids.water, 0.0066666f).boost().optional(true, false);
        giantPowerDrill.consumeLiquid(SolarisContent.liquid("石油"), 0.1f);
        giantPowerDrill.requirements(Category.production,
                ItemStack.with(SolarisContent.item("铝"), 180, SolarisContent.item("钢"), 100,
                        Items.graphite, 60, Items.tungsten, 50));

        // 电磁冲击钻
        plasmaDrill = new Drill("电磁冲击钻");
        plasmaDrill.description = "装配4台比激光切割机效率更高的离子切割器，全部启动会导致热量过高，需要冷冻液来辅助散热以达到最高效率";
        plasmaDrill.size = 4;
        plasmaDrill.tier = 8;
        plasmaDrill.health = 550;
        plasmaDrill.rotateSpeed = 20.65f;
        plasmaDrill.warmupSpeed = 0.06f;
        plasmaDrill.itemCapacity = 15;
        plasmaDrill.liquidCapacity = 20;
        plasmaDrill.drawRim = true;
        plasmaDrill.squareSprite = false;
        plasmaDrill.drillTime = 56f;
        plasmaDrill.liquidBoostIntensity = 1.6f;
        plasmaDrill.updateEffect = Fx.none;
        plasmaDrill.drillEffect = new MultiEffect(new ParticleEffect(){{
            particles = 20;
            line = true;
            length = 23f;
            baseLength = 3f;
            lifetime = 20f;
            colorFrom = Color.valueOf("FFFFFF");
            colorTo = Color.valueOf("FFE176");
            cone = 360f;
        }});
        plasmaDrill.consumePower(9f);
        plasmaDrill.consumeLiquid(Liquids.cryofluid, 0.1f).boost().optional(true, false);
        plasmaDrill.requirements(Category.production,
                ItemStack.with(Items.copper, 95, Items.graphite, 55, SolarisContent.item("磁钢"), 30,
                        SolarisContent.item("铝"), 70, SolarisContent.item("氮化硅"), 90, SolarisContent.item("钢"), 40));
        research(plasmaDrill, "blast-drill");

        // 地面破碎钻
        groundBreaker = new BurstDrill("地面破碎钻");
        groundBreaker.description = "可以快速破碎地面较软矿物";
        groundBreaker.size = 3;
        groundBreaker.tier = 2;
        groundBreaker.drillTime = 75f;
        groundBreaker.itemCapacity = 36;
        groundBreaker.liquidCapacity = 20;
        groundBreaker.liquidBoostIntensity = 1.53f;
        groundBreaker.warmupSpeed = 0.01f;
        groundBreaker.hasPower = true;
        groundBreaker.updateEffectChance = 0.06f;
        groundBreaker.drillEffect = new MultiEffect(Fx.massiveExplosion, Fx.drillSteam);
        groundBreaker.shake = 1f;
        groundBreaker.arrowOffset = 0f;
        groundBreaker.arrowSpacing = 0f;
        groundBreaker.arrows = 0;
        groundBreaker.blockedItems = Seq.with(Items.copper, Items.lead, Items.scrap);
        groundBreaker.blockedItem = Items.scrap;
        groundBreaker.consumeLiquid(SolarisContent.liquid("硝化甘油"), 0.0125f).boost().optional(true, false);
        groundBreaker.consumeLiquid(Liquids.hydrogen, 0.06666666f);
        groundBreaker.consumePower(1.2f);
        groundBreaker.requirements(Category.production,
                ItemStack.with(SolarisContent.item("钢"), 35, SolarisContent.item("铝"), 90,
                        Items.silicon, 15, SolarisContent.item("硅铁"), 20));

        // 地壳石油深井
        oilDerrick = new SolidPump("地壳石油钻井");
        oilDerrick.localizedName = "地壳石油深井";
        oilDerrick.description = "高强度石油钻井，可挖取深层石油。";
        oilDerrick.health = 850;
        oilDerrick.size = 4;
        oilDerrick.floating = true;
        oilDerrick.hasPower = true;
        oilDerrick.hasLiquids = true;
        oilDerrick.squareSprite = false;
        oilDerrick.liquidCapacity = 120;
        oilDerrick.pumpAmount = 0.4f;
        oilDerrick.rotateSpeed = 1.3f;
        oilDerrick.result = Liquids.oil;
        oilDerrick.baseEfficiency = 0.8f;
        oilDerrick.attribute = Attribute.oil;
        oilDerrick.consumePower(2f);
        oilDerrick.consumeItem(SolarisContent.item("铁"), 5);
        oilDerrick.consumeLiquid(Liquids.water, 0.2f);
        oilDerrick.requirements(Category.production,
                ItemStack.with(Items.silicon, 85, Items.graphite, 90, Items.metaglass, 85,
                        SolarisContent.item("钢"), 90, SolarisContent.item("铝"), 100));

        // 墙壁粉碎机
        wallCrusher = new BeamDrill("墙壁粉碎机");
        wallCrusher.description = "放置在矿物前用激光束进行挖掘";
        wallCrusher.size = 2;
        wallCrusher.health = 260;
        wallCrusher.itemCapacity = 20;
        wallCrusher.liquidCapacity = 20;
        wallCrusher.tier = 4;
        wallCrusher.range = 6;
        wallCrusher.fogRadius = 2;
        wallCrusher.drillTime = 120f;
        wallCrusher.hasShadow = true;
        wallCrusher.alwaysUnlocked = true;
        wallCrusher.hasPower = true;
        wallCrusher.consumePower(0.4f);
        wallCrusher.consumeLiquid(Liquids.hydrogen, 0.03333333f).boost().optional(true, false);
        wallCrusher.requirements(Category.production,
                ItemStack.with(Items.graphite, 10, SolarisContent.item("铁"), 20));

        // 综合开采设备（模块化钻头主机）
        comprehensiveDrill = new ComprehensiveDrill("综合开采设备", "综合开采", 5, 5f);
        comprehensiveDrill.description = "可附属多种模块以实现不同功能";
        comprehensiveDrill.health = 900;
        comprehensiveDrill.size = 4;
        comprehensiveDrill.hasPower = true;
        comprehensiveDrill.hasLiquids = true;
        comprehensiveDrill.hasItems = true;
        comprehensiveDrill.itemCapacity = 30;
        comprehensiveDrill.squareSprite = false;
        comprehensiveDrill.tier = 6;
        comprehensiveDrill.drillTime = 240f;
        comprehensiveDrill.hardnessDrillMultiplier = 20f;
        comprehensiveDrill.liquidBoostIntensity = 1.6f;
        comprehensiveDrill.consumeLiquid(Liquids.water, 0.2f).boost().optional(true, false);
        comprehensiveDrill.requirements(Category.production,
                ItemStack.with(SolarisContent.item("钢"), 120, Items.tungsten, 40,
                        Items.graphite, 100, Items.copper, 120));
        research(comprehensiveDrill, "pneumatic-drill");

        // 勘探模块
        explorationModule = new DrillModule("勘探模块", "综合开采", 2, 2f, 0f, 0f, true, 0f, 1f, false, 1);
        explorationModule.description = "消耗硅铁进行地质勘探，将钻井的矿物判定范围向外扩张";
        explorationModule.health = 800;
        explorationModule.size = 3;
        explorationModule.hasPower = false;
        explorationModule.hasItems = true;
        explorationModule.hasLiquids = false;
        explorationModule.craftTime = 300f;
        explorationModule.craftEffect = Fx.formsmoke;
        explorationModule.canOverdrive = false;
        explorationModule.consumeItem(SolarisContent.item("铁"), 3);
        explorationModule.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawRegion("-rotator", 4f, true),
                new DrawDefault());
        explorationModule.requirements(Category.production,
                ItemStack.with(SolarisContent.item("钢"), 90, Items.tungsten, 40,
                        SolarisContent.item("铝"), 60, SolarisContent.item("塑料"), 20));
        research(explorationModule, "综合开采设备");

        // 增压模块
        pressurizeModule = new DrillModule("增压模块", "综合开采", 2, 0f, 0.2f, 0.2f, false, 0f, 1f, false, 0);
        pressurizeModule.description = " ";
        pressurizeModule.health = 600;
        pressurizeModule.size = 2;
        pressurizeModule.hasPower = false;
        pressurizeModule.hasItems = false;
        pressurizeModule.hasLiquids = false;
        pressurizeModule.craftTime = 60f;
        pressurizeModule.craftEffect = Fx.formsmoke;
        pressurizeModule.canOverdrive = false;
        pressurizeModule.requirements(Category.production,
                ItemStack.with(SolarisContent.item("钢"), 80, Items.tungsten, 20,
                        SolarisContent.item("银"), 60, SolarisContent.item("氮化硅"), 20));
        research(pressurizeModule, "综合开采设备");

        // 强化增压模块
        reinforcedPressurizeModule = new DrillModule("强化增压模块", "综合开采", 2, 0f, 0.5f, 0.4f, false, 0f, 1f, false, 0);
        reinforcedPressurizeModule.description = " ";
        reinforcedPressurizeModule.health = 720;
        reinforcedPressurizeModule.size = 2;
        reinforcedPressurizeModule.hasPower = false;
        reinforcedPressurizeModule.hasItems = false;
        reinforcedPressurizeModule.hasLiquids = false;
        reinforcedPressurizeModule.craftTime = 60f;
        reinforcedPressurizeModule.craftEffect = Fx.formsmoke;
        reinforcedPressurizeModule.canOverdrive = false;
        reinforcedPressurizeModule.requirements(Category.production,
                ItemStack.with(SolarisContent.item("钢"), 100, Items.tungsten, 40,
                        SolarisContent.item("气凝胶"), 80, SolarisContent.item("氮化硅"), 30));
        research(reinforcedPressurizeModule, "增压模块");

        // 复合矿物模块
        compositeOreModule = new DrillModule("复合矿物模块", "综合开采", 1, 0f, 0f, 0f, false, 0f, 1f, true, 0);
        compositeOreModule.description = " ";
        compositeOreModule.health = 600;
        compositeOreModule.size = 2;
        compositeOreModule.hasPower = false;
        compositeOreModule.hasItems = false;
        compositeOreModule.hasLiquids = false;
        compositeOreModule.craftTime = 60f;
        compositeOreModule.craftEffect = Fx.formsmoke;
        compositeOreModule.canOverdrive = false;
        compositeOreModule.requirements(Category.production,
                ItemStack.with(SolarisContent.item("钢"), 80, Items.tungsten, 20,
                        SolarisContent.item("银"), 60, SolarisContent.item("氮化硅"), 20));
        research(compositeOreModule, "综合开采设备");

        // 爆破模块
        blastingModule = new DrillModule("爆破模块", "综合开采", 1, 1f, 0f, 0.6f, true, 0.25f, 2f, false, 0);
        blastingModule.description = " ";
        blastingModule.health = 800;
        blastingModule.size = 2;
        blastingModule.hasPower = false;
        blastingModule.hasItems = true;
        blastingModule.hasLiquids = false;
        blastingModule.craftTime = 300f;
        blastingModule.craftEffect = Fx.formsmoke;
        blastingModule.canOverdrive = false;
        blastingModule.consumeItem(SolarisContent.item("硝基炸药"), 2);
        blastingModule.requirements(Category.production,
                ItemStack.with(SolarisContent.item("钢"), 90, Items.tungsten, 40,
                        SolarisContent.item("硅铁"), 60, Items.copper, 20));
        research(blastingModule, "综合开采设备");

        // 酸浸模块
        acidLeachingModule = new DrillModule("酸浸模块", "综合开采", 1, 2.5f, 0f, 0.6f, true, 0f, 1f, false, 0);
        acidLeachingModule.description = "消耗硫酸浸洗矿物，提升钻井速率并增加能耗";
        acidLeachingModule.health = 800;
        acidLeachingModule.size = 2;
        acidLeachingModule.hasPower = false;
        acidLeachingModule.hasItems = false;
        acidLeachingModule.hasLiquids = true;
        acidLeachingModule.liquidCapacity = 20f;
        acidLeachingModule.craftTime = 300f;
        acidLeachingModule.craftEffect = Fx.formsmoke;
        acidLeachingModule.canOverdrive = false;
        acidLeachingModule.consumeLiquid(SolarisContent.liquid("硫酸"), 0.5f);
        acidLeachingModule.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawLiquidTile(SolarisContent.liquid("硫酸")),
                new DrawRegion("-top"));
        acidLeachingModule.requirements(Category.production,
                ItemStack.with(SolarisContent.item("钢"), 90, Items.tungsten, 40,
                        SolarisContent.item("硅铁"), 60, Items.copper, 20));
        research(acidLeachingModule, "综合开采设备");
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
