package solaris2.liquid;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.BuildVisibility;

/**
 * 液体限流阀：沿旋转方向单向输送液体，输入框设定每秒流量上限。
 * 由 scripts/建筑/液体/液体限流阀.js 与 content/blocks/液体/液体限流阀.json 移植而来。
 */
public class LiquidFlowLimiter extends GenericCrafter{

    private static final int[] offsets = {-1, 0, 0, -1, 1, 0, 0, 1};

    public TextureRegion topRegion;

    public LiquidFlowLimiter(String name){
        super(name);

        description = "沿旋转方向输送液体，输入框设定每秒流量上限";
        health = 60;
        size = 1;
        itemCapacity = 0;
        liquidCapacity = 20f;
        rotate = true;
        rotateDraw = false;
        drawArrow = true;
        configurable = true;
        saveConfig = true;
        hasLiquids = true;
        outputsLiquid = true;
        buildVisibility = BuildVisibility.shown;
        requirements(Category.liquid, ItemStack.with(Items.titanium, 30));

        buildType = LiquidFlowLimiterBuild::new;
    }

    @Override
    public void load(){
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        if(topRegion != null && topRegion.found()){
            Draw.rect(topRegion, plan.drawx(), plan.drawy(), plan.rotation * 90f);
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid");
    }

    public class LiquidFlowLimiterBuild extends GenericCrafterBuild{

        public float limitPerSecond = 60f;

        @Override
        public void buildConfiguration(Table table){
            Label label = new Label("当前限制: " + limitPerSecond + " 单位/秒");
            table.add(label).row();

            table.add("新限制: ");
            TextField field = new TextField("" + limitPerSecond);
            table.add(field).width(120f);
            table.button("设置", () -> {
                try{
                    float val = Float.parseFloat(field.getText());
                    if(!Float.isNaN(val) && val >= 0f){
                        limitPerSecond = val;
                        label.setText("当前限制: " + limitPerSecond + " 单位/秒");
                    }
                }catch(NumberFormatException ignored){
                }
            }).width(60f);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            noSleep();
            //只拒绝来自正前方(输出方向)的灌入，其余方向可正常接收进液
            if(source != this && tile != null && (source.relativeTo(tile.x, tile.y) + 2) % 4 == rotation){
                return false;
            }
            return super.acceptLiquid(source, liquid);
        }

        @Override
        public void updateTile(){
            noSleep();
            float set = limitPerSecond * Time.delta / 60f;

            Building other = Vars.world.build(tileX() + offsets[rotation * 2], tileY() + offsets[1 + rotation * 2]);
            Building target = Vars.world.build(tileX() - offsets[rotation * 2], tileY() - offsets[1 + rotation * 2]);

            //从背后建筑抽取液体
            if(other != null && other.block.hasLiquids && other.liquids.currentAmount() > 0.001f){
                Liquid nowliquid = other.liquids.current();
                float current = liquids.get(nowliquid);
                float flux = Math.min(other.liquids.get(nowliquid), set);
                float take = Math.min(flux, block.liquidCapacity - current);
                if(take > 0.001f){
                    handleLiquid(this, nowliquid, take);
                    other.liquids.remove(nowliquid, take);
                    other.updateTile();
                }
            }

            //向正前方建筑输出液体
            if(target != null && target.block.hasLiquids && liquids.currentAmount() > 0.001f){
                Liquid nowliquid = liquids.current();
                float flux = Math.min(liquids.get(nowliquid), set);
                float give = Math.min(flux, target.block.liquidCapacity - target.liquids.get(nowliquid));
                if(give > 0.001f){
                    target.handleLiquid(target, nowliquid, give);
                    liquids.remove(nowliquid, give);
                    target.updateTile();
                }
            }
        }

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            if(topRegion != null && topRegion.found()){
                Draw.rect(topRegion, x, y, rotdeg());
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(limitPerSecond);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            limitPerSecond = read.f();
            if(Float.isNaN(limitPerSecond) || limitPerSecond < 0f || limitPerSecond > 1000000f){
                limitPerSecond = 60f;
            }
        }
    }
}
