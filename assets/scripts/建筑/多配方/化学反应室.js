const library = require("lib/library");
const 化学反应室 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "化学反应室",
  [
    {
      input: {
        items: ["coal/2"],
        liquids: ["索拉里斯2.0-蒸汽/10"],
        power: 3,
      },
      output: {
        liquids: ["索拉里斯2.0-一氧化碳/10", "hydrogen/10"],
      },
      craftTime: 15,
    },
    {
      input: {
        liquids: ["索拉里斯2.0-醇/10"],
        power: 1,
      },
      output: {
        liquids: ["water/4", "索拉里斯2.0-烯烃/6"],
      },
      craftTime: 30,
    },
    {
      input: {
        liquids: ["索拉里斯2.0-氧气/10", "hydrogen/20"],
      },
      output: {
        liquids: ["water/10"],
        power: 2,
      },
      craftTime: 15,
    },
    {
	input:{
	    liquids:[
	        "索拉里斯2.0-硫酸/15",
	        "hydrogen/15",
	    ],
	    power:3,
	},
	output:{
	    items:[
	        "pyratite/3"
	    ],
	    liquids:[
	        "water/15"
	    ]
	},
	crafttime:30,
	},
      {
      input: {
        liquids: ["索拉里斯2.0-一氧化碳/10", "hydrogen/10"],
        power: 1,
      },
      output: {
        liquids: ["索拉里斯2.0-烷烃/6"],
      },
      craftTime: 30,
    },
          {
      input: {
        items: ["索拉里斯2.0-硝石/1"],
               liquids: ["索拉里斯2.0-硫酸/6"],
        power: 1,
      },
      output: {
        liquids: ["索拉里斯2.0-硝酸/3"],
      },
      craftTime: 30,
    },
  ],
);

化学反应室.localizedName = "化学反应室";
化学反应室.description = "化学合成的场所";
化学反应室.size = 3;
化学反应室.squareSprite = true;
化学反应室.hasItems = true;
化学反应室.hasLiquids = true;
化学反应室.hasPower = true;
化学反应室.health = 360;
化学反应室.itemCapacity = 25;
化学反应室.liquidCapacity = 240;
化学反应室.updateEffect = Fx.smoke;
化学反应室.updateEffectChance = 0.08;
化学反应室.ambientSound = Sounds.loopSmelter;
化学反应室.ambientSoundVolume = 0.4;
化学反应室.setupRequirements(Category.crafting, ItemStack.with(
  Vars.content.item("索拉里斯2.0-钢"), 55,
  Items.graphite, 20,
  Items.silicon, 20,
  Items.copper, 85
));

function liquidTile(liquid, padLeft, padRight, padBottom, padTop) {
  var tile = new DrawLiquidTile(liquid);
  tile.padLeft = padLeft;
  tile.padRight = padRight;
  tile.padBottom = padBottom;
  tile.padTop = padTop;
  return tile;
}

化学反应室.drawer = new DrawMulti(
  new DrawRegion("-bottom"),
  liquidTile(Liquids.water, 14.1, 4, 14.1, 4),
  liquidTile(Liquids.water, 4, 14.1, 14.1, 4),
  liquidTile(Liquids.water, 4, 14.1, 4, 14.1),
  liquidTile(Liquids.water, 14.1, 4, 4, 14.1),
  liquidTile(Vars.content.liquid("索拉里斯2.0-一氧化碳"), 8, 8, 8, 8),
  liquidTile(Liquids.hydrogen, 14.1, 4, 14.1, 4),
  liquidTile(Liquids.hydrogen, 4, 14.1, 14.1, 4),
  liquidTile(Liquids.hydrogen, 4, 14.1, 4, 14.1),
  liquidTile(Liquids.hydrogen, 14.1, 4, 4, 14.1),
  liquidTile(Vars.content.liquid("索拉里斯2.0-氧气"), 8, 8, 8, 8),
  liquidTile(Vars.content.liquid("索拉里斯2.0-醇"), 14.1, 4, 14.1, 4),
  liquidTile(Vars.content.liquid("索拉里斯2.0-醇"), 4, 14.1, 14.1, 4),
  liquidTile(Vars.content.liquid("索拉里斯2.0-醇"), 4, 14.1, 4, 14.1),
  liquidTile(Vars.content.liquid("索拉里斯2.0-醇"), 14.1, 4, 4, 14.1),
  liquidTile(Vars.content.liquid("索拉里斯2.0-氧气"), 8, 8, 8, 8),
  liquidTile(Liquids.hydrogen, 14.1, 4, 14.1, 4),
  liquidTile(Liquids.hydrogen, 4, 14.1, 14.1, 4),
  liquidTile(Liquids.hydrogen, 4, 14.1, 4, 14.1),
  liquidTile(Liquids.hydrogen, 14.1, 4, 4, 14.1),
  liquidTile(Vars.content.liquid("索拉里斯2.0-硫酸"), 8, 8, 8, 8),
  new DrawDefault()
);
