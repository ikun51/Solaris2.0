package solaris2.liquid;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.production.Pump;
import mindustry.world.meta.*;

/**
 * 由 scripts/建筑/液体/脉冲泵.js 移植而来。
 */
public class PulsePump extends Pump{

    public static final float warmupTime = 270f;
    public static final float pumpOnce = 480f;
    public static final float hydrogenRate = 0.1f;
    public static final float powerUse = 1f;
    public static final float outCapacity = 720f;
    public static final float hydrogenCapacity = 100f;

    public PulsePump(String name){
        super(name);

        liquidCapacity = outCapacity + hydrogenCapacity;
        pumpAmount = 0.1666f;

        consumePower(powerUse);
        consumeLiquid(Liquids.hydrogen, hydrogenRate);

        buildType = PulsePumpBuild::new;
    }

    @Override
    public void setBars(){
        super.setBars();

        removeBar("liquid-hydrogen");
        removeBar("liquid");

        addBar("氢气", (PulsePumpBuild e) -> new Bar(
            () -> "氢气",
            () -> Liquids.hydrogen.color,
            () -> Math.min(e.liquids.get(Liquids.hydrogen) / hydrogenCapacity, 1f)
        ));

        addBar("抽出液体", (PulsePumpBuild e) -> new Bar(
            () -> e.liquidDrop == null ? Core.bundle.get("bar.liquid") : e.liquidDrop.localizedName,
            () -> e.liquidDrop == null ? Pal.accent : e.liquidDrop.color,
            () -> e.liquidDrop == null ? 0f : Math.min(e.liquids.get(e.liquidDrop) / outCapacity, 1f)
        ));

        addBar("抽取速度", (PulsePumpBuild e) -> new Bar(
            () -> "抽取速度 " + (int)Math.floor(e.ratio() * 100f) + "%",
            () -> Pal.powerBar,
            e::ratio
        ));
    }

    public class PulsePumpBuild extends PumpBuild{

        public float progress;

        public float ratio(){
            return Mathf.clamp(progress / warmupTime, 0f, 1f);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return liquid == Liquids.hydrogen && liquids.get(Liquids.hydrogen) < hydrogenCapacity - 0.001f;
        }

        @Override
        public boolean shouldConsume(){
            return liquidDrop != null && liquids.get(liquidDrop) < outCapacity - 0.01f && enabled;
        }

        @Override
        public void updateTile(){
            Liquid drop = liquidDrop;
            if(drop != null) dumpLiquid(drop);

            if(!enabled || drop == null) return;

            if(efficiency <= 0.001f) return;
            progress += delta() * efficiency;

            if(progress < warmupTime) return;

            progress -= warmupTime;
            liquids.add(drop, Math.min(pumpOnce, outCapacity - liquids.get(drop)));

            for(int i = 0; i < 5; i++){
                Fx.drillSteam.at(x + Mathf.range(block.size * 4f), y + Mathf.range(block.size * 4f));
            }
            Sounds.drillImpact.at(x, y, 1f, 0.8f);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
        }
    }
}
