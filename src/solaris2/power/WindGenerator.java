package solaris2.power;

import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** 风力发电机：根据周围空旷程度与天气获得发电效率。 */
public class WindGenerator extends ConsumeGenerator{

    public static final int range = 4;

    public static final float[][] weightMap = new float[range * 2 + 1][range * 2 + 1];

    static{
        float totalWeight = 0f;
        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                if(Math.abs(dx) <= 1 && Math.abs(dy) <= 1) continue;
                int dist = Math.abs(dx) + Math.abs(dy);
                float w = 1f / (1f + dist * 0.5f);
                weightMap[dx + range][dy + range] = w;
                totalWeight += w;
            }
        }
        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                weightMap[dx + range][dy + range] /= totalWeight;
            }
        }
    }

    public WindGenerator(){
        super("风力发电机");
        update = true;
        powerProduction = 3f;
        warmupSpeed = 0f;
        buildType = WindGeneratorBuild::new;
    }

    public static float weathermul(String name){
        switch(name){
            case "rain": return 1.3f;
            case "snowing": return 0.8f;
            case "sandstorm": return 1.2f;
            case "sporestorm": return 1.5f;
            default: return 1f;
        }
    }

    public static float weight(int dx, int dy){
        return weightMap[dx + range][dy + range];
    }

    public static void drawBlockedTiles(int cx, int cy, float off){
        for(int dx = -range; dx <= range; dx++){
            for(int dy = -range; dy <= range; dy++){
                if(Math.abs(dx) <= off && Math.abs(dy) <= off) continue;
                Tile t = world.tile(cx + dx, cy + dy);
                if(t == null) continue;
                if(t.build != null){
                    Drawf.dashSquare(Pal.breakInvalid, t.worldx(), t.worldy(), tilesize);
                }else{
                    Block b = t.block();
                    if(b != null && b != Blocks.air && b.solid){
                        Drawf.dashSquare(Pal.breakInvalid, t.worldx(), t.worldy(), tilesize);
                    }
                }
            }
        }
        for(int i = 0; i < Groups.unit.size(); i++){
            Unit u = Groups.unit.index(i);
            int dx = u.tileX() - cx;
            int dy = u.tileY() - cy;
            if(Math.abs(dx) > range || Math.abs(dy) > range) continue;
            if(Math.abs(dx) <= off && Math.abs(dy) <= off) continue;
            Drawf.dashSquare(Pal.breakInvalid, u.tileX() * tilesize + tilesize / 2f, u.tileY() * tilesize + tilesize / 2f, tilesize);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashSquare(Pal.accent, x * tilesize + offset, y * tilesize + offset, (range * 2 + 1) * tilesize);
        drawBlockedTiles(x, y, (size - 1) / 2f);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, range * 2 + 1, StatUnit.blocks);
        stats.add(new Stat("条件", new StatCat("条件")), table -> {
            table.add("需要空旷区域");
            table.row();
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("效率", (WindGeneratorBuild e) -> new Bar(
            () -> "效率" + Math.round(e.geteff() * 100) + "%",
            () -> Color.valueOf("ffd06d"),
            () -> e.geteff()
        ));
    }

    public class WindGeneratorBuild extends ConsumeGeneratorBuild{
        public int count = 0;
        public int ix = -range;
        public int iy = -range;
        public float bar = 0f;
        public float windWarmup = 0.6f;
        public float target = 0.6f;
        public float timer = 0f;

        public float getbars(){
            if(bar < 1f) return bar;
            return 1f;
        }

        public float geteff(){
            return windWarmup * (1f - getbars()) * weatherboost();
        }

        public float weatherboost(){
            float boost = 1f;
            for(int i = 0; i < Groups.weather.size(); i++){
                WeatherState w = Groups.weather.index(i);
                boost *= weathermul(w.weather.name);
            }
            return boost;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            float off = (block.size - 1) / 2f;
            int cx = tileX();
            int cy = tileY();

            for(int s = 0; s < 15; s++){
                if(!(Math.abs(ix) <= off && Math.abs(iy) <= off)){
                    Building other = world.build(cx + ix, cy + iy);
                    if(other != null && other != this){
                        count += weight(ix, iy);
                    }else{
                        // 地形阻挡：山体/悬崖/墙等固体方块
                        Tile tt = world.tile(cx + ix, cy + iy);
                        if(tt != null){
                            Block b = tt.block();
                            if(b != null && b != Blocks.air && b.solid){
                                count += weight(ix, iy);
                            }
                        }
                    }
                }
                ix++;
                if(ix >= range + 1){
                    ix = -range;
                    iy++;
                }
                if(iy >= range + 1){
                    iy = -range;
                    bar = count;
                    count = 0;
                }
            }

            float total = bar;
            for(int i = 0; i < Groups.unit.size(); i++){
                Unit u = Groups.unit.index(i);
                int dx = u.tileX() - cx;
                int dy = u.tileY() - cy;
                if(Math.abs(dx) > range || Math.abs(dy) > range) continue;
                if(Math.abs(dx) <= off && Math.abs(dy) <= off) continue;
                if(world.build(u.tileX(), u.tileY()) != null) continue;
                total += weight(dx, dy);
            }
            if(total > 1f) total = 1f;
            bar = total;

            timer -= Time.delta / 60f;
            if(timer <= 0f){
                float r = Mathf.random();
                if(r < 0.12f){
                    target = 1.05f + Mathf.random() * 0.15f;
                    timer = (5f + Mathf.random() * 2f) * 2f;
                }else if(r < 0.24f){
                    target = 0.05f + Mathf.random() * 0.05f;
                    timer = (4f + Mathf.random() * 6f) * 2f;
                }else{
                    target = 0.2f + Mathf.random() * 0.8f;
                    timer = (9f + Mathf.random() * 51f) * 2f;
                }
            }

            windWarmup = windWarmup + (target - windWarmup) * Math.min(1f, Time.delta / 60f * 0.1f);

            float eff = windWarmup * (1f - bar) * weatherboost();

            warmup = windWarmup;
            productionEfficiency = eff;
        }

        @Override
        public void drawSelect(){
            Drawf.dashSquare(Pal.accent, x, y, (range * 2 + 1) * tilesize);
            drawBlockedTiles(tile.x, tile.y, (block.size - 1) / 2f);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(bar);
            write.f(windWarmup);
            write.f(target);
            write.f(timer);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            bar = read.f();
            windWarmup = read.f();
            target = read.f();
            timer = read.f();
            count = 0;
            ix = -range;
            iy = -range;
        }
    }
}
