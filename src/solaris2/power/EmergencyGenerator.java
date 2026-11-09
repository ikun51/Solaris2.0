package solaris2.power;

import arc.Core;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** 应急发电机：灌满可燃液体后，缺电时自动启动发电。 */
public class EmergencyGenerator extends GenericCrafter{

    public static float auto = 0f;

    public EmergencyGenerator(){
        super("应急发电机");
        consumesPower = false;
        configurable = true;
        saveConfig = true;
        buildType = EmergencyGeneratorBuild::new;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.output);
        stats.remove(Stat.productionTime);
        stats.add(new Stat("阈值", StatCat.function), table -> {
            TextField tex = new TextField(String.valueOf(auto / 1000f));
            tex.update(() -> {
                try{
                    float value = Float.parseFloat(tex.getText());
                    if(value > 0) auto = value * 1000f;
                }catch(Exception e){
                }
            });
            table.add(tex);
            table.add("K");
            table.background(((TextureRegionDrawable)Tex.whiteui).tint(Pal.darkestGray));
            table.row();
        });
        stats.add(Stat.input, StatValues.liquidEffMultiplier(
            l -> l.flammability,
            60f,
            l -> l.flammability >= 0.001f
        ));
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("power");
        addBar("power", e -> new Bar(
            () -> Core.bundle.format("bar.poweroutput", Strings.fixed(e.getPowerProduction() * 60f * e.timeScale(), 1)),
            () -> Pal.powerBar,
            () -> e.getPowerProduction() > 0f ? 1f : 0f
        ));
    }

    public class EmergencyGeneratorBuild extends GenericCrafterBuild{
        public boolean start = false;
        public boolean prepare = false;
        public int o = 0;
        public int coolDown = 0;

        @Override
        public void buildConfiguration(Table table){
            super.buildConfiguration(table);
            Label statusLabel = table.add("").get();
            statusLabel.update(() -> {
                if(coolDown > 0) statusLabel.setText("状态：[yellow]冷却中 " + (int)Math.ceil(coolDown / 60f) + "秒!");
                else if(start) statusLabel.setText("状态：[red]运行中!");
                else if(prepare) statusLabel.setText("状态：[green]准备就绪!");
                else statusLabel.setText("状态：[grey]未就绪!");
            });
            table.row();
            table.add("当前检查阈值：" + (auto / 1000f) + "K");
            table.row();
            table.button(prepare ? start ? "[grey]启动" : "[green]启动" : "[grey]启动", () -> {
                if(prepare) start = true;
            });
            table.background(((TextureRegionDrawable)Tex.whiteui).tint(Pal.darkestGray));
            table.row();
        }

        public Liquid currentFuel(){
            Seq<Liquid> liqs = content.liquids();
            for(int i = 0; i < liqs.size; i++){
                Liquid liq = liqs.get(i);
                if(liq.flammability >= 0.001f && liquids.get(liq) > 0f){
                    return liq;
                }
            }
            return null;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(start) return false;
            if(liquid.flammability >= 0.001f){
                return liquids.get(liquid) < block.liquidCapacity;
            }
            return false;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(coolDown > 0) coolDown--;
            if(o > 60){
                o = 0;
                if(coolDown <= 0){
                    Liquid fuel = currentFuel();
                    if(fuel != null && liquids.get(fuel) == 1500f) prepare = true;
                    if(power.graph.getLastCapacity() < auto && prepare) start = true;
                }
                if(start){
                    Liquid fuel = currentFuel();
                    if(fuel == null || liquids.get(fuel) < 1f){
                        // 燃料耗尽：进入 3 分钟冷却
                        start = false;
                        prepare = false;
                        coolDown = 18000;
                    }else{
                        // 燃烧性：燃烧性越高烧得越慢 发电时间越长
                        float rate = 2f / Math.max(fuel.flammability, 0.2f);
                        liquids.set(fuel, liquids.get(fuel) - rate);
                    }
                }
            }
            o++;
            warmup = start ? 1f : 0f;
        }

        @Override
        public float getPowerProduction(){
            if(!start) return 0f;
            Liquid fuel = currentFuel();
            return fuel != null ? 45f * fuel.flammability : 0f;
        }

        public boolean canBreak(Tile tile){
            return start ? false : true;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(auto);
            write.bool(start);
            write.i(coolDown);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            auto = read.f();
            start = read.bool();
            coolDown = read.i();
        }
    }
}
