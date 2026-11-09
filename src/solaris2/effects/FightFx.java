package solaris2.effects;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/**
 * 由 scripts/lib/fightFx.js 移植而来。
 * 提供硅合金墙与磁暴线圈使用的特效。
 */
public class FightFx{

    public static final Effect chainLightningFade = new Effect(25f, 500f, e -> {
        if(!(e.data instanceof Position p)) return;
        float tx = p.getX(), ty = p.getY(), dst = Mathf.dst(e.x, e.y, tx, ty);
        Tmp.v1.set(p.getX(), p.getY()).sub(e.x, e.y).nor();

        float normx = Tmp.v1.x, normy = Tmp.v1.y, range = e.rotation;
        float links = Mathf.ceil(dst / range), spacing = dst / links;

        Lines.stroke(2.5f * Mathf.curve(e.fout(), 0f, 0.7f), e.color);

        Lines.beginLine();
        Lines.linePoint(e.x, e.y);
        Fx.rand.setSeed(e.id);

        float fin = Mathf.curve(e.fin(), 0f, 0.5f);
        for(int i = 0; i < links * fin; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(Fx.rand).scl(range / 2f);
                nx = e.x + normx * len + Tmp.v1.x;
                ny = e.y + normy * len + Tmp.v1.y;
            }
            Lines.linePoint(nx, ny);
        }
        Lines.endLine();
    });
    static{
        chainLightningFade.followParent = true;
    }

    public static final Effect chainLightningFade2 = new Effect(20f, 500f, e -> {
        if(!(e.data instanceof Position p)) return;
        float tx = e.x, ty = e.y, dst = Mathf.dst(p.getX(), p.getY(), tx, ty);
        Tmp.v1.set(e.x, e.y).sub(p.getX(), p.getY()).nor();

        float normx = Tmp.v1.x, normy = Tmp.v1.y, range = e.rotation;
        float links = Mathf.ceil(dst / range), spacing = dst / links;

        Lines.stroke(2.5f * e.fout());
        Draw.color(Color.white, e.color, e.fin());

        Lines.beginLine();
        Lines.linePoint(p);
        Fx.rand.setSeed(e.id);

        for(int i = 0; i < links; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(Fx.rand).scl(range / 2f);
                nx = p.getX() + normx * len + Tmp.v1.x;
                ny = p.getY() + normy * len + Tmp.v1.y;
            }
            Lines.linePoint(nx, ny);
        }
        Lines.endLine();
    });
    static{
        chainLightningFade2.followParent = false;
    }

    public static final Effect chainLightningFadeReversed = new Effect(25f, 500f, e -> {
        if(!(e.data instanceof Position p)) return;
        float tx = e.x, ty = e.y, dst = Mathf.dst(p.getX(), p.getY(), tx, ty);
        Tmp.v1.set(e.x, e.y).sub(p.getX(), p.getY()).nor();

        float normx = Tmp.v1.x, normy = Tmp.v1.y, range = e.rotation;
        float links = Mathf.ceil(dst / range), spacing = dst / links;

        Lines.stroke(2.5f * Mathf.curve(e.fout(), 0f, 0.7f), e.color);

        Lines.beginLine();
        Lines.linePoint(p);
        Fx.rand.setSeed(e.id);

        float fin = Mathf.curve(e.fin(), 0f, 0.5f);
        for(int i = 0; i < links * fin; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(Fx.rand).scl(range / 2f);
                nx = p.getX() + normx * len + Tmp.v1.x;
                ny = p.getY() + normy * len + Tmp.v1.y;
            }
            Lines.linePoint(nx, ny);
        }
        Lines.endLine();
    });
    static{
        chainLightningFadeReversed.followParent = false;
    }

    public static final Effect smallRectBurst = new Effect(18f, 60f, e -> {
        Draw.color(e.color);
        Draw.alpha(e.fout());
        for(int i = 0; i < 12; i++){
            float rot = Mathf.randomSeed(e.id * 7 + i, 360f);
            float dist = Mathf.randomSeed(e.id + i, 42f) * e.fin();
            Tmp.v1.trns(rot, dist);
            Fill.rect(e.x + Tmp.v1.x, e.y + Tmp.v1.y, 3f, 1f, rot);
        }
    });
    static{
        smallRectBurst.followParent = false;
    }
}
