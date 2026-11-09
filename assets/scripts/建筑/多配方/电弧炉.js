const library = require("lib/library");
const 电弧炉 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "电弧炉",
  [
    {
      input: {
        items: ["索拉里斯2.0-铁/4", "silicon/2"],
        power: 3,
      },
      output: {
        items: ["索拉里斯2.0-硅铁/2"], 
      },
      craftTime: 45,
    },
        {
      input: {
        items: ["graphite/2", "sand/3"
        ],
        power: 3,
      },
      output: {
        items: ["silicon/4"], 
      },
      craftTime: 30,
    },
    {
      input: {
        items: ["索拉里斯2.0-铁/3", "索拉里斯2.0-铬/1", "索拉里斯2.0-铱/1"], 
      
        power: 4.5,
      },
      output: {
        items: ["索拉里斯2.0-钢/4"], 
      },
      craftTime: 35, 

    },
  ],
);

电弧炉.localizedName = "电弧炉";
电弧炉.description = "利用电弧高温熔化炉料炼钢，有效除杂，适用于合金钢熔炼。";
电弧炉.size = 3;
电弧炉.health = 240;
电弧炉.itemCapacity = 40;
电弧炉.hasItems = true;
电弧炉.hasLiquids = false;
电弧炉.hasPower = true;
电弧炉.updateEffect = Fx.none;
电弧炉.ambientSound = Sounds.loopSmelter;
电弧炉.ambientSoundVolume = 0.6;
电弧炉.setupRequirements(Category.crafting, ItemStack.with(
  Items.copper, 140,
  Items.lead, 155,
  Items.graphite, 165,
  Vars.content.item("索拉里斯2.0-钢"), 180
));

电弧炉.craftEffect = new MultiEffect(
  extend(ParticleEffect, {
    particles: 8,
    sizeFrom: 8,
    sizeTo: 0,
    length: 35,
    baseLength: 13,
    lifetime: 35,
    colorFrom: Color.valueOf("73737390"),
    colorTo: Color.valueOf("73737390"),
    cone: 360
  }),
  extend(ParticleEffect, {
    particles: 12,
    line: true,
    length: 43,
    baseLength: 3,
    lifetime: 20,
    colorFrom: Color.valueOf("FFFFFF"),
    colorTo: Color.valueOf("FFE176FF"),
    cone: 360
  }),
  extend(ParticleEffect, {
    particles: 1,
    sizeFrom: 0,
    sizeTo: 45,
    length: 0,
    baseLength: 0,
    lifetime: 10,
    colorFrom: Color.valueOf("FFFFFF80"),
    colorTo: Color.valueOf("FFFFFF00"),
    cone: 360
  })
);

电弧炉.drawer = new DrawMulti(
  new DrawDefault(),
  extend(DrawFlame, {
    flameRadius: 0,
    flameRadiusIn: 0,
    flameRadiusMag: 0,
    flameRadiusInMag: 0
  }),
  new DrawGlowRegion("-glow")
);

new TechTree.TechNode(
  TechTree.all.find(function(node) { return node.content != null && node.content.name == "silicon-smelter"; }),
  电弧炉, 电弧炉.researchRequirements()
);
