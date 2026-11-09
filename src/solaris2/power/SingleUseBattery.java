package solaris2.power;

import arc.struct.*;
import mindustry.gen.*;
import mindustry.world.blocks.power.Battery;

/** 一次性电池：建造即满电，无法被电网充电，只能放电。 */
public class SingleUseBattery extends Battery{

    public SingleUseBattery(){
        super("一次性电池");
        update = true;
        buildType = SingleUseBatteryBuild::new;
    }

    public class SingleUseBatteryBuild extends BatteryBuild{
        public float pbLast = Float.NaN;

        @Override
        public void created(){
            super.created();
            power.status = 1.0f;
            pbLast = 1.0f;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(Float.isNaN(pbLast)){
                pbLast = power.status;
            }

            if(power.status > pbLast){
                power.status = pbLast;
            }
            pbLast = power.status;
        }

        @Override
        public void overwrote(Seq<Building> previous){
        }
    }
}
