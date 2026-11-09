package solaris2.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** 气象屏蔽器：消耗电力，清除范围内单位的气象状态。 */
public class WeatherShield extends GenericCrafter{

    public static final float shieldRadius = 15f * tilesize;
    public static final float powerUse = 8f;

    public WeatherShield(){
        super("气象屏蔽器");
        hasItems = false;
        hasLiquids = false;
        consumePower(powerUse);
        buildType = WeatherShieldBuild::new;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, shieldRadius / tilesize, StatUnit.blocks);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        float px = x * tilesize + offset;
        float py = y * tilesize + offset;

        // 电网样式：半透明填充 + 实线圆边框
        Draw.color(Pal.accent, 0.15f);
        Fill.circle(px, py, shieldRadius);
        Draw.color(Pal.accent, 0.5f);
        Lines.stroke(1f);
        Lines.circle(px, py, shieldRadius);
        Draw.reset();
    }

    public class WeatherShieldBuild extends GenericCrafterBuild{
        public float purgeTimer = 0f;       // 净化计时
        public float purgeInterval = 3f;    // 每 3 帧净化一次

        @Override
        public void updateTile(){
            super.updateTile();
            // 只要通电就生效
            if(power == null || power.status <= 0) return;

            purgeTimer += Time.delta;
            if(purgeTimer >= purgeInterval){
                purgeTimer = 0;
                Team team = this.team;
                float bx = x, by = y;
                Groups.weather.each(state -> {
                    StatusEffect st = state.weather.status;
                    if(st == null || st == StatusEffects.none) return;
                    Units.nearby(team, bx, by, shieldRadius, u -> {
                        if(u != null && !u.dead) u.unapply(st);
                    });
                });
            }
        }

        @Override
        public boolean shouldConsume(){
            return true;
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            Draw.color(Pal.accent, 0.15f);
            Fill.circle(x, y, shieldRadius);
            Draw.color(Pal.accent, 0.5f);
            Lines.stroke(1f);
            Lines.circle(x, y, shieldRadius);
            Draw.reset();
        }
    }
}
