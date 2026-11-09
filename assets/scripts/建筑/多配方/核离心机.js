const library = require("lib/library");

const 核离心机 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "核离心机",
  [
    {
      input: {
        items: ["索拉里斯2.0-核废料/4"],
        power: 6,
      },
      output: {
        items: ["索拉里斯2.0-高纯铀/1"],
         result: [
          "索拉里斯2.0-贫铀/0.3/2"
        ],
      },
      craftTime: 840,
    },
    {
      input: {
        items: ["索拉里斯2.0-核废料/6"],
        liquids: ["索拉里斯2.0-硫酸/4"],
        power: 10,
        heat: 6,
      },
      output: {
        items: ["索拉里斯2.0-高纯铀/2"],
        result: [
          "索拉里斯2.0-钋/0.08/1",
        ],
      },
      maxEfficiency: 2,
      craftTime: 900,
    },
    {
      input: {
        items: ["索拉里斯2.0-铀/10"],
        power: 5,
      },
      output: {
        items: ["索拉里斯2.0-高纯铀/1", "索拉里斯2.0-贫铀/5"],
         result: [
          "索拉里斯2.0-钋/0.006/1",  
          "索拉里斯2.0-贫铀/0.4/1"
        ],
      },
      craftTime: 660,
    },
  ],
);

核离心机.localizedName = "核离心机";
核离心机.description = "通过高速旋转 分离不同质量核材料，";
核离心机.size = 3;
核离心机.category = Category.crafting;
核离心机.itemCapacity = 20;
核离心机.hasPower = true;
核离心机.hasItems = true;
核离心机.ambientSound = Sounds.loopHum;
核离心机.ambientSoundVolume = 0.03;
核离心机.requirements = ItemStack.with(
  Items.copper, 200,
  Items.graphite, 130,
  Items.titanium, 180,
  Vars.content.item("索拉里斯2.0-硅铁"), 100,
  Items.plastanium, 70
);
核离心机.buildVisibility = BuildVisibility.shown;

核离心机.drawer = new DrawMulti(
  new DrawDefault(),
  extend(DrawRegion, {
    suffix: "-rotator",
    rotateSpeed: 12,
    spinSprite: true
  }),
  extend(DrawRegion, {
    suffix: "-rotator",
    rotateSpeed: 12,
    spinSprite: true,
    rotation: 45
  }),
  new DrawRegion("-top")
);
