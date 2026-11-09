package solaris2.liquid;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import solaris2.SolarisContent;

/**
 * 气化器：将液氧/液氢/液氮按 1:2 气化为氧气/氢气/氮气。
 * 由 scripts/建筑/液体/气化器.js 与 content/blocks/工厂/气化器.json 移植而来。
 */
public class Gasifier extends GenericCrafter{

    public Liquid liquidOxygen, liquidHydrogen, liquidNitrogen, oxygen;

    public Gasifier(String name){
        super(name);

        liquidOxygen = SolarisContent.liquid("液氧");
        liquidHydrogen = SolarisContent.liquid("液氢");
        liquidNitrogen = SolarisContent.liquid("液氮");
        oxygen = SolarisContent.liquid("氧气");

        description = "将液体气化为气体";
        size = 2;
        health = 420;
        hasPower = true;
        hasLiquids = true;
        liquidCapacity = 60f;
        outputLiquid = new LiquidStack(Liquids.water, 0f);
        updateEffect = Fx.plasticburn;
        buildVisibility = BuildVisibility.shown;
        consumePower(3.3333333f);
        requirements(Category.crafting, ItemStack.with(
                Items.silicon, 11, SolarisContent.item("铝"), 175,
                Items.metaglass, 80, SolarisContent.item("塑料"), 70));
        drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawLiquidTile(liquidNitrogen),
                new DrawLiquidTile(liquidOxygen),
                new DrawLiquidTile(liquidHydrogen),
                new DrawDefault());
        buildType = GasifierBuild::new;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.output);

        stats.add(new Stat("气化-01", StatCat.crafting), table -> {
            table.add(StatValues.displayLiquid(liquidOxygen, 6f, true)).right();
            table.add(new Image(Icon.right).setScaling(Scaling.fit)).size(30f);
            table.add(StatValues.displayLiquid(oxygen, 12f, true)).right();
            table.row();
        });
        stats.add(new Stat("气化-02", StatCat.crafting), table -> {
            table.add(StatValues.displayLiquid(liquidHydrogen, 6f, true)).right();
            table.add(new Image(Icon.right).setScaling(Scaling.fit)).size(30f);
            table.add(StatValues.displayLiquid(Liquids.hydrogen, 12f, true)).right();
            table.row();
        });
        stats.add(new Stat("气化-03", StatCat.crafting), table -> {
            table.add(StatValues.displayLiquid(liquidNitrogen, 6f, true)).right();
            table.add(new Image(Icon.right).setScaling(Scaling.fit)).size(30f);
            table.add(StatValues.displayLiquid(Liquids.nitrogen, 12f, true)).right();
            table.row();
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid-water");

        addBar("入液", (GasifierBuild e) -> {
            LiquidDisplay display = inputDisplay(e);
            return new Bar(() -> display.name, () -> display.liquid.color, () -> display.amount / 90f);
        });
        addBar("出液", (GasifierBuild e) -> {
            LiquidDisplay display = outputDisplay(e);
            return new Bar(() -> display.name, () -> display.liquid.color, () -> display.amount / 90f);
        });
    }

    private LiquidDisplay inputDisplay(GasifierBuild e){
        Liquid liquid = liquidOxygen;
        String liquidName = "液氧";
        float maxInput = Math.max(e.liquids.get(liquidOxygen),
                Math.max(e.liquids.get(liquidHydrogen), e.liquids.get(liquidNitrogen)));

        if(maxInput == e.liquids.get(liquidNitrogen)){
            liquid = liquidNitrogen;
            liquidName = "液氮";
        }
        if(maxInput == e.liquids.get(liquidHydrogen)){
            liquid = liquidHydrogen;
            liquidName = "液氢";
        }

        return new LiquidDisplay(liquidName, liquid, maxInput);
    }

    private LiquidDisplay outputDisplay(GasifierBuild e){
        Liquid liquid = oxygen;
        String liquidName = "氧气";
        float maxOutput = Math.max(e.liquids.get(oxygen),
                Math.max(e.liquids.get(Liquids.hydrogen), e.liquids.get(Liquids.nitrogen)));

        if(maxOutput == e.liquids.get(Liquids.nitrogen)){
            liquid = Liquids.nitrogen;
            liquidName = "氮气";
        }
        if(maxOutput == e.liquids.get(Liquids.hydrogen)){
            liquid = Liquids.hydrogen;
            liquidName = "氢气";
        }

        return new LiquidDisplay(liquidName, liquid, maxOutput);
    }

    private static class LiquidDisplay{
        final String name;
        final Liquid liquid;
        final float amount;

        LiquidDisplay(String name, Liquid liquid, float amount){
            this.name = name;
            this.liquid = liquid;
            this.amount = amount;
        }
    }

    public class GasifierBuild extends GenericCrafterBuild{

        @Override
        public boolean shouldConsume(){
            if(liquids.get(liquidOxygen) <= 0.8f && liquids.get(liquidHydrogen) <= 0.8f && liquids.get(liquidNitrogen) <= 0.8f){
                return false;
            }
            return super.shouldConsume();
        }

        @Override
        public void update(){
            super.update();
            dumpLiquid(oxygen);
            dumpLiquid(Liquids.hydrogen);
            dumpLiquid(Liquids.nitrogen);

            if(enabled){
                work(liquidOxygen, oxygen);
                work(liquidHydrogen, Liquids.hydrogen);
                work(liquidNitrogen, Liquids.nitrogen);
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(liquid == liquidOxygen) return liquids.get(liquidOxygen) < block.liquidCapacity;
            if(liquid == liquidHydrogen) return liquids.get(liquidHydrogen) < block.liquidCapacity;
            if(liquid == liquidNitrogen) return liquids.get(liquidNitrogen) < block.liquidCapacity;
            return false;
        }

        public void work(Liquid input, Liquid output){
            if(enabled){
                float efficiency = Time.delta * power.status * timeScale();
                float inputAmount = liquids.get(input);
                if(inputAmount > 0.8f && liquids.get(output) < 90f){
                    float consumed = efficiency * Math.min(inputAmount, 0.4f);
                    liquids.remove(input, consumed);
                    liquids.add(output, consumed * 2f);
                }
            }
        }
    }
}
