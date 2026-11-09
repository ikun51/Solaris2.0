const library = require("lib/library");

const 衰变室 = library.MultiCrafter(
  GenericCrafter,
  GenericCrafter.GenericCrafterBuild,
  "衰变室",
  [

    {
      input: {
        items: ["索拉里斯2.0-高纯铀/3", "索拉里斯2.0-钋/1"],
        liquids: ["nitrogen/6"],
        power: 2,
      },
      output: {
        items: ["索拉里斯2.0-贫铀/3"],
         result: [
          "索拉里斯2.0-钋/0.5/1",
          "索拉里斯2.0-钋/0.8/1"
        ],
      },
      craftTime: 1080,
    },
        {
      input: {
        items: ["索拉里斯2.0-核废料/3", "索拉里斯2.0-钋/1"],
        liquids: ["nitrogen/6"],
        power: 2,
      },
      output: {
        items: ["索拉里斯2.0-贫铀/2"],
         result: [
          "索拉里斯2.0-钋/0.85/1",
          "索拉里斯2.0-钋/0.5/2"
        ],
      },
      craftTime: 900,
    },
  ],
  null,
  {
    afterDestroyed: function(){
      if(this.efficiency <= 0 || this.block.destroyBullet == null) return;
      this.block.destroyBullet.create(this, this.block.destroyBulletSameTeam ? this.team : Team.derelict, this.x, this.y, Mathf.random(360));
    }
  }
);

衰变室.localizedName = "衰变室";
衰变室.description = "衰变核材料，产出高纯钋";
衰变室.size = 3;
衰变室.category = Category.crafting;
衰变室.itemCapacity = 30;
衰变室.hasPower = true;
衰变室.hasItems = true;
衰变室.ambientSound = Sounds.loopHum;
衰变室.ambientSoundVolume = 0.03;
衰变室.requirements = ItemStack.with(
  Items.copper, 200,
  Items.graphite, 130,
  Items.titanium, 180,
  Vars.content.item("索拉里斯2.0-硅铁"), 100,
  Items.plastanium, 70
);
衰变室.buildVisibility = BuildVisibility.shown;

衰变室.drawer = new DrawMulti(
  new DrawDefault(),
  extend(DrawGlowRegion, {
    suffix: "-glow",
    alpha: 0.9
  })
);

衰变室.destroyBullet = extend(BasicBulletType, {
  despawnSound: Sounds.wind3,
  despawnEffect: Fx.massiveExplosion,
  damage: 0,
  speed: 0,
  lifetime: 1,
  frontColor: Color.valueOf("ffffff"),
  backColor: Color.valueOf("ffffff"),
  width: 32,
  height: 32,
  fragBullets: 360,
  hitSize: 1,
  hittable: false,
  fragSpread: 1,
  fragRandomSpread: 0,
  fragVelocityMin: 1,
  fragBullet: extend(BasicBulletType, {
    damage: 0,
    knockback: 16,
    lifetime: 18,
    impact: true,
    speed: 5,
    height: 32,
    width: 32,
    buildingDamageMultiplier: 0,
    hitEffect: Fx.smeltsmoke,
    despawnEffect: Fx.none,
    frontColor: Color.valueOf("ffffff"),
    backColor: Color.valueOf("ffffff"),
    collidesTiles: false,
    hitSize: 16,
    collidesGround: false,
    hittable: false,
    reflectable: false
  })
});
