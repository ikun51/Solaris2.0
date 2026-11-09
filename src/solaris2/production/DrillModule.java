package solaris2.production;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

import static mindustry.Vars.content;
import static mindustry.Vars.tilesize;

/**
 * 由 scripts/lib/ModuleDrillLib.js 中的 DrillModule 移植而来。
 * 提供各种被动加成的钻头模块，可被综合开采设备吸附。
 */
public class DrillModule extends GenericCrafter{

    public static final Color moduleColor = Color.valueOf("84f491");

    public final String clan;
    public final int sameMax;
    public final float powerAddition;
    public final float powerMultiplication;
    public final float efficiencyMultiplication;
    public final boolean consumable;
    public final float probability;
    public final float multiplier;
    public final boolean multiOre;
    public final int expand;

    public DrillModule(String name, String clan, int sameMax, float powerAddition, float powerMultiplication,
                       float efficiencyMultiplication, boolean consumable, float probability, float multiplier,
                       boolean multiOre, int expand){
        super(name);
        this.clan = clan;
        this.sameMax = sameMax;
        this.powerAddition = powerAddition;
        this.powerMultiplication = powerMultiplication;
        this.efficiencyMultiplication = efficiencyMultiplication;
        this.consumable = consumable;
        this.probability = probability;
        this.multiplier = multiplier;
        this.multiOre = multiOre;
        this.expand = expand;

        configurable = true;
        buildType = () -> new DrillModuleBuild();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(new Stat("族", StatCat.function), clan);
        stats.add(new Stat("同类模块上限", StatCat.function), sameMax);
        if(powerAddition != 0f) stats.add(new Stat("能耗提升", StatCat.function), "+ " + powerAddition * 60f);
        if(powerMultiplication != 0f) stats.add(new Stat("能耗提升", StatCat.function), "+ " + powerMultiplication * 100f + "%");
        if(efficiencyMultiplication != 0f) stats.add(new Stat("速率提升", StatCat.function), "+ " + efficiencyMultiplication * 100f + "%");
        if(probability != 0f) stats.add(new Stat("挖掘倍率", StatCat.function), multiplier + " 倍 (" + Math.round(probability * 100f) + "%/秒)");
        if(multiOre) stats.add(new Stat("多矿物", StatCat.function), "可挖掘被覆盖的矿物");
        if(expand != 0) stats.add(new Stat("探测扩张", StatCat.function), "+ " + expand + " 格");
    }

    public class DrillModuleBuild extends GenericCrafterBuild{

        public ComprehensiveDrill.ComprehensiveDrillBuild drill;
        public Seq<Item> oreFilter = new Seq<>();

        public String getClan(){
            return clan;
        }

        public int getSM(){
            return sameMax;
        }

        public float getPA(){
            return powerAddition;
        }

        public float getPM(){
            return powerMultiplication;
        }

        public float getEM(){
            return efficiencyMultiplication;
        }

        public boolean getC(){
            return consumable;
        }

        public float getPR(){
            return probability;
        }

        public float getML(){
            return multiplier;
        }

        public boolean getMO(){
            return multiOre;
        }

        public int getEX(){
            return expand;
        }

        public Seq<Item> getFilter(){
            return oreFilter;
        }

        public boolean isOreSelected(Item item){
            if(item == null || oreFilter == null) return false;
            for(int i = 0; i < oreFilter.size; i++){
                if(oreFilter.get(i).id == item.id) return true;
            }
            return false;
        }

        public void toggleOre(Item item){
            if(item == null) return;
            if(oreFilter == null) oreFilter = new Seq<>();
            for(int i = 0; i < oreFilter.size; i++){
                if(oreFilter.get(i).id == item.id){
                    oreFilter.remove(i);
                    return;
                }
            }
            oreFilter.add(item);
        }

        public boolean hasDrill(){
            return drill != null && drill.isAdded();
        }

        public void bindDrill(ComprehensiveDrill.ComprehensiveDrillBuild who){
            drill = who;
        }

        @Override
        public boolean shouldConsume(){
            return drill != null && drill.isAdded() && drill.shouldConsume() && drill.efficiency > 0f && super.shouldConsume();
        }

        @Override
        public void updateTile(){
            boolean drillWorking = drill != null && drill.isAdded() && drill.shouldConsume() && drill.efficiency > 0f;

            float a = efficiency;
            if(!drillWorking) efficiency = 0f;
            super.updateTile();
            efficiency = a;

            if(!timer.get(10f)) return;
            if(drill != null && !drill.isAdded()) drill = null;
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();
            if(drill != null && drill.isAdded()){
                Drawf.square(drill.x, drill.y, drill.block.size * tilesize / 2f + 1f, moduleColor);
            }
            Draw.color();
        }

        @Override
        public void configured(Unit builder, Object value){
            super.configured(builder, value);
            if(multiOre && value instanceof Item item) toggleOre(item);
        }

        @Override
        public void buildConfiguration(Table table){
            if(!multiOre) return;
            if(drill == null || !drill.isAdded()){
                table.add("需先绑定到综合开采设备").pad(8f);
                return;
            }
            Seq<Item> items = drill.getOreItems();
            if(items.size == 0){
                table.add("当前无可选矿物").pad(8f);
                return;
            }
            Table cont = new Table();
            cont.background(Styles.black6);
            cont.defaults().size(40f).pad(2f);
            for(int i = 0; i < items.size; i++){
                Item itm = items.get(i);
                ImageButton btn = cont.button(Tex.whiteui, Styles.clearNoneTogglei, 40f, () -> configure(itm)).get();
                btn.getStyle().imageUp = new TextureRegionDrawable(itm.uiIcon);
                btn.update(() -> btn.setChecked(isOreSelected(itm)));
                if(i % 5 == 4) cont.row();
            }
            table.add(cont);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            if(!multiOre) return;
            write.b((byte)oreFilter.size);
            for(int i = 0; i < oreFilter.size; i++){
                write.s((short)oreFilter.get(i).id);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(!multiOre) return;
            oreFilter = new Seq<>();
            int n = read.b();
            for(int i = 0; i < n; i++){
                Item itm = content.item(read.s());
                if(itm != null) oreFilter.add(itm);
            }
        }
    }
}
