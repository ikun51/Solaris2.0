/**
 * powder-turret-test.js — 火药系统模板测试炮台
 *
 * 配合 content/blocks/炮台/powder-turret-test.json 使用：
 *   - JSON 负责定义炮台属性（尺寸、射程、原版弹药 ammoTypes 等）
 *   - 本脚本只负责给炮台挂载独立火药系统（PowderSystem 库）
 *
 * 测试方法：
 *   1. 给炮台正常供给原版弹药（如铜）——否则无法射击
 *   2. 再给炮台供给火药（粗火药 / 高能火药 / 硝基炸药）——激活后才有加成
 *   3. 两个条件缺一不可，只有原版弹药没有火药时炮台无法攻击
 *   4. 切换机制：当前火药未耗尽前，输入其它火药会被拒绝；
 *      只有当前火药库存耗尽后，新输入的火药才会装载生效
 *   5. 建造栏点击炮台可查看支持的火药类型及倍率数值
 */

const PowderSystem = require("lib/PowderSystem");

// 火药系统配置
const powderConfig = {
    // 火药物品名 -> 加成数据（物品名不带 mod 前缀，会自动匹配）
    // 差异化配置：每种火药的 伤害/倍率 等参数均可在此调整，无需改代码
    ammoTypes: {
        "一氧化碳": {
            mode: "liquid",
            damage: 1.2,
            range: 20,
            reload: 1.3,
            PerShot: 10
        }
    },
    itemCapacity: 30,
    liquidCapacity: 120,
};

// 创建火药炮台：extend + 建造栏统计 + 应用火药系统（属性由同名 JSON 填充）
const powderTurretTest = PowderSystem.create(ItemTurret, "powder-turret-test", powderConfig);

powderTurretTest.size = 2;
powderTurretTest.health = 800;
powderTurretTest.reload = 24;
powderTurretTest.range = 160;
powderTurretTest.inaccuracy = 3;
powderTurretTest.shootCone = 15;
powderTurretTest.recoil = 1.5;
powderTurretTest.shootSound = Sounds.shoot;
powderTurretTest.maxAmmo = 30;
powderTurretTest.ammoPerShot = 1;
powderTurretTest.hasLiquids = true;
powderTurretTest.liquidCapacity = 60;
powderTurretTest.targetAir = true;
powderTurretTest.targetGround = true;
powderTurretTest.setupRequirements(Category.turret, ItemStack.with(
  Items.copper, 40,
  Items.graphite, 25
));

powderTurretTest.ammoTypes.put(Items.copper, extend(BasicBulletType, {
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

powderTurretTest.ammoTypes.put(Items.graphite, extend(BasicBulletType, {
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
  powderTurretTest, powderTurretTest.researchRequirements()
);
