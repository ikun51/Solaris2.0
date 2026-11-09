const library = require("lib/library");
const 重整炉 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "重整炉",
  [
    // 烷烃 氧化 -> 醇
    {
      input: {
        liquids: ["索拉里斯2.0-烷烃/10", "索拉里斯2.0-氧气/8"],
        power: 3,
      },
      output: {
        liquids: ["索拉里斯2.0-醇/8", "water/2"],
      },
      craftTime: 30,
    },
    // 烷烃 蒸汽裂解 -> 烯烃
    {
      input: {
        liquids: ["索拉里斯2.0-烷烃/12", "索拉里斯2.0-蒸汽/6"],
        power: 4,
      },
      output: {
        liquids: ["索拉里斯2.0-烯烃/10", "hydrogen/4"],
      },
      craftTime: 30,
    },
    // 烯烃 芳构化 -> 芳香烃
    {
      input: {
        liquids: ["索拉里斯2.0-烯烃/12", "索拉里斯2.0-蒸汽/4"],
        power: 5,
      },
      output: {
        liquids: ["索拉里斯2.0-芳香烃/6", "hydrogen/8"],
      },
      craftTime: 40,
    },
    // 芳香烃 加氢裂解 -> 烷烃
    {
      input: {
        liquids: ["索拉里斯2.0-芳香烃/8", "hydrogen/10"],
        power: 3,
      },
      output: {
        liquids: ["索拉里斯2.0-烷烃/10"],
      },
      craftTime: 35,
    },
  ],
);

重整炉.localizedName = "重整炉";
重整炉.description = "对轻烃进行催化重整，使醇、烯烃、烷烃与芳香烃相互转化。";
重整炉.size = 3;
重整炉.health = 400;
重整炉.hasItems = true;
重整炉.hasLiquids = true;
重整炉.hasPower = true;
重整炉.itemCapacity = 20;
重整炉.liquidCapacity = 300;
重整炉.updateEffectChance = 0.08;
重整炉.ambientSound = Sounds.loopSmelter;
重整炉.ambientSoundVolume = 0.4;
重整炉.setupRequirements(Category.crafting, ItemStack.with(
  Vars.content.item("索拉里斯2.0-钢"), 60,
  Items.graphite, 30,
  Vars.content.item("索拉里斯2.0-氮化硅"), 35,
  Items.copper, 95,
  Items.metaglass, 35
));

重整炉.craftEffect = new MultiEffect(
  extend(RadialEffect, {
    effect: Fx.surgeCruciSmoke,
    rotationSpacing: 90,
    amount: 4,
    lengthOffset: 6
  })
);

重整炉.drawer = new DrawMulti(
  new DrawDefault(),
  new DrawFlame(),
  extend(DrawGlowRegion, {
    suffix: "-glow",
    alpha: 0.4,
    glowScale: 4,
    color: Color.valueOf("FF9166FF")
  })
);

new TechTree.TechNode(
  TechTree.all.find(function(node) { return node.content != null && (node.content.name == "石油裂解塔" || node.content.name == "索拉里斯2.0-石油裂解塔"); }),
  重整炉, 重整炉.researchRequirements()
);
