const library = require("lib/library");

const 酸液反应釜 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "酸液反应釜",
  [
    {
      input: {
        items: ["索拉里斯2.0-二氧化铀/3"],
        liquids: ["索拉里斯2.0-硫酸/4"],
        power: 6,
        heat: 5,
      },
      output: {
        items: ["索拉里斯2.0-铀/2"],
      },
      craftTime: 240,
      maxEfficiency : 2
    },
    {
      input: {
        items: ["索拉里斯2.0-银矿/4"],
        liquids: ["索拉里斯2.0-硝酸/5"],
        power: 8,
        heat: 6,
      },
      output: {
        items: ["索拉里斯2.0-银/2"],
        result: [
          "索拉里斯2.0-铱/0.1/1",
        ],
      },
      craftTime: 180,
      maxEfficiency : 2
    },
    {
      input: {
        items: ["索拉里斯2.0-铬铁矿/5"],
        liquids: ["索拉里斯2.0-硫酸/4"],
        power: 6,
        heat: 5,
      },
      output: {
        items: ["索拉里斯2.0-铬/2"],
        result: [
          "索拉里斯2.0-钼/0.15/1",
        ],
      },
      craftTime: 180,
      maxEfficiency : 2
    },
    {
      input: {
        items: ["索拉里斯2.0-黄石/4"],
        liquids: ["索拉里斯2.0-氧气/12"],
        power: 5,
        heat: 6,
      },
      output: {
        items: ["tungsten/2"],
        liquids: ["索拉里斯2.0-硫酸/6"],
      },
      craftTime: 180,
      maxEfficiency : 2
    },
  ],
);

酸液反应釜.localizedName = "酸液反应釜";
酸液反应釜.description = "酸浸分离高纯金属";
酸液反应釜.size = 2;
酸液反应釜.health = 400;
酸液反应釜.hasItems = true;
酸液反应釜.hasLiquids = true;
酸液反应釜.hasPower = true;
酸液反应釜.itemCapacity = 50;
酸液反应釜.liquidCapacity = 120;
酸液反应釜.craftEffect = Fx.smeltsmoke;
酸液反应釜.setupRequirements(Category.crafting, ItemStack.with(
  Vars.content.item("索拉里斯2.0-铁"), 125,
  Items.copper, 100,
  Items.silicon, 120
));

酸液反应釜.drawer = new DrawMulti(
  new DrawDefault(),
  extend(DrawFlame, {
    flameRadius: 0,
    flameRadiusIn: 0,
    flameRadiusMag: 0,
    flameRadiusInMag: 0
  })
);
