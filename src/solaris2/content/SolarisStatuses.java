package solaris2.content;

import arc.graphics.Color;
import arc.struct.ObjectSet;
import mindustry.content.StatusEffects;
import mindustry.entities.effect.ParticleEffect;
import mindustry.type.StatusEffect;

/**
 * 状态效果：原 assets/content/status 下 json 的 Java 实现。
 */
public class SolarisStatuses{

    public static StatusEffect emp1, brokenArmor, radiationDamage;

    static{
        emp1 = new StatusEffect("emp1");
        emp1.localizedName = "EMP";
        emp1.color = color("88DDFF");
        emp1.speedMultiplier = 0.5f;
        emp1.reloadMultiplier = 0.2f;
        emp1.effectChance = 0.4f;
        emp1.opposites = ObjectSet.with(StatusEffects.boss);
        emp1.effect = spark();

        brokenArmor = new StatusEffect("碎甲");
        brokenArmor.color = color("00000000");
        brokenArmor.speedMultiplier = 0.75f;
        brokenArmor.reloadMultiplier = 0.6f;
        brokenArmor.healthMultiplier = 0.75f;
        brokenArmor.damageMultiplier = 0.8f;
        brokenArmor.show = true;
        brokenArmor.effect = shard();

        radiationDamage = new StatusEffect("辐射损害");
        radiationDamage.color = color("00FF66");
        radiationDamage.damage = 0.1f;
        radiationDamage.effectChance = 0.1f;
        radiationDamage.effect = radiation();
    }

    /** 由主类或 JS 调用，触发静态初始化。 */
    public static void load(){
    }

    private static Color color(String hex){
        return Color.valueOf(hex.length() == 6 ? hex + "FF" : hex);
    }

    private static ParticleEffect spark(){
        ParticleEffect fx = new ParticleEffect();
        fx.particles = 2;
        fx.baseLength = 0f;
        fx.length = 20f;
        fx.lifetime = 8f;
        fx.line = true;
        fx.lenFrom = 14f;
        fx.lenTo = 0f;
        fx.strokeFrom = 1.5f;
        fx.strokeTo = 0f;
        fx.colorFrom = color("88DDFF");
        fx.colorTo = color("FFFFFF");
        return fx;
    }

    private static ParticleEffect shard(){
        ParticleEffect fx = new ParticleEffect();
        fx.particles = 1;
        fx.baseLength = 0f;
        fx.length = 65f;
        fx.lifetime = 10f;
        fx.spin = 12f;
        fx.region = "shape-3";
        fx.sizeFrom = 4f;
        fx.sizeTo = 0f;
        fx.colorFrom = color("FF5B5B");
        fx.colorTo = color("FFFFFF");
        fx.cone = 360f;
        return fx;
    }

    private static ParticleEffect radiation(){
        ParticleEffect fx = new ParticleEffect();
        fx.particles = 2;
        fx.baseLength = 0f;
        fx.length = 15f;
        fx.lifetime = 26f;
        fx.line = true;
        fx.lenFrom = 8f;
        fx.lenTo = 0f;
        fx.strokeFrom = 1.2f;
        fx.strokeTo = 0f;
        fx.cone = 360f;
        fx.colorFrom = color("00FF66");
        fx.colorTo = color("FFFFFF");
        return fx;
    }
}
