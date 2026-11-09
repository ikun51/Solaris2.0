package solaris2.power;

import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.consumers.*;
import solaris2.SolarisContent;

/** 蒸汽轮机：消耗蒸汽发电，副产水，出力有爬升过程。 */
public class SteamTurbine extends ConsumeGenerator{

    public static final float STEAM_IN = 1f;
    public static final float WATER_OUT = 0.5f;
    public static final float RAMP_SECONDS = 18000f;
    public static final float SMOOTH_SPEED = 0.01f;

    public static final Liquid steam = SolarisContent.liquid("蒸汽");

    public SteamTurbine(){
        super("蒸汽轮机");
        hasLiquids = true;
        liquidCapacity = 270f;
        powerProduction = 20f;

        consume(new ConsumeLiquid(steam, STEAM_IN));
        outputLiquid = new LiquidStack(Liquids.water, WATER_OUT);

        configurable = false;
        buildType = SteamTurbineBuild::new;
    }

    public class SteamTurbineBuild extends ConsumeGeneratorBuild{
        public float repow = 0f;
        public float smoothEff = 0f;

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return liquid == steam && liquids.get(steam) < block.liquidCapacity;
        }

        @Override
        public float efficiencyScale(){
            return (block.liquidCapacity - liquids.get(Liquids.water)) > 0.01f ? 1f : 0f;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            smoothEff = Mathf.lerpDelta(smoothEff, productionEfficiency, SMOOTH_SPEED);
            float dir = productionEfficiency > 0f ? 1f : -1f;
            repow = Mathf.clamp(repow + dir * Time.delta / RAMP_SECONDS, 0f, 1f);
        }

        @Override
        public float getPowerProduction(){
            return enabled ? ((PowerGenerator)block).powerProduction * smoothEff * repow : 0f;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(repow);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            repow = read.f();
        }
    }
}
