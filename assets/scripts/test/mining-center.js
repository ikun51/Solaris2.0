/**
 * 采集中心 (Mining Center)
 * 建造完成后自动释放3个独立的采集矿机单位。
 * 矿机在采集范围内自动采集资源，装满后返回卸货，循环工作。
 * 资源全部采完或超出范围时，矿机返回建筑待机。
 *
 * 矿机单位定义（静态属性）在 content/units/采集矿机.json
 * 脚本中只负责动态逻辑：自定义AI控制器、释放/销毁、补召、UI等。
 */

// ========================================================================
// 工具函数（放最前面，避免引用未定义）
// ========================================================================
function mcRunnable(fn){
    return new JavaAdapter(java.lang.Runnable, { run: fn });
}

// 调试日志开关
const MC_DEBUG = false;
function mcLog(msg){ if(MC_DEBUG) print("[MiningCenter] " + msg); }

// AI 详细日志开关（记录状态切换、挖矿结果、绑定关系等）
// 设为 true 后日志会输出到游戏控制台（Print 输出流）
const MC_LOG_AI = true;
var _mcFrameCounter = 0;  // 全局帧计数器（调试日志用）

// 全局 AI 日志函数：带矿机id+坐标 和 绑定的采集中心坐标
function aiLog(unit, center, msg){
    if(!MC_LOG_AI) return;
    var uid = "?", ux = 0, uy = 0, ctag = "@?";
    try{
        if(unit != null && unit.isValid()){
            uid = unit.id;
            ux = Math.round(unit.x);
            uy = Math.round(unit.y);
        }
    }catch(e){}
    try{
        if(center != null && center.isValid()){
            ctag = "@" + center.tileX() + "," + center.tileY();
        }
    }catch(e){}
    print("[Drone#" + uid + "(" + ux + "," + uy + ")↔Center" + ctag + "] " + msg);
}

// ========================================================================
// ⚙️ 配置区：在这里调整矿机和采集中心的所有参数（代码配置优先于 JSON）
// ========================================================================
const CONFIG = {
    // --- 采集中心建筑 ---
    centerRange: 18,          // 采集范围（格，圆形半径，以建筑为中心）
    centerItemCapacity: 400,  // 建筑物品存储容量（每种物品上限）
    droneCount: 3,            // 每个建筑释放的矿机数量
    centerPowerUse: 2,        // 建筑功耗（每tick，buffered 供电）

    // --- 矿机单位属性（覆盖 units/采集矿机.json 的值）---
    droneSpeed: 2.2,          // 移动速度（原版dagger=1.8，2.2快22%）
    droneItemCapacity: 25,    // 物品容量（装满后返回卸货）
    droneMineTier: 3,         // 采矿等级：1=铜铅 / 2=钛 / 3=钍 / 4=所有
    droneMineSpeed: 1.8,      // 采矿速度（1.0=原版dagger，1.8快80%）
    droneMineRange: 22,       // 采矿距离（像素，÷8=约2.75格）
    droneHealth: 150,         // 生命值（矿机无敌，仅防边界错误）
    droneHitSize: 5,          // 碰撞体积

    // --- AI 行为参数 ---
    droneIdleRetryInterval: 60,  // 待机时重新寻矿的间隔（tick，60=1秒）
    droneMineProgressLogInterval: 30, // 采集进度日志间隔（tick）
};

// 兼容旧代码的别名（避免大规模重命名）
const MINING_CENTER_RANGE = CONFIG.centerRange;
const MINING_CENTER_ITEMS = CONFIG.centerItemCapacity;
const MINING_CENTER_DRONE_COUNT = CONFIG.droneCount;

// ========================================================================
// 全局建筑数据管理（用 tileX_tileY 做稳定key）
// ========================================================================
var mcBuildings = {};

function getMCData(build){
    var key = build.tileX() + "_" + build.tileY();
    if(!mcBuildings[key]){
        mcBuildings[key] = { drones: [], spawned: false };
    }
    return mcBuildings[key];
}

function removeMCData(build){
    var key = build.tileX() + "_" + build.tileY();
    delete mcBuildings[key];
}

// ========================================================================
// 取得矿机 UnitType（由 content/units/采集矿机.json 加载）
// 直接用名字查找，不用 modName 前缀（ContentLoader 会自动匹配）
// 首次获取时用 CONFIG 覆盖 JSON 的值，方便在代码顶部统一调整参数
// ========================================================================
var _droneTypeCache = null;
function getDroneType(){
    if(_droneTypeCache == null){
        // Vars.content.unit() 需要 mod 前缀（索拉里斯-采集矿机），直接用纯名找不到
        // 遍历所有单位，按名字匹配（不用 modName 前缀）
        var all = Vars.content.units();
        for(var i = 0; i < all.size; i++){
            var u = all.get(i);
            if(u.name == "采集矿机" || (u.name != null && u.name.endsWith("-采集矿机"))){
                _droneTypeCache = u;
                mcLog("找到矿机 UnitType: " + u.name + " internalName=" + u.name);
                break;
            }
        }
        if(_droneTypeCache == null){
            mcLog("WARN: 找不到 采集矿机 UnitType！共扫描 " + all.size + " 个单位");
        }else{
            var t = _droneTypeCache;
            // 用 CONFIG 覆盖 JSON 的值（代码配置优先，方便统一调整）
            try{
                t.speed = CONFIG.droneSpeed;
                t.itemCapacity = CONFIG.droneItemCapacity;
                t.mineTier = CONFIG.droneMineTier;
                t.mineSpeed = CONFIG.droneMineSpeed;
                t.mineRange = CONFIG.droneMineRange;
                t.health = CONFIG.droneHealth;
                t.hitSize = CONFIG.droneHitSize;
                mcLog("矿机属性已从 CONFIG 加载: speed=" + t.speed
                    + " mineTier=" + t.mineTier
                    + " mineSpeed=" + t.mineSpeed
                    + " mineRange=" + t.mineRange
                    + " itemCap=" + t.itemCapacity);
            }catch(e){
                mcLog("应用 CONFIG 到 UnitType 失败: " + e);
            }
        }
    }
    return _droneTypeCache;
}

// ========================================================================
// 自定义矿机AI控制器（替代原版MinerAI）
// state字段放到 controller 对象上，方便建筑UI读取
// ========================================================================
function createDroneController(centerBuilding){
    return new JavaAdapter(AIController, {
        // 可被外部读取的状态（UI用）
        state: "idle",        // idle / seeking / moving / mining / returning / unloading
        targetTile: null,
        targetX: 0,
        targetY: 0,
        // ======== 关键修复（v2）========
        // 背景：AIController 中 `unit` 既是 protected 字段名，又是重载方法名
        //       （unit() getter / unit(Unit) setter，见 AIController.java:429-440）。
        // 旧方案问题：在 JS JavaAdapter 中重写 unit(u) 后，this.super$unit(u) 会被
        //       Rhino 解析为读取 Java 字段（Unit 对象）而非调用父类方法，于是
        //       "调用一个对象" 报错：Cannot call property unit... It is not a function,
        //       it is "object"。这就是当前崩溃的根因。
        // 新方案：完全不移除/重写 unit(u) setter。
        //   - setter 由 Java 端触发：u.controller(c) 内部会调用 c.unit(this) 绑定
        //     （Java 方法分派，不经 Rhino 属性查找，见 UnitComp.java:432-441）。
        //   - JS 读取 unit 用 this.unit 属性访问：Rhino 命中 Java 字段，返回 Unit 对象。
        //   - 绝不在 JS 中写 this.unit() 或 this.unit(x)：会触发同样的 "not a function" 崩溃。

        // Null 保护：在每帧 updateUnit 入口处拦截 unit==null，防止父类 updateVisuals NPE
        updateUnit(){
            if(this.unit == null) return;
            this.super$updateUnit();
        },

        // 矿机不攻击、不瞄准敌人（防止变成攻击单位AI）
        updateTargeting(){
        },

        // AIController核心方法：updateMovement() 每帧调用
        updateMovement(){
            try{
                var u = this.unit;
                if(u == null || !u.isValid()) return;
                if(centerBuilding == null || !centerBuilding.isValid()) return;

                var cx = centerBuilding.x;
                var cy = centerBuilding.y;
                var rangePx = MINING_CENTER_RANGE * Vars.tilesize;

                // 0. 全局超时保护：任何状态停留过久 → 强制回 seeking（避免卡死）
                //    用 controller 字段而不是 unit 字段，避免污染 unit
                this.stateTimer = (this.stateTimer || 0) + 1;
                // 不同状态的超时阈值（tick）
                var timeout = 600; // 默认 10 秒
                if(this.state == "moving")    timeout = 240;  // 4 秒走不到 → 重选
                else if(this.state == "mining") timeout = 3600; // 60 秒采不满（罕见）→ 重选
                else if(this.state == "returning") timeout = 240; // 4 秒回不去 → 重选
                else if(this.state == "unloading") timeout = 120; // 2 秒卸不完 → 重选
                else if(this.state == "idle") timeout = 60;   // 1 秒后重新寻矿
                else if(this.state == "seeking") timeout = 30; // 半秒内必出结果

                // 绑定单位日志（每 3 秒打一次，避免刷屏）
                if(this.stateTimer % 180 == 1){
                    var stk = (u.stack != null && u.stack.item != null)
                        ? (u.stack.amount + "/" + u.type.itemCapacity + " " + u.stack.item.name)
                        : "0/" + u.type.itemCapacity;
                    aiLog(u, centerBuilding, "绑定确认 state=" + this.state + " timer=" + this.stateTimer + " stack=" + stk);
                }

                if(this.stateTimer > timeout){
                    aiLog(u, centerBuilding, "⚠ 超时 " + this.state + "(" + this.stateTimer + "t) → seeking");
                    // 强制清理并重置
                    if(u.mineTile != null) u.mineTile = null;
                    this.targetTile = null;
                    this.state = "seeking";
                    this.stateTimer = 0;
                }

                // 1. 矿机物品已满 → 强制返回卸货（优先级最高，覆盖一切）
                if(u.stack.amount >= u.type.itemCapacity && u.stack.item != null
                    && this.state != "returning" && this.state != "unloading"){
                    if(u.mineTile != null) u.mineTile = null;
                    aiLog(u, centerBuilding, "📦 已满 → returning (" + u.stack.amount + "/" + u.type.itemCapacity + " " + u.stack.item.name + ")");
                    this.state = "returning";
                    this.stateTimer = 0;
                }

                // 2. 状态机执行
                switch(this.state){
                    case "idle":      this.doIdle(cx, cy, rangePx); break;
                    case "seeking":   this.doSeek(cx, cy, rangePx); break;
                    case "moving":    this.doMove(); break;
                    case "mining":    this.doMine(cx, cy); break;
                    case "returning": this.doReturn(cx, cy); break;
                    case "unloading": this.doUnload(cx, cy, u); break;
                }
            }catch(e){
                this.state = "idle";
                this.stateTimer = 0;
                // 始终输出 AI 异常，便于排查（之前 MC_DEBUG=false 会静默吞掉错误）
                print("[DroneAI] err: " + e);
            }
        },

        // 切换状态时重置计时器（统一入口）
        setState(newState){
            if(this.state != newState){
                var old = this.state;
                this.state = newState;
                this.stateTimer = 0;
                // 带上 stack 信息便于追踪
                var u = this.unit;
                var stk = "";
                try{
                    if(u != null && u.stack != null && u.stack.item != null){
                        stk = " [stack=" + u.stack.amount + "/" + u.type.itemCapacity + " " + u.stack.item.name + "]";
                    }
                }catch(e){}
                aiLog(u, centerBuilding, "🔄 " + old + " → " + newState + stk);
            }
        },

        doIdle(cx, cy, rangePx){
            var u = this.unit;
            var dist = Mathf.dst(u.x, u.y, cx, cy);
            var preferredR = rangePx * 0.3 + 8;
            var ang = u.angleTo(cx, cy) + (dist < preferredR ? 120 : 60);
            var tx = cx + Mathf.cosDeg(ang) * preferredR;
            var ty = cy + Mathf.sinDeg(ang) * preferredR;
            this.moveToPoint(tx, ty, 5, 2);

            // 待机时每秒尝试一次寻矿；超时保护也会强制切回 seeking
            if((this.stateTimer % 60) == 0 && this.stateTimer > 0){
                this.setState("seeking");
            }
        },

        doSeek(cx, cy, rangePx){
            var tile = this.findBestOre(cx, cy, rangePx);
            var u = this.unit;
            if(tile != null){
                this.targetTile = tile;
                this.targetX = tile.worldx();
                this.targetY = tile.worldy();
                // 寻矿结果日志：找到矿
                var itm = null;
                try{ itm = u.getMineResult(tile); }catch(e){}
                var itemName = itm != null ? itm.name : "?";
                var dist = Math.round(Mathf.dst(tile.worldx(), tile.worldy(), cx, cy));
                aiLog(u, centerBuilding, "✅ 寻矿命中: " + itemName
                    + " @tile(" + tile.x + "," + tile.y + ")"
                    + " 距中心=" + dist + "px");
                this.setState("moving");
            }else{
                // 寻矿结果日志：范围内无矿
                aiLog(u, centerBuilding, "❌ 寻矿失败: 范围" + MINING_CENTER_RANGE + "格内无矿");
                this.setState("idle");
            }
        },

        doMove(){
            var u = this.unit;
            var t = this.targetTile;
            // 目标失效 → 立即重选
            if(t == null || !this.isValidOre(t)){
                aiLog(u, centerBuilding, "⚠ 目标失效 (t=" + (t==null?"null":"invalid") + ") → seeking");
                this.targetTile = null;
                this.setState("seeking");
                return;
            }
            this.moveToPoint(this.targetX, this.targetY, u.type.mineRange * 0.4, 6);

            // 到达采矿范围 + 可采 → 开采（用 0.7 倍 range 确保在 MinerComp 距离检查范围内）
            if(u.within(this.targetX, this.targetY, u.type.mineRange * 0.7) && u.validMine(t, false)){
                u.mineTile = t;
                var itm = null;
                try{ itm = u.getMineResult(t); }catch(e){}
                var itemName = itm != null ? itm.name : "?";
                aiLog(u, centerBuilding, "⛏ 开始采集: " + itemName
                    + " @tile(" + t.x + "," + t.y + ")"
                    + " stack=" + u.stack.amount + "/" + u.type.itemCapacity);
                this.setState("mining");
            }
            // 超时由全局保护处理
        },

        doMine(cx, cy){
            var u = this.unit;
            var t = this.targetTile;
            // 矿石被采完/失效 → 立即重选
            if(t == null || !this.isValidOre(t)){
                aiLog(u, centerBuilding, "⚠ 矿石失效 @tile(" + (t==null?"?":t.x+","+t.y) + ") → seeking");
                u.mineTile = null;
                this.targetTile = null;
                this.setState("seeking");
                return;
            }
            // 矿机意外飘出采矿范围 → 回 moving 状态走过去
            // 注意：MinerComp.update() 也会检查距离并清除 mineTile，所以必须保持在范围内
            if(!u.within(this.targetX, this.targetY, u.type.mineRange * 0.8)){
                aiLog(u, centerBuilding, "⚠ 飘出采矿范围 → moving");
                u.mineTile = null;
                this.setState("moving");
                return;
            }
            // 持续靠近矿石中心，防止飘移被 MinerComp 清除 mineTile
            this.moveToPoint(this.targetX, this.targetY, u.type.mineRange * 0.3, 8);
            u.rotation = Mathf.lerpDelta(u.rotation, u.angleTo(this.targetX, this.targetY), 0.3);

            // 确保 mineTile 设置（MinerComp 可能因距离波动清除它）
            if(u.mineTile == null){
                u.mineTile = t;
            }

            // 采集进度日志（每 30 帧 = 0.5 秒打一次，避免刷屏）
            if(this.stateTimer % 30 == 0 && this.stateTimer > 0){
                var itm = (u.stack != null && u.stack.item != null) ? u.stack.item.name : "-";
                aiLog(u, centerBuilding, "⛏ 采集中: " + itm
                    + " " + u.stack.amount + "/" + u.type.itemCapacity
                    + " timer=" + this.stateTimer);
            }

            // 采满 → 返回（全局保护也会强制切，但这里立即响应）
            if(u.stack.amount >= u.type.itemCapacity){
                var itm = u.stack.item != null ? u.stack.item.name : "?";
                aiLog(u, centerBuilding, "📦 采满 → returning (" + u.stack.amount + " " + itm + ")");
                u.mineTile = null;
                this.setState("returning");
            }
        },

        doReturn(cx, cy){
            var u = this.unit;
            u.mineTile = null;
            this.moveToPoint(cx, cy, Vars.tilesize * 1.6, 4);
            var d = Math.round(Mathf.dst(u.x, u.y, cx, cy));
            if(u.within(cx, cy, Vars.tilesize * 2.2)){
                aiLog(u, centerBuilding, "🏠 到达中心 (距离=" + d + "px) → unloading");
                this.setState("unloading");
            }
            // 超时由全局保护处理（比如建筑被拆/卡墙）
        },

        doUnload(cx, cy, u){
            u.mineTile = null;
            // 卸完了 → 寻下一个矿
            if(u.stack.amount == 0 || u.stack.item == null){
                u.stack.item = null;
                u.stack.amount = 0;
                this.setState("seeking");
                return;
            }
            u.rotation = Mathf.lerpDelta(u.rotation, u.angleTo(cx, cy), 0.3);

            // 没到位 → 移过去（防止状态进入时刚好被推开）
            if(!u.within(cx, cy, Vars.tilesize * 2.6)){
                this.moveToPoint(cx, cy, Vars.tilesize * 1.6, 4);
                return;
            }

            var item = u.stack.item;
            var amt = u.stack.amount;
            var transferred = 0;
            var toCenter = 0, toCore = 0;

            // 1. 优先 → 采集中心建筑
            if(centerBuilding != null && centerBuilding.isValid() && centerBuilding.items != null){
                var acc1 = centerBuilding.acceptStack(item, amt, u);
                if(acc1 > 0){
                    Call.transferItemTo(u, item, acc1, u.x, u.y, centerBuilding);
                    u.stack.amount -= acc1;
                    transferred += acc1;
                    toCenter = acc1;
                    this.spawnUnloadFx(centerBuilding.x, centerBuilding.y, u, item);
                }
            }

            // 2. 建筑不接收或满了 → 最近核心兜底
            if(u.stack.amount > 0){
                var core = u.closestCore();
                if(core != null && u.within(core, 80)){
                    var acc2 = core.acceptStack(item, u.stack.amount, u);
                    if(acc2 > 0){
                        Call.transferItemTo(u, item, acc2, u.x, u.y, core);
                        u.stack.amount -= acc2;
                        transferred += acc2;
                        toCore = acc2;
                    }
                }
            }

            // 卸货结果日志
            var itemName = item != null ? item.name : "?";
            if(u.stack.amount <= 0){
                // 卸干净了 → 立即切回 seeking
                aiLog(u, centerBuilding, "✅ 卸货完成: " + itemName
                    + " 中心+" + toCenter + " 核心+" + toCore
                    + " (总" + transferred + "/" + amt + ")");
                u.stack.item = null;
                u.stack.amount = 0;
                this.setState("seeking");
            }else if(transferred == 0){
                // 两个目标都拒绝接收（都满了）→ 等一会儿再尝试，但用超时保护避免永久卡住
                aiLog(u, centerBuilding, "❌ 卸货失败: 建筑和核心都满了 (剩余" + u.stack.amount + " " + itemName + ")");
            }else{
                // 部分卸货
                aiLog(u, centerBuilding, "⚠ 部分卸货: " + itemName
                    + " 转移" + transferred + "/" + amt
                    + " (剩余" + u.stack.amount + ")");
            }
        },

        // ---------- 寻矿算法：距离+稀缺评分 ----------
        // 多选矿物逻辑：
        //   - selectedItems 非空 → 只采集选中的矿物（多种）
        //   - selectedItems 为空 → 采集范围内所有可采矿物
        findBestOre(cx, cy, rangePx){
            var u = this.unit;
            if(u == null || !u.canMine()) return null;

            // 读取建筑的选中物品列表
            var selected = (centerBuilding != null && centerBuilding.selectedItems != null)
                ? centerBuilding.selectedItems : [];
            var hasSelection = selected.length > 0;

            var best = null;
            var bestScore = -Infinity;
            var radius = Mathf.round(rangePx / Vars.tilesize) + 1;
            var cTX = Mathf.round(cx / Vars.tilesize);
            var cTY = Mathf.round(cy / Vars.tilesize);

            for(var dx = -radius; dx <= radius; dx++){
                for(var dy = -radius; dy <= radius; dy++){
                    var tile = Vars.world.tile(cTX + dx, cTY + dy);
                    if(tile == null) continue;
                    if(!this.isValidOre(tile)) continue;

                    var itm = u.getMineResult(tile);
                    if(itm == null || !u.canMine(itm)) continue;

                    // 有选中物品时，只采集选中的；跳过未选中的
                    if(hasSelection){
                        var isSelected = false;
                        for(var i = 0; i < selected.length; i++){
                            if(selected[i] == itm){ isSelected = true; break; }
                        }
                        if(!isSelected) continue;
                    }

                    var tileDist = Mathf.dst(tile.worldx(), tile.worldy(), cx, cy);
                    if(tileDist > rangePx + 0.1) continue;

                    var inCenter = (centerBuilding != null && centerBuilding.items != null) ? centerBuilding.items.get(itm) : 0;
                    var score = -tileDist * 0.12 + 1.0 / (1 + inCenter) * 25;
                    if(score > bestScore){
                        bestScore = score;
                        best = tile;
                    }
                }
            }
            return best;
        },

        isValidOre(tile){
            var u = this.unit;
            return u != null && tile != null && u.validMine(tile, false);
        },

        // ---------- 移动：优先AIController.moveTo，失败降级手动 ----------
        moveToPoint(tx, ty, radius, smooth){
            var u = this.unit;
            try{
                this.super$moveTo(tx, ty, radius, smooth);
            }catch(e){
                var dx = tx - u.x;
                var dy = ty - u.y;
                var d = Mathf.dst(dx, dy);
                if(d > radius){
                    u.lookAt(tx, ty);
                    var sp = u.type.speed;
                    u.vel.x = Mathf.lerpDelta(u.vel.x, (dx / d) * sp, u.type.accel);
                    u.vel.y = Mathf.lerpDelta(u.vel.y, (dy / d) * sp, u.type.accel);
                }else{
                    u.vel.x = Mathf.lerpDelta(u.vel.x, 0, u.type.drag);
                    u.vel.y = Mathf.lerpDelta(u.vel.y, 0, u.type.drag);
                }
            }
        },

        spawnUnloadFx(bx, by, u, item){
            try{
                if(Mathf.random() < 0.28){
                    Fx.itemTransfer.at(bx, by, 2.2, item.color,
                        new Vec2(u.x, u.y));
                }
            }catch(e){}
        }
    });
}

// ========================================================================
// 采集中心建筑（3×3，不通电，生产矿机单位，可选物品）
// 参考 UnitCargoLoader（单位生产动画）和 UnitCargoUnloadPoint（物品选择UI）
// ========================================================================
var MiningCenter = extend(Block, "采集中心", {
    size: 3,
    health: 800,
    hasItems: true,
    itemCapacity: CONFIG.centerItemCapacity,
    solid: true,
    update: true,
    destructible: true,
    configurable: true,      // 可配置（物品选择UI）
    saveConfig: true,         // 保存配置
    group: BlockGroup.drills,
    category: Category.production,
    rotate: false,
    regionRotated1: 0,
    envEnabled: Env.any,
    envDisabled: 0,
    // 单位生产参数（参考 UnitCargoLoader）
    unitBuildTime: 60 * 8,    // 生产一个矿机需要8秒

    setStats(){
        this.super$setStats();
        this.stats.add(Stat.itemsMoved, 120, StatUnit.itemsSecond);
        this.stats.add(Stat.range,
            StatValues.blocks(MINING_CENTER_RANGE, false));
        this.stats.add(Stat.buildTime, this.unitBuildTime / 60, StatUnit.seconds);
    },

    setBars(){
        this.super$setBars();
        // 矿机生产进度条
        this.addBar("drone-progress", function(entity){
            return new Bar(
                function(){
                    var count = 0;
                    try{
                        for(var i = 0; i < entity.drones.length; i++){
                            if(entity.drones[i] != null && entity.drones[i].isValid()) count++;
                        }
                    }catch(e){}
                    return "进度: " + count + "/" + CONFIG.droneCount;
                },
                function(){ return Pal.accent; },
                function(){
                    var count = 0;
                    var prog = 0;
                    try{
                        for(var i = 0; i < entity.drones.length; i++){
                            if(entity.drones[i] != null && entity.drones[i].isValid()) count++;
                        }
                        prog = entity.buildProgress || 0;
                    }catch(e){}
                    return Math.min(1, (count + prog) / CONFIG.droneCount);
                }
            );
        });
    },
    icons(){ return [this.region]; },

    drawPlace(x, y, rotation, valid){
        this.super$drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(
            x * Vars.tilesize + this.offset,
            y * Vars.tilesize + this.offset,
            MINING_CENTER_RANGE * Vars.tilesize,
            Pal.accent
        );
    }
});

// 物品多选 config 注册
// 用 Item.class 注册：configure(item) 切换选中/取消
MiningCenter.config(Items.copper.getClass(), new Cons2({ get: function(build, item){
    build.toggleSelectedItem(item);
}}));
MiningCenter.configClear(cons(function(build){
    build.selectedItems = [];
}));

// ========================================================================
// 建筑类型
// ========================================================================
MiningCenter.buildType = prov(function(){
    return new JavaAdapter(Building, {
        // 生产进度状态（参考 UnitCargoLoader.UnitTransportSourceBuild）
        // 用 _ 前缀避免和 Building 的 warmup()/totalProgress() 方法冲突
        buildProgress: 0,
        _totalProgress: 0,
        _warmup: 0,
        // 物品多选（数组，空=采全部）
        selectedItems: [],
        // 矿机列表
        drones: [],
        // 存档读取延迟恢复
        readUnitIds: null,

        // ---------- 多选矿物：选中状态管理 ----------
        // 判断物品是否已被选中
        isItemSelected(item){
            if(item == null || this.selectedItems == null) return false;
            for(var i = 0; i < this.selectedItems.length; i++){
                if(this.selectedItems[i] == item) return true;
            }
            return false;
        },

        // 切换物品选中状态（选中→取消 / 未选中→加入）
        toggleSelectedItem(item){
            if(item == null) return;
            if(this.selectedItems == null) this.selectedItems = [];
            var idx = -1;
            for(var i = 0; i < this.selectedItems.length; i++){
                if(this.selectedItems[i] == item){ idx = i; break; }
            }
            if(idx === -1){
                this.selectedItems.push(item);
            }else{
                this.selectedItems.splice(idx, 1);
            }
        },

        created(){
            this.super$created();
            if(!this.drones) this.drones = [];
            mcLog("created @" + this.tileX() + "," + this.tileY());
        },

        onRemoved(){
            try{
                for(var i = 0; i < this.drones.length; i++){
                    var d = this.drones[i];
                    if(d != null && d.isValid()){
                        Call.unitDespawn(d);
                    }
                }
            }catch(e){ mcLog("onRemoved err: " + e); }
            this.drones.length = 0;
            removeMCData(this);
            this.super$onRemoved();
        },

        acceptItem(source, item){
            return this.items.total() < MiningCenter.itemCapacity;
        },

        updateTile(){
            this.super$updateTile();

            // 1. 存档读取后恢复矿机（单位在建筑之后加载，延迟到 updateTile 恢复）
            if(this.readUnitIds != null){
                for(var i = 0; i < this.readUnitIds.length; i++){
                    var uid = this.readUnitIds[i];
                    if(uid >= 0){
                        var u = Groups.unit.getByID(uid);
                        if(u != null){
                            try{
                            var c = createDroneController(this);
                            // u.controller(c) 内部已调用 c.unit(this) 绑定 unit（Java 方法分派）。
                            // 不能在 JS 中再调 c.unit(u)：AIController 的 unit 既是字段又是方法，
                            // Rhino 会把 c.unit 解析为字段（Unit 对象）而非方法，调用即崩溃。
                            u.controller(c);
                            this.drones.push(u);
                        }catch(e){ mcLog("restore drone err: " + e); }
                        }
                    }
                }
                this.readUnitIds = null;
            }

            // 2. 清理失效矿机
            var alive = [];
            for(var i = 0; i < this.drones.length; i++){
                var d = this.drones[i];
                if(d != null && d.isValid() && !d.dead) alive.push(d);
            }
            this.drones = alive;

            // 3. 逐个生产矿机（参考 UnitCargoLoader：有进度、有动画）
            var type = getDroneType();
            var aliveCount = this.drones.length;
            // 调试日志：每60帧打印一次生产条件状态
            _mcFrameCounter = (_mcFrameCounter || 0) + 1;
            if(_mcFrameCounter % 60 == 0){
                mcLog("生产检查: type=" + (type == null ? "null" : "ok")
                    + " aliveCount=" + aliveCount + "/" + CONFIG.droneCount
                    + " canCreate=" + (type != null ? Units.canCreate(this.team, type) : "N/A")
                    + " buildProgress=" + this.buildProgress
                    + " team=" + this.team);
            }
            if(type != null && aliveCount < CONFIG.droneCount && Units.canCreate(this.team, type)){
                // 生产进度推进
                this._warmup = Mathf.approachDelta(this._warmup, 1, 1/60);
                this.buildProgress += Time.delta / MiningCenter.unitBuildTime;
                this._totalProgress += Time.delta;

                if(this.buildProgress >= 1){
                    this.buildProgress = 0;
                    var u = type.create(this.team);
                    if(u != null){
                        var angle = (aliveCount / CONFIG.droneCount) * 360;
                        var spx = this.x + Mathf.cosDeg(angle) * Vars.tilesize * 2;
                        var spy = this.y + Mathf.sinDeg(angle) * Vars.tilesize * 2;
                        u.set(spx, spy);
                        u.elevation = 1;
                        u.add();

                        try{
                            var c = createDroneController(this);
                            // u.controller(c) 内部已调用 c.unit(this) 绑定 unit（Java 方法分派）。
                            // 不能在 JS 中再调 c.unit(u)：AIController 的 unit 既是字段又是方法，
                            // Rhino 会把 c.unit 解析为字段（Unit 对象）而非方法，调用即崩溃。
                            u.controller(c);
                        }catch(e){ mcLog("controller err: " + e); }

                        Fx.spawn.at(this.x, this.y);
                        this.drones.push(u);
                        mcLog("矿机生产完成 #" + (aliveCount+1) + " id=" + u.id);
                    }
                }
            }else{
                this._warmup = Mathf.approachDelta(this._warmup, 0, 1/60);
            }

            // 同步到 data（供 drawSelect 等使用）
            var data = getMCData(this);
            data.drones = this.drones;
        },

        draw(){
            var self = this;
            // 参考 UnitCargoLoader：绘制建筑本体
            Draw.rect(MiningCenter.region, this.x, this.y);

            // 多选矿物：选中物品时叠加颜色（用第一个选中物品作为主色调标识）
            if(this.selectedItems != null && this.selectedItems.length > 0){
                Draw.color(this.selectedItems[0].color);
                Draw.rect(MiningCenter.region, this.x, this.y);
                Draw.color();
            }

            // 参考 UnitCargoLoader：生产进度动画（Drawf.construct）
            var type = getDroneType();
            var aliveCount = 0;
            for(var i = 0; i < this.drones.length; i++){
                if(this.drones[i] != null && this.drones[i].isValid()) aliveCount++;
            }

            if(type != null && aliveCount < CONFIG.droneCount){
                // 正在生产：绘制 construct 动画
                Draw.draw(Layer.blockOver, mcRunnable(function(){
                    Drawf.construct(self, type.fullIcon, 0, self.buildProgress, self._warmup, self._totalProgress);
                }));
            }

            if(aliveCount > 0){
                // 参考 UnitCargoLoader：矿机就绪时画旋转多边形
                Draw.z(Layer.bullet - 0.01);
                Draw.color(Pal.accent);
                Lines.stroke(1.8);
                Lines.poly(this.x, this.y, 6, 8, Time.time * 1);
                Draw.reset();
                Draw.z(Layer.block);
            }
        },

        drawSelect(){
            try{
                // 范围圈
                Drawf.dashCircle(this.x, this.y,
                    CONFIG.centerRange * Vars.tilesize, Pal.accent);

                // 矿机连线
                for(var i = 0; i < this.drones.length; i++){
                    var d = this.drones[i];
                    if(d != null && d.isValid()){
                        Lines.stroke(1.5, Pal.accent);
                        Lines.line(this.x, this.y, d.x, d.y);
                    }
                }
                Lines.stroke(1);
                Draw.color();

                // 多选矿物：为每个选中物品画一圈（半径递增，颜色区分）
                if(this.selectedItems != null){
                    for(var i = 0; i < this.selectedItems.length; i++){
                        var itm = this.selectedItems[i];
                        if(itm == null) continue;
                        Drawf.dashCircle(this.x, this.y,
                            CONFIG.centerRange * Vars.tilesize + i * 3, itm.color);
                    }
                }
            }catch(e){ mcLog("drawSelect err: " + e); }
        },

        buildConfiguration(table){
            var self = this;
            // 多选矿物 UI：参考 ItemSelection.buildTable，但去掉 ButtonGroup
            // 每个物品按钮独立 toggle，点击调用 configure(item) 切换选中状态
            var columns = 5;
            var cont = new Table().top();
            cont.defaults().size(40);

            var items = Vars.content.items();
            var i = 0;
            for(var idx = 0; idx < items.size; idx++){
                var item = items.get(idx);
                if(!item.unlockedNow() || !item.isOnPlanet(Vars.state.getPlanet()) || item.isHidden()) continue;

                // IIFE 捕获 item（避免 Rhino 闭包变量共享问题）
                (function(itm){
                    var btn = cont.button(Tex.whiteui, Styles.clearNoneTogglei,
                        Mathf.clamp(itm.selectionSize, 0, 40),
                        function(){ self.configure(itm); }
                    ).tooltip(itm.localizedName).get();
                    btn.getStyle().imageUp = new TextureRegionDrawable(itm.uiIcon);
                    // 每帧根据选中状态刷新按钮高亮（多选核心）
                    btn.update(function(){
                        btn.setChecked(self.isItemSelected(itm));
                    });
                })(item);

                if(i++ % columns == (columns - 1)){
                    cont.row();
                }
            }

            var main = new Table().background(Styles.black6);
            var pane = new ScrollPane(cont, Styles.smallPane);
            pane.setScrollingDisabled(true, false);
            pane.setOverscroll(false, false);
            main.add(pane).maxHeight(40 * 5);
            table.top().add(main);
        },

        // 多选模式下 config() 不再返回单个 item（UI 通过 isItemSelected 刷新）
        // 返回 null 避免 saveConfig 自动序列化 JS 数组失败，存档由 write/read 处理
        config(){
            return null;
        },

        textOfState(s){
            switch(s){
                case "idle": return "待机";
                case "seeking": return "寻矿";
                case "moving": return "移动";
                case "mining": return "采集";
                case "returning": return "返回";
                case "unloading": return "卸货";
                default: return s;
            }
        },

        write(write){
            this.super$write(write);
            // 存档版本号（1=多选矿物格式）
            write.b(1);
            // 保存选中物品列表
            var selCount = (this.selectedItems == null) ? 0 : this.selectedItems.length;
            write.b(selCount);
            for(var i = 0; i < selCount; i++){
                write.s(this.selectedItems[i] == null ? -1 : this.selectedItems[i].id);
            }
            // 保存矿机数量和ID
            write.b(this.drones.length);
            for(var i = 0; i < this.drones.length; i++){
                var d = this.drones[i];
                write.i(d == null ? -1 : d.id);
            }
        },

        read(read, revision){
            this.super$read(read, revision);
            // 读取存档版本号
            var ver = read.b();
            this.selectedItems = [];
            if(ver >= 1){
                // 多选格式：读取选中物品列表
                var selCount = read.b();
                for(var i = 0; i < selCount; i++){
                    var itemId = read.s();
                    if(itemId >= 0){
                        var itm = Vars.content.item(itemId);
                        if(itm != null) this.selectedItems.push(itm);
                    }
                }
            }
            // 读取矿机ID（延迟恢复，单位在建筑之后加载）
            var count = read.b();
            this.readUnitIds = [];
            for(var i = 0; i < count; i++){
                this.readUnitIds.push(read.i());
            }
        }
    });
});
