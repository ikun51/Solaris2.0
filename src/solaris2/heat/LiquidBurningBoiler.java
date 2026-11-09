package solaris2.heat;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import solaris2.SolarisContent;

/**
 * 由 scripts/建筑/热量/液体燃烧锅炉.js 移植而来。
 */
public class LiquidBurningBoiler extends GenericCrafter{

    public LiquidBurningBoiler(String name){
        super(name);

        hasItems = false;
        hasLiquids = true;
        liquidCapacity = 200f;
        craftTime = 60f;

        consume(new ConsumeLiquidFlammable(0.001f, 0.3f));
        consume(new ConsumeLiquid(Liquids.water, 0.2f));
        outputLiquid = new LiquidStack(SolarisContent.liquid("蒸汽"), 0.4f);

        buildType = LiquidBurningBoilerBuild::new;
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("燃料", (LiquidBurningBoilerBuild e) -> new Bar(
            () -> {
                Liquid liq = e.currentFuel();
                return liq != null ? liq.localizedName : "液体";
            },
            () -> {
                Liquid liq = e.currentFuel();
                return liq != null ? liq.color : Pal.darkishGray;
            },
            () -> {
                Liquid liq = e.currentFuel();
                return liq != null ? Math.min(e.liquids.get(liq) / e.block.liquidCapacity, 1f) : 0f;
            }
        ));
    }

    public class LiquidBurningBoilerBuild extends GenericCrafterBuild{

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(liquid == Liquids.water){
                return liquids.get(Liquids.water) < block.liquidCapacity;
            }
            if(liquid.flammability >= 0.001f){
                return liquids.get(liquid) < block.liquidCapacity;
            }
            return false;
        }

        public Liquid currentFuel(){
            Seq<Liquid> liqs = Vars.content.liquids();
            for(int i = 0; i < liqs.size; i++){
                Liquid liq = liqs.get(i);
                if(liq.flammability >= 0.001f && liquids.get(liq) > 0){
                    return liq;
                }
            }
            return null;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            Liquid liq = currentFuel();
            if(liq != null && Vars.state.rules.reactorExplosions && Mathf.chance(delta() * 0.06f * Mathf.clamp(liq.explosiveness - 0.5f))){
                damage(4f);
                Fx.generatespark.at(x + Mathf.range(block.size * Vars.tilesize / 2f), y + Mathf.range(block.size * Vars.tilesize / 2f));
                Events.fire(Trigger.blastGenerator);
            }
        }

        @Override
        public float efficiencyScale(){
            Liquid liq = currentFuel();
            if(liq != null){
                return liq.flammability;
            }
            return 0f;
        }
    }
}
