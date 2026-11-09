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
            range: 10
        }
    },
    itemCapacity: 15,
};

const 多管发射器 = PowderSystem.create(ItemTurret, "多管发射器", powderConfig);

多管发射器.localizedName = "多管发射器";
多管发射器.description = "多管齐射的近距离火力塔。一轮倾泻二十二发穿甲弹幕，之后需要漫长装填。";
多管发射器.size = 3;
多管发射器.health = 900;
多管发射器.reload = 500;
多管发射器.range = 72;
多管发射器.inaccuracy = 6;
多管发射器.shootCone = 8;
多管发射器.recoil = 2;
多管发射器.shootSound = Vars.tree.loadSound("火炮");
多管发射器.shoot.shots = 22;
多管发射器.shoot.shotDelay = 7;
多管发射器.maxAmmo = 132;
多管发射器.ammoPerShot = 2;
多管发射器.targetAir = true;
多管发射器.targetGround = true;
多管发射器.setupRequirements(Category.turret, ItemStack.with(
  Items.copper, 70,
  Items.graphite, 50,
  Vars.content.item("索拉里斯2.0-铁"), 60
));
多管发射器.alwaysUnlocked = true;

多管发射器.shootEffect = new MultiEffect(
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

多管发射器.ammoTypes.put(Vars.content.item("索拉里斯2.0-铁"), extend(BasicBulletType, {
  speed: 8,
  lifetime: 9,
  damage: 20,
  pierce: true,
  pierceCap: 3,
  width: 8,
  height: 11,
  backColor: Color.valueOf("6B6B6BFF"),
  frontColor: Color.valueOf("C9C9C9FF"),
  trailLength: 3,
  trailWidth: 1.2,
  trailColor: Color.valueOf("6B6B6BFF")
}));

多管发射器.ammoTypes.put(Vars.content.item("索拉里斯2.0-铝"), extend(BasicBulletType, {
  speed: 8,
  lifetime: 9,
  damage: 16,
  pierce: true,
  pierceCap: 4,
  width: 8,
  height: 11,
  backColor: Color.valueOf("8395A3FF"),
  frontColor: Color.valueOf("E3ECF2FF"),
  trailLength: 3,
  trailWidth: 1.2,
  trailColor: Color.valueOf("8395A3FF")
}));

多管发射器.drawer = new DrawTurret("基座-");
