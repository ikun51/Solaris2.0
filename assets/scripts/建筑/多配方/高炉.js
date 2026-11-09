const library = require("lib/library");
const 高炉 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "高炉", // 工厂名称
  [
    {
      input: {
        items: ["索拉里斯2.0-赤铁矿/3", "coal/2"],
        power: 1,
      },
      output: {
        items: ["索拉里斯2.0-铁/2"], 
        liquids: ["索拉里斯2.0-一氧化碳/12"], 
        heat: 4,
      },
      craftTime: 75,
    },
            {
      input: {
        items: ["索拉里斯2.0-黄铜矿/5"], 
      
        power: 1.5,
      },
      output: {
        items: ["copper/4"], 
        liquids: ["索拉里斯2.0-二氧化硫/6"], 
                result: [
          "索拉里斯2.0-铁/0.5/3",
        ],
                heat: 4,
      },
      craftTime: 75, 
    },
                {
      input: {
        items: ["pyratite/4"], 
              liquids: ["索拉里斯2.0-氧气/8"], 
      },
      output: {
        liquids: ["索拉里斯2.0-二氧化硫/4"], 
                heat: 2,
      },
      craftTime: 65, 
    },
  ],
);

高炉.localizedName = "高炉";
高炉.description = "可以冶炼基础金属";
高炉.size = 2;
高炉.health = 400;
高炉.hasItems = true;
高炉.hasLiquids = true;
高炉.hasPower = true;
高炉.itemCapacity = 20;
高炉.liquidCapacity = 60;
高炉.craftEffect = Fx.smeltsmoke;
高炉.squareSprite = true;
高炉.setupRequirements(Category.crafting, ItemStack.with(
  Vars.content.item("索拉里斯2.0-铁"), 30,
  Items.graphite, 25
));

高炉.drawer = new DrawMulti(
  new DrawDefault(),
  new DrawFlame(),
  new DrawGlowRegion("-glow")
);
