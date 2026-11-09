package solaris2.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import solaris2.SolarisContent;

/**
 * 由 scripts/建筑/无人机/无人机母舰平台.js 移植而来。
 * 无人机单位本身仍由 scripts/建筑/无人机/无人机.js 创建。
 */
public class DroneCarrier extends Block{

    private static final float RADIUS = 30f * 8f;
    // 运动拉回上限：只用来防无人机丢得太远，远大于索敌半径，绕圈不会被索敌范围卡边缘
    private static final float RANGE_LIMIT = RADIUS * 2f;
    private static final float RETURN_DIST = 4f * 8f;
    private static final float BUILD_TIME = 60f * 10f;

    /** 单位 id -> 归属建筑，代替 JS 里挂在单位实例上的 getBuilding/setBuilding。 */
    public static final IntMap<Building> droneOwners = new IntMap<>();

    private static UnitType cannonType, missileType;
    private static Item siliconIron;

    /** 首次使用时查找，避免依赖加载顺序。 */
    public static UnitType cannonType(){
        if(cannonType == null) cannonType = SolarisContent.unit("无人机-机炮");
        return cannonType;
    }

    public static UnitType missileType(){
        if(missileType == null) missileType = SolarisContent.unit("无人机-导弹");
        return missileType;
    }

    public static Item siliconIron(){
        if(siliconIron == null) siliconIron = SolarisContent.item("硅铁");
        return siliconIron;
    }

    /**
     * 等价于原 JS 里的“设置无人机类型”：
     * flying = true、countsAsUnit = false（v160 中对应 useUnitCap = false）。
     */
    public static void configureTypes(){
        UnitType cannon = cannonType(), missile = missileType();
        if(cannon != null){
            cannon.flying = true;
            cannon.useUnitCap = false;
        }
        if(missile != null){
            missile.flying = true;
            missile.useUnitCap = false;
        }
    }

    public DroneCarrier(){
        super("无人机母舰平台");
        buildType = () -> new DroneCarrierBuild();

        consume(new ConsumeItems(ItemStack.with(Items.silicon, 20, siliconIron(), 15)));
        // 电力消耗：1.2/tick = 72/秒（电力不足时暂停建造）
        consumePower(1.2f);
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("无人机", (DroneCarrierBuild e) -> new Bar(
            () -> "无人机: " + e.aliveDrones() + "/3",
            () -> Color.valueOf("ff8844"),
            () -> Math.min(1f, e.aliveDrones() / 3f)
        ));

        addBar("建造进度", (DroneCarrierBuild e) -> new Bar(
            () -> {
                int slot = e.buildingSlot();
                if(slot != -1) return "建造中: " + (int)(Math.min(1f, Math.max(0f, e.buildProgress())) * 100f) + "%";
                int queue = e.queueLength();
                return queue > 0 ? "队列等待: " + queue + " 架" : "空闲";
            },
            () -> {
                if(e.buildingSlot() != -1) return Color.valueOf("ff8844");
                return e.queueLength() > 0 ? Color.valueOf("ffcc44") : Color.valueOf("666666");
            },
            () -> {
                if(e.buildingSlot() != -1) return Math.min(1f, Math.max(0f, e.buildProgress()));
                return e.queueLength() > 0 ? 1f : 0f;
            }
        ));
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.output, table -> {
            table.row();
            UnitType[] types = {cannonType(), missileType()};
            for(int i = 0; i < types.length; i++){
                if(types[i] == null) continue;
                UnitType type = types[i];
                table.table(Styles.grayPanel, t -> {
                    t.image(type.uiIcon).size(40f).pad(10f).left().scaling(Scaling.fit);
                    t.add(type.localizedName).left();
                }).growX().pad(5f);
                table.row();
            }
        });
    }

    /** 等价于 JS 的空 AI 控制器：不自动移动/索敌，只由母舰平台驱动。 */
    public static class EmptyController extends AIController{
        @Override
        public void updateUnit(){
        }
    }

    public class DroneCarrierBuild extends Building{

        public int drone0 = -1, drone1 = -1, drone2 = -1;
        public int pending0 = -1, pending1 = -1, pending2 = -1;
        public int q0 = -1, q1 = -1, q2 = -1;
        // 机炮无人机的骚扰状态机：phs相位(0冲刺/1冲出/2掉头)，pht累计路程(px)，phf帧计数兜底，phtp记录冲出方向(度)
        public int[] phs = {0, 0, 0};
        public float[] pht = {0f, 0f, 0f};
        public int[] phf = {0, 0, 0};
        public float[] phtp = {0f, 0f, 0f};
        public int curSlot = -1;
        public float curProgress = 0f;
        public float buildWarmup = 0f;
        public float buildTotalTime = 0f;
        public float lastScanTime = 30f;
        public int droneCount = 0;
        public Seq<Unit> enemies = new Seq<>();

        public int getDroneId(int i){
            return i == 0 ? drone0 : i == 1 ? drone1 : drone2;
        }

        public void setDroneId(int i, int id){
            if(i == 0) drone0 = id;
            else if(i == 1) drone1 = id;
            else drone2 = id;
        }

        public int getPendingId(int i){
            return i == 0 ? pending0 : i == 1 ? pending1 : pending2;
        }

        public void setPendingId(int i, int id){
            if(i == 0) pending0 = id;
            else if(i == 1) pending1 = id;
            else pending2 = id;
        }

        public int getQueueId(int i){
            return i == 0 ? q0 : i == 1 ? q1 : q2;
        }

        public void setQueueId(int i, int id){
            if(i == 0) q0 = id;
            else if(i == 1) q1 = id;
            else q2 = id;
        }

        public int buildingSlot(){
            return curSlot;
        }

        public float buildProgress(){
            return curProgress;
        }

        public int queueLength(){
            int c = 0;
            for(int i = 0; i < 3; i++) if(getQueueId(i) != -1) c++;
            return c;
        }

        public int aliveDrones(){
            return droneCount;
        }

        public void enqueue(int i){
            if(i < 0 || i >= 3 || i == curSlot) return;
            for(int k = 0; k < 3; k++){
                if(getQueueId(k) == i) return;
            }
            for(int k = 0; k < 3; k++){
                if(getQueueId(k) == -1){
                    setQueueId(k, i);
                    return;
                }
            }
        }

        public int dequeue(){
            int slot = q0;
            q0 = q1;
            q1 = q2;
            q2 = -1;
            return slot;
        }

        public void registerPending(){
            for(int i = 0; i < 3; i++){
                if(getPendingId(i) == -1) continue;
                UnitType type = i == 2 ? missileType() : cannonType();
                Unit unit = Groups.unit.getByID(getPendingId(i));

                if(unit != null && unit.isValid() && !unit.dead && unit.type == type){
                    if(getDroneId(i) == -1) droneCount = Math.min(3, droneCount + 1);
                    droneOwners.put(unit.id, this);
                    setDroneId(i, unit.id);
                    setPendingId(i, -1);
                }
            }
        }

        @Override
        public void updateTile(){
            if(lastScanTime <= 0){
                enemies.clear();
                Units.nearbyEnemies(team, x - RADIUS, y - RADIUS, RADIUS * 2f, RADIUS * 2f, u -> {
                    if(u != null && u.isValid() && u.team != team) enemies.add(u);
                });
                lastScanTime = 30f;
            }else{
                lastScanTime -= Time.delta;
            }

            registerPending();

            if(curSlot == -1 && queueLength() > 0){
                curSlot = dequeue();
                curProgress = 0f;
            }

            if(curSlot != -1){
                // 资源不足（20 硅 + 15 硅铁）或没电时暂停建造
                if(items.get(Items.silicon) >= 20 && items.get(siliconIron()) >= 15
                && power != null && power.status > 0){
                    buildTotalTime += Time.delta;
                    curProgress += Time.delta / BUILD_TIME;
                    buildWarmup = Mathf.approachDelta(buildWarmup, 1f, 1f / 60f);
                }else{
                    buildWarmup = Mathf.approachDelta(buildWarmup, 0f, 1f / 60f);
                }

                if(curProgress >= 1f){
                    int slot = curSlot;
                    UnitType type = slot == 2 ? missileType() : cannonType();
                    if(type != null){
                        try{
                            Unit unit = type.spawn(team, x, y);
                            if(unit != null && unit.isValid()){
                                items.remove(Items.silicon, 20);
                                items.remove(siliconIron(), 15);
                                droneOwners.put(unit.id, this);
                                unit.controller(new EmptyController());
                                setDroneId(slot, unit.id);
                                droneCount = Math.min(3, droneCount + 1);
                                Fx.spawn.at(x, y);
                            }else{
                                enqueue(slot);
                            }
                        }catch(Exception e){
                            enqueue(slot);
                        }
                    }
                    curSlot = -1;
                    curProgress = 0f;
                }
            }else{
                buildWarmup = Mathf.approachDelta(buildWarmup, 0f, 1f / 60f);
            }

            for(int i = 0; i < 3; i++){
                int id = getDroneId(i);
                if(id == -1) continue;
                Unit drone = Groups.unit.getByID(id);
                if(drone == null || !drone.isValid() || drone.dead || drone.health <= 0){
                    droneCount = Math.max(0, droneCount - 1);
                    setDroneId(i, -1);
                    droneOwners.remove(id);
                    enqueue(i);
                    continue;
                }

                if(drone.team != team) drone.team = team;
                if(droneOwners.get(drone.id) != this) droneOwners.put(drone.id, this);

                float dx = x - drone.x;
                float dy = y - drone.y;
                float distToBase = (float)Math.sqrt(dx * dx + dy * dy);

                if(distToBase > RANGE_LIMIT){
                    float angle = (float)Math.atan2(dy, dx);
                    drone.vel.set(
                        (x + Mathf.cos(angle) * (RANGE_LIMIT - 20f) - drone.x) * 0.1f,
                        (y + Mathf.sin(angle) * (RANGE_LIMIT - 20f) - drone.y) * 0.1f
                    );
                    continue;
                }

                if(drone.x < 0 || drone.x >= Vars.world.width() * Vars.tilesize || drone.y < 0 || drone.y >= Vars.world.height() * Vars.tilesize){
                    drone.vel.set((x - drone.x) * 0.1f, (y - drone.y) * 0.1f);
                    continue;
                }

                Unit nearestEnemy = null;
                float minDist = Float.MAX_VALUE;
                for(int j = 0; j < enemies.size; j++){
                    Unit enemy = enemies.get(j);
                    if(enemy == null || !enemy.isValid()) continue;
                    float edx = drone.x - enemy.x;
                    float edy = drone.y - enemy.y;
                    float d = edx * edx + edy * edy;
                    if(d < minDist){
                        minDist = d;
                        nearestEnemy = enemy;
                    }
                }

                if(nearestEnemy != null){
                    float dxEnemy = nearestEnemy.x - drone.x;
                    float dyEnemy = nearestEnemy.y - drone.y;
                    float dist = (float)Math.sqrt(dxEnemy * dxEnemy + dyEnemy * dyEnemy);

                    if(i == 2){
                        if(dist < 80f){
                            drone.vel.set(-(dxEnemy / dist) * drone.type.speed, -(dyEnemy / dist) * drone.type.speed);
                        }else if(dist > 100f){
                            drone.vel.set((dxEnemy / dist) * drone.type.speed, (dyEnemy / dist) * drone.type.speed);
                        }else{
                            drone.vel.set(0f, 0f);
                        }
                    }else{
                        // ===== 机炮无人机：冲刺骚扰模式（冲→攻→冲出→掉头→再冲），按累积路程计量两段长度 =====
                        float phSpeed = drone.type.speed;
                        float turnRate = 8f;          // 每帧最大转向角度，转弯成弧线不生硬
                        float strikeDist = 40f * 6.5f; // 冲刺段路程(px)：跑满才算打穿，冲刺全程持续开火
                        float outDist = 40f * 6.5f;    // 冲出段路程(px)：跑满才掉头
                        int strikeMax = 800;           // 冲刺兜底帧数（敌方高速移动追不上时防卡）
                        int outMax = 800;              // 冲出兜底帧数
                        int turnMax = 30;              // 掉头阶段帧数

                        int phase = phs[i];
                        float angDeg = (float)(Math.atan2(dyEnemy, dxEnemy) * Mathf.radDeg);
                        float curDeg = drone.vel.len() > 0.1f ? (float)(Math.atan2(drone.vel.y, drone.vel.x) * Mathf.radDeg) : 0f;
                        float wantDeg;
                        float mag;

                        if(phase == 0){
                            // 冲刺：朝敌飞行并持续索敌开火，直到贴脸打穿或跑满strikeDist
                            wantDeg = angDeg;
                            mag = 1f;
                            drone.rotation = angDeg;
                            drone.aim(nearestEnemy);
                            drone.controlWeapons(true);
                            pht[i] += drone.vel.len(); // 累计冲刺路程
                            phf[i]++;
                            if(dist <= 24f || pht[i] > strikeDist || phf[i] > strikeMax){
                                // 攻击完成：记下继续前冲的方向
                                phtp[i] = (float)(Math.atan2(drone.vel.y, drone.vel.x) * Mathf.radDeg);
                                pht[i] = 0f;
                                phf[i] = 0;
                                phase = 1;
                            }
                        }else if(phase == 1){
                            // 冲出：停火，沿记录方向冲出outDist路程后掉头
                            wantDeg = phtp[i];
                            mag = 1f;
                            drone.rotation = wantDeg;
                            drone.controlWeapons(false);
                            pht[i] += drone.vel.len(); // 累计冲出路程
                            phf[i]++;
                            if(pht[i] > outDist || phf[i] > outMax){
                                pht[i] = 0f;
                                phf[i] = 0;
                                phase = 2;
                            }
                        }else{
                            // 掉头：低速调头对准敌人，准备下一轮冲刺
                            wantDeg = angDeg;
                            mag = 0.35f;
                            drone.rotation = angDeg;
                            drone.controlWeapons(false);
                            phf[i]++;
                            if(phf[i] > turnMax){
                                phf[i] = 0;
                                phase = 0;
                            }
                        }

                        // 平滑转向：速度方向每帧最多转过 turnRate，形成弧线
                        float smDeg = drone.vel.len() > 0.1f ? Angles.moveToward(curDeg, wantDeg, turnRate) : wantDeg;
                        drone.vel.set(Mathf.cosDeg(smDeg) * phSpeed * mag, Mathf.sinDeg(smDeg) * phSpeed * mag);
                        phs[i] = phase;
                    }

                    if(i == 2){
                        drone.rotation = (float)(Math.atan2(dyEnemy, dxEnemy) * Mathf.radDeg);
                        drone.aim(nearestEnemy);
                        drone.controlWeapons(true);
                    }
                }else{
                    float dxReturn = x - drone.x;
                    float dyReturn = y - drone.y;
                    float dist = (float)Math.sqrt(dxReturn * dxReturn + dyReturn * dyReturn);
                    if(dist > RETURN_DIST){
                        drone.vel.set((dxReturn / dist) * drone.type.speed, (dyReturn / dist) * drone.type.speed);
                    }else{
                        drone.vel.set(dxReturn * 0.05f, dyReturn * 0.05f);
                    }
                    if(dist > 1f) drone.rotation = (float)(Math.atan2(dyReturn, dxReturn) * Mathf.radDeg);
                    drone.controlWeapons(false);
                }
            }
        }

        @Override
        public void created(){
            super.created();
            Time.runTask(1f, () -> {
                for(int i = 0; i < 3; i++){
                    if(getDroneId(i) != -1 || getPendingId(i) != -1) continue;
                    enqueue(i);
                }
            });
        }

        /** 对应原 JS 的 清除无人机()。 */
        public void clearDrones(){
            for(int i = 0; i < 3; i++){
                int id = getDroneId(i);
                if(id != -1){
                    Unit drone = Groups.unit.getByID(id);
                    if(drone != null && drone.isValid()) drone.kill();
                    droneOwners.remove(id);
                }
                setDroneId(i, -1);
            }
            droneCount = 0;
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();
            clearDrones();
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
            clearDrones();
        }

        @Override
        public void draw(){
            super.draw();

            if(curSlot != -1){
                UnitType type = curSlot == 2 ? missileType() : cannonType();
                if(type != null){
                    Draw.draw(Layer.blockOver, () -> Drawf.construct(this, type, 0f, curProgress, buildWarmup, buildTotalTime));
                }
            }

            Draw.color(Color.valueOf("ffaa66"), 0.3f);
            Lines.stroke(1f);
            for(int i = 0; i < 3; i++){
                int id = getDroneId(i);
                if(id == -1) continue;
                Unit drone = Groups.unit.getByID(id);
                if(drone != null && drone.isValid() && !drone.dead){
                    Lines.line(drone.x, drone.y, x, y);
                }
            }
            Draw.reset();
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            Drawf.circles(x, y, RADIUS, Pal.accent);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(3);
            for(int i = 0; i < 3; i++){
                write.i(getDroneId(i));
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int count = read.s();
            for(int i = 0; i < count && i < 3; i++){
                int id = read.i();
                setPendingId(i, id != -1 ? id : -1);
                setDroneId(i, -1);
            }
            droneCount = 0;

            Time.runTask(5f, () -> {
                for(int i = 0; i < 3; i++){
                    if(getDroneId(i) != -1 || getPendingId(i) != -1) continue;
                    enqueue(i);
                }
            });
        }
    }
}
