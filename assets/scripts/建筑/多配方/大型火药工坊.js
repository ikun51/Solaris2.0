const library = require("lib/library");
    const 大型火药工坊 = library.MultiCrafter(
      GenericCrafter,
      GenericCrafter.GenericCrafterBuild,
      "大型火药工坊",
      [
        {
          input: {
            items: ["coal/1", "pyratite/1", "索拉里斯2.0-硝石/2"],
            power: 0.5,
          },
          output: {
            items: ["索拉里斯2.0-粗火药/3"], 
          },
          craftTime: 150,
        },
        {
          input: {
            items: ["索拉里斯2.0-塑料/1","索拉里斯2.0-硝石/2"],
            liquids: ["索拉里斯2.0-醇/6"], 
            power: 0.5,
          },
          output: {
            items: ["索拉里斯2.0-无烟火药/3"], 
          },
          craftTime: 150,
        },
                {
          input: {
            items: ["索拉里斯2.0-无烟火药/2"],
            liquids: ["索拉里斯2.0-硝酸/10","hydrogen/6"], 
            power: 1,
          },
          output: {
            items: ["索拉里斯2.0-高能火药/3"], 
          },
          craftTime: 260,
        },
      ],
    );

大型火药工坊.localizedName = "大型火药工坊";
大型火药工坊.description = "将煤炭、硝石与火焰素混合研磨，产出粗火药。";
大型火药工坊.size = 2;
大型火药工坊.health = 120;
大型火药工坊.itemCapacity = 20;
大型火药工坊.hasItems = true;
大型火药工坊.hasLiquids = true;
大型火药工坊.hasPower = true;
大型火药工坊.craftEffect = Fx.smoke;
大型火药工坊.setupRequirements(Category.crafting, ItemStack.with(
  Items.copper, 50,
  Items.silicon, 40,
  Vars.content.item("索拉里斯2.0-铁"), 40,
  Vars.content.item("索拉里斯2.0-塑料"), 25
));
