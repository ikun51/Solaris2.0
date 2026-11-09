package solaris2.production;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.struct.ObjectFloatMap;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.ui.Bar;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.Drill;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

/**
 * 由 scripts/建筑/综合开采设备.js 中的 ModuleDrill 移植而来。
 * 可吸附周围同族模块，并根据模块属性提升钻速、耗电、探测范围与矿物筛选。
 */
public class ComprehensiveDrill extends Drill{

    public static final Color moduleColor = Color.valueOf("84f491");

    public final String clan;
    public final int maxModules;
    public final float power;

    public ComprehensiveDrill(String name, String clan, int maxModules, float power){
        super(name);
        this.clan = clan;
        this.maxModules = maxModules;
        this.power = power;

        configurable = true;
        buildType = () -> new ComprehensiveDrillBuild();

        if(power != 0f){
            consume(new ConsumePower(0f, 0f, false){
                @Override
                public float requestedPower(Building build){
                    return build.shouldConsume() ? power * ((ComprehensiveDrillBuild)build).getPM() : 0f;
                }
            });
        }
    }

    public float getPower(){
        return power;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(new Stat("族", StatCat.function), clan);
        stats.add(new Stat("模块上限", StatCat.function), maxModules);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("modules", (ComprehensiveDrillBuild build) -> new Bar(
                () -> "模块: " + build.getModules() + " / " + maxModules,
                () -> moduleColor,
                () -> (float)build.getModules() / maxModules
        ));
    }

    public static class OreCount{
        public final Item item;
        public int count;

        public OreCount(Item item){
            this.item = item;
            this.count = 1;
        }
    }

    public class ComprehensiveDrillBuild extends DrillBuild{

        public Seq<DrillModule.DrillModuleBuild> modules = new Seq<>();
        public Seq<OreCount> oreList = new Seq<>();
        public Seq<OreCount> outOres = new Seq<>();
        public final ObjectFloatMap<Item> oreAcc = new ObjectFloatMap<>();

        public int atimer = 0;
        public float em = 1f;
        public float pm = 1f;

        public void addModule(){
            if(modules.size >= maxModules) return;
            for(Building who : proximity){
                if(modules.size >= maxModules) break;
                if(!(who instanceof DrillModule.DrillModuleBuild module)) continue;
                if(modules.contains(module) || module.hasDrill()) continue;
                if(!module.getClan().equals(clan)) continue;
                int sameMax = module.getSM();
                if(sameMax > 0){
                    int sameCount = 0;
                    for(DrillModule.DrillModuleBuild added : modules){
                        if(added.block == module.block) sameCount++;
                    }
                    if(sameCount >= sameMax) continue;
                }
                module.bindDrill(this);
                modules.add(module);
            }
        }

        @Override
        public void offload(Item item){
            super.offload(item);
            if(!hasMultiOre() || outOres.size < 2) return;
            int mainCount = outOres.get(0).count;
            if(mainCount <= 0) return;
            for(int i = 0; i < outOres.size; i++){
                OreCount ore = outOres.get(i);
                if(ore.item.id == item.id) continue;
                oreAcc.put(ore.item, oreAcc.get(ore.item, 0f) + ore.count / (float)mainCount);
                while(oreAcc.get(ore.item, 0f) >= 1f && items.total() < block.itemCapacity){
                    super.offload(ore.item);
                    oreAcc.put(ore.item, oreAcc.get(ore.item, 0f) - 1f);
                }
            }
        }

        public void applyEM(){
            float m = 1f;
            for(DrillModule.DrillModuleBuild who : modules){
                if(!who.isAdded()) continue;
                float k = who.getC() ? who.efficiency : 1f;
                m += who.getEM() * k;
            }
            em = (float)Math.sqrt(m);
        }

        public void applyPM(){
            float n = ComprehensiveDrill.this.power;
            float m = 1f;
            for(DrillModule.DrillModuleBuild who : modules){
                if(!who.isAdded()) continue;
                float k = who.getC() ? who.efficiency : 1f;
                n += who.getPA() * k;
                m += who.getPM() * k;
            }
            pm = (n * m) / ComprehensiveDrill.this.power;
        }

        @Override
        public void updateTile(){
            float mul = 1f;
            for(int i = 0; i < modules.size; i++){
                DrillModule.DrillModuleBuild who = modules.get(i);
                if(!who.isAdded()) continue;
                float k = who.getC() ? who.efficiency : 1f;
                if(who.getML() > 1f && Mathf.chanceDelta(who.getPR() * k)) mul *= who.getML();
            }

            float a = efficiency;
            efficiency *= em;
            if(mul != 1f) dominantItems = (int)(dominantItems * mul);
            super.updateTile();
            if(mul != 1f) dominantItems = (int)(dominantItems / mul);
            efficiency = a;

            if(atimer++ < 10) return;
            atimer = 0;
            int l = modules.size;
            for(int i = modules.size - 1; i >= 0; i--){
                if(!modules.get(i).isAdded()) modules.remove(i);
            }
            if(modules.size != l) onProximityUpdate();
            addModule();
            if(modules.size != l) onProximityUpdate();
            applyEM();
            if(ComprehensiveDrill.this.power != 0f) applyPM();
            refreshOre();
        }

        public int getExpandRange(){
            float r = 0f;
            for(int i = 0; i < modules.size; i++){
                DrillModule.DrillModuleBuild m = modules.get(i);
                if(!m.isAdded()) continue;
                float k = m.getC() ? m.efficiency : 1f;
                r += m.getEX() * k;
            }
            return (int)Math.floor(r);
        }

        public Seq<Item> getOreFilter(){
            for(int i = 0; i < modules.size; i++){
                DrillModule.DrillModuleBuild m = modules.get(i);
                if(!m.isAdded()) continue;
                Seq<Item> f = m.getFilter();
                if(f != null && f.size > 0) return f;
            }
            return null;
        }

        public Seq<Item> getOreItems(){
            Seq<Item> out = new Seq<>();
            for(int i = 0; i < oreList.size; i++) out.add(oreList.get(i).item);
            return out;
        }

        public void refreshOre(){
            countAllOre(getExpandRange());
            Seq<Item> f = getOreFilter();
            if(f == null){
                outOres = oreList;
            }else{
                outOres = new Seq<>();
                for(int i = 0; i < oreList.size; i++){
                    OreCount o = oreList.get(i);
                    for(int j = 0; j < f.size; j++){
                        if(f.get(j).id == o.item.id){
                            outOres.add(o);
                            break;
                        }
                    }
                }
            }
            OreCount cur = outOres.size > 0 ? outOres.get(0) : null;

            dominantItem = cur == null ? null : cur.item;
            dominantItems = cur == null ? 0 : cur.count;
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            refreshOre();
        }

        public void countAllOre(int expand){
            oreList = new Seq<>();
            int size = block.size;
            int o = block.sizeOffset;
            boolean multi = hasMultiOre();
            for(int dx = -expand; dx < size + expand; dx++){
                for(int dy = -expand; dy < size + expand; dy++){
                    Tile t = world.tile(tile.x + dx + o, tile.y + dy + o);
                    if(t == null) continue;
                    addOreList(t.drop());
                    if(multi){
                        Floor overlay = t.overlay();
                        if(overlay != null && overlay != Blocks.air && overlay.itemDrop != null){
                            Floor f = t.floor();
                            if(f != null && f.itemDrop != null) addOreList(f.itemDrop);
                        }
                    }
                }
            }
            oreList.sort((a, b) -> {
                int t = (a.item.lowPriority ? 1 : 0) - (b.item.lowPriority ? 1 : 0);
                if(t != 0) return t;
                int count = Integer.compare(b.count, a.count);
                return count != 0 ? count : Integer.compare(a.item.id, b.item.id);
            });
        }

        public void addOreList(Item item){
            if(item == null) return;
            for(int i = 0; i < oreList.size; i++){
                if(oreList.get(i).item.id == item.id){
                    oreList.get(i).count++;
                    return;
                }
            }
            oreList.add(new OreCount(item));
        }

        public boolean hasMultiOre(){
            for(int i = 0; i < modules.size; i++){
                DrillModule.DrillModuleBuild m = modules.get(i);
                if(m.isAdded() && m.getMO()) return true;
            }
            return false;
        }

        @Override
        public void drawConfigure(){
            super.drawConfigure();
            int expand = getExpandRange();
            if(expand > 0) Drawf.dashSquare(Pal.accent, x, y, (block.size + expand * 2) * tilesize);
            for(int i = 0; i < modules.size; i++){
                DrillModule.DrillModuleBuild who = modules.get(i);
                if(!who.isAdded()) continue;
                Drawf.square(who.x, who.y, who.block.size * tilesize / 2f + 1f, moduleColor);
            }
            Draw.color();
        }

        public int getModules(){
            return modules.size;
        }

        public float getPM(){
            return pm;
        }
    }
}
