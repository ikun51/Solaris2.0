package solaris2.heat;

import arc.graphics.Blending;
import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.content.TechTree;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.heat.HeatConductor;
import mindustry.world.blocks.heat.HeatProducer;
import mindustry.world.draw.*;
import solaris2.HeatCategory;
import solaris2.SolarisContent;

/**
 * 由 scripts/建筑/热量 下的 6 个 JS 建筑移植而来。
 * 原 assets/content/blocks/热量 下 json 已并入本类。
 */
public class HeatBlocks{

    public static BurningBoiler burningBoiler;
    public static LiquidBurningBoiler liquidBurningBoiler;
    public static HeatBoiler heatBoiler;
    public static ThermalPump thermalPump;
    public static HeatDriver heatDriver;
    public static HeatLimiter heatLimiter;
    public static HeatConductor heatSplitter, heatPipe;
    public static HeatProducer electricHeater, thermiteReactor;

    public static void load(){
        burningBoiler = new BurningBoiler("燃烧锅炉");
        liquidBurningBoiler = new LiquidBurningBoiler("液体燃烧锅炉");
        heatBoiler = new HeatBoiler("热量锅炉");

        // 燃烧锅炉 json 覆盖字段
        burningBoiler.description = "燃烧任意可燃物品，将水加热为蒸汽。燃料燃烧性越高，进水→蒸汽的转换速率越快（线性，1:1）";
        burningBoiler.health = 700;
        burningBoiler.size = 2;
        burningBoiler.itemCapacity = 20;
        burningBoiler.hasLiquids = true;
        burningBoiler.liquidCapacity = 200f;
        burningBoiler.ambientSound = Sounds.loopFire;
        burningBoiler.ambientSoundVolume = 0.04f;
        burningBoiler.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawCrucibleFlame(),
                new DrawDefault(),
                new DrawGlowRegion("-heat"){{
                    blending = Blending.additive;
                    color = Color.valueOf("d76a6a80");
                }});
        burningBoiler.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("铁"), 120, SolarisContent.item("氮化硅"), 60));
        research(burningBoiler, "combustion-generator");

        // 液体燃烧锅炉 json 覆盖字段
        liquidBurningBoiler.description = "燃烧任意可燃液体，将水加热为蒸汽。液体燃料燃烧性越高，进水→蒸汽的转换速率越快（线性，1:1）";
        liquidBurningBoiler.health = 700;
        liquidBurningBoiler.size = 2;
        liquidBurningBoiler.hasLiquids = true;
        liquidBurningBoiler.liquidCapacity = 200f;
        liquidBurningBoiler.ambientSound = Sounds.loopFire;
        liquidBurningBoiler.ambientSoundVolume = 0.04f;
        liquidBurningBoiler.drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawCrucibleFlame(),
                new DrawDefault(),
                new DrawGlowRegion("-heat"){{
                    blending = Blending.additive;
                    color = Color.valueOf("d76a6a80");
                }});
        liquidBurningBoiler.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("铁"), 120, SolarisContent.item("氮化硅"), 60));
        research(liquidBurningBoiler, "combustion-generator");

        // 热量锅炉 json 覆盖字段
        heatBoiler.description = "利用原版热量系统（需邻接热源或热导管供热）将水加热为蒸汽。热量越高，进水→蒸汽的转换速率越快（线性，1:1）";
        heatBoiler.health = 700;
        heatBoiler.size = 2;
        heatBoiler.hasLiquids = true;
        heatBoiler.liquidCapacity = 200f;
        heatBoiler.ambientSound = Sounds.loopFire;
        heatBoiler.ambientSoundVolume = 0.04f;
        heatBoiler.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawGlowRegion("-heat"){{
                    blending = Blending.additive;
                    color = Color.valueOf("d76a6a80");
                }});
        heatBoiler.requirements(Category.crafting,
                ItemStack.with(SolarisContent.item("铁"), 120, SolarisContent.item("氮化硅"), 60));
        research(heatBoiler, "combustion-generator");

        thermalPump = new ThermalPump("热能泵");
        heatDriver = new HeatDriver("热量驱动器");
        heatLimiter = new HeatLimiter("热量限制器");

        // 分热器
        heatSplitter = new HeatConductor("分热器");
        heatSplitter.description = "将热量分隔成多个输出端";
        heatSplitter.visualMaxHeat = 50f;
        heatSplitter.splitHeat = true;
        heatSplitter.size = 1;
        heatSplitter.regionRotated1 = 1;
        heatSplitter.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawHeatOutput(),
                new DrawHeatOutput(){{
                    drawGlow = false;
                    rotOffset = 1;
                }},
                new DrawHeatOutput(){{
                    drawGlow = false;
                    rotOffset = -1;
                }},
                new DrawHeatInput("-input"));
        heatSplitter.requirements(heatCategory(),
                ItemStack.with(SolarisContent.item("银"), 20, Items.graphite, 30));

        // 热管
        heatPipe = new HeatConductor("热管");
        heatPipe.description = "用于传输热量的设备";
        heatPipe.health = 100;
        heatPipe.visualMaxHeat = 100f;
        heatPipe.size = 1;
        heatPipe.regionRotated1 = 1;
        heatPipe.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawHeatOutput(),
                new DrawHeatInput("-input"));
        heatPipe.requirements(heatCategory(),
                ItemStack.with(SolarisContent.item("银"), 10, Items.graphite, 5));

        // 电热器
        electricHeater = new HeatProducer("电热器");
        electricHeater.description = "消耗大量电力产生热量。";
        electricHeater.size = 2;
        electricHeater.health = 200;
        electricHeater.heatOutput = 3f;
        electricHeater.regionRotated1 = 1;
        electricHeater.rotateDraw = false;
        electricHeater.itemCapacity = 0;
        electricHeater.researchCostMultiplier = 4f;
        electricHeater.ambientSound = Sounds.loopTech;
        electricHeater.drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput());
        electricHeater.consumePower(1.6666666f);
        electricHeater.requirements(heatCategory(),
                ItemStack.with(Items.copper, 30, SolarisContent.item("银"), 15,
                        Items.silicon, 30, SolarisContent.item("铁"), 40));

        // 铝热反应炉
        thermiteReactor = new HeatProducer("铝热反应炉");
        thermiteReactor.description = "将铝和赤铁矿反应，生成大量热量";
        thermiteReactor.size = 2;
        thermiteReactor.health = 200;
        thermiteReactor.heatOutput = 10f;
        thermiteReactor.craftTime = 120f;
        thermiteReactor.outputItems = ItemStack.with(SolarisContent.item("铁"), 1);
        thermiteReactor.ambientSound = Sounds.loopSmelter;
        thermiteReactor.ambientSoundVolume = 0.06f;
        thermiteReactor.drawer = new DrawMulti(
                new DrawDefault(),
                new DrawHeatOutput(),
                new DrawFlame(),
                new DrawGlowRegion("-glow"){{ alpha = 0.9f; }});
        thermiteReactor.consumePower(0.5f);
        thermiteReactor.consumeItems(ItemStack.with(SolarisContent.item("铝"), 1, SolarisContent.item("赤铁矿"), 1));
        thermiteReactor.requirements(heatCategory(),
                ItemStack.with(Items.graphite, 40, Items.tungsten, 20, SolarisContent.item("铁"), 30));

        // json 覆盖字段
        heatLimiter.category = heatCategory();

        heatDriver.size = 2;
        heatDriver.visualMaxHeat = 120f;
        heatDriver.buildVisibility = mindustry.world.meta.BuildVisibility.shown;
        heatDriver.requirements(heatCategory(),
                ItemStack.with(Items.tungsten, 150, SolarisContent.item("银"), 50,
                        SolarisContent.item("钢"), 125, SolarisContent.item("硅铁"), 65));
        heatDriver.consumePower(4f);
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

    private static Category heatCategory(){
        return HeatCategory.available && HeatCategory.heatCategory != null
                ? HeatCategory.heatCategory : Category.crafting;
    }
}
