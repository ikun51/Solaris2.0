package solaris2.support;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.graphics.Drawf;
import mindustry.world.blocks.defense.OverdriveProjector;

import static mindustry.Vars.*;

/** 超频投影：将超速场范围由圆形改为正方形，其余逻辑与原版 OverdriveProjector 保持一致。 */
public class OverdriveProjection extends OverdriveProjector{

    static final Rect rangeRect = new Rect();

    public OverdriveProjection(String name){
        super(name);
        buildType = OverdriveProjectionBuild::new;
    }

    // 建造预览：方形范围 + 方形选区高亮
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        // 只执行基类 Block 的放置预览（电力连线 + 覆盖层），跳过父类 OverdriveProjector 的圆形范围绘制
        drawPotentialLinks(x, y);

        float cx = x * tilesize + offset;
        float cy = y * tilesize + offset;
        float range = this.range;

        drawOverlay(cx, cy, rotation);
        Drawf.dashSquare(baseColor, cx, cy, range * 2f);

        indexer.eachBlock(player.team(), rangeRect.setCentered(cx, cy, range * 2f),
                other -> other.block.canOverdrive
                        && Math.abs(other.x - cx) <= range + other.hitSize() / 2f
                        && Math.abs(other.y - cy) <= range + other.hitSize() / 2f,
                other -> {
                    Tmp.c1.set(baseColor);
                    Tmp.c1.a = Mathf.absin(4f, 1f);
                    Drawf.selected(other, Tmp.c1);
                });
    }

    public class OverdriveProjectionBuild extends OverdriveBuild{

        @Override
        public void updateTile(){
            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency, 0.08f);
            heat = Mathf.lerpDelta(heat, efficiency > 0 ? 1f : 0f, 0.08f);
            charge += heat * Time.delta;

            if(hasBoost){
                phaseHeat = Mathf.lerpDelta(phaseHeat, optionalEfficiency, 0.1f);
            }

            if(charge >= reload){
                float realRange = range + phaseHeat * phaseRangeBoost;
                charge = 0f;

                indexer.eachBlock(team, rangeRect.setCentered(x, y, realRange * 2f),
                        other -> other.block.canOverdrive
                                && Math.abs(other.x - x) <= realRange + other.hitSize() / 2f
                                && Math.abs(other.y - y) <= realRange + other.hitSize() / 2f,
                        other -> other.applyBoost(realBoost(), reload + 1f));
            }

            if(efficiency > 0){
                useProgress += delta();
            }

            if(useProgress >= useTime){
                consume();
                useProgress = useProgress % useTime;
            }
        }

        // 选中时：方形范围 + 方形选区高亮
        @Override
        public void drawSelect(){
            float realRange = range + phaseHeat * phaseRangeBoost;

            indexer.eachBlock(team, rangeRect.setCentered(x, y, realRange * 2f),
                    other -> other.block.canOverdrive
                            && Math.abs(other.x - x) <= realRange + other.hitSize() / 2f
                            && Math.abs(other.y - y) <= realRange + other.hitSize() / 2f,
                    other -> {
                        Tmp.c1.set(baseColor);
                        Tmp.c1.a = Mathf.absin(4f, 1f);
                        Drawf.selected(other, Tmp.c1);
                    });

            Drawf.dashSquare(baseColor, x, y, realRange * 2f);
        }
    }
}
