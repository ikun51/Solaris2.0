package solaris2.content;

import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.TechTree;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.type.Item;

import static mindustry.Vars.content;

/**
 * 物品内容：原 assets/content/items 下 json 的 Java 实现。
 * 在静态初始化块中创建，JS 可通过 getClass 提前触发，保证脚本执行时内容已存在。
 */
public class SolarisItems{

    public static Item u7xAlloy, plastic, nuclearWaste, aerogel, siliconNitride, electronTube, ferrosilicon,
            saltpeter, magneticSteel, energyCell, chip, depletedUranium, polonium, steel, molybdenum,
            uranium, uraniumMolybdenum, iron, aluminum, chromium, iridium, silver, highPurityUranium,
            smokelessPowder, nitroExplosive, crudePowder, highEnergyPowder, uraniumDioxide, hematite,
            bauxite, chromite, silverOre, fluorite, chalcopyrite;

    static{
        u7xAlloy = make("U-7X合金", null, "以贫铀为基体炼制的超级合金，兼具高强度与耐高温特性。",
                "4A7D9C", 0, 5f, 0.5f, 0f, 0f);
        research(u7xAlloy, Items.surgeAlloy);

        plastic = make("塑料", null, "由高温液态金属冷却后产物，具有高导热、高导电特点。",
                "E6E6E6", 0, 5f, 0f, 0f, 0.5f);
        research(plastic, Items.copper);

        nuclearWaste = make("核废料", null, "反应堆使用后的强放射性废料，",
                "7CC46A", 0, 0.1f, 2.5f, 0f, 0f);
        research(nuclearWaste, Items.thorium);

        aerogel = make("气凝胶", null, "由高温液态金属冷却后产物，具有高导热、高导电特点。",
                "C8DAF0", 0, 5f, 0f, 0f, 0f);
        research(aerogel, Items.copper);

        siliconNitride = make("氮化硅", null, "一种具高硬度、高强度、耐高温及耐腐蚀特性的材料，是工业领域理想的高性能结构与功能材料。",
                "595A5D", 0, 2f, 0f, 0f, 0f);

        electronTube = make("电子管", null, "老式真空电子元件，是组装复杂设备的基础部件。",
                "C8A84A", 0, 2f, 0f, 0f, 0f);
        research(electronTube, Items.silicon);

        ferrosilicon = make("硅铁", null, "一种能够同时具备高强度、高硬度和高韧性等多种优点的合金",
                "8899AE", 0, 3f, 0f, 0f, 0f);
        research(ferrosilicon, Items.titanium);

        saltpeter = make("硝石", null, "一种常用的燃烧物，用于制造炮弹和弹药。",
                "F0F0E8", 0, 5f, 0f, 0f, 0.5f);
        research(saltpeter, Items.phaseFabric);

        magneticSteel = make("磁钢", null, "具有强磁性的特种合金，是电磁设备的核心材料。",
                "6B7480", 1, 2f, 0f, 0f, 0f);
        research(magneticSteel, Items.titanium);

        energyCell = make("能量电池", null, "", "823CE5", 0, 1f, 2f, 0f, 0f);

        chip = make("芯片", "晶体管", "科技的结晶，制作高级单位的必要资源。",
                "6E7080", 0, 5f, 0f, 0f, 0f);
        research(chip, Items.phaseFabric);

        depletedUranium = make("贫铀", null, "铀浓缩后剩余的副产物，密度极大，可制作高密度合金与穿甲弹药。",
                "5C7A4A", 2, 1f, 1f, 0f, 0f);
        research(depletedUranium, Items.thorium);

        polonium = make("钋", null, "人造放射性金属，可用于制造先进的核燃料与武器。",
                "B070D0", 0, 3f, 3f, 0f, 0f);

        steel = make("钢", null, "一种能够同时具备高强度、高硬度和高韧性等多种优点的合金",
                "6FDEDC", 0, 3f, 0f, 0f, 0f);
        research(steel, Items.titanium);

        molybdenum = make("钼", null, "高熔点金属，能显著提升合金的耐热与抗腐蚀性能。",
                "8C8C94", 2, 1f, 0f, 0f, 0f);
        research(molybdenum, Items.titanium);

        uranium = make("铀", "天然铀", "天然铀，是核工业的核心原料。",
                "7A8A7A", 0, 2f, 1.5f, 0f, 0f);
        research(uranium, Items.thorium);

        uraniumMolybdenum = make("铀钼合金", null, "将铀和钼打印制成，硬度极高",
                "BD4882", 0, 1f, 0.5f, 0f, 0f);
        research(uraniumMolybdenum, Items.thorium);

        iron = make("铁", null, "铁是一种高强度、耐腐蚀、广泛应用于建筑和制造业的金属材料。",
                "8C8C8C", 0, 0.1f, 0f, 0f, 0f);
        research(iron, Items.plastanium);

        aluminum = make("铝", null, "铝是一种高强度、耐腐蚀、广泛应用于建筑和制造业的金属材料。",
                "768A9A", 0, 0.1f, 0f, 0f, 0f);
        research(aluminum, Items.plastanium);

        chromium = make("铬", null, "坚硬耐腐蚀的蓝白色金属，是制造不锈钢与耐磨部件的重要原料。",
                "C0D0E8", 2, 1f, 0f, 0f, 0f);
        research(chromium, Items.titanium);

        iridium = make("铱", null, "", "768A9A", 0, 0.1f, 0f, 0f, 0f);
        research(iridium, Items.plastanium);

        silver = make("银", null, "具有极佳导电导热性的贵金属，用于精密电子与电路制造。",
                "D0D8E0", 0, 0.5f, 0f, 0f, 0f);
        research(silver, Items.copper);

        highPurityUranium = make("高纯铀", null, "经过高度提纯的铀，放射性更强，是高效核燃料的核心原料。",
                "70D070", 0, 2f, 3f, 0f, 0f);
        research(highPurityUranium, Items.thorium);

        smokelessPowder = make("无烟火药", null, "燃烧稳定、烟雾极少的现代火药，弹药制造的优质原料。",
                "CAC9C4", 0, 1f, 0f, 0.8f, 1.2f);
        research(smokelessPowder, Items.blastCompound);

        nitroExplosive = make("硝基炸药", null, "威力巨大的烈性炸药，常用于爆破与重火力弹药。",
                "C8B050", 0, 2f, 0f, 1.5f, 0.5f);
        research(nitroExplosive, Items.blastCompound);

        crudePowder = make("粗火药", null, "未经提纯的简易火药，燃烧剧烈但威力有限。",
                "8C6A4A", 0, 0.5f, 0f, 0.6f, 1.5f);
        research(crudePowder, Items.coal);

        highEnergyPowder = make("高能火药", null, "能量密度极高的高性能火药，爆炸威力远超普通火药。",
                "C86040", 0, 2f, 0f, 1.4f, 1.8f);
        research(highEnergyPowder, Items.blastCompound);

        uraniumDioxide = make("二氧化铀", null, "铀的矿石，主要是铀氧化物，用于提炼铀。在高炉中处理",
                "114514", 3, 1f, 1f, 0f, 0f);
        research(uraniumDioxide, Liquids.slag);

        hematite = make("赤铁矿", null, "红褐色的铁矿石，冶炼后可提取金属铁。",
                "B05A3C", 1, 0.5f, 0f, 0f, 0f);
        research(hematite, Items.copper);

        bauxite = make("铝土矿", null, "含有铝和土的矿石，用于提炼铝",
                "114514", 2, 1f, 0f, 0f, 0f);
        research(bauxite, "铂系矿泥");

        chromite = make("铬铁矿", null, "含有铬和铁的矿石，用于提炼铬和铁。",
                "114514", 2, 1f, 0f, 0f, 0f);
        research(chromite, "钨酸盐矿");

        silverOre = make("银矿", null, "含有银的矿石，用于提炼银",
                "114514", 2, 1f, 0f, 0f, 0f);
        research(silverOre, "铂系矿泥");

        fluorite = make("黄石", null, "主要成分是硫化钨，用于提取钨和副产硫酸。",
                "114514", 1, 1f, 0f, 0f, 0f);
        research(fluorite, Liquids.slag);

        chalcopyrite = make("黄铜矿", null, "含有黄铜的矿石，用于提炼黄铜",
                "6A5010", 1, 1f, 0f, 0f, 0f);
        research(chalcopyrite, "铂系矿泥");
    }

    /** 由主类或 JS 调用，触发静态初始化。 */
    public static void load(){
    }

    private static Item make(String name, String localizedName, String description, String color,
                             int hardness, float cost, float radioactivity, float explosiveness, float flammability){
        Item item = new Item(name);
        if(localizedName != null) item.localizedName = localizedName;
        item.description = description;
        item.color = color(color);
        item.hardness = hardness;
        item.cost = cost;
        item.radioactivity = radioactivity;
        item.explosiveness = explosiveness;
        item.flammability = flammability;
        return item;
    }

    private static Color color(String hex){
        return Color.valueOf(hex.length() == 6 ? hex + "FF" : hex);
    }

    private static void research(Item item, Content parent){
        if(parent == null) return;
        TechTree.TechNode node = new TechTree.TechNode(null, item, item.researchRequirements());
        TechTree.TechNode parentNode = TechTree.all.find(t -> t.content == parent);
        if(parentNode == null) return;
        node.parent = parentNode;
        parentNode.children.add(node);
    }

    private static void research(Item item, String parentName){
        Content parent = content.getByName(ContentType.item, parentName);
        research(item, parent);
    }
}
