/**
 * powder-mode-test.js — 火药系统三种模式测试炮台
 *
 * 每种火药的 mode 由各自的配置条目决定：
 *   - mode: "powder"  只需要固体火药即可攻击
 *   - mode: "liquid"  只需要液体火药即可攻击
 *   - mode: "both"    需要固体+液体都装载才能攻击，加成相加
 *
 * 液体火药直接写液体名，走管道自动识别为液体
 *
 * 测试方法：
 *   1. 装"粗火药"（powder）：只送火药 + 原版弹药即可射击
 *   2. 装"燃气"（liquid）：只送液体 + 原版弹药即可射击
 *   3. 装"无烟火药"（both）：必须同时送火药和液体 + 原版弹药才能射击
 */

const PowderSystem = require("lib/PowderSystem");

// 火药系统配置
const powderConfig = {
    ammoTypes: {
        // 模式1：只用火药
        "粗火药": {
            mode: "powder",
            damage: 1.5,
            range: 30,
            PerShot: 1
        },
        // 模式2：只用液体（直接写液体名，走管道自动识别为液体）
        "硝基炸药": {
            mode: "powder",
            damage: 1.2,
            range: 20,
            reload: 1.3,
            PerShot: 10,
            selfDamagePercent: 2
        },
        // 模式3：火药 + 液体都要（加成相加）
        "无烟火药": {
            mode: "powder",
            damage: 1.5,
            range: 40,
            reload: 1.5,
            PerShot: 1
        }
    },

};

// 创建火药炮台：extend + 建造栏统计 + 应用火药系统（属性由同名 JSON 填充）
const powderModeTest = PowderSystem.create(ItemTurret, "powder-mode-test", powderConfig);

powderModeTest.size = 2;
powderModeTest.health = 800;
powderModeTest.reload = 24;
powderModeTest.range = 160;
powderModeTest.inaccuracy = 3;
powderModeTest.shootCone = 15;
powderModeTest.recoil = 1.5;
powderModeTest.shootSound = Sounds.shoot;
powderModeTest.maxAmmo = 30;
powderModeTest.ammoPerShot = 1;
powderModeTest.liquidCapacity = 60;
powderModeTest.targetAir = true;
powderModeTest.targetGround = true;
powderModeTest.setupRequirements(Category.turret, ItemStack.with(
  Items.copper, 40,
  Items.graphite, 25
));

powderModeTest.ammoTypes.put(Items.copper, extend(BasicBulletType, {
  speed: 3,
  lifetime: 55,
  damage: 14,
  width: 7,
  height: 9,
  backColor: Color.valueOf("D99D4EFF"),
  frontColor: Color.valueOf("FFF2D7FF"),
  trailLength: 4,
  trailWidth: 1.5,
  trailColor: Color.valueOf("D99D4EFF")
}));

powderModeTest.ammoTypes.put(Items.graphite, extend(BasicBulletType, {
  speed: 3.5,
  lifetime: 50,
  damage: 20,
  width: 8,
  height: 10,
  backColor: Color.valueOf("4B4B57FF"),
  frontColor: Color.valueOf("C6C6D2FF"),
  trailLength: 4,
  trailWidth: 1.5,
  trailColor: Color.valueOf("4B4B57FF")
}));

new TechTree.TechNode(
  TechTree.all.find(function(node) { return node.content != null && node.content.name == "duo"; }),
  powderModeTest, powderModeTest.researchRequirements()
);
