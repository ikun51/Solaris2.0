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
 * 液化器：将氧气/氢气/氮气按 2:1 液化为液氧/液氢/液氮。
 * 由 scripts/建筑/液体/液化器.js 与 content/blocks/工厂/液化器.json 移植而来。
 */
public class Liquefier extends GenericCrafter{

    public Liquid oxygen;
    public Liquid liquidOxygen, liquidHydrogen, liquidNitrogen;

    public Liquefier(String name){
        super(name);

        oxygen = SolarisContent.liquid("氧气");
        liquidOxygen = SolarisContent.liquid("液氧");
        liquidHydrogen = SolarisContent.liquid("液氢");
        liquidNitrogen = SolarisContent.liquid("液氮");

        description = "将气体液化为液体";
        size = 3;
        health = 420;
        hasPower = true;
        hasLiquids = true;
        liquidCapacity = 90f;
        outputLiquid = new LiquidStack(Liquids.water, 0f);
        updateEffect = Fx.plasticburn;
        buildVisibility = BuildVisibility.shown;
        consumePower(5.3333333f);
        requirements(Category.crafting, ItemStack.with(
                Items.silicon, 11, SolarisContent.item("铝"), 175,
                Items.metaglass, 80, SolarisContent.item("塑料"), 70));
        drawer = new DrawMulti(
                new DrawPistons(){{
                    sinMag = 1f;
                }},
                new DrawDefault());
        buildType = LiquefierBuild::new;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.output);

        stats.add(new Stat("液化-01", StatCat.crafting), table -> {
            table.add(StatValues.displayLiquid(oxygen, 12f, true)).right();
            table.add(new Image(Icon.right).setScaling(Scaling.fit)).size(30f);
            table.add(StatValues.displayLiquid(liquidOxygen, 6f, true)).right();
            table.row();
        });
        stats.add(new Stat("液化-02", StatCat.crafting), table -> {
            table.add(StatValues.displayLiquid(Liquids.hydrogen, 12f, true)).right();
            table.add(new Image(Icon.right).setScaling(Scaling.fit)).size(30f);
            table.add(StatValues.displayLiquid(liquidHydrogen, 6f, true)).right();
            table.row();
        });
        stats.add(new Stat("液化-03", StatCat.crafting), table -> {
            table.add(StatValues.displayLiquid(Liquids.nitrogen, 12f, true)).right();
            table.add(new Image(Icon.right).setScaling(Scaling.fit)).size(30f);
            table.add(StatValues.displayLiquid(liquidNitrogen, 6f, true)).right();
            table.row();
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid-water");

        addBar("入液", (LiquefierBuild e) -> {
            LiquidDisplay display = inputDisplay(e);
            return new Bar(() -> display.name, () -> display.liquid.color, () -> display.amount / 90f);
        });
        addBar("出液", (LiquefierBuild e) -> {
            LiquidDisplay display = outputDisplay(e);
            return new Bar(() -> display.name, () -> display.liquid.color, () -> display.amount / 90f);
        });
    }

    private LiquidDisplay inputDisplay(LiquefierBuild e){
        Liquid liquid = oxygen;
        String liquidName = "氧气";
        float maxInput = Math.max(e.liquids.get(oxygen),
                Math.max(e.liquids.get(Liquids.hydrogen), e.liquids.get(Liquids.nitrogen)));

        if(maxInput == e.liquids.get(Liquids.nitrogen)){
            liquid = Liquids.nitrogen;
            liquidName = "氮气";
        }
        if(maxInput == e.liquids.get(Liquids.hydrogen)){
            liquid = Liquids.hydrogen;
            liquidName = "氢气";
        }

        return new LiquidDisplay(liquidName, liquid, maxInput);
    }

    private LiquidDisplay outputDisplay(LiquefierBuild e){
        Liquid liquid = liquidOxygen;
        String liquidName = "液氧";
        float maxOutput = Math.max(e.liquids.get(liquidOxygen),
                Math.max(e.liquids.get(liquidHydrogen), e.liquids.get(liquidNitrogen)));

        if(maxOutput == e.liquids.get(liquidNitrogen)){
            liquid = liquidNitrogen;
            liquidName = "液氮";
        }
        if(maxOutput == e.liquids.get(liquidHydrogen)){
            liquid = liquidHydrogen;
            liquidName = "液氢";
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

    public class LiquefierBuild extends GenericCrafterBuild{

        @Override
        public boolean shouldConsume(){
            if(liquids.get(oxygen) <= 0.8f && liquids.get(Liquids.hydrogen) <= 0.8f && liquids.get(Liquids.nitrogen) <= 0.8f){
                return false;
            }
            return super.shouldConsume();
        }

        @Override
        public void update(){
            super.update();
            dumpLiquid(liquidNitrogen);
            dumpLiquid(liquidHydrogen);
            dumpLiquid(liquidOxygen);

            if(enabled){
                work(oxygen, liquidOxygen);
                work(Liquids.hydrogen, liquidHydrogen);
                work(Liquids.nitrogen, liquidNitrogen);
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(liquid == oxygen) return liquids.get(oxygen) < block.liquidCapacity;
            if(liquid == Liquids.hydrogen) return liquids.get(Liquids.hydrogen) < block.liquidCapacity;
            if(liquid == Liquids.nitrogen) return liquids.get(Liquids.nitrogen) < block.liquidCapacity;
            return false;
        }

        public void work(Liquid input, Liquid output){
            if(enabled){
                float efficiency = Time.delta * power.status * timeScale();
                float inputAmount = liquids.get(input);
                if(inputAmount > 0.8f && liquids.get(output) < 90f){
                    float consumed = efficiency * Math.min(inputAmount, 0.4f);
                    liquids.remove(input, consumed);
                    liquids.add(output, consumed * 0.5f);
                }
            }
        }
    }
}
