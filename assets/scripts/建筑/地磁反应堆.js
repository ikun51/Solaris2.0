/*var fFX = require("lib/fightFx");


var 地磁反应堆 = extend(PowerGenerator, "地磁反应堆", {});
地磁反应堆.size = 5;
地磁反应堆.health = 2400;
地磁反应堆.powerProduction = 1750;
地磁反应堆.explosionRadius = 25;
地磁反应堆.explosionDamage = 2500;
地磁反应堆.explosionShake = 8;
地磁反应堆.explosionShakeDuration = 120;
地磁反应堆.configurable = true;
地磁反应堆.buildCostMultiplier = 0.85;
地磁反应堆.buildVisibility = BuildVisibility.shown;
地磁反应堆.category = Category.power;
地磁反应堆.hasItems = false;
地磁反应堆.hasLiquids = true;
地磁反应堆.liquidCapacity = 40;
// 需要输入电力 500/s，消耗冷却液 0.5/s
地磁反应堆.consumePower(500);
地磁反应堆.consumeCoolant(0.5);

var color = Color.valueOf("cedef3");

地磁反应堆.buildType = prov(function() {
    var drain = 500;
    var output = 0;

    return new JavaAdapter(PowerGenerator.GeneratorBuild, {
        update: function() {
            this.super$update();
            // efficiency 由消耗系统自动计算 = min(供电覆盖率, 冷却液供给率)
            output = drain * 3.5 * this.efficiency;

            if (output > 0 && Mathf.chance(0.06)) {
                var tx = this.x + Mathf.range(80);
                var ty = this.y + Mathf.range(80);
                fFX.chainLightningFade2.at(this.x, this.y, Mathf.random(15, 35), color, new Vec2(tx, ty));
                Damage.damage(tx, ty, 8, 85);
                Sounds.shootArc.at(new Vec2(tx, ty));
            }

            if (output > 0 && Mathf.chance(0.06)) {
                var self = this;
                Units.nearby(null, self.x, self.y, 128, function(other) {
                    fFX.chainLightningFade2.at(self.x, self.y, 25, color, other);
                    Damage.damage(other.x, other.y, 8, 47);
                });
            }
        },

        getPowerProduction: function() {
            return output;
        },

        onDestroyed: function() {
            this.super$onDestroyed();
            for (var i = 0; i < 5; i++) {
                Sounds.explosionReactor2.at(new Vec2(this.x + Mathf.range(160), this.y + Mathf.range(160)));
            }
        }
    }, 地磁反应堆);
});

module.exports = { 地磁反应堆: 地磁反应堆 };*/
