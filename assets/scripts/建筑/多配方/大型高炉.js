const library = require("lib/library");
const 大型高炉 = library.MultiCrafter(
  GenericCrafter, // 基类
  GenericCrafter.GenericCrafterBuild, // 构建函数或静态方法
  "大型高炉", // 工厂名称
  [
    // 第一个冶炼工艺：黄铁矿和煤合成钢
    {
      input: {
        items: ["索拉里斯2.0-赤铁矿/4", "coal/3"], // 输入物品及数量，使用数组
        power: 1.1, // 所需电力
      },
      output: {
        items: ["索拉里斯2.0-铁/4"], 
        liquids: ["索拉里斯2.0-一氧化碳/6"], 
        heat: 3,
        // 输出物品及数量
      },
      craftTime: 45, // 工艺时间
    },
    {
      input: {
        items: ["索拉里斯2.0-铁/3", "索拉里斯2.0-铬/2", "索拉里斯2.0-铱/2"], 
      
        power: 1.6, // 所需电力
      },
      output: {
        items: ["索拉里斯2.0-钢/5"], 
                heat: 3,
        // 输出物品及数量
      },
      craftTime: 45, 
    },
        {
      input: {
        items: ["索拉里斯2.0-铝土矿/6", "coal/3"], // 输入物品及数量，使用数组
        power: 1.1, // 所需电力
      },
      output: {
        items: ["索拉里斯2.0-铝/4"], 
                 result: [
          "索拉里斯2.0-铁/0.4/2",  
          "silicon/0.5/1"
        ],
        heat: 3,
        // 输出物品及数量
      },
      craftTime: 45, // 工艺时间
    },
            {
      input: {
        items: ["索拉里斯2.0-铬铁矿/6", "coal/4"], // 输入物品及数量，使用数组
        power: 1.1, // 所需电力
      },
      output: {
        items: ["索拉里斯2.0-铬/4", "索拉里斯2.0-铁/2"], 
        heat: 3,
        // 输出物品及数量
      },
      craftTime: 45, // 工艺时间
    },
            {
      input: {
        items: ["pyratite/4"], 
              liquids: ["索拉里斯2.0-氧气/8"], 
        power: 1.5, // 所需电力
      },
      output: {
        liquids: ["索拉里斯2.0-二氧化硫/4"], 

                heat: 2,
        // 输出物品及数量
      },
      craftTime: 35, 
    },
                {
      input: {
        items: ["索拉里斯2.0-黄铜矿/4"], 
      
        power: 1.5, // 所需电力
      },
      output: {
        items: ["copper/4"], 
        liquids: ["索拉里斯2.0-二氧化硫/8"], 
                result: [
          "索拉里斯2.0-铁/0.5/3",
        ],
                heat: 4,
        // 输出物品及数量
      },
      craftTime: 55, 
    },
  ],
);

大型高炉.localizedName = "大型高炉";
大型高炉.description = "可以冶炼高级金属";
大型高炉.size = 4;
大型高炉.health = 400;
大型高炉.hasItems = true;
大型高炉.hasLiquids = true;
大型高炉.hasPower = true;
大型高炉.itemCapacity = 50;
大型高炉.liquidCapacity = 120;
大型高炉.craftEffect = Fx.smeltsmoke;
大型高炉.setupRequirements(Category.crafting, ItemStack.with(
  Vars.content.item("索拉里斯2.0-铁"), 65,
  Items.graphite, 40,
  Items.metaglass, 30,
  Items.copper, 30
));

大型高炉.drawer = new DrawMulti(
  new DrawDefault(),
  extend(DrawFlame, {
    flameRadius: 0,
    flameRadiusIn: 0,
    flameRadiusMag: 0,
    flameRadiusInMag: 0
  })
);
