const PowderSystem = require("lib/PowderSystem");

const powderConfig = {
    ammoTypes: {
        "粗火药": {
            mode: "powder",
            PerShot: 3
        },
            "无烟火药": {
            mode: "powder",
            PerShot: 3,
              damage: 1.45,
              reload: 1.3,
            range: 20,
        }
    },
    itemCapacity: 15,
};

const 重型铳炮 = PowderSystem.create(ItemTurret, "重型铳炮", powderConfig);

重型铳炮.localizedName = "重型铳炮";
重型铳炮.description = "更加重型的铳炮。";
重型铳炮.size = 3;
重型铳炮.health = 600;
重型铳炮.reload = 480;
重型铳炮.range = 200;
重型铳炮.inaccuracy = 1;
重型铳炮.shootCone = 5;
重型铳炮.recoil = 1;
重型铳炮.shootSound = Vars.tree.loadSound("火炮");
重型铳炮.shake = 2;
重型铳炮.maxAmmo = 14;
重型铳炮.ammoPerShot = 2;
重型铳炮.targetAir = false;
重型铳炮.targetGround = true;
重型铳炮.setupRequirements(Category.turret, ItemStack.with(
  Items.copper, 30,
  Vars.content.item("索拉里斯2.0-铁"), 40,
  Items.graphite, 20
));
重型铳炮.alwaysUnlocked = true;

重型铳炮.shootEffect = new MultiEffect(
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

重型铳炮.ammoTypes.put(Vars.content.item("索拉里斯2.0-铁"), extend(BasicBulletType, {
  speed: 7.65,
  lifetime: 26.14,
  damage: 75,
  splashDamage: 52,
  splashDamageRadius: 11.4,
  pierceCap: 2,
  width: 8,
  height: 11,
  backColor: Color.valueOf("6B6B6BFF"),
  frontColor: Color.valueOf("C9C9C9FF"),
  trailLength: 5,
  trailWidth: 1.5,
  trailColor: Color.valueOf("6B6B6BFF")
}));

重型铳炮.ammoTypes.put(Items.copper, extend(BasicBulletType, {
  speed: 6.35,
  lifetime: 31.49,
  splashDamage: 45,
  splashDamageRadius: 8.9,
  damage: 67,
  width: 7,
  height: 10,
  reloadMultiplier: 1.3,
  backColor: Color.valueOf("8395A3FF"),
  frontColor: Color.valueOf("E3ECF2FF"),
  trailLength: 5,
  trailWidth: 1.5,
  trailColor: Color.valueOf("8395A3FF")
}));

重型铳炮.ammoTypes.put(Vars.content.item("索拉里斯2.0-铝"), extend(BasicBulletType, {
  speed: 6.35,
  lifetime: 31.49,
  damage: 50,
  fragBullets: 8,
  fragVelocityMin: 0.6,
  fragVelocityMax: 1.0,
  fragLifeMin: 0.5,
  fragLifeMax: 0.8,
  fragBullet: extend(BasicBulletType, {
    speed: 2.2,
    lifetime: 20,
    damage: 30,
    width: 6,
    height: 8,
    backColor: Color.valueOf("8395A3FF"),
    frontColor: Color.valueOf("E3ECF2FF"),
    splashDamage: 46,
    splashDamageRadius: 8.3,
    hitEffect: Fx.none,
    despawnEffect: Fx.none
  }),
  width: 7,
  height: 10,
  backColor: Color.valueOf("8395A3FF"),
  frontColor: Color.valueOf("E3ECF2FF"),
  trailLength: 5,
  trailWidth: 1.5,
  trailColor: Color.valueOf("8395A3FF")
}));

重型铳炮.drawer = new DrawTurret("基座-");
