package solaris2.transport;

import arc.Core;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.Styles;
import mindustry.world.blocks.distribution.Junction;

import static mindustry.Vars.*;

/**
 * 反向交叉器：交叉器的反向版本，物品向相邻侧偏转穿行，点击按钮可切换穿行方向。
 * 由 scripts/建筑/运输/反向交叉器.js 移植。
 */
public class InvertedJunction extends Junction{
    // 流向小箭头参数
    private static final float arrowDist = 2.9f, arrowOffset = 1.2f, arrowRadius = 1.7f;

    public InvertedJunction(String name){
        super(name);
        // 关闭缓存绘制并改为逐帧绘制，切换方向后箭头能立刻刷新
        drawCached = false;
        drawDynamic = true;
        // 不打开可配置，点击方块根本不会弹面板，buildConfiguration 不会被调用
        configurable = true;
        buildType = InvertedJunctionBuild::new;
    }

    public class InvertedJunctionBuild extends JunctionBuild{
        public int loc = 1;

        @Override
        public void updateTile(){
            for(int i = 0; i < 4; i++){
                int p = (i + loc) % 4;
                if(buffer.indexes[i] > 0){
                    if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
                    long l = buffer.buffers[i][0];
                    float time = BufferItem.time(l);
                    if(Time.time >= time + speed / timeScale() || Time.time < time){
                        Item item = content.item(BufferItem.item(l));
                        Building dest = nearby(p);
                        if(item == null || dest == null || !dest.acceptItem(this, item) || dest.team != team){
                            continue;
                        }
                        dest.handleItem(this, item);
                        System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
                        buffer.indexes[i]--;
                    }
                }
            }
        }

        @Override
        public void draw(){
            Draw.rect(Core.atlas.find(name), x, y);
            // 方向贴图存在才画，否则会画出 error 占位图
            TextureRegion dir = Core.atlas.find(name + "-junction-" + loc);
            if(dir.found()) Draw.rect(dir, x, y);

            // 四角小箭头沿切向排布，指示当前偏转旋向：loc = 1 顺时针，loc = 3 逆时针
            float rot = loc == 3 ? 90f : -90f;
            Draw.color(Pal.accent);
            for(int k = 0; k < 4; k++){
                float angle = 45 + k * 90, pointAngle = angle + rot;
                Fill.poly(
                    x + Angles.trnsx(angle, arrowDist) + Angles.trnsx(pointAngle, arrowOffset),
                    y + Angles.trnsy(angle, arrowDist) + Angles.trnsy(pointAngle, arrowOffset),
                    3, arrowRadius, pointAngle
                );
            }
            Draw.color();
        }

        // 更改物品进入原则
        @Override
        public boolean acceptItem(Building source, Item item){
            int relative = source.relativeTo(tile);
            if(relative == -1 || !buffer.accepts(relative)) return false;
            Building to = nearby((relative + loc) % 4);
            return to != null && to.team == team;
        }

        // 点击显示按钮
        @Override
        public void buildConfiguration(Table table){
            // sprites/ 下的贴图会被自动加上 mod 前缀，flip.png 在图集里叫「索拉里斯2.0-flip」
            TextureRegion icon = Core.atlas.find(minfo.mod.name + "-flip", Core.atlas.find("clear"));
            table.button(new TextureRegionDrawable(icon), Styles.cleari, () -> switchf()).size(36).tooltip("switch");
        }

        // 用于转换
        public void switchf(){
            loc = loc == 1 ? 3 : 1;
            deselect();
            configure(loc);
        }

        // 点击反馈
        @Override
        public Object config(){
            return loc;
        }

        @Override
        public void configured(Unit builder, Object value){
            super.configured(builder, value);
            loc = (Integer)value;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(loc);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            loc = (int)read.f();
        }
    }
}
