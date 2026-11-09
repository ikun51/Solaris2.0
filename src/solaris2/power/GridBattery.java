package solaris2.power;

import mindustry.world.blocks.power.Battery;

/** 电网电池：自动接入虚拟电网的可充放电电池。 */
public class GridBattery extends Battery{

    public GridBattery(){
        super("电网电池");
        update = true;
        buildType = GridBatteryBuild::new;
    }

    public class GridBatteryBuild extends BatteryBuild{

        @Override
        public void created(){
            super.created();
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
        }
    }
}
