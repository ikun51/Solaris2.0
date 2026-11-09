package solaris2.heat;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

/**
 * 由 scripts/建筑/热量/热能泵.js 移植而来。
 */
public class ThermalPump extends Pump{

    public static TextureRegion glowRegion;

    public float heatRequirement = 4f;

    public ThermalPump(String name){
        super(name);
        buildType = ThermalPumpBuild::new;
    }

    @Override
    public void load(){
        super.load();
        glowRegion = Core.atlas.find(name + "-glow");
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("heat", (ThermalPumpBuild e) -> new Bar(
            () -> {
                float heatVal = e.heat();
                float eff = Math.min(heatVal / heatRequirement, 1f);
                return "热量：" + (int)Math.floor(heatVal) + "（" + (int)Math.floor(eff * 100) + "%）";
            },
            () -> Pal.lightOrange,
            () -> Math.min(e.heat() / heatRequirement, 1f)
        ));
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.input, heatRequirement, StatUnit.heatUnits);
    }

    public class ThermalPumpBuild extends PumpBuild{

        public float heat = 0f;
        public float[] sideHeat = new float[4];

        public float heat(){
            return heat;
        }

        @Override
        public void draw(){
            super.draw();
            if(glowRegion != null && heat > 0.001f){
                float heatFrac = Math.min(heat / heatRequirement, 1f);
                float z = Draw.z();
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                Draw.color(Pal.lightOrange);
                Draw.alpha((Mathf.absin(Time.time, 10f, 0.9f) * 0.5f + 0.5f) * heatFrac * 0.9f);
                Draw.rect(glowRegion, x, y);
                Draw.reset();
                Draw.blend();
                Draw.z(z);
            }
        }

        @Override
        public void updateTile(){
            heat = calculateHeat(sideHeat);
            super.updateTile();
        }

        @Override
        public boolean shouldConsume(){
            return heat >= heatRequirement && super.shouldConsume();
        }
    }
}
