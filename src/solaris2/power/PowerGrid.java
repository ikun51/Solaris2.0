package solaris2.power;

import arc.Events;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** 供电网（普通/高级/终极）：跨接范围内部的发电机、用电器与电池的自定义虚拟电网。 */
public class PowerGrid extends Block{

    public static final float POWER_GRID_CAPACITY = 10000f;
    public static final float POWER_GRID_RANGE = 12f;
    public static final float POWER_GRID_OVERLOAD_LIMIT = 4500f;
    public static final float POWER_GRID_LOSS_RATE = 0.03f;
    public static final float POWER_GRID_GENERATOR_LOSS_RATE = 0.005f;
    public static final float POWER_GRID_MAX_LOSS_RATE = 0.65f;

    public static final float ADVANCED_POWER_GRID_OVERLOAD_LIMIT = 9500f;
    public static final float ADVANCED_POWER_GRID_LOSS_RATE = 0.01f;
    public static final float ADVANCED_POWER_GRID_GENERATOR_LOSS_RATE = 0.0015f;

    public static final float POWER_GRID_LOSS_EXPONENT = 1.2f;

    public static final float ULTIMATE_POWER_GRID_OVERLOAD_LIMIT = 30000f;
    public static final float ULTIMATE_POWER_GRID_LOSS_RATE = 0f;
    public static final float ULTIMATE_POWER_GRID_GENERATOR_LOSS_RATE = 0f;
    public static final float ULTIMATE_POWER_GRID_PRE_DEDUCTION = 1000f;

    public static final boolean POWER_GRID_OVERLOAD_KILL_ENABLED = true;
    public static final float POWER_GRID_OVERLOAD_SAFE_RATIO = 1.15f;
    public static final float POWER_GRID_OVERLOAD_DAMAGE = 8f;
    public static final float POWER_GRID_OVERLOAD_DAMAGE_CHANCE = 0.06f;
    public static final float POWER_GRID_OVERLOAD_LIGHTNING_CHANCE = 1.5f;
    public static final float POWER_GRID_OVERLOAD_WARN_RATIO = 0.93f;
    public static final float POWER_GRID_OVERLOAD_TRIGGER_RATIO = 1.0f;
    public static final float POWER_GRID_WARN_HIGH_RATIO = 0.66f;
    public static final float POWER_GRID_WARN_MID_RATIO = 0.33f;

    public static final float GRAPH_REBUILD_DEBOUNCE = 60f;

    public static final Color GRID_LIGHTNING_COLOR_NORMAL = Color.valueOf("a9d8ff");
    public static final Color GRID_LIGHTNING_COLOR_ADVANCED = Color.valueOf("ffd37f");
    public static final Color GRID_LIGHTNING_COLOR_ULTIMATE = Color.valueOf("ff8aff");

    public static final GridData data = new GridData();

    public static final Effect gridLightningEffect = new Effect(8f, 80f, e -> {
        Vec2 end = (Vec2)e.data;
        float endX = end.x;
        float endY = end.y;
        float dst = Mathf.dst(e.x, e.y, endX, endY);
        if(dst < 0.1f) return;

        Tmp.v1.set(endX - e.x, endY - e.y).nor();
        float normx = Tmp.v1.x;
        float normy = Tmp.v1.y;

        float range = 4f;
        int links = Mathf.ceil(dst / range);
        float spacing = dst / links;

        Lines.stroke(1.5f * e.fout());
        Draw.color(Color.white, e.color, e.fin());

        Lines.beginLine();
        Lines.linePoint(e.x, e.y);

        for(int i = 0; i < links; i++){
            float nx, ny;
            if(i == links - 1){
                nx = endX;
                ny = endY;
            }else{
                float len = (i + 1) * spacing;
                float perpX = -normy;
                float perpY = normx;
                float jitter = Mathf.randomSeed(i * 7777L + e.id, -range / 2f, range / 2f);
                nx = e.x + normx * len + perpX * jitter;
                ny = e.y + normy * len + perpY * jitter;
            }
            Lines.linePoint(nx, ny);
        }
        Lines.endLine();
    });

    static{
        gridLightningEffect.followParent = false;
        gridLightningEffect.rotWithParent = false;

        Events.run(Trigger.update, PowerGrid::globalUpdate);
        Events.on(BlockBuildEndEvent.class, e -> {
            if(data.instances.size == 0) return;
            data.graphDirty = true;
        });
        Events.on(BlockDestroyEvent.class, e -> {
            Building db = (e != null && e.tile != null) ? e.tile.build : null;
            int idx = db != null ? data.instances.indexOf(db) : -1;
            if(idx >= 0){
                unregisterPowerGrid(db);
                return;
            }
            if(data.instances.size > 0) data.graphDirty = true;
        });
        Events.on(WorldLoadEvent.class, e -> worldLoad());
    }

    public Color lightningColor;
    public float lightningChance;

    public PowerGrid(String name, int health, Color lightningColor, float lightningChance){
        super(name);
        this.health = health;
        this.lightningColor = lightningColor;
        this.lightningChance = lightningChance;

        size = 2;
        category = Category.power;
        rotate = false;
        buildVisibility = BuildVisibility.shown;
        update = true;
        solid = true;
        destructible = true;

        buildType = PowerGridBuild::new;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("power-grid-surplus", (Building entity) -> new Bar(
            () -> {
                int net = Math.round(data.surplusBalance);
                int loss = Math.round(data.lossAmount);
                String txt = "[white]电力: " + (net >= 0 ? "+" : "") + net + "/秒";
                txt += " [red]损耗: -" + loss + "/秒[]";
                return txt;
            },
            () -> data.surplusBalance < 0f ? Color.valueOf("ff4444") : Color.valueOf("ffd37fff"),
            () -> {
                float ratio = data.currentDemand > 0f ? data.currentProduction / data.currentDemand : (data.currentProduction > 0f ? 1f : 0f);
                return Math.min(1f, Math.max(0f, ratio));
            }
        ));

        addBar("power-grid-storage", (Building entity) -> new Bar(
            () -> "电力储存: " + Math.round(data.totalStoredPower) + "/" + Math.round(data.totalStorageCapacity),
            () -> Pal.power,
            () -> data.totalStorageCapacity > 0f ? data.totalStoredPower / data.totalStorageCapacity : 0f
        ));

        addBar("power-grid-load", (Building entity) -> new Bar(
            () -> {
                float limit = getTotalOverloadLimit();
                float ratio = Math.min(1f, data.currentProduction / Math.max(1f, limit));
                if(ratio >= POWER_GRID_OVERLOAD_WARN_RATIO) return "[red]负荷: 极限";
                if(ratio >= POWER_GRID_WARN_HIGH_RATIO) return "[orange]负荷: 高";
                if(ratio >= POWER_GRID_WARN_MID_RATIO) return "[yellow]负荷: 中";
                return "[green]负荷: 低";
            },
            () -> {
                float limit = getTotalOverloadLimit();
                float ratio = Math.min(1f, data.currentProduction / Math.max(1f, limit));
                if(ratio >= POWER_GRID_OVERLOAD_WARN_RATIO) return Color.valueOf("ff0000");
                if(ratio >= POWER_GRID_WARN_HIGH_RATIO) return Color.valueOf("ffa500");
                if(ratio >= POWER_GRID_WARN_MID_RATIO) return Color.valueOf("ffff00");
                return Color.valueOf("00ff00");
            },
            () -> {
                float limit = getTotalOverloadLimit();
                float jitter = 1.0f + (Mathf.random() - 0.5f) * 0.1f;
                float raw = data.currentProduction / Math.max(1f, limit);
                return Math.min(1f, Math.max(0f, raw * jitter));
            }
        ));
    }

    public static boolean nameEndsWith(String name, String suffix){
        return name != null && name.length() >= suffix.length() && name.substring(name.length() - suffix.length()).equals(suffix);
    }

    public static boolean isAdvancedGrid(Building build){
        return build != null && build.isValid() && build.block != null && nameEndsWith(build.block.name, "-高级供电网");
    }

    public static boolean isUltimateGrid(Building build){
        return build != null && build.isValid() && build.block != null && nameEndsWith(build.block.name, "-终极电网");
    }

    public static void triggerGridLightning(Building build, Color color){
        float angle = Mathf.random(360f);
        float dist = Mathf.random(15f, 50f);
        float endX = build.x + Mathf.cosDeg(angle) * dist;
        float endY = build.y + Mathf.sinDeg(angle) * dist;
        gridLightningEffect.at(build.x, build.y, 0f, color, new Vec2(endX, endY));
    }

    public static float getTotalOverloadLimit(){
        float total = 0f;
        for(int i = 0; i < data.instances.size; i++){
            Building pg = data.instances.get(i);
            if(pg == null || !pg.isValid()) continue;
            if(isUltimateGrid(pg)) total += ULTIMATE_POWER_GRID_OVERLOAD_LIMIT;
            else if(isAdvancedGrid(pg)) total += ADVANCED_POWER_GRID_OVERLOAD_LIMIT;
            else total += POWER_GRID_OVERLOAD_LIMIT;
        }
        return Math.max(POWER_GRID_OVERLOAD_LIMIT, total);
    }

    public static void drawPowerGridUI(Building build){
        Draw.z(Layer.effect);
        float cx = build.x;
        float cy = build.y;
        float rangePx = POWER_GRID_RANGE * tilesize;

        Draw.color(data.isOverloaded ? Pal.remove : Pal.accent, 0.15f);
        Fill.rect(cx, cy, rangePx * 2f, rangePx * 2f);
        Draw.color(data.isOverloaded ? Pal.remove : Pal.accent, 0.5f);
        Lines.stroke(1f);
        Lines.rect(cx - rangePx, cy - rangePx, rangePx * 2f, rangePx * 2f);

        if(data.isOverloaded && Math.floor(data.overloadFlashTimer / 4f) % 2 == 0){
            Draw.color(Pal.remove, 0.3f);
            Fill.rect(cx, cy, rangePx * 2f * 0.6f, rangePx * 2f * 0.6f);
        }

        if(data.instances.size > 1){
            Draw.color(Pal.accent, 0.4f);
            Lines.stroke(1.5f);
            for(int i = 0; i < data.instances.size; i++){
                Building other = data.instances.get(i);
                if(other != build){
                    Lines.line(cx, cy, other.x, other.y);
                }
            }
        }

        Draw.color(Pal.accent, 0.6f);
        Lines.stroke(1.5f);
        for(int i = 0; i < data.connectedGenerators.size; i++){
            Building gen = data.connectedGenerators.get(i).build;
            if(gen != null && gen.isValid() && Math.abs(cx - gen.x) <= rangePx && Math.abs(cy - gen.y) <= rangePx){
                Lines.line(cx, cy, gen.x, gen.y);
            }
        }

        Draw.color(Pal.lightishOrange, 0.6f);
        for(int i = 0; i < data.connectedBatteries.size; i++){
            Building bat = data.connectedBatteries.get(i);
            if(bat != null && bat.isValid()){
                Lines.line(cx, cy, bat.x, bat.y);
            }
        }

        Draw.color(Color.blue, 0.4f);
        for(int i = 0; i < data.connectedConsumers.size; i++){
            Building con = data.connectedConsumers.get(i);
            if(con != null && con.isValid()){
                Lines.line(cx, cy, con.x, con.y);
            }
        }

        Draw.color();
    }

    public static void registerPowerGrid(Building build){
        if(data.instances.indexOf(build) >= 0) return;
        data.instances.add(build);
        data.totalCapacity = data.instances.size * POWER_GRID_CAPACITY;
    }

    public static void unregisterPowerGrid(Building build){
        int idx = data.instances.indexOf(build);
        if(idx >= 0){
            data.instances.remove(idx);
        }
        data.totalCapacity = data.instances.size * POWER_GRID_CAPACITY;
        if(data.instances.size == 0){
            clearVirtualGraph();
        }else{
            rebuildVirtualGraph();
        }
    }

    public static void addToVirtualGraph(Building b){
        if(data.virtualGraph == null) return;

        if(b != null && b.isValid() && b.power != null){
            data.virtualGraph.add(b);
        }
    }

    public static boolean isGridBlock(Block block){
        return nameEndsWith(block.name, "-供电网") || nameEndsWith(block.name, "-高级供电网") || nameEndsWith(block.name, "-终极电网");
    }

    public static boolean isBatteryBlock(Block block){
        return nameEndsWith(block.name, "-电网电池") || nameEndsWith(block.name, "-一次性电池") || (block.consPower != null && block.consPower.buffered);
    }

    public static Team referenceTeam(){
        for(int i = 0; i < data.instances.size; i++){
            Building r = data.instances.get(i);
            if(r != null && r.isValid() && r.team != null) return r.team;
        }
        return player.team();
    }

    public static void rebuildVirtualGraph(){
        if(data.instances.size == 0){
            clearVirtualGraph();
            return;
        }

        clearVirtualGraph();
        data.virtualGraph = new PowerGraph(true);
        data.connectedGenerators.clear();
        data.connectedConsumers.clear();
        data.connectedBatteries.clear();
        data.clusterGens.clear();
        data.clusterCons.clear();

        final Team refTeam = referenceTeam();
        final float rangePx = POWER_GRID_RANGE * tilesize;

        // 第一轮：电池与其他电力建筑逐个接入
        Groups.build.each(b -> {
            if(b == null || !b.isValid() || b.team != refTeam || b.power == null || b.block == null) return;
            if(isGridBlock(b.block)) return;

            Block block = b.block;
            boolean isBat = isBatteryBlock(block);

            if(isBat){
                // 电池：全地图接入
                addToVirtualGraph(b);
                data.connectedBatteries.add(b);
            }else if(!block.outputsPower && block.consPower == null){
                // 其他有 power 模块的建筑：直接接入
                addToVirtualGraph(b);
            }
        });

        // 第二轮：发电机按原版邻接聚类，每组只接入一个代表，其余电力自动折算
        Seq<Building> cand = new Seq<>();
        IntSet candIds = new IntSet();
        Groups.build.each(b -> {
            if(b == null || !b.isValid() || b.team != refTeam || b.power == null || b.block == null) return;
            if(isGridBlock(b.block)) return;
            Block block = b.block;
            if(isBatteryBlock(block)) return;
            if(block.outputsPower){
                cand.add(b);
                candIds.add(b.id);
            }
        });

        IntSet visited = new IntSet();
        for(int ci = 0; ci < cand.size; ci++){
            Building start = cand.get(ci);
            if(visited.contains(start.id)) continue;

            Seq<Building> cluster = new Seq<>();
            Seq<Building> queue = new Seq<>();
            queue.add(start);
            visited.add(start.id);
            while(queue.size > 0){
                Building cur = queue.pop();
                cluster.add(cur);
                Seq<Building> prox = cur.proximity;
                if(prox == null) continue;
                for(int pi = 0; pi < prox.size; pi++){
                    Building nb = prox.get(pi);
                    if(nb != null && nb.isValid() && candIds.contains(nb.id) && !visited.contains(nb.id)){
                        visited.add(nb.id);
                        queue.add(nb);
                    }
                }
            }

            Building contact = null;
            String contactType = null;
            Building genInContact = null;
            for(int mi = 0; mi < cluster.size; mi++){
                Building m = cluster.get(mi);
                for(int gi2 = 0; gi2 < data.instances.size; gi2++){
                    Building pg = data.instances.get(gi2);
                    if(pg != null && pg.isValid() && Mathf.dst(pg.x, pg.y, m.x, m.y) <= rangePx){
                        if(contact == null){
                            contact = m;
                            contactType = isUltimateGrid(pg) ? "ultimate" : (isAdvancedGrid(pg) ? "advanced" : "normal");
                        }
                        if(m.block.outputsPower && genInContact == null) genInContact = m;
                        break;
                    }
                }
            }

            Building rep = null;
            String gridType = null;
            if(genInContact != null){
                rep = genInContact;
                gridType = contactType;
            }else if(contact != null){
                rep = contact;
                gridType = contactType;
            }else{
                for(int mi = 0; mi < cluster.size; mi++){
                    Building m = cluster.get(mi);
                    if(!m.block.outputsPower && m.block.consPower != null){
                        rep = m;
                        break;
                    }
                }
            }
            if(rep == null) continue;

            addToVirtualGraph(rep);
            if(rep.block.outputsPower){
                data.connectedGenerators.add(new GeneratorEntry(rep, gridType != null ? gridType : "normal"));
            }else if(rep.block.consPower != null){
                data.connectedConsumers.add(rep);
            }

            for(int mi = 0; mi < cluster.size; mi++){
                Building m = cluster.get(mi);
                if(m == rep) continue;
                if(m.block.outputsPower) data.clusterGens.add(m);
                else if(m.block.consPower != null) data.clusterCons.add(m);
            }
        }

        // 第三轮：用电器不参与邻接合并，必须自身处于电网覆盖范围内才算连接
        Groups.build.each(b -> {
            if(b == null || !b.isValid() || b.team != refTeam || b.power == null || b.block == null) return;
            if(isGridBlock(b.block)) return;

            Block block = b.block;
            if(block.outputsPower || block.consPower == null) return;
            if(isBatteryBlock(block)) return;

            for(int gi = 0; gi < data.instances.size; gi++){
                Building pg = data.instances.get(gi);
                if(pg != null && pg.isValid() && Mathf.dst(pg.x, pg.y, b.x, b.y) <= rangePx){
                    addToVirtualGraph(b);
                    data.connectedConsumers.add(b);
                    break;
                }
            }
        });

        if(data.virtualGraph != null){
            data.virtualGraph.checkAdd();
        }
    }

    public static void clearVirtualGraph(){
        final Team refTeam = referenceTeam();

        Groups.build.each(b -> {
            if(b == null || !b.isValid() || b.team != refTeam || b.power == null || b.block == null) return;
            if(isGridBlock(b.block)) return;
            PowerGraph tempGraph = new PowerGraph(true);
            tempGraph.add(b);
            if(b.block.consPower != null && !b.block.consPower.buffered) b.power.status = 0f;
        });

        if(data.virtualGraph != null){
            data.virtualGraph.clear();
        }

        data.virtualGraph = null;
        data.connectedGenerators.clear();
        data.connectedConsumers.clear();
        data.connectedBatteries.clear();
        data.clusterGens.clear();
        data.clusterCons.clear();
        data.isOverloaded = false;
        data.lossAmount = 0f;
        data.totalStoredPower = 0f;
        data.totalStorageCapacity = 0f;
        data.smoothCoverage = -1f;
    }

    public static float calculateLossRate(){
        int nc = 0, ac = 0, uc = 0, ng = 0, ag = 0, ug = 0;

        for(int i = 0; i < data.instances.size; i++){
            Building g = data.instances.get(i);
            if(isUltimateGrid(g)) uc++;
            else if(isAdvancedGrid(g)) ac++;
            else nc++;
        }
        for(int i = 0; i < data.connectedGenerators.size; i++){
            String gt = data.connectedGenerators.get(i).gridType;
            if(gt.equals("ultimate")) ug++;
            else if(gt.equals("advanced")) ag++;
            else ng++;
        }

        ng += data.clusterGens.size;

        double gridLoss = POWER_GRID_LOSS_RATE * Math.pow(nc, POWER_GRID_LOSS_EXPONENT)
                        + ADVANCED_POWER_GRID_LOSS_RATE * Math.pow(ac, POWER_GRID_LOSS_EXPONENT)
                        + ULTIMATE_POWER_GRID_LOSS_RATE * Math.pow(uc, POWER_GRID_LOSS_EXPONENT);
        double genLoss = POWER_GRID_GENERATOR_LOSS_RATE * Math.pow(ng, POWER_GRID_LOSS_EXPONENT)
                       + ADVANCED_POWER_GRID_GENERATOR_LOSS_RATE * Math.pow(ag, POWER_GRID_LOSS_EXPONENT)
                       + ULTIMATE_POWER_GRID_GENERATOR_LOSS_RATE * Math.pow(ug, POWER_GRID_LOSS_EXPONENT);

        data.gridLossRate = Math.min(POWER_GRID_MAX_LOSS_RATE, (float)gridLoss);
        data.genLossRate = Math.min(POWER_GRID_MAX_LOSS_RATE, (float)genLoss);
        data.totalLossRate = Math.min(POWER_GRID_MAX_LOSS_RATE, (float)(gridLoss + genLoss));
        return data.totalLossRate;
    }

    public static boolean hasActiveProduction(){
        if(data.connectedGenerators.size == 0 && data.clusterGens.size == 0) return false;
        for(int i = 0; i < data.connectedGenerators.size; i++){
            Building gen = data.connectedGenerators.get(i).build;
            if(gen != null && gen.isValid() && gen.getPowerProduction() > 0f) return true;
        }
        for(int i = 0; i < data.clusterGens.size; i++){
            Building gen = data.clusterGens.get(i);
            if(gen != null && gen.isValid() && gen.getPowerProduction() > 0f) return true;
        }
        return false;
    }

    public static void updateStats(){
        if(data.virtualGraph == null){
            data.currentDemand = 0f;
            data.currentProduction = 0f;
            data.surplusBalance = 0f;
            data.isOverloaded = false;
            data.totalStoredPower = 0f;
            data.totalStorageCapacity = 0f;
            data.lossAmount = 0f;
            data.smoothProduction = 0f;
            data.smoothDemand = 0f;
            return;
        }

        float rawProd = 0f;
        for(int i = 0; i < data.connectedGenerators.size; i++){
            Building gen = data.connectedGenerators.get(i).build;
            if(gen != null && gen.isValid()) rawProd += gen.getPowerProduction() * 60f;
        }

        for(int i = 0; i < data.clusterGens.size; i++){
            Building gen = data.clusterGens.get(i);
            if(gen != null && gen.isValid()) rawProd += gen.getPowerProduction() * 60f;
        }

        float rawDem = 0f;
        for(int i = 0; i < data.connectedConsumers.size; i++){
            Building con = data.connectedConsumers.get(i);
            if(con != null && con.isValid() && con.shouldConsumePower){
                ConsumePower cp = con.block.consPower;
                if(cp != null) rawDem += cp.requestedPower(con) * 60f;
            }
        }

        for(int i = 0; i < data.clusterCons.size; i++){
            Building con = data.clusterCons.get(i);
            if(con != null && con.isValid() && con.shouldConsumePower){
                ConsumePower cp = con.block.consPower;
                if(cp != null) rawDem += cp.requestedPower(con) * 60f;
            }
        }

        data.smoothProduction = data.smoothProduction * 0.85f + rawProd * 0.15f;
        data.smoothDemand = data.smoothDemand * 0.85f + rawDem * 0.15f;

        float totalProd = data.smoothProduction;
        calculateLossRate();
        float genLoss = totalProd * data.genLossRate;
        float gridLoss = (totalProd > 0f ? Math.min(totalProd, data.smoothDemand) : 0f) * data.gridLossRate;

        int uc = 0;
        for(int i = 0; i < data.instances.size; i++){
            if(isUltimateGrid(data.instances.get(i))) uc++;
        }
        float preDed = uc * ULTIMATE_POWER_GRID_PRE_DEDUCTION;
        data.lossAmount = Math.min(genLoss + gridLoss + preDed, totalProd);
        data.currentProduction = totalProd - data.lossAmount;

        data.currentDemand = data.smoothDemand;
        data.surplusBalance = data.currentProduction - data.currentDemand;
        data.isOverloaded = data.currentDemand > (data.currentProduction + data.totalCapacity);

        data.totalStoredPower = data.virtualGraph.getLastPowerStored();
        data.totalStorageCapacity = data.virtualGraph.getLastCapacity();
    }

    public static void enforceVirtualGraph(){
        if(data.virtualGraph == null || data.isLoading) return;

        for(int i = data.connectedGenerators.size - 1; i >= 0; i--){
            Building b = data.connectedGenerators.get(i).build;
            if(b == null || !b.isValid()){
                data.connectedGenerators.remove(i);
                continue;
            }
            if(b.power != null && b.power.graph != data.virtualGraph) data.virtualGraph.add(b);
        }
        for(int i = data.connectedConsumers.size - 1; i >= 0; i--){
            Building b = data.connectedConsumers.get(i);
            if(b == null || !b.isValid()){
                data.connectedConsumers.remove(i);
                continue;
            }
            if(b.power != null && b.power.graph != data.virtualGraph) data.virtualGraph.add(b);
        }
        for(int i = data.connectedBatteries.size - 1; i >= 0; i--){
            Building b = data.connectedBatteries.get(i);
            if(b == null || !b.isValid()){
                data.connectedBatteries.remove(i);
                continue;
            }
            if(b.power != null && b.power.graph != data.virtualGraph) data.virtualGraph.add(b);
        }
    }

    public static void globalUpdate(){
        if(state.isPaused()) return;
        if(data.instances.size == 0){
            if(data.virtualGraph != null) clearVirtualGraph();
            return;
        }

        enforceVirtualGraph();

        if(data.graphDirty && Time.time - data.lastRebuildTime >= GRAPH_REBUILD_DEBOUNCE){
            data.graphDirty = false;
            data.lastRebuildTime = Time.time;
            rebuildVirtualGraph();
        }

        if(data.virtualGraph != null){
            float lossRate = calculateLossRate();
            int uc = 0;
            for(int i = 0; i < data.instances.size; i++){
                if(isUltimateGrid(data.instances.get(i))) uc++;
            }
            float preDed = uc * ULTIMATE_POWER_GRID_PRE_DEDUCTION / 60f;

            data.virtualGraph.update();

            float produced = data.virtualGraph.getPowerProduced();
            float needed = data.virtualGraph.getLastPowerNeeded();
            float cProd = 0f, cDem = 0f;
            for(int i = 0; i < data.clusterGens.size; i++){
                Building g = data.clusterGens.get(i);
                if(g != null && g.isValid()) cProd += g.getPowerProduction() * g.delta();
            }
            for(int i = 0; i < data.clusterCons.size; i++){
                Building c = data.clusterCons.get(i);
                if(c != null && c.isValid() && c.shouldConsumePower && c.block.consPower != null){
                    cDem += c.block.consPower.requestedPower(c) * c.delta();
                }
            }
            produced += cProd;
            needed += cDem;
            float genLoss = produced * data.genLossRate;
            float gridLoss = Math.min(produced, needed) * data.gridLossRate;
            float totalDed = Math.min(genLoss + gridLoss + preDed, produced);
            // 仅在真正缺电时才扣电池，避免电池存量反复震荡
            if(totalDed > 0f && needed > produced) data.virtualGraph.useBatteries(totalDed);

            // 仅在缺电时接管供电覆盖判定；盈余时交给原版 distributePower 处理
            if((lossRate > 0f || preDed > 0f) && needed > produced){
                needed = data.virtualGraph.getLastPowerNeeded();
                for(int i = 0; i < data.clusterCons.size; i++){
                    Building c = data.clusterCons.get(i);
                    if(c != null && c.isValid() && c.shouldConsumePower && c.block.consPower != null){
                        needed += c.block.consPower.requestedPower(c) * c.delta();
                    }
                }
                if(needed > 0f){
                    float genProd = 0f;
                    Seq<Building> prods = data.virtualGraph.producers;
                    if(prods != null){
                        for(int i = 0; i < prods.size; i++){
                            Building p = prods.get(i);
                            if(p != null && p.isValid()) genProd += p.getPowerProduction() * p.delta();
                        }
                    }
                    for(int i = 0; i < data.clusterGens.size; i++){
                        Building g = data.clusterGens.get(i);
                        if(g != null && g.isValid()) genProd += g.getPowerProduction() * g.delta();
                    }
                    float genNet = genProd * (1f - lossRate) - preDed;
                    float stored = data.virtualGraph.getLastPowerStored();
                    float gap = Math.max(0f, needed - genNet);
                    float battPower = Math.min(gap, stored);
                    float rawCov = Math.min(1f, Math.max(0f, (genNet + battPower) / needed));
                    data.smoothCoverage = data.smoothCoverage < 0f ? rawCov : data.smoothCoverage * 0.85f + rawCov * 0.15f;
                    float cov = data.smoothCoverage;
                    Seq<Building> consumers = data.virtualGraph.consumers;
                    for(int i = 0; i < consumers.size; i++){
                        Building con = consumers.get(i);
                        if(con != null && con.block != null && con.block.consPower != null && !con.block.consPower.buffered && con.shouldConsumePower){
                            con.power.status = cov;
                        }
                    }
                    for(int i = 0; i < data.clusterCons.size; i++){
                        Building con = data.clusterCons.get(i);
                        if(con != null && con.isValid() && con.block != null && con.block.consPower != null && !con.block.consPower.buffered && con.shouldConsumePower){
                            con.power.status = cov;
                        }
                    }
                }
            }
        }
        updateStats();

        if(data.instances.size > 0 && data.virtualGraph != null){
            float totalRate = 0f;
            Seq<Building> prods = data.virtualGraph.producers;
            if(prods != null){
                for(int i = 0; i < prods.size; i++){
                    totalRate += prods.get(i).getPowerProduction();
                }
            }
            for(int i = 0; i < data.clusterGens.size; i++){
                Building g = data.clusterGens.get(i);
                if(g != null && g.isValid()) totalRate += g.getPowerProduction();
            }
            boolean isDangerOverload = totalRate * 60f >= getTotalOverloadLimit() * POWER_GRID_OVERLOAD_TRIGGER_RATIO;
            if(POWER_GRID_OVERLOAD_KILL_ENABLED && isDangerOverload){
                if(data.overloadSnapshot == null){
                    data.overloadSnapshot = new float[data.instances.size];
                    for(int i = 0; i < data.instances.size; i++){
                        Building pg0 = data.instances.get(i);
                        data.overloadSnapshot[i] = (pg0 != null && pg0.isValid()) ? pg0.health : 0f;
                    }
                }
                for(int i = 0; i < data.instances.size; i++){
                    Building pg = data.instances.get(i);
                    if(pg == null || !pg.isValid()) continue;
                    if(Mathf.chance(Time.delta * POWER_GRID_OVERLOAD_LIGHTNING_CHANCE)){
                        Color color = isUltimateGrid(pg) ? GRID_LIGHTNING_COLOR_ULTIMATE : (isAdvancedGrid(pg) ? GRID_LIGHTNING_COLOR_ADVANCED : GRID_LIGHTNING_COLOR_NORMAL);
                        triggerGridLightning(pg, color);
                    }
                    if(Mathf.chance(Time.delta * POWER_GRID_OVERLOAD_DAMAGE_CHANCE)){
                        pg.damage(POWER_GRID_OVERLOAD_DAMAGE);
                    }
                    if(i < data.overloadSnapshot.length){
                        pg.health = Math.min(pg.health, data.overloadSnapshot[i]);
                    }
                    if(pg.health <= 0f) pg.kill();
                }
            }else{
                data.overloadSnapshot = null;
            }
        }
    }

    public static void worldLoad(){
        data.isLoading = true;
        // 清空上一张地图残留的电网实例与过载快照
        data.instances.clear();
        data.totalCapacity = 0f;
        data.overloadSnapshot = null;
        data.isOverloaded = false;
        data.graphDirty = false;
        data.lastRebuildTime = 0f;
        data.currentDemand = 0f;
        data.currentProduction = 0f;
        data.surplusBalance = 0f;
        data.lossAmount = 0f;

        Time.run(10f, () -> {
            data.isLoading = false;
            Groups.build.each(b -> {
                if(b != null && b.isValid() && b.block != null && isGridBlock(b.block)){
                    if(data.instances.indexOf(b) < 0) data.instances.add(b);
                }
            });

            if(data.instances.size > 0){
                data.totalCapacity = data.instances.size * POWER_GRID_CAPACITY;
                rebuildVirtualGraph();
            }else{
                clearVirtualGraph();
            }
        });
    }

    public static class GeneratorEntry{
        public final Building build;
        public final String gridType;

        public GeneratorEntry(Building build, String gridType){
            this.build = build;
            this.gridType = gridType;
        }
    }

    public static class GridData{
        public Seq<Building> instances = new Seq<>();
        public float totalCapacity = 0f;
        public boolean isOverloaded = false;
        public float currentDemand = 0f;
        public float currentProduction = 0f;
        public float surplusBalance = 0f;
        public float lossAmount = 0f;
        public float overloadFlashTimer = 0f;
        public float[] overloadSnapshot = null;
        public PowerGraph virtualGraph = null;
        public Seq<GeneratorEntry> connectedGenerators = new Seq<>();
        public Seq<Building> connectedConsumers = new Seq<>();
        public Seq<Building> connectedBatteries = new Seq<>();
        public Seq<Building> clusterGens = new Seq<>();
        public Seq<Building> clusterCons = new Seq<>();
        public float totalStoredPower = 0f;
        public float totalStorageCapacity = 0f;
        public float smoothProduction = 0f;
        public float smoothDemand = 0f;
        public boolean isLoading = false;
        public float totalLossRate = 0f;
        public float gridLossRate = 0f;
        public float genLossRate = 0f;
        public float smoothCoverage = -1f;
        public boolean graphDirty = false;
        public float lastRebuildTime = 0f;
    }

    public class PowerGridBuild extends Building{

        @Override
        public void created(){
            super.created();
            registerPowerGrid(this);
        }

        @Override
        public void onRemoved(){
            unregisterPowerGrid(this);
            super.onRemoved();
        }

        @Override
        public void updateTile(){
            super.updateTile();
            data.overloadFlashTimer++;
            if(hasActiveProduction() && Mathf.random() < lightningChance){
                triggerGridLightning(this, lightningColor);
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            drawPowerGridUI(this);
        }

        @Override
        public void tapped(){
            super.tapped();
        }

        @Override
        public void write(Writes write){
            super.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
        }
    }
}
