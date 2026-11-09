package solaris2.heat;

import arc.*;
import arc.math.*;
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
 * 由 scripts/建筑/热量/燃烧锅炉.js 移植而来。
 */
public class BurningBoiler extends GenericCrafter{

    public BurningBoiler(String name){
        super(name);

        hasItems = true;
        itemCapacity = 20;
        hasLiquids = true;
        liquidCapacity = 200f;
        craftTime = 120f;

        consume(new ConsumeItemFlammable(0.001f));
        consume(new ConsumeLiquid(Liquids.water, 0.2f));
        outputLiquid = new LiquidStack(SolarisContent.liquid("蒸汽"), 0.4f);

        buildType = BurningBoilerBuild::new;
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("效率", (BurningBoilerBuild e) -> new Bar(
            () -> "效率 " + (int)Math.floor(e.efficiency * 100) + "%",
            () -> Pal.accent,
            () -> Math.max(0f, Math.min(e.efficiency, 1f))
        ));
    }

    public class BurningBoilerBuild extends GenericCrafterBuild{

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return liquid == Liquids.water && liquids.get(Liquids.water) < block.liquidCapacity;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            Item it = items.first();
            if(it != null && Vars.state.rules.reactorExplosions && Mathf.chance(delta() * 0.06f * Mathf.clamp(it.explosiveness - 0.5f))){
                damage(4f);
                Fx.generatespark.at(x + Mathf.range(block.size * Vars.tilesize / 2f), y + Mathf.range(block.size * Vars.tilesize / 2f));
                Events.fire(Trigger.blastGenerator);
            }
        }

        @Override
        public float efficiencyScale(){
            Item it = items.first();
            if(it != null && it.flammability > 0){
                return it.flammability;
            }
            return 0f;
        }
    }
}
