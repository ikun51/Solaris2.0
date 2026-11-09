package solaris2.defense;

import arc.*;
import arc.graphics.*;
import arc.math.geom.*;
import mindustry.gen.*;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.*;
import solaris2.effects.FightFx;

/**
 * 由 scripts/建筑/墙/硅合金墙.js 移植而来。
 */
public class SiliconAlloyWall extends Wall{

    public static final Color LIGHTNING_COLOR = Color.valueOf("c0ecffff");
    public static final float REFLECT_RATIO = 0.25f;

    public SiliconAlloyWall(){
        super("硅合金墙");
        description = "受到攻击时，将25%的伤害以闪电形式反击给攻击者。";
        health = 2200;
        size = 1;
        absorbLasers = true;
        buildType = () -> new SiliconAlloyWallBuild();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, Core.bundle.format("ability.reflectwall", Math.round(REFLECT_RATIO * 100)));
    }

    private boolean isHittable(Entityc owner){
        return owner instanceof Unit u ? u.hittable() : true;
    }

    public class SiliconAlloyWallBuild extends WallBuild{

        @Override
        public boolean collision(Bullet bullet){
            boolean handled = super.collision(bullet);

            if(bullet != null && bullet.damage > 0f){
                Entityc owner = bullet.owner;
                if(owner instanceof Healthc h && owner instanceof Teamc t
                    && t.team() != team && isHittable(owner)){
                    float reflect = bullet.damage * REFLECT_RATIO;
                    FightFx.chainLightningFade2.at(x, y, 8f, LIGHTNING_COLOR, new Vec2(t.getX(), t.getY()));
                    h.damage(reflect);
                    FightFx.smallRectBurst.at(t.getX(), t.getY(), LIGHTNING_COLOR);
                }
            }

            return handled;
        }
    }
}
