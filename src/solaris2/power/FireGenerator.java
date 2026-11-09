package solaris2.power;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.Vec2;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.world.blocks.power.ConsumeGenerator;

/** 火力发电机：燃烧可燃物品发电，带烟雾特效。 */
public class FireGenerator extends ConsumeGenerator{

    // 发电烟雾特效
    public static final Effect smoke = new Effect(160f, e -> {
        Draw.color(Color.valueOf("6E685A"));   // 烟雾颜色：灰褐色
        Draw.alpha(0.6f);                      // 整体透明度 60%
        Rand rand = Fx.rand;
        Vec2 v = Fx.v;
        rand.setSeed(e.id);                    // 用特效自身 id 做随机种子，保证每个特效稳定

        for(int i = 0; i < 3; i++){            // 每次喷 3 股烟
            float len = rand.random(6f);       // 随机的偏移长度
            float rot = rand.range(40f) + e.rotation;   // 以建筑朝向为中心 ±20° 散开
            e.scaled(e.lifetime * rand.random(0.3f, 1f), b -> {   // 每股烟寿命随机缩短
                v.trns(rot, len * b.finpow());          // 沿随机方向向外飘
                Fill.circle(e.x + v.x, e.y + v.y,
                        1.5f * b.fslope() + 0.5f);       // 先大后小的实心圆，模拟烟点
            });
        }
    });

    public FireGenerator(String name){
        super(name);
        generateEffect = smoke;     // 单个特效，直接喷在建筑中心
        effectChance = 0.2f;        // 每帧 20% 概率触发
    }
}
