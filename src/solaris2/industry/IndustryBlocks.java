package solaris2.industry;

import arc.graphics.Color;
import arc.math.Interp;
import arc.struct.Seq;
import mindustry.content.*;
import mindustry.entities.effect.ParticleEffect;
import mindustry.entities.part.DrawPart.PartMove;
import mindustry.entities.part.DrawPart.PartProgress;
import mindustry.entities.part.RegionPart;
import mindustry.gen.Sounds;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.HeatCrafter;
import mindustry.world.draw.*;
import solaris2.SolarisContent;
import solaris2.support.SupportBlocks;

import static mindustry.Vars.content;

public class IndustryBlocks{

    public static Block industrialArm, timeDilator;
    public static GenericCrafter cryofluidMixer, plasticPress, atmosphericLiquefier, smallSiliconSmelter,
            fluidizedBed, coalCompressor, coalLiquefier, vacuumPyrolyzer, oilFractionator,
            magneticSteelSmelter, industrialSiliconFurnace, ferrosiliconFurnace, powderWorkshop, explosiveWorkshop;
    public static HeatCrafter aerogelCondenser, heatSiliconSmelter, glassKiln;
    public static GenericCrafter electrolyzer;
    public static AlloyWeaver alloyWeaver;

    public static void load(){
        industrialArm = new IndustrialArm("工业机械臂");
        industrialArm.description = "机械臂传送带：点击底座框选范围内的容器，第1次点击设为提取端（蓝色），再点一次设为放置端（黄色），第三次点击取消。双臂逆向运动学自动伸缩，从提取端抓取物品送往放置端。";
        industrialArm.health = 160;
        industrialArm.size = 2;
        industrialArm.requirements(Category.distribution,
                ItemStack.with(SolarisContent.item("钢"), 60, SolarisContent.item("氮化硅"), 40,
                        Items.tungsten, 20, Items.copper, 40));

        timeDilator = new TimeDilator("时间迟滞器");
        timeDilator.description = "全图只能建造一座。\n启动后，将本轮次所有未被消灭的敌人迟滞，\n与下一波的敌人一同出现在生成区。\n每 6 波可使用一次。";
        timeDilator.health = 2400;
        timeDilator.size = 3;
        timeDilator.buildVisibility = mindustry.world.meta.BuildVisibility.shown;
        timeDilator.requirements(Category.effect,
                ItemStack.with(Items.copper, 300, Items.lead, 250,
                        Items.titanium, 250, Items.silicon, 300));

        // 冷冻液混合器
        cryofluidMixer = new GenericCrafter("冷冻液混合器");
        cryofluidMixer.description = "混合制造冷冻液";
        cryofluidMixer.health = 660;
        cryofluidMixer.size = 4;
        cryofluidMixer.itemCapacity = 14;
        cryofluidMixer.liquidCapacity = 180f;
        cryofluidMixer.craftTime = 65f;
        cryofluidMixer.outputLiquid = new LiquidStack(Liquids.cryofluid, 0.7f);
        cryofluidMixer.outputsLiquid = true;
        cryofluidMixer.updateEffect = Fx.freezing;
        cryofluidMixer.hasPower = true;
        cryofluidMixer.consumePower(3.5f);
        cryofluidMixer.consumeLiquids(LiquidStack.with(
                Liquids.water, 0.3f,
                SolarisContent.liquid("醇"), 0.4f,
                SolarisContent.liquid("液氮"), 0.2f));
        cryofluidMixer.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawLiquidTile(Liquids.water, 1f),
                new DrawBubbles(Color.valueOf("7693e3")){{
                    sides = 8;
                    recurrence = 3f;
                    spread = 12f;
                    amount = 15;
                }},
                new DrawLiquidTile(Liquids.cryofluid, 1f),
                new DrawBubbles(Color.valueOf("73afc6")){{
                    sides = 8;
                    recurrence = 3f;
                    spread = 12f;
                    amount = 5;
                }},
                new DrawRegion("-rotator1"){{
                    rotateSpeed = -2.5f;
                }},
                new DrawRegion("-rotator1"){{
                    rotateSpeed = 2.5f;
                }},
                new DrawDefault());
        cryofluidMixer.requirements(Category.crafting,
                ItemStack.with(Items.lead, 70, SolarisContent.item("钢"), 60, Items.metaglass, 40,
                        Items.silicon, 40, Items.graphite, 30));
        research(cryofluidMixer, "cryofluid-mixer");

        // 塑料压制机
        plasticPress = new GenericCrafter("塑料压制机");
        plasticPress.outputItem = new ItemStack(SolarisContent.item("塑料"), 2);
        plasticPress.size = 4;
        plasticPress.health = 360;
        plasticPress.craftTime = 60f;
        plasticPress.itemCapacity = 20;
        plasticPress.liquidCapacity = 20f;
        plasticPress.squareSprite = true;
        plasticPress.craftEffect = Fx.formsmoke;
        plasticPress.updateEffect = Fx.plasticburn;
        plasticPress.hasPower = true;
        plasticPress.consumePower(5.5f);
        plasticPress.consumeLiquids(LiquidStack.with(
                SolarisContent.liquid("烯烃"), 0.2f,
                SolarisContent.liquid("芳香烃"), 0.1f));
        plasticPress.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawPistons(){{
                    sinMag = 1f;
                }},
                new DrawDefault());
        plasticPress.requirements(Category.crafting,
                ItemStack.with(Items.silicon, 40, SolarisContent.item("氮化硅"), 60,
                        SolarisContent.item("铁"), 80, SolarisContent.item("铝"), 100));
        research(plasticPress, "phase-weaver");

        // 大气液化器
        atmosphericLiquefier = new GenericCrafter("大气液化器");
        atmosphericLiquefier.description = "液化态空气获得不同成分";
        atmosphericLiquefier.itemCapacity = 50;
        atmosphericLiquefier.health = 1000;
        atmosphericLiquefier.size = 3;
        atmosphericLiquefier.hasItems = false;
        atmosphericLiquefier.craftTime = 60f;
        atmosphericLiquefier.craftEffect = Fx.steam;
        atmosphericLiquefier.updateEffect = Fx.plasticburn;
        atmosphericLiquefier.ambientSound = Sounds.loopExtract;
        atmosphericLiquefier.ambientSoundVolume = 0.3f;
        atmosphericLiquefier.liquidCapacity = 60f;
        atmosphericLiquefier.outputLiquids = LiquidStack.with(
                SolarisContent.liquid("液氧"), 0.1f,
                SolarisContent.liquid("液氮"), 0.26666666666f);
        atmosphericLiquefier.outputsLiquid = true;
        atmosphericLiquefier.liquidOutputDirections = new int[]{1, 3};
        atmosphericLiquefier.rotate = true;
        atmosphericLiquefier.rotateDraw = false;
        atmosphericLiquefier.hasPower = true;
        atmosphericLiquefier.consumePower(2f);
        atmosphericLiquefier.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawLiquidTile(SolarisContent.liquid("液氮"), 4f),
                new DrawParticles(){{
                    color = Color.valueOf("DCCAF4FF");
                    alpha = 0.6f;
                    particles = 10;
                    particleSize = 4f;
                    particleRad = 12f;
                    particleLife = 140f;
                }},
                new DrawDefault());
        atmosphericLiquefier.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("铝"), 75, Items.copper, 100,
                        Items.silicon, 80, SolarisContent.item("钢"), 50));
        research(atmosphericLiquefier, "melter");

        // 小型硅炉
        smallSiliconSmelter = new GenericCrafter("小型硅炉");
        smallSiliconSmelter.description = "将石墨和煤炭熔炼成硅";
        smallSiliconSmelter.size = 2;
        smallSiliconSmelter.itemCapacity = 10;
        smallSiliconSmelter.craftTime = 80f;
        smallSiliconSmelter.outputItem = new ItemStack(Items.silicon, 1);
        smallSiliconSmelter.craftEffect = Fx.smeltsmoke;
        smallSiliconSmelter.updateEffect = Fx.fuelburn;
        smallSiliconSmelter.hasPower = true;
        smallSiliconSmelter.consumePower(0.55f);
        smallSiliconSmelter.consumeItems(ItemStack.with(Items.sand, 2, Items.graphite, 1));
        smallSiliconSmelter.drawer = new DrawMulti(new DrawDefault(), new DrawArcSmelt());
        smallSiliconSmelter.requirements(Category.crafting,
                ItemStack.with(Items.copper, 30, Items.lead, 25));

        // 气凝机
        aerogelCondenser = new HeatCrafter("气凝机");
        aerogelCondenser.outputItem = new ItemStack(SolarisContent.item("气凝胶"), 1);
        aerogelCondenser.itemCapacity = 10;
        aerogelCondenser.size = 2;
        aerogelCondenser.craftTime = 240f;
        aerogelCondenser.heatRequirement = 2f;
        aerogelCondenser.updateEffect = Fx.none;
        aerogelCondenser.craftEffect = new ParticleEffect(){{
            lifetime = 180f;
            colorFrom = Color.valueOf("ffffff99");
            colorTo = Color.valueOf("ffffff99");
            particles = 3;
            cone = 360f;
            baseLength = 40f;
            length = -40f;
            sizeFrom = 2f;
            sizeTo = 1.5f;
        }};
        aerogelCondenser.hasPower = true;
        aerogelCondenser.consumePower(10f);
        aerogelCondenser.consumeItem(Items.silicon, 1);
        aerogelCondenser.requirements(Category.crafting,
                ItemStack.with(Items.tungsten, 120, SolarisContent.item("铝"), 40,
                        Items.silicon, 50, Items.copper, 110));
        aerogelCondenser.ambientSound = Sounds.loopMachine;
        aerogelCondenser.ambientSoundVolume = 0.5f;
        research(aerogelCondenser, "blast-mixer");

        // 流化床
        fluidizedBed = new GenericCrafter("流化床");
        fluidizedBed.description = "将原油以极低效的裂解方式转换为石油";
        fluidizedBed.outputItems = new ItemStack[]{new ItemStack(Items.pyratite, 1)};
        fluidizedBed.outputLiquids = LiquidStack.with(SolarisContent.liquid("石油"), 0.5f);
        fluidizedBed.outputsLiquid = true;
        fluidizedBed.health = 700;
        fluidizedBed.size = 3;
        fluidizedBed.itemCapacity = 20;
        fluidizedBed.liquidCapacity = 120f;
        fluidizedBed.craftTime = 120f;
        fluidizedBed.craftEffect = Fx.smeltsmoke;
        fluidizedBed.squareSprite = true;
        fluidizedBed.hasPower = true;
        fluidizedBed.consumePower(2.4f);
        fluidizedBed.consumeLiquid(Liquids.oil, 1f);
        fluidizedBed.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawLiquidTile(SolarisContent.liquid("石油"), 4f),
                new DrawDefault());
        fluidizedBed.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("钢"), 55, Items.graphite, 20, Items.silicon, 20));

        // 热能硅炉
        heatSiliconSmelter = new HeatCrafter("热能硅炉");
        heatSiliconSmelter.description = "高效熔炼硅";
        heatSiliconSmelter.health = 360;
        heatSiliconSmelter.size = 4;
        heatSiliconSmelter.outputItem = new ItemStack(Items.silicon, 4);
        heatSiliconSmelter.itemCapacity = 50;
        heatSiliconSmelter.craftTime = 60f;
        heatSiliconSmelter.heatRequirement = 2f;
        heatSiliconSmelter.updateEffect = Fx.none;
        heatSiliconSmelter.craftEffect = Fx.none;
        heatSiliconSmelter.squareSprite = true;
        heatSiliconSmelter.hasPower = true;
        heatSiliconSmelter.consumePower(4.1f);
        heatSiliconSmelter.consumeItems(ItemStack.with(Items.graphite, 1, Items.sand, 4));
        heatSiliconSmelter.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawHeatInput(),
                new DrawArcSmelt(),
                new DrawDefault());
        heatSiliconSmelter.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("钢"), 105, Items.graphite, 80,
                        Items.metaglass, 65, SolarisContent.item("硅铁"), 90));
        research(heatSiliconSmelter, "spore-press");

        // 煤炭压缩机
        coalCompressor = new GenericCrafter("煤炭压缩机");
        coalCompressor.description = "以高压活塞将煤炭反复挤压，压制成致密的石墨。";
        coalCompressor.size = 2;
        coalCompressor.health = 180;
        coalCompressor.craftTime = 180f;
        coalCompressor.itemCapacity = 20;
        coalCompressor.craftEffect = Fx.smeltsmoke;
        coalCompressor.outputItem = new ItemStack(Items.graphite, 2);
        coalCompressor.consumeItems(ItemStack.with(Items.coal, 3));
        coalCompressor.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawBlockParts(){{
                    parts = Seq.with(
                            pistonPart("索拉里斯2.0-煤炭压缩机-piston-lu", 2.25f, 2.25f),
                            pistonPart("索拉里斯2.0-煤炭压缩机-piston-ru", 2.25f, -2.25f),
                            pistonPart("索拉里斯2.0-煤炭压缩机-piston-rd", -2.25f, -2.25f),
                            pistonPart("索拉里斯2.0-煤炭压缩机-piston-ld", -2.25f, 2.25f));
                }},
                new DrawGlowRegion("-glow"),
                new DrawRegion("-top"));
        coalCompressor.requirements(Category.crafting,
                ItemStack.with(Items.copper, 40, Items.lead, 30, Items.graphite, 15));

        // 煤炭液化机
        coalLiquefier = new GenericCrafter("煤炭液化机");
        coalLiquefier.localizedName = "煤炭液化厂";
        coalLiquefier.description = "通过加氢将煤炭液化成原油。";
        coalLiquefier.health = 120;
        coalLiquefier.outputLiquid = new LiquidStack(Liquids.oil, 0.1f);
        coalLiquefier.outputsLiquid = true;
        coalLiquefier.itemCapacity = 10;
        coalLiquefier.size = 2;
        coalLiquefier.craftTime = 60f;
        coalLiquefier.updateEffect = Fx.none;
        coalLiquefier.craftEffect = Fx.smeltsmoke;
        coalLiquefier.hasPower = true;
        coalLiquefier.consumePower(2.2f);
        coalLiquefier.consumeItem(Items.coal, 1);
        coalLiquefier.consumeLiquid(Liquids.hydrogen, 0.1333333f);
        coalLiquefier.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("银"), 25, SolarisContent.item("铁"), 80, Items.graphite, 15));
        research(coalLiquefier, "煤炭分解机");

        // 玻璃窑炉
        glassKiln = new HeatCrafter("玻璃窑炉");
        glassKiln.localizedName = "特化窑炉";
        glassKiln.description = "快速炼制钢化玻璃，拒绝玻璃不足。";
        glassKiln.outputItems = new ItemStack[]{new ItemStack(Items.metaglass, 4)};
        glassKiln.health = 400;
        glassKiln.size = 3;
        glassKiln.itemCapacity = 20;
        glassKiln.craftTime = 192f;
        glassKiln.heatRequirement = 4f;
        glassKiln.updateEffect = Fx.none;
        glassKiln.hasPower = true;
        glassKiln.consumePower(2.4f);
        glassKiln.consumeItems(ItemStack.with(Items.sand, 5));
        glassKiln.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawFlame(),
                new DrawGlowRegion("-glow"),
                new DrawHeatInput());
        glassKiln.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("钢"), 55, Items.graphite, 20,
                        Items.silicon, 20, Items.copper, 70));
        research(glassKiln, "kiln");

        // 真空热解器
        vacuumPyrolyzer = new GenericCrafter("真空热解器");
        vacuumPyrolyzer.localizedName = "真空热解炉";
        vacuumPyrolyzer.description = "在真空下将煤炭热解成石墨，同时得到副产";
        vacuumPyrolyzer.itemCapacity = 50;
        vacuumPyrolyzer.health = 900;
        vacuumPyrolyzer.size = 3;
        vacuumPyrolyzer.craftTime = 360f;
        vacuumPyrolyzer.craftEffect = Fx.formsmoke;
        vacuumPyrolyzer.updateEffect = Fx.plasticburn;
        vacuumPyrolyzer.liquidCapacity = 120f;
        vacuumPyrolyzer.outputLiquids = LiquidStack.with(SolarisContent.liquid("芳香烃"), 0.25f);
        vacuumPyrolyzer.outputItems = new ItemStack[]{
                new ItemStack(Items.graphite, 5),
                new ItemStack(Items.pyratite, 2)
        };
        vacuumPyrolyzer.outputsLiquid = true;
        vacuumPyrolyzer.hasPower = true;
        vacuumPyrolyzer.consumePower(6f);
        vacuumPyrolyzer.consumeItems(ItemStack.with(Items.coal, 5));
        vacuumPyrolyzer.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawCrucibleFlame(),
                new DrawDefault());
        vacuumPyrolyzer.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("铝"), 1000, Items.tungsten, 250,
                        SolarisContent.item("铱"), 100, SolarisContent.item("塑料"), 600));
        research(vacuumPyrolyzer, "焦化塔");

        // 石油分馏塔
        oilFractionator = new GenericCrafter("石油分馏塔");
        oilFractionator.description = "从原油中分馏出不同的成分";
        oilFractionator.itemCapacity = 50;
        oilFractionator.health = 1000;
        oilFractionator.size = 5;
        oilFractionator.craftTime = 80f;
        oilFractionator.updateEffect = Fx.steam;
        oilFractionator.craftEffect = Fx.smeltsmoke;
        oilFractionator.liquidCapacity = 120f;
        oilFractionator.liquidOutputDirections = new int[]{1, 3, 2, 4};
        oilFractionator.rotate = true;
        oilFractionator.rotateDraw = false;
        oilFractionator.outputLiquids = LiquidStack.with(
                SolarisContent.liquid("芳香烃"), 1f,
                SolarisContent.liquid("烷烃"), 0.25f,
                SolarisContent.liquid("石油"), 0.5f,
                SolarisContent.liquid("燃气"), 0.25f);
        oilFractionator.outputItem = new ItemStack(Items.coal, 8);
        oilFractionator.outputsLiquid = true;
        oilFractionator.hasPower = true;
        oilFractionator.consumePower(10f);
        oilFractionator.consumeLiquid(Liquids.oil, 1f);
        oilFractionator.requirements(Category.crafting,
                ItemStack.with(Items.copper, 200, Items.tungsten, 150,
                        SolarisContent.item("铝"), 200, Items.silicon, 120));

        // 磁钢冶炼炉
        magneticSteelSmelter = new GenericCrafter("磁钢冶炼炉");
        magneticSteelSmelter.description = "用于冶炼磁钢的设备";
        magneticSteelSmelter.size = 3;
        magneticSteelSmelter.health = 260;
        magneticSteelSmelter.craftTime = 90f;
        magneticSteelSmelter.itemCapacity = 30;
        magneticSteelSmelter.craftEffect = Fx.none;
        magneticSteelSmelter.ambientSound = Sounds.loopSmelter;
        magneticSteelSmelter.ambientSoundVolume = 0.12f;
        magneticSteelSmelter.outputItem = new ItemStack(SolarisContent.item("磁钢"), 3);
        magneticSteelSmelter.hasPower = true;
        magneticSteelSmelter.consumePower(3.5f);
        magneticSteelSmelter.consumeItems(ItemStack.with(
                SolarisContent.item("氮化硅"), 2, SolarisContent.item("钢"), 2, SolarisContent.item("钼"), 1));
        magneticSteelSmelter.squareSprite = true;
        magneticSteelSmelter.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawArcSmelt(),
                new DrawDefault());
        magneticSteelSmelter.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("钢"), 170, SolarisContent.item("氮化硅"), 80,
                        SolarisContent.item("铝"), 100, Items.copper, 50, SolarisContent.item("硅铁"), 80));

        // 重硅合炉
        industrialSiliconFurnace = new GenericCrafter("重硅合炉");
        industrialSiliconFurnace.localizedName = "工业硅炉";
        industrialSiliconFurnace.description = "电解水还原钢化玻璃，产出硅并副产氧气";
        industrialSiliconFurnace.size = 3;
        industrialSiliconFurnace.health = 765;
        industrialSiliconFurnace.craftTime = 75f;
        industrialSiliconFurnace.itemCapacity = 35;
        industrialSiliconFurnace.liquidCapacity = 36f;
        industrialSiliconFurnace.craftEffect = Fx.none;
        industrialSiliconFurnace.ambientSound = Sounds.loopSmelter;
        industrialSiliconFurnace.ambientSoundVolume = 0.12f;
        industrialSiliconFurnace.squareSprite = false;
        industrialSiliconFurnace.outputItem = new ItemStack(Items.silicon, 7);
        industrialSiliconFurnace.outputLiquid = new LiquidStack(SolarisContent.liquid("氧气"), 0.1f);
        industrialSiliconFurnace.outputsLiquid = true;
        industrialSiliconFurnace.hasPower = true;
        industrialSiliconFurnace.consumePower(6f);
        industrialSiliconFurnace.consumeItem(Items.metaglass, 3);
        industrialSiliconFurnace.consumeLiquid(Liquids.water, 0.2f);
        industrialSiliconFurnace.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawArcSmelt(),
                new DrawDefault());
        industrialSiliconFurnace.requirements(Category.crafting,
                ItemStack.with(Items.metaglass, 160, SolarisContent.item("硅铁"), 80,
                        SolarisContent.item("钢"), 130, SolarisContent.item("铝"), 100));

        // 重硅铁熔炉
        ferrosiliconFurnace = new GenericCrafter("重硅铁熔炉");
        ferrosiliconFurnace.description = "用于冶炼硅铁的设备";
        ferrosiliconFurnace.size = 3;
        ferrosiliconFurnace.health = 960;
        ferrosiliconFurnace.craftTime = 100f;
        ferrosiliconFurnace.itemCapacity = 40;
        ferrosiliconFurnace.craftEffect = Fx.none;
        ferrosiliconFurnace.ambientSound = Sounds.loopSmelter;
        ferrosiliconFurnace.ambientSoundVolume = 0.12f;
        ferrosiliconFurnace.squareSprite = true;
        ferrosiliconFurnace.outputItem = new ItemStack(SolarisContent.item("硅铁"), 3);
        ferrosiliconFurnace.hasPower = true;
        ferrosiliconFurnace.consumePower(4.8f);
        ferrosiliconFurnace.consumeItems(ItemStack.with(Items.silicon, 2, SolarisContent.item("铁"), 2));
        ferrosiliconFurnace.consumeLiquid(Liquids.hydrogen, 0.4f);
        ferrosiliconFurnace.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawLiquidTile(Liquids.hydrogen, 4f),
                new DrawParticles(){{
                    color = Color.valueOf("BBC4F7FF");
                    alpha = 0.6f;
                    particles = 10;
                    particleSize = 4f;
                    particleRad = 12f;
                    particleLife = 140f;
                }},
                new DrawArcSmelt(),
                new DrawDefault());
        ferrosiliconFurnace.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("钢"), 170, Items.graphite, 80,
                        SolarisContent.item("铝"), 120, Items.tungsten, 85));

        // 火药工坊
        powderWorkshop = new GenericCrafter("火药工坊");
        powderWorkshop.description = "将煤炭、硝石与火焰素混合研磨，产出粗火药。";
        powderWorkshop.size = 1;
        powderWorkshop.health = 120;
        powderWorkshop.craftTime = 270f;
        powderWorkshop.itemCapacity = 20;
        powderWorkshop.craftEffect = Fx.smoke;
        powderWorkshop.outputItem = new ItemStack(SolarisContent.item("粗火药"), 3);
        powderWorkshop.consumeItems(ItemStack.with(
                Items.coal, 1, SolarisContent.item("硝石"), 2, Items.pyratite, 1));
        powderWorkshop.requirements(Category.crafting,
                ItemStack.with(Items.copper, 40, SolarisContent.item("铁"), 30));

        // 炸药工坊
        explosiveWorkshop = new GenericCrafter("炸药工坊");
        explosiveWorkshop.description = "将多种化工原料混合生产硝基炸药";
        explosiveWorkshop.itemCapacity = 20;
        explosiveWorkshop.health = 1000;
        explosiveWorkshop.size = 3;
        explosiveWorkshop.craftTime = 120f;
        explosiveWorkshop.craftEffect = Fx.neoplasiaSmoke;
        explosiveWorkshop.updateEffect = Fx.plasticburn;
        explosiveWorkshop.liquidCapacity = 80f;
        explosiveWorkshop.outputItem = new ItemStack(SolarisContent.item("硝基炸药"), 2);
        explosiveWorkshop.hasPower = true;
        explosiveWorkshop.consumePower(4f);
        explosiveWorkshop.consumeLiquids(LiquidStack.with(
                SolarisContent.liquid("芳香烃"), 0.5f,
                SolarisContent.liquid("硝酸"), 0.5f,
                SolarisContent.liquid("硫酸"), 0.25f));
        explosiveWorkshop.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("塑料"), 600, Items.tungsten, 100,
                        SolarisContent.item("氮化硅"), 300, Items.metaglass, 150,
                        SolarisContent.item("铁"), 100));

        // 电解机
        electrolyzer = new GenericCrafter("电解机");
        electrolyzer.health = 600;
        electrolyzer.size = 3;
        electrolyzer.hasPower = true;
        electrolyzer.hasLiquids = true;
        electrolyzer.rotate = true;
        electrolyzer.craftTime = 120f;
        electrolyzer.liquidCapacity = 50f;
        electrolyzer.updateEffect = Fx.none;
        electrolyzer.canOverdrive = false;
        electrolyzer.liquidOutputDirections = new int[]{1, 3};
        electrolyzer.outputLiquids = LiquidStack.with(SolarisContent.liquid("氧气"), 0.05f, Liquids.hydrogen, 0.15f);
        electrolyzer.outputsLiquid = true;
        electrolyzer.consumePower(1f);
        electrolyzer.consumeLiquid(Liquids.water, 0.2f);
        electrolyzer.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawLiquidTile(Liquids.water, 4f),
                new DrawBubbles(Color.valueOf("7693e3")){{
                    sides = 8;
                    recurrence = 3f;
                    spread = 4f;
                    amount = 15;
                }},
                new DrawRegion(""),
                new PrefixedLiquidOutputs());
        electrolyzer.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("铁"), 120, Items.graphite, 90,
                        SolarisContent.item("氮化硅"), 20, Items.metaglass, 20,
                        SolarisContent.item("铱"), 120));
        electrolyzer.buildVisibility = mindustry.world.meta.BuildVisibility.shown;

        // 合金编织器
        alloyWeaver = new AlloyWeaver("合金编织器");

        // 辅助类方块
        SupportBlocks.load();
    }

    private static RegionPart pistonPart(String name, float x, float y){
        RegionPart part = new RegionPart(name);
        part.outline = false;
        part.clampProgress = false;
        part.rotation = 90f;
        part.moves.add(new PartMove(
                PartProgress.time.loop(80f).mul(PartProgress.warmup)
                        .curve(Interp.circleOut).curve(Interp.slope).curve(Interp.pow2Out).curve(Interp.sine),
                x, y, 0f));
        return part;
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
