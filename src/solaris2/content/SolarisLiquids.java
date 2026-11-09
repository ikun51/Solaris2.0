package solaris2.content;

import arc.graphics.Color;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.content.TechTree;
import mindustry.ctype.Content;
import mindustry.type.Liquid;

/**
 * 液体内容：原 assets/content/liquids 下 json 的 Java 实现。
 * hydrogen / nitrogen 为原版液体，这里只做字段补丁。
 */
public class SolarisLiquids{

    public static Liquid carbonMonoxide, sulfurDioxide, ammonia, techLiquid, oxygen, liquidHydrogen,
            liquidOxygen, liquidNitrogen, alkene, alkane, fuelGas, glycerol, oil, nitroglycerin,
            nitricAcid, sulfuricAcid, aromaticHydrocarbon, steam, alcohol;

    static{
        // 原版液体补丁
        Liquids.hydrogen.alwaysUnlocked = true;
        Liquids.hydrogen.heatCapacity = 0.5f;
        Liquids.hydrogen.flammability = 4f;
        Liquids.hydrogen.incinerable = false;
        research(Liquids.nitrogen, Liquids.hydrogen);

        carbonMonoxide = make("一氧化碳", null, "无味的有毒气体，易燃，可作为工业燃料。");
        carbonMonoxide.gas = true;
        carbonMonoxide.color = color("A8B8B8");
        carbonMonoxide.lightColor = color("D0D8D8");
        carbonMonoxide.flammability = 1.1f;
        carbonMonoxide.temperature = 0.1f;
        carbonMonoxide.viscosity = 0.2f;
        carbonMonoxide.heatCapacity = 0.3f;

        sulfurDioxide = make("二氧化硫", null, "有毒的氧化气体。");
        sulfurDioxide.gas = true;
        sulfurDioxide.color = color("F3C72E");
        sulfurDioxide.lightColor = color("F7DC6F");
        sulfurDioxide.flammability = 0f;
        sulfurDioxide.temperature = 0.1f;
        sulfurDioxide.viscosity = 0.1f;
        sulfurDioxide.heatCapacity = 0.4f;

        ammonia = make("合成氨", null, "刺激性气体，化工与化肥工业的基础原料。");
        ammonia.gas = true;
        ammonia.color = color("6FBF6F");
        ammonia.lightColor = color("A8E0A8");
        ammonia.flammability = 0f;
        ammonia.temperature = 0f;
        ammonia.viscosity = 0.2f;
        ammonia.heatCapacity = 0.5f;

        techLiquid = make("技霸液体", null, "非常waaaaaagh的液体");
        techLiquid.color = color("D7D7D7");
        techLiquid.lightColor = color("D7D7D7");
        techLiquid.flammability = 0f;
        techLiquid.temperature = 0f;
        techLiquid.heatCapacity = 114514f;
        techLiquid.viscosity = 0f;

        oxygen = make("氧气", null, "无味的无毒气体,普通的氧化气体。");
        oxygen.gas = true;
        oxygen.color = color("6FDEDC");
        oxygen.lightColor = color("B8FFFE");
        oxygen.flammability = 0f;
        oxygen.temperature = 0.1f;
        oxygen.viscosity = 0.1f;
        oxygen.heatCapacity = 0.4f;

        liquidHydrogen = make("液氢", null, "较强燃烧性");
        liquidHydrogen.color = color("BBC4F7");
        liquidHydrogen.barColor = color("BBC4F7");
        liquidHydrogen.lightColor = color("BBC4F7");
        liquidHydrogen.flammability = 3.2f;
        liquidHydrogen.explosiveness = 0.55f;
        liquidHydrogen.effect = StatusEffects.freezing;
        research(liquidHydrogen, Liquids.hydrogen);

        liquidOxygen = make("液氧", null, "助燃(并非)，较强燃烧性");
        liquidOxygen.color = color("B8FFFE");
        liquidOxygen.barColor = color("B8FFFE");
        liquidOxygen.lightColor = color("6FDEDC");
        liquidOxygen.flammability = 3.2f;
        liquidOxygen.explosiveness = 2.75f;
        liquidOxygen.effect = StatusEffects.freezing;
        research(liquidOxygen, Liquids.ozone);

        liquidNitrogen = make("液氮", null, "温度极低，用作冷却剂");
        liquidNitrogen.coolant = false;
        liquidNitrogen.color = color("DCCAF4");
        liquidNitrogen.barColor = color("DCCAF4");
        liquidNitrogen.lightColor = color("DCCAF4");
        liquidNitrogen.heatCapacity = 1.75f;
        liquidNitrogen.temperature = 0.05f;
        liquidNitrogen.effect = StatusEffects.freezing;
        research(liquidNitrogen, Liquids.nitrogen);

        alkene = make("烯烃", null, "易燃气态烯烃，重要的化工原料。");
        alkene.gas = true;
        alkene.color = color("6FBCB4");
        alkene.lightColor = color("A8E0D8");
        alkene.flammability = 1.1f;
        alkene.temperature = 0f;
        alkene.viscosity = 0.15f;
        alkene.heatCapacity = 0.25f;
        alkene.explosiveness = 0.5f;

        alkane = make("烷烃", null, "多种饱和烃的混合物，主要成分是乙烷，丙烷，和丁烷，用于裂化成乙烯以生产塑料");
        alkane.alwaysUnlocked = true;
        alkane.gas = true;
        alkane.incinerable = false;
        alkane.color = color("E925254F");
        alkane.heatCapacity = 0.5f;
        alkane.temperature = 0.5f;
        alkane.viscosity = 0.2f;
        alkane.flammability = 3f;
        alkane.explosiveness = 3f;
        research(alkane, Liquids.oil);

        fuelGas = make("燃气", null, "易燃易爆，供热发电，是高效的能源。");
        fuelGas.gas = true;
        fuelGas.color = color("FF9166");
        fuelGas.lightColor = color("FFB570");
        fuelGas.flammability = 1.2f;
        fuelGas.temperature = 0f;
        fuelGas.viscosity = 0.15f;
        fuelGas.heatCapacity = 0.2f;

        glycerol = make("甘油", null, "粘稠液体，性质温和，是制备炸药的重要原料。");
        glycerol.color = color("D8B860");
        glycerol.lightColor = color("E8D080");
        glycerol.flammability = 0.2f;
        glycerol.temperature = 0f;
        glycerol.viscosity = 0.8f;
        glycerol.heatCapacity = 0.6f;
        glycerol.explosiveness = 0.4f;

        oil = make("石油", null, "易燃，供热发电，是高效的能源。");
        oil.color = color("FF9166");
        oil.lightColor = color("FFB570");
        oil.flammability = 1.2f;
        oil.temperature = 0.1f;
        oil.viscosity = 0.7f;
        oil.heatCapacity = 0.4f;

        nitroglycerin = make("硝化甘油", null, "淡黄色烈性液体炸药，受撞击便会剧烈爆炸。");
        nitroglycerin.incinerable = false;
        nitroglycerin.color = color("D8B448");
        nitroglycerin.lightColor = color("E8C060");
        nitroglycerin.flammability = 0.4f;
        nitroglycerin.temperature = 0f;
        nitroglycerin.viscosity = 0.6f;
        nitroglycerin.heatCapacity = 0.2f;
        nitroglycerin.explosiveness = 1.5f;

        nitricAcid = make("硝酸", null, "淡黄绿色强腐蚀性液体，化工生产的关键原料。");
        nitricAcid.coolant = false;
        nitricAcid.color = color("A8C860");
        nitricAcid.lightColor = color("C0D880");
        nitricAcid.flammability = 0f;
        nitricAcid.temperature = 0f;
        nitricAcid.viscosity = 0.5f;
        nitricAcid.heatCapacity = 0.5f;

        sulfuricAcid = make("硫酸", null, "强无机酸，用于洗矿及蚀刻");
        sulfuricAcid.alwaysUnlocked = true;
        sulfuricAcid.coolant = false;
        sulfuricAcid.color = color("EDDA567F");
        sulfuricAcid.heatCapacity = 0.5f;
        sulfuricAcid.temperature = 0.5f;
        sulfuricAcid.viscosity = 0.6f;
        sulfuricAcid.flammability = 0f;
        research(sulfuricAcid, mindustry.content.Items.pyratite);

        aromaticHydrocarbon = make("芳香烃", null, "多种芳香烃的混合物，主要成分是苯，苯烷，和苯烷");
        aromaticHydrocarbon.gas = true;
        aromaticHydrocarbon.color = color("E925254F");
        aromaticHydrocarbon.heatCapacity = 0.5f;
        aromaticHydrocarbon.temperature = 0.5f;
        aromaticHydrocarbon.viscosity = 0.2f;
        aromaticHydrocarbon.flammability = 3f;
        aromaticHydrocarbon.explosiveness = 3f;
        research(aromaticHydrocarbon, Liquids.oil);

        steam = make("蒸汽", null, "古老但实用的动力源");
        steam.gas = true;
        steam.incinerable = false;
        steam.color = color("FFFFFF");
        steam.lightColor = color("FFFFFF");
        steam.flammability = 0f;
        steam.temperature = 0.9f;
        steam.viscosity = 0.2f;
        steam.heatCapacity = 0.4f;

        alcohol = make("醇", null, "透明液体燃料，易燃，工业用途广泛。");
        alcohol.color = color("7AB8D8");
        alcohol.lightColor = color("A8D8E8");
        alcohol.flammability = 1.1f;
        alcohol.temperature = 0f;
        alcohol.viscosity = 0.3f;
        alcohol.heatCapacity = 0.6f;
    }

    /** 由主类或 JS 调用，触发静态初始化。 */
    public static void load(){
    }

    private static Liquid make(String name, String localizedName, String description){
        Liquid liquid = new Liquid(name);
        if(localizedName != null) liquid.localizedName = localizedName;
        liquid.description = description;
        return liquid;
    }

    private static Color color(String hex){
        return Color.valueOf(hex.length() == 6 ? hex + "FF" : hex);
    }

    private static void research(mindustry.ctype.UnlockableContent content, Content parent){
        if(parent == null) return;
        if(TechTree.all.find(t -> t.content == content) != null) return;
        TechTree.TechNode parentNode = TechTree.all.find(t -> t.content == parent);
        if(parentNode == null) return;
        TechTree.TechNode node = new TechTree.TechNode(null, content, content.researchRequirements());
        node.parent = parentNode;
        parentNode.children.add(node);
    }
}
