const PowderSystem = require("lib/PowderSystem");

const powderConfig = {
    ammoTypes: {
        "粗火药": {
            mode: "powder",
            PerShot: 1
        },
    },
    itemCapacity: 30,
};

const 铳炮 = PowderSystem.create(ItemTurret, "铳炮", powderConfig);

铳炮.localizedName = "铳炮";
铳炮.description = "发射炮弹，非常原始";
铳炮.size = 2;
铳炮.health = 600;
铳炮.reload = 300;
铳炮.range = 150;
铳炮.inaccuracy = 2;
铳炮.shootCone = 3;
铳炮.recoil = 3;
铳炮.shootSound = Vars.tree.loadSound("火炮");
铳炮.shake = 1.5;
铳炮.maxAmmo = 10;
铳炮.ammoPerShot = 1;
铳炮.targetAir = true;
铳炮.targetGround = true;
铳炮.setupRequirements(Category.turret, ItemStack.with(
  Items.copper, 30,
  Items.graphite, 20
));
铳炮.alwaysUnlocked = true;

铳炮.shootEffect = new MultiEffect(
  extend(ParticleEffect, {
    particles: 14,
    sizeFrom: 5,
    sizeTo: 2,
    lifetime: 26,
    length: 7,
    cone: 30,
    colorFrom: Color.valueOf("C4C4C4FF"),
    colorTo: Color.valueOf("4A4A4AFF"),
    interp: Interp.pow2Out
  }),
  extend(ParticleEffect, {
    particles: 8,
    sizeFrom: 9,
    sizeTo: 3,
    lifetime: 34,
    length: 11,
    cone: 40,
    colorFrom: Color.valueOf("949494FF"),
    colorTo: Color.valueOf("333333FF"),
    interp: Interp.pow2In
  }),
  extend(ParticleEffect, {
    particles: 10,
    sizeFrom: 4,
    sizeTo: 1,
    lifetime: 20,
    length: 5,
    cone: 25,
    colorFrom: Color.valueOf("E0E0E0FF"),
    colorTo: Color.valueOf("6E6E6EFF"),
    interp: Interp.pow3Out
  })
);

铳炮.ammoTypes.put(Vars.content.item("索拉里斯2.0-铁"), extend(BasicBulletType, {
  speed: 6.8,
  lifetime: 23.53,
  damage: 50,
  width: 8,
  height: 11,
  backColor: Color.valueOf("6B6B6BFF"),
  frontColor: Color.valueOf("C9C9C9FF"),
  trailLength: 5,
  trailWidth: 1.5,
  trailColor: Color.valueOf("6B6B6BFF")
}));

铳炮.ammoTypes.put(Items.copper, extend(BasicBulletType, {
  speed: 7.65,
  lifetime: 21.18,
  damage: 38,
  width: 7,
  height: 10,
  reloadMultiplier: 1.3,
  backColor: Color.valueOf("8395A3FF"),
  frontColor: Color.valueOf("E3ECF2FF"),
  trailLength: 5,
  trailWidth: 1.5,
  trailColor: Color.valueOf("8395A3FF")
}));

铳炮.drawer = new DrawTurret("基座-");
