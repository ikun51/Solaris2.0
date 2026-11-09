package solaris2.defense;

import arc.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.*;

/**
 * 由 scripts/建筑/墙/大型磁钢墙.js 移植而来。
 */
public class LargeMagneticWall extends Wall{

    public static final float MAX_REDUCTION = 0.30f;
    public static final float CAP_HEALTH_FRACTION = 0.40f;

    public LargeMagneticWall(){
        super("大型磁钢墙");
        description = "受到的伤害越高，减伤比例越高，最高减少30%的伤害。";
        health = 4680;
        size = 2;
        absorbLasers = true;
        buildType = () -> new LargeMagneticWallBuild();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, Core.bundle.format("ability.alloywall", Math.round(MAX_REDUCTION * 100), Math.round(CAP_HEALTH_FRACTION * 100)));
    }

    public class LargeMagneticWallBuild extends WallBuild{

        @Override
        public float handleDamage(float amount){
            float ratio = Math.min(amount / (maxHealth * CAP_HEALTH_FRACTION) * MAX_REDUCTION, MAX_REDUCTION);
            Fx.healBlock.at(x, y, size, Tmp.c1.set(Pal.shield).lerp(Pal.shield, 0.3f));
            return amount * (1f - ratio);
        }
    }
}
