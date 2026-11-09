package solaris2.power;

import arc.graphics.Color;
import mindustry.content.*;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.effect.*;
import mindustry.gen.Sounds;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.blocks.power.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import solaris2.SolarisContent;

/** 电力类建筑的统一入口。 */
public class PowerBlocks{

    public static SingleUseBattery singleUseBattery;
    public static PowerGrid powerGrid;
    public static PowerGrid advancedPowerGrid;
    public static PowerGrid ultimatePowerGrid;
    public static WindGenerator windGenerator;
    public static WeatherShield weatherShield;
    public static EmergencyGenerator emergencyGenerator;
    public static GridBattery gridBattery;
    public static SteamTurbine steamTurbine;
    public static SolarGenerator solarPanel;
    public static ConsumeGenerator steamEngine, flareReactor, decayReactor;
    public static HandCrankGenerator handCrankGenerator;
    public static FireGenerator fireGenerator;

    public static void load(){
        // 一次性电池
        singleUseBattery = new SingleUseBattery();
        singleUseBattery.description = "建造即满电，[red]无法被电网充电[]，只能放电\n用完后需拆除重建";
        singleUseBattery.health = 160;
        singleUseBattery.size = 1;
        singleUseBattery.consumePowerBuffered(1000f);
        singleUseBattery.requirements(Category.power,
                ItemStack.with(Items.copper, 25, Items.lead, 25));
        research(singleUseBattery, "battery",
                ItemStack.with(SolarisContent.item("铁"), 25, Items.graphite, 25));

        // 特斯拉供电网
        powerGrid = new PowerGrid("供电网", 300, PowerGrid.GRID_LIGHTNING_COLOR_NORMAL, 0.01f);
        powerGrid.localizedName = "特斯拉供电网";
        powerGrid.description = "可无视距离数量超距离供电\n[yellow]每个容量 4500 电力/秒，可叠加[]\n"
                + "[cyan]3% 线路损耗 + 0.5% 接入损耗[]\n[red]过载：总产能超过上限时电网摧毁[]";
        powerGrid.requirements(Category.power,
                ItemStack.with(Items.copper, 200, Items.lead, 100, Items.silicon, 150, Items.titanium, 100));
        research(powerGrid, "combustion-generator",
                ItemStack.with(Items.copper, 300, Items.silicon, 200));

        // 高级特斯拉供电网
        advancedPowerGrid = new PowerGrid("高级供电网", 500, PowerGrid.GRID_LIGHTNING_COLOR_ADVANCED, 0.015f);
        advancedPowerGrid.localizedName = "高级特斯拉供电网";
        advancedPowerGrid.description = "升级版供电网，采用优质导电材料\n[yellow]每个容量 9500 电力/秒，可叠加[]\n"
                + "[cyan]1% 线路损耗 + 0.15% 接入损耗[]\n[red]过载：总产能超过上限时电网摧毁[]";
        advancedPowerGrid.requirements(Category.power,
                ItemStack.with(Items.copper, 400, Items.lead, 300, Items.silicon, 300,
                        Items.titanium, 200, Items.thorium, 100, Items.surgeAlloy, 50));
        research(advancedPowerGrid, "供电网",
                ItemStack.with(Items.copper, 600, Items.silicon, 400, Items.titanium, 300, Items.thorium, 200));

        // 终极特斯拉供电网
        ultimatePowerGrid = new PowerGrid("终极电网", 1000, PowerGrid.GRID_LIGHTNING_COLOR_ULTIMATE, 0.02f);
        ultimatePowerGrid.localizedName = "终极特斯拉供电网";
        ultimatePowerGrid.description = "终极超导电网\n[yellow]每个容量 30000 电力/秒，可叠加[]\n"
                + "[gold]零损耗[]\n[red]每个终极电网预消耗 1000 电力/秒[]\n[red]过载：总产能超过上限时电网摧毁[]";
        ultimatePowerGrid.size = 3;
        ultimatePowerGrid.requirements(Category.power,
                ItemStack.with(Items.copper, 800, Items.lead, 600, Items.silicon, 500, Items.titanium, 400,
                        Items.thorium, 300, Items.surgeAlloy, 200, Items.phaseFabric, 100));
        research(ultimatePowerGrid, "高级供电网",
                ItemStack.with(SolarisContent.item("磁钢"), 100, Items.silicon, 100,
                        SolarisContent.item("银"), 150, Items.tungsten, 240));

        // 风力发电机
        windGenerator = new WindGenerator();
        windGenerator.description = "使用风力进行发电。风会被一定范围内的固体方块阻挡。发电效率与阻挡量成负相关。";
        windGenerator.size = 3;
        windGenerator.health = 1500;
        windGenerator.powerProduction = 6f;
        windGenerator.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawRegion("-rot"){{
                    rotateSpeed = 4f;
                    spinSprite = true;
                }});
        windGenerator.requirements(Category.power,
                ItemStack.with(SolarisContent.item("铁"), 100, SolarisContent.item("铝"), 90, Items.silicon, 85));
        research(windGenerator, "combustion-generator");

        weatherShield = new WeatherShield();
        weatherShield.description = "在建筑附近建立气象立场，保护范围内的建筑与单位。\n任何气象伤害落在护罩内都会被吸收。\n";
        weatherShield.health = 1200;
        weatherShield.size = 3;
        weatherShield.buildVisibility = mindustry.world.meta.BuildVisibility.shown;
        weatherShield.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion("-square"){{
                    layer = 100f;
                    rotateSpeed = 0.5f;
                    color = Color.white;
                    alpha = 1f;
                }},
                new DrawGlowRegion("-square"){{
                    layer = 100f;
                    rotateSpeed = -0.5f;
                    color = Color.white;
                    alpha = 1f;
                }});
        weatherShield.requirements(Category.effect,
                ItemStack.with(Items.copper, 150, Items.lead, 150,
                        Items.titanium, 100, Items.silicon, 150));

        // 应急发电机
        emergencyGenerator = new EmergencyGenerator();
        emergencyGenerator.description = "燃烧可燃流体发电。利用率较低。一次性灌满燃料，缺电时自动启动，启动期间无法进料，燃料耗尽后停机，可重新灌入燃料";
        emergencyGenerator.details = "什么！这不是发电机，这是一个压缩电池。它遇可燃流体变大变高";
        emergencyGenerator.size = 4;
        emergencyGenerator.health = 800;
        emergencyGenerator.conductivePower = true;
        emergencyGenerator.hasPower = true;
        emergencyGenerator.outputsPower = true;
        emergencyGenerator.hasLiquids = true;
        emergencyGenerator.hasItems = false;
        emergencyGenerator.squareSprite = false;
        emergencyGenerator.liquidCapacity = 1500f;
        emergencyGenerator.canOverdrive = false;
        emergencyGenerator.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawCrucibleFlame(),
                new DrawDefault(),
                new DrawGlowRegion(){{
                    alpha = 0.8f;
                    glowScale = 3.53429173f;
                    color = Color.valueOf("FF0000FF");
                }});
        emergencyGenerator.ambientSound = Sounds.loopGlow;
        emergencyGenerator.ambientSoundVolume = 0.04f;
        emergencyGenerator.requirements(Category.power,
                ItemStack.with(Items.silicon, 180, SolarisContent.item("钢"), 100, Items.metaglass, 110,
                        SolarisContent.item("气凝胶"), 90, SolarisContent.item("硅铁"), 180));
        research(emergencyGenerator, "impact-reactor");

        // 电网电池
        gridBattery = new GridBattery();
        gridBattery.description = "可充放电的电网电池，自动接入虚拟电网\n[cyan]容量 5000[]\n[cyan]全地图连接虚拟电网[]";
        gridBattery.health = 160;
        gridBattery.size = 1;
        gridBattery.consumePowerBuffered(5000f);
        gridBattery.requirements(Category.power,
                ItemStack.with(Items.copper, 25, Items.lead, 25));
        research(gridBattery, "battery",
                ItemStack.with(SolarisContent.item("铁"), 50, Items.graphite, 50));

        // 蒸汽轮机
        steamTurbine = new SteamTurbine();
        steamTurbine.description = "蒸汽轮机，利用高压蒸汽驱动涡轮发电，蒸汽量越高功率越大，最低20%蒸汽量才能启动，产出水";
        steamTurbine.size = 3;
        steamTurbine.health = 700;
        steamTurbine.hasPower = true;
        steamTurbine.hasLiquids = true;
        steamTurbine.outputsLiquid = true;
        steamTurbine.ambientSound = Sounds.loopSpray;
        steamTurbine.ambientSoundVolume = 0.1f;
        steamTurbine.squareSprite = false;
        steamTurbine.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawWarmupRegion(),
                new DrawLiquidRegion(SolarisContent.liquid("蒸汽")){{
                    suffix = "-liquid";
                }},
                new DrawRegion("-turbine", 30f, false),
                new DrawRegion("-turbine", 30f, false){{
                    rotation = 30f;
                }},
                new DrawRegion("-turbine", 30f, false){{
                    rotation = 60f;
                }},
                new DrawRegion("-top", 30f, true),
                new DrawRegion("-top1"));
        steamTurbine.requirements(Category.power,
                ItemStack.with(Items.thorium, 150, Items.graphite, 300, Items.titanium, 200));
        research(steamTurbine, "燃烧锅炉");

        // 太阳能光伏板
        solarPanel = new SolarGenerator("太阳能光伏板");
        solarPanel.description = "太阳能板，将太阳能转换为电能";
        solarPanel.health = 700;
        solarPanel.size = 3;
        solarPanel.powerProduction = 2f;
        solarPanel.requirements(Category.power,
                ItemStack.with(Items.silicon, 100, SolarisContent.item("铝"), 50,
                        SolarisContent.item("塑料"), 100, SolarisContent.item("氮化硅"), 80));

        // 蒸汽机
        steamEngine = new ConsumeGenerator("蒸汽机");
        steamEngine.description = "原始的能源，通过蒸汽生成电力。";
        steamEngine.size = 2;
        steamEngine.health = 400;
        steamEngine.hasLiquids = true;
        steamEngine.hasItems = false;
        steamEngine.powerProduction = 1.4f;
        steamEngine.liquidCapacity = 20f;
        steamEngine.consume(new ConsumeLiquid(SolarisContent.liquid("蒸汽"), 0.07f));
        steamEngine.drawer = new DrawMulti(
                new DrawPistons(){{
                    sinMag = 1f;
                }},
                new DrawDefault());
        steamEngine.generateEffect = new ParticleEffect(){{
            particles = 5;
            sizeFrom = 3f;
            sizeTo = 0f;
            length = 5f;
            baseLength = 5f;
            lifetime = 50f;
            colorFrom = Color.valueOf("ffffff80");
            colorTo = Color.valueOf("ffffff80");
        }};
        steamEngine.requirements(Category.power,
                ItemStack.with(Items.copper, 50, SolarisContent.item("铁"), 50));
        research(steamEngine, "combustion-generator");

        // 爆燃反应堆
        flareReactor = new ConsumeGenerator("爆燃反应堆");
        flareReactor.description = "将可燃物磨成粉雾化后点燃，利用爆燃冲击波驱动微型对冲反应核心发电。";
        flareReactor.size = 3;
        flareReactor.health = 900;
        flareReactor.hasLiquids = false;
        flareReactor.hasItems = true;
        flareReactor.itemDuration = 45f;
        flareReactor.powerProduction = 11f;
        flareReactor.itemCapacity = 30;
        flareReactor.destroyBullet = new BasicBulletType(){{
            splashDamageRadius = 30f;
            splashDamage = 90f;
            makeFire = true;
        }};
        flareReactor.generateEffect = new MultiEffect(
                new ParticleEffect(){{
                    particles = 20;
                    sizeFrom = 0.6f;
                    sizeTo = 0f;
                    length = -10f;
                    baseLength = 15f;
                    lifetime = 20f;
                    colorFrom = Color.valueOf("E3E3E390");
                    colorTo = Color.valueOf("FFFFFFFF");
                }},
                new WaveEffect(){{
                    lifetime = 50f;
                    sizeFrom = 0f;
                    sizeTo = 30f;
                    colorFrom = Color.valueOf("E3E3E390");
                    colorTo = Color.valueOf("FFFFFFFF");
                    strokeFrom = 3f;
                    strokeTo = 0.2f;
                    sides = 12;
                }});
        flareReactor.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawPlasma(),
                new DrawDefault(),
                new DrawFade(){{
                    scale = 10f;
                    suffix = "-glow";
                }});
        flareReactor.consume(new ConsumeItemFlammable());
        flareReactor.requirements(Category.power,
                ItemStack.with(Items.silicon, 30, Items.graphite, 25,
                        SolarisContent.item("钢"), 50, SolarisContent.item("银"), 50));
        research(flareReactor, "thermal-generator");

        // 衰变反应堆
        decayReactor = new ConsumeGenerator("衰变反应堆");
        decayReactor.description = "利用强放射性物质衰变产生电力";
        decayReactor.size = 3;
        decayReactor.health = 800;
        decayReactor.armor = 4f;
        decayReactor.squareSprite = false;
        decayReactor.powerProduction = 5f;
        decayReactor.itemDuration = 1260f;
        decayReactor.generateEffect = Fx.generatespark;
        decayReactor.ambientSound = Sounds.loopDifferential;
        decayReactor.ambientSoundVolume = 0.05f;
        decayReactor.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion("-glow"){{
                    alpha = 0.5f;
                    color = Color.valueOf("bf92f9FF");
                    glowScale = 2f;
                }});
        decayReactor.consume(new ConsumeItemRadioactive());
        decayReactor.itemDurationMultipliers.put(SolarisContent.item("贫铀"), 15f);
        decayReactor.requirements(Category.power,
                ItemStack.with(SolarisContent.item("钢"), 100, SolarisContent.item("硅铁"), 180,
                        SolarisContent.item("塑料"), 90, Items.graphite, 120));

        // 手摇发电机
        handCrankGenerator = new HandCrankGenerator("手摇发电机");
        handCrankGenerator.description = "利用动力产生电力，每次点击都会增加电力产出";
        handCrankGenerator.health = 120;
        handCrankGenerator.size = 2;
        handCrankGenerator.buildVisibility = mindustry.world.meta.BuildVisibility.shown;
        handCrankGenerator.category = Category.power;
        handCrankGenerator.requirements(Category.power,
                ItemStack.with(Items.copper, 40, Items.lead, 60));
        research(handCrankGenerator, "solar-panel");

        // 火力发电机
        fireGenerator = new FireGenerator("火力发电机");
        fireGenerator.description = "使用可燃物品作为燃料，生成电力。";
        fireGenerator.powerProduction = 2.6f;
        fireGenerator.size = 2;
        fireGenerator.ambientSound = Sounds.loopCircuit;
        fireGenerator.ambientSoundVolume = 0.1f;
        fireGenerator.consume(new ConsumeItemFlammable(){{
            minFlammability = 0.3f;
        }});
        fireGenerator.itemDuration = 50f;
        fireGenerator.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawDefault(),
                new DrawWarmupRegion());
        fireGenerator.itemDurationMultipliers.put(Items.pyratite, 3f);
        fireGenerator.requirements(Category.power,
                ItemStack.with(Items.graphite, 40, Items.copper, 15));
    }

    private static void research(Block block, String parentName){
        research(block, parentName, block.researchRequirements());
    }

    private static void research(Block block, String parentName, ItemStack[] requirements){
        Block parent = SolarisContent.block(parentName);
        if(parent == null) return;
        TechTree.TechNode parentNode = TechTree.all.find(t -> t.content == parent);
        if(parentNode == null) return;
        TechTree.TechNode node = new TechTree.TechNode(null, block, requirements);
        node.parent = parentNode;
        parentNode.children.add(node);
    }
}
