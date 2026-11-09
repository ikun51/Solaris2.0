const library = require("lib/library");
const 石油裂解塔 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "石油裂解塔",
  [
    {
      input: {
        liquids: ["索拉里斯2.0-石油/10","索拉里斯2.0-蒸汽/4"],
        power: 8,
        heat: 10,
      },
      output: {
        liquids: ["索拉里斯2.0-烯烃/8"],
        result: [
          "索拉里斯2.0-芳香烃/0.45/5",
          "pyratite/0.3/2", 
        ],
      },
            maxEfficiency : 1,
      craftTime: 30,
    },
    {
      input: {
        liquids: ["索拉里斯2.0-燃气/10","索拉里斯2.0-蒸汽/4"],
        power: 1,
        heat: 10,
      },
      output: {
        liquids: ["water/4", "索拉里斯2.0-烯烃/8"],
                         result: [
          "hydrogen/0.3/5",
        ],
      },
            maxEfficiency : 1,  
      craftTime: 30,
    },
  ],
);

石油裂解塔.localizedName = "石油裂解塔";
石油裂解塔.description = "对石化产品进行蒸汽裂解";
石油裂解塔.size = 4;
石油裂解塔.health = 700;
石油裂解塔.itemCapacity = 20;
石油裂解塔.liquidCapacity = 120;
石油裂解塔.hasItems = true;
石油裂解塔.hasLiquids = true;
石油裂解塔.hasPower = true;
石油裂解塔.craftTime = 192;
石油裂解塔.updateEffect = Fx.steam;
石油裂解塔.craftEffect = Fx.smeltsmoke;
石油裂解塔.setupRequirements(Category.crafting, ItemStack.with(
  Vars.content.item("索拉里斯2.0-钢"), 105,
  Items.graphite, 100,
  Items.silicon, 120,
  Items.copper, 185,
  Items.metaglass, 50
));

石油裂解塔.drawer = new DrawMulti(
  new DrawDefault(),
  new DrawHeatInput()
);

new TechTree.TechNode(
  TechTree.all.find(function(node) { return node.content != null && node.content.name == "kiln"; }),
  石油裂解塔, 石油裂解塔.researchRequirements()
);
