package solaris2.defense;

import arc.*;
import arc.struct.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.*;

/**
 * 由 scripts/建筑/墙/凝胶墙.js 移植而来。
 */
public class GelWall extends Wall{

    public static final float UNIT_HEALTH = 410f;
    public static final float SELF_RATIO = 0.45f;
    public static final float SHARE_RATIO = 0.55f;

    private static final int[][] DIRS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    // 分摊重入守卫：防止把伤害转嫁给相邻墙时，相邻墙又触发一次分摊造成递归
    private static boolean distributing = false;

    public GelWall(){
        super("凝胶墙");
        description = "气凝胶制成";
        health = (int)UNIT_HEALTH;
        size = 1;
        absorbLasers = true;
        buildType = () -> new GelWallBuild();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, Core.bundle.format("ability.cohesionwall", (int)UNIT_HEALTH));
    }

    private static boolean nameEndsWith(String name, String suffix){
        return name != null && name.length() >= suffix.length() && name.endsWith(suffix);
    }

    private static boolean isCohesionWall(Building build){
        if(build == null || !build.isValid() || build.block == null) return false;
        return build.block instanceof GelWall || nameEndsWith(build.block.name, "凝胶墙");
    }

    // 上下左右正交相邻的凝胶墙
    private static Seq<Building> neighborsOf(Building build){
        Seq<Building> result = new Seq<>();
        if(build.tile == null) return result;
        int tx = build.tile.x, ty = build.tile.y;
        for(int[] dir : DIRS){
            Tile t = Vars.world.tile(tx + dir[0], ty + dir[1]);
            if(t != null && t.build != null && isCohesionWall(t.build)){
                result.add(t.build);
            }
        }
        return result;
    }

    private static Seq<Building> collectGroup(Building start){
        Seq<Building> result = new Seq<>();
        IntSet visited = new IntSet();
        Seq<Building> queue = new Seq<>();
        queue.add(start);
        visited.add(start.id);

        while(queue.size > 0){
            Building cur = queue.pop();
            if(!isCohesionWall(cur)) continue;
            result.add(cur);
            Seq<Building> ns = neighborsOf(cur);
            for(Building n : ns){
                if(!visited.contains(n.id)){
                    visited.add(n.id);
                    queue.add(n);
                }
            }
        }
        return result;
    }

    // 沿墙体正交连通的步数，作为距离衰减依据
    private static IntIntMap distancesFrom(Building start){
        IntIntMap dist = new IntIntMap();
        dist.put(start.id, 0);
        Seq<Building> queue = new Seq<>();
        queue.add(start);
        int head = 0;

        while(head < queue.size){
            Building cur = queue.get(head++);
            int d = dist.get(cur.id);
            Seq<Building> ns = neighborsOf(cur);
            for(Building n : ns){
                if(!dist.containsKey(n.id)){
                    dist.put(n.id, d + 1);
                    queue.add(n);
                }
            }
        }
        return dist;
    }

    public class GelWallBuild extends WallBuild{

        @Override
        public void created(){
            super.created();
            // 血量由引擎直接维护，放置时即满血
            maxHealth = UNIT_HEALTH;
            health = UNIT_HEALTH;
        }

        // 返回值 = 引擎实际从本块墙扣除的血量
        @Override
        public float handleDamage(float amount){
            if(amount <= 0) return amount;
            // 被分摊进来的伤害：本块墙自己扣，不再向下转嫁
            if(distributing) return amount;

            Seq<Building> group = collectGroup(this);
            // 孤立墙没有分担对象，全额承伤
            if(group.size <= 1) return amount;

            IntIntMap dist = distancesFrom(this);
            Seq<Building> others = new Seq<>();
            FloatSeq weights = new FloatSeq();
            float totalWeight = 0f;

            for(Building b : group){
                if(b == this) continue;
                if(!dist.containsKey(b.id)) continue;
                int d = dist.get(b.id);
                if(d <= 0) continue;
                float w = 1f / d;
                others.add(b);
                weights.add(w);
                totalWeight += w;
            }

            // 其余相连墙按 1/距离 的权重分担伤害
            if(totalWeight > 0f){
                float shareDmg = amount * SHARE_RATIO;
                distributing = true;
                try{
                    for(int i = 0; i < others.size; i++){
                        Building o = others.get(i);
                        float dmg = shareDmg * weights.get(i) / totalWeight;
                        if(dmg > 0 && o.isValid()) o.damage(dmg);
                    }
                }finally{
                    distributing = false;
                }
            }

            // 本体只承担剩余比例，交给引擎扣血（血量归零时引擎自行摧毁）
            return amount * SELF_RATIO;
        }
    }
}
