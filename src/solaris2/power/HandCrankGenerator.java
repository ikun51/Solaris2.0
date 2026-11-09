package solaris2.power;

import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.io.*;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.world.blocks.power.SolarGenerator;

/** 手摇发电机：点击按钮推动飞轮，飞轮靠惯性逐渐减速。 */
public class HandCrankGenerator extends SolarGenerator{

    public static final float maxSpeed = 1f;
    public static final float pushPerClick = 0.06f;
    public static final float accelerate = 0.008f;
    public static final float friction = 0.0005f;

    public HandCrankGenerator(String name){
        super(name);
        buildType = HandCrankGeneratorBuild::new;
        update = true;
        configurable = true;
        hasPower = true;
        powerProduction = 3f;
    }

    public class HandCrankGeneratorBuild extends SolarGeneratorBuild{
        public float speed = 0f;
        public float targetSpeed = 0f;

        @Override
        public void updateTile(){
            speed = Mathf.approachDelta(speed, targetSpeed, accelerate);
            targetSpeed = Mathf.approachDelta(targetSpeed, 0f, friction);
            //同步到原版字段，供原版电力条显示
            productionEfficiency = speed;
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.upOpen, Styles.defaulti, () -> {
                targetSpeed = Math.min(targetSpeed + pushPerClick, maxSpeed);
            }).size(45f);
        }

        @Override
        public float getPowerProduction(){
            return speed * powerProduction;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(speed);
            write.f(targetSpeed);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            speed = read.f();
            targetSpeed = read.f();
        }
    }
}
