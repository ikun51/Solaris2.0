package solaris2.support;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.meta.Stat;

import static mindustry.Vars.*;

/** 冷冻力场：消耗冷冻液，熄灭范围内火焰、冻结敌人、减速子弹。 */
public class CryoField extends BaseShield{

    public static final int jhr = 10;
    public static final int jhrn = 10;
    public static final float bulletMinSpeed = 1f;

    public static final Effect jhy = new Effect(10f, e -> {
        Draw.color(Color.blue);
        Lines.stroke(e.fout() * 2f);
        Lines.poly(e.x, e.y, 4, e.fin() * jhr * jhrn, 45f);
    });

    public CryoField(String name){
        super(name);
        buildType = CryoFieldBuild::new;
        update = true;
        hasPower = false;
        radius = jhr * jhrn;
        sides = 24;
        shieldColor = Color.blue;
        hasLiquids = true;
        liquidCapacity = 60f;
        consumeLiquid(Liquids.cryofluid, 1.5f);
        itemCapacity = 10;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, Core.bundle.format("ability.zerofield", Math.round(jhr * jhrn / 8f)));
    }

    public class CryoFieldBuild extends BaseShieldBuild{
        public boolean work = false;
        public float amout = 2f;

        @Override
        public void updateTile(){
            smoothRadius = Mathf.lerpDelta(smoothRadius, radius, 0.05f);

            if(liquids.get(Liquids.cryofluid) >= 6f){
                work = true;

                if(timer.get(90f)){
                    jhy.at(x, y, jhr * jhrn, Color.blue);

                    int radiusInTiles = (int)Math.floor(jhr * jhrn / 8f);
                    for(int dx = -radiusInTiles; dx <= radiusInTiles; dx++){
                        for(int dy = -radiusInTiles; dy <= radiusInTiles; dy++){
                            Tile tile = world.tile(tileX() + dx, tileY() + dy);
                            if(tile != null){
                                Fires.extinguish(tile, 9999f);
                            }
                        }
                    }

                    amout += 1f;
                }

                Units.nearbyEnemies(team,
                        x - jhr * jhrn / 2f,
                        y - jhr * jhrn / 2f,
                        jhr * jhrn,
                        jhr * jhrn,
                        other -> {
                            if(other != null && other.isValid()){
                                other.apply(StatusEffects.freezing, 300f);
                            }
                        });

                Groups.bullet.intersect(
                        x - jhr * jhrn / 2f,
                        y - jhr * jhrn / 2f,
                        jhr * jhrn,
                        jhr * jhrn,
                        other -> {
                            if(other.team != team && other.vel.len() > bulletMinSpeed){
                                other.vel.scl(0.95f);
                                if(other.vel.len() < bulletMinSpeed){
                                    other.vel.setLength(bulletMinSpeed);
                                }
                            }
                        });
            }else{
                work = false;
            }

            broken = !work;

            if(amout >= 2f){
                liquids.remove(Liquids.cryofluid, 6f);
                amout = 0f;
            }
        }
    }
}
