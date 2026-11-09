package solaris2.production;

import arc.util.io.*;
import mindustry.world.blocks.production.Drill;

/**
 * 由 scripts/建筑/机械钻头.js 移植而来。
 */
public class MechanicalDrill extends Drill{

    public static final float wearTime = 3f * 60f * 60f;

    public MechanicalDrill(){
        super("机械钻头");
        buildType = () -> new MechanicalDrillBuild();
    }

    public class MechanicalDrillBuild extends DrillBuild{

        public float age = 0f;

        @Override
        public void updateTile(){
            float wear = 1f - age / wearTime;
            if(wear < 0f) wear = 0f;
            float oldEff = efficiency;
            efficiency = oldEff * wear;

            super.updateTile();

            efficiency = oldEff;
            age += delta();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(age);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            age = read.f();
        }
    }
}
