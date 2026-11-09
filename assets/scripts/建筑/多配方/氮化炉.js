const library = require("lib/library");
const 氮化炉 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "氮化炉",
  [
    {
      input: {
        items: ["silicon/9"],
        liquids: ["nitrogen/6"], 
        power: 4,
        heat: 6,
      },
      output: {
        items: ["索拉里斯2.0-氮化硅/3"], 
      },
      craftTime: 360,
      maxEfficiency : 2
    },
       {
      input: {
        items: ["silicon/9"],
        liquids: ["索拉里斯2.0-合成氨/12"], 
        power: 4,
        heat: 8,
      },
      output: {
        items: ["索拉里斯2.0-氮化硅/3"], 
        liquids: ["hydrogen/6"], 
      },
      craftTime: 360,
      maxEfficiency : 2
    },
  ],
);

氮化炉.localizedName = "氮化炉";
氮化炉.description = "通入氮气与硅,合成氮化硅晶体";
氮化炉.size = 3;
氮化炉.health = 440;
氮化炉.hasItems = true;
氮化炉.hasLiquids = true;
氮化炉.hasPower = true;
氮化炉.itemCapacity = 40;
氮化炉.ambientSound = Sounds.loopSmelter;
氮化炉.ambientSoundVolume = 0.6;
氮化炉.setupRequirements(Category.crafting, ItemStack.with(
  Items.copper, 140,
  Items.tungsten, 155,
  Items.graphite, 165,
  Vars.content.item("索拉里斯2.0-钢"), 180
));

氮化炉.drawer = new DrawMulti(
  new DrawRegion("-bottom"),
  new DrawLiquidTile(Liquids.nitrogen),
  new DrawArcSmelt(),
  new DrawDefault(),
  new DrawHeatInput()
);

new TechTree.TechNode(
  TechTree.all.find(function(node) { return node.content != null && node.content.name == "silicon-smelter"; }),
  氮化炉, 氮化炉.researchRequirements()
);
