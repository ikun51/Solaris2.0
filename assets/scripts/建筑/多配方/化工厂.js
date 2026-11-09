const library = require("lib/library");
const 化工厂 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "化工厂",
  [
      {
	input:{
	    liquids:[
	        "索拉里斯2.0-氧气/10",
	        "索拉里斯2.0-蒸汽/10",
			"索拉里斯2.0-二氧化硫/6"
	    ],
	    power:3,
	},
	output:{
	    liquids:[
	        "索拉里斯2.0-硫酸/20"
	    ],
	},
	crafttime:30,
	 },
	{
	input:{
	    liquids:[
	        "nitrogen/10",
	        "hydrogen/15"
	    ],
	    power:4,
	},
	output:{
	    liquids:[
	        "索拉里斯2.0-合成氨/20"
	    ],
	},
	crafttime:15,
	},
	{
	input:{
	    liquids:[
	        "索拉里斯2.0-合成氨/20",
	        "索拉里斯2.0-氧气/18",
	        "索拉里斯2.0-蒸汽/2"
	    ],
	    power:4,
	},
	output:{
	    liquids:[
	        "索拉里斯2.0-硝酸/30"
	    ],
	},
	crafttime:30,
	},
	{
	input:{
	    items:[
	        "graphite/2"
	    ],
	    liquids:[
	        "索拉里斯2.0-合成氨/22"
	    ],
	    power:3,
	},
	output:{
	    liquids:[
	        "cyanogen/28"
	    ],
	},
	crafttime:15,
	},
		{
	input:{
	    items:[
	        "graphite/2"
	    ],
	    liquids:[
	        "索拉里斯2.0-燃气/18","water/6"
	    ],
	    power:3,
	},
	output:{
	    liquids:[
	        "hydrogen/12","索拉里斯2.0-一氧化碳/10"
	    ],
	},
	crafttime:15,
		},
  ]
);

化工厂.localizedName = "化工厂";
化工厂.description = "进行大型化工生产,无法直接建造";
化工厂.size = 4;
化工厂.health = 360;
化工厂.hasItems = true;
化工厂.hasLiquids = true;
化工厂.hasPower = true;
化工厂.itemCapacity = 30;
化工厂.liquidCapacity = 280;
化工厂.craftEffect = Fx.steam;
化工厂.ambientSound = Vars.tree.loadSound("steam");
化工厂.ambientSoundVolume = 0.5;
化工厂.setupRequirements(Category.crafting, ItemStack.with(
  Vars.content.item("索拉里斯2.0-塑料"), 2555,
  Items.metaglass, 400,
  Items.silicon, 520,
  Items.thorium, 240,
  Items.titanium, 930,
  Items.graphite, 160
));
