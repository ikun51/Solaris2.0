/**
 * PowderSystem.js — 炮台独立火药系统库 v2.0
 */

const CONFIG_KEY = "_powderConfig";
var systems = {};

// 昼夜射程修正：通过模组类加载器取到 Java 侧 DayNight，保证与主类共用同一份静态状态
var DayNightClass = null;
try{
    DayNightClass = new Packages.rhino.NativeJavaClass(Vars.mods.getScripts().scope, Vars.mods.mainLoader().loadClass("solaris2.daynight.DayNight"));
}catch(e){
    DayNightClass = null;
}

function dayNightRangeMultiplier(x, y){
    if(DayNightClass == null) return 1;
    try{
        return DayNightClass.rangeMultiplier(x, y);
    }catch(e){
        return 1;
    }
}

function baseName(name){
    if(name == null) return null;
    var idx = name.lastIndexOf("-");
    return idx >= 0 ? name.substring(idx + 1) : name;
}

function isPowderItemName(config, name){
    if(name == null) return false;
    if(config.ammoTypes[name] != null) return true;
    var base = baseName(name);
    return base != null && config.ammoTypes[base] != null;
}

function powderDataOf(config, name){
    if(name == null) return null;
    if(config.ammoTypes[name] != null) return config.ammoTypes[name];
    var base = baseName(name);
    return base != null && config.ammoTypes[base] != null ? config.ammoTypes[base] : null;
}

// 液体火药通过输入通道自动区分：管道送来的走 acceptLiquid/handleLiquid，传送带送来的走 acceptItem/handleItem
function isPowderLiquidName(config, name){
    return isPowderItemName(config, name);
}

function powderLiquidDataOf(config, name){
    return powderDataOf(config, name);
}

function getTurretBuildClass(block){
    var n = block.getClass().getName();
    if(block instanceof LiquidTurret || n.indexOf("LiquidTurret") >= 0){
        return LiquidTurret.LiquidTurretBuild;
    }else if(block instanceof ItemTurret || n.indexOf("ItemTurret") >= 0){
        return ItemTurret.ItemTurretBuild;
    }else if(block instanceof PowerTurret || n.indexOf("PowerTurret") >= 0){
        return PowerTurret.PowerTurretBuild;
    }else if(block instanceof Turret || n.indexOf("Turret") >= 0){
        return Turret.TurretBuild;
    }
    return null;
}

function apply(block, config){
    if(block == null) return block;
    if(config == null || config.ammoTypes == null) return block;

    if(config.itemCapacity == null) config.itemCapacity = 60;
    if(config.PerShot == null) config.PerShot = 0;
    if(config.liquidCapacity == null) config.liquidCapacity = 60;

    block[CONFIG_KEY] = config;

    var buildClass = getTurretBuildClass(block);
    if(buildClass == null){
        return block;
    }

    block.buildType = prov(function(){
        return new JavaAdapter(buildClass, {

            powderInit: function(){
                if(this.powderSystem != null) return;
                var cfg = this.block != null ? this.block[CONFIG_KEY] : null;
                if(cfg == null) cfg = config;
                this.powderSystem = {
                    config: cfg,
                    currentAmmoId: null,
                    currentAmmoData: null,
                    currentLiquidId: null,
                    currentLiquidData: null,
                    hasAmmo: false,
                    hasPowder: false,
                    hasLiquid: false,
                    stored: {},
                    storedLiquids: {}
                };
                systems[this.id] = this.powderSystem;
            },

            powderUpdate: function(){
                var sys = this.powderSystem;
                if(sys == null) return;

                sys.hasAmmo = this.super$hasAmmo();

                var pick = null;
                for(var k in sys.stored){
                    if(sys.stored[k] > 0){
                        pick = k;
                        break;
                    }
                }
                sys.currentAmmoId = pick;
                sys.currentAmmoData = pick == null ? null : powderDataOf(sys.config, pick);
                sys.hasPowder = sys.currentAmmoId != null;

                var pickL = null;
                for(var kl in sys.storedLiquids){
                    if(sys.storedLiquids[kl] > 0){
                        pickL = kl;
                        break;
                    }
                }
                sys.currentLiquidId = pickL;
                sys.currentLiquidData = pickL == null ? null : powderLiquidDataOf(sys.config, pickL);
                sys.hasLiquid = sys.currentLiquidId != null;

                if(!this._powderReady(sys) && this.target != null && !this.isControlled()){
                    this.target = null;
                }
            },

            canShoot: function(){
                var sys = this.powderSystem;
                if(sys == null) return false;
                return sys.hasAmmo && this._powderReady(sys);
            },

            hasAmmo: function(){
                var sys = this.powderSystem;
                var base = this.super$hasAmmo();
                if(sys == null) return base;
                sys.hasAmmo = base;
                return base && this._powderReady(sys);
            },

            _currentMode: function(sys){
                if(sys.hasPowder && sys.currentAmmoData != null){
                    return sys.currentAmmoData.mode || "powder";
                }
                if(sys.hasLiquid && sys.currentLiquidData != null){
                    return sys.currentLiquidData.mode || "liquid";
                }
                return null;
            },

            _usePowder: function(sys){
                var mode = this._currentMode(sys);
                return mode == "powder" || mode == "both";
            },

            _useLiquid: function(sys){
                var mode = this._currentMode(sys);
                return mode == "liquid" || mode == "both";
            },

            _powderReady: function(sys){
                var mode = this._currentMode(sys);
                if(mode == null) return false;
                if(mode == "liquid") return sys.hasLiquid;
                if(mode == "both") return sys.hasPowder && sys.hasLiquid;
                return sys.hasPowder;
            },

            range: function(){
                var sys = this.powderSystem;
                var base = this.super$range();
                var mult = dayNightRangeMultiplier(this.x, this.y);
                if(sys == null) return base * mult;
                var total = 0;
                if(this._usePowder(sys) && sys.hasPowder && sys.currentAmmoData != null && sys.currentAmmoData.range != null) total += sys.currentAmmoData.range;
                if(this._useLiquid(sys) && sys.hasLiquid && sys.currentLiquidData != null && sys.currentLiquidData.range != null) total += sys.currentLiquidData.range;
                if(total == 0) return base * mult;
                return (base + total) * mult;
            },

            ammoReloadMultiplier: function(){
                var sys = this.powderSystem;
                var base = this.super$ammoReloadMultiplier();
                if(sys == null) return base;
                var total = 0;
                if(this._usePowder(sys) && sys.hasPowder && sys.currentAmmoData != null && sys.currentAmmoData.reload != null) total += sys.currentAmmoData.reload;
                if(this._useLiquid(sys) && sys.hasLiquid && sys.currentLiquidData != null && sys.currentLiquidData.reload != null) total += sys.currentLiquidData.reload;
                if(total > 0) return base * total;
                return base;
            },

            handleBullet: function(bullet, offsetX, offsetY, angleOffset){
                var sys = this.powderSystem;
                if(sys != null && bullet != null){
                    var dmg = 0;
                    var self = 0;
                    var rng = 0;

                    if(this._usePowder(sys) && sys.hasPowder && sys.currentAmmoData != null){
                        var data = sys.currentAmmoData;
                        if(data.damage != null) dmg += data.damage;
                        if(data.selfDamagePercent != null) self += data.selfDamagePercent;
                        if(data.range != null) rng += data.range;

                        var consume = typeof data.PerShot == "number" ? data.PerShot : sys.config.PerShot;
                        if(consume > 0 && sys.stored[sys.currentAmmoId] != null){
                            sys.stored[sys.currentAmmoId] -= consume;
                            if(sys.stored[sys.currentAmmoId] <= 0){
                                sys.stored[sys.currentAmmoId] = 0;
                                sys.hasPowder = false;
                                sys.currentAmmoId = null;
                                sys.currentAmmoData = null;
                            }
                        }
                    }

                    if(this._useLiquid(sys) && sys.hasLiquid && sys.currentLiquidData != null){
                        var ldata = sys.currentLiquidData;
                        if(ldata.damage != null) dmg += ldata.damage;
                        if(ldata.selfDamagePercent != null) self += ldata.selfDamagePercent;
                        if(ldata.range != null) rng += ldata.range;

                        var lconsume = typeof ldata.PerShot == "number" ? ldata.PerShot : sys.config.PerShot;
                        if(lconsume > 0 && sys.storedLiquids[sys.currentLiquidId] != null){
                            sys.storedLiquids[sys.currentLiquidId] -= lconsume;
                            if(sys.storedLiquids[sys.currentLiquidId] <= 0){
                                sys.storedLiquids[sys.currentLiquidId] = 0;
                                sys.hasLiquid = false;
                                sys.currentLiquidId = null;
                                sys.currentLiquidData = null;
                            }
                        }
                    }

                    if(dmg > 0) bullet.damage = bullet.damage * dmg;
                    if(self > 0) this.damage(this.maxHealth * self / 100);
                    if(rng > 0){
                        // 子弹实际飞行距离 = speed * lifetime，这里按同样的射程加成比例延长寿命，否则打不到加成后的射程
                        var baseRange = this.super$range();
                        if(baseRange > 0) bullet.lifetime = bullet.lifetime * (baseRange + rng) / baseRange;
                    }
                }
                this.super$handleBullet(bullet, offsetX, offsetY, angleOffset);
            },

            updateTile: function(){
                this.powderInit();
                this.powderUpdate();
                this.super$updateTile();
            },

            acceptItem: function(source, item){
                this.powderInit();
                var sys = this.powderSystem;
                if(isPowderItemName(sys.config, item.name)){
                    var other = this._hasOtherPowder(sys, item.name);
                    var amt = sys.stored[item.name] || 0;
                    return !other && amt < sys.config.itemCapacity;
                }
                return this.super$acceptItem(source, item);
            },

            handleItem: function(source, item){
                this.powderInit();
                var sys = this.powderSystem;
                if(isPowderItemName(sys.config, item.name)){
                    if(this._hasOtherPowder(sys, item.name)) return;
                    if(sys.stored[item.name] == null) sys.stored[item.name] = 0;
                    sys.stored[item.name]++;
                    sys.currentAmmoId = item.name;
                    sys.currentAmmoData = powderDataOf(sys.config, item.name);
                    sys.hasPowder = true;
                    return;
                }
                this.super$handleItem(source, item);
            },

            acceptStack: function(item, amount, source){
                this.powderInit();
                var sys = this.powderSystem;
                if(isPowderItemName(sys.config, item.name)){
                    if(this._hasOtherPowder(sys, item.name)) return 0;
                    var cur = sys.stored[item.name] || 0;
                    return Math.max(0, Math.min(amount, sys.config.itemCapacity - cur));
                }
                return this.super$acceptStack(item, amount, source);
            },

            handleStack: function(item, amount, source){
                this.powderInit();
                var sys = this.powderSystem;
                if(isPowderItemName(sys.config, item.name)){
                    if(this._hasOtherPowder(sys, item.name)) return;
                    if(sys.stored[item.name] == null) sys.stored[item.name] = 0;
                    sys.stored[item.name] += amount;
                    sys.currentAmmoId = item.name;
                    sys.currentAmmoData = powderDataOf(sys.config, item.name);
                    sys.hasPowder = true;
                    return;
                }
                this.super$handleStack(item, amount, source);
            },

            acceptLiquid: function(source, liquid){
                this.powderInit();
                var sys = this.powderSystem;
                if(isPowderLiquidName(sys.config, liquid.name)){
                    var other = this._hasOtherLiquid(sys, liquid.name);
                    var amt = sys.storedLiquids[liquid.name] || 0;
                    return !other && amt < sys.config.liquidCapacity;
                }
                return this.super$acceptLiquid(source, liquid);
            },

            handleLiquid: function(source, liquid, amount){
                this.powderInit();
                var sys = this.powderSystem;
                if(isPowderLiquidName(sys.config, liquid.name)){
                    if(this._hasOtherLiquid(sys, liquid.name)) return;
                    if(sys.storedLiquids[liquid.name] == null) sys.storedLiquids[liquid.name] = 0;
                    sys.storedLiquids[liquid.name] += amount;
                    sys.currentLiquidId = liquid.name;
                    sys.currentLiquidData = powderLiquidDataOf(sys.config, liquid.name);
                    sys.hasLiquid = true;
                    return;
                }
                this.super$handleLiquid(source, liquid, amount);
            },

            _hasOtherPowder: function(sys, name){
                for(var k in sys.stored){
                    if(k != name && sys.stored[k] > 0) return true;
                }
                return false;
            },

            _hasOtherLiquid: function(sys, name){
                for(var k in sys.storedLiquids){
                    if(k != name && sys.storedLiquids[k] > 0) return true;
                }
                return false;
            },

            write: function(w){
                this.super$write(w);
                var sys = this.powderSystem;
                if(sys == null){
                    w.s(0);
                    w.s(0);
                    return;
                }
                var keys = [];
                for(var k in sys.stored){
                    if(sys.stored[k] > 0) keys.push(k);
                }
                w.s(keys.length);
                for(var i = 0; i < keys.length; i++){
                    w.str(keys[i]);
                    w.s(sys.stored[keys[i]]);
                }
                var lkeys = [];
                for(var kl in sys.storedLiquids){
                    if(sys.storedLiquids[kl] > 0) lkeys.push(kl);
                }
                w.s(lkeys.length);
                for(var j = 0; j < lkeys.length; j++){
                    w.str(lkeys[j]);
                    w.f(sys.storedLiquids[lkeys[j]]);
                }
            },

            read: function(r, revision){
                this.super$read(r, revision);
                this.powderInit();
                var sys = this.powderSystem;
                sys.stored = {};
                var n = r.s();
                for(var i = 0; i < n; i++){
                    var k = r.str();
                    var amt = r.s();
                    sys.stored[k] = amt;
                }
                sys.storedLiquids = {};
                var ln = r.s();
                for(var j = 0; j < ln; j++){
                    var lk = r.str();
                    var lamt = r.f();
                    sys.storedLiquids[lk] = lamt;
                }
            },

            created: function(){
                this.powderInit();
                this.super$created();
            },

            onRemoved: function(){
                delete systems[this.id];
                this.super$onRemoved();
            }
        }, block);
    });

    // 判断建筑需要哪些 bar：
    //   mode "liquid" 条目 -> 只需要液体火药 bar
    //   mode "both" 条目  -> 火药 bar 和液体 bar 都要
    //   其余条目(默认 powder) -> 只需要火药 bar
    var needsPowder = false;
    var needsLiquid = false;
    for(var k in config.ammoTypes){
        var m = config.ammoTypes[k].mode || "powder";
        if(m == "liquid"){
            needsLiquid = true;
        }else{
            needsPowder = true;
            if(m == "both") needsLiquid = true;
        }
    }

    // 不需要液体加速：移除原版自带的液体 bar（建筑有液体用途但不用火药加速）
    if(!needsLiquid && block.hasLiquids){
        block.removeBar("liquid");
    }

    if(needsPowder){
        block.addBar("powder", function(entity){
            return new Bar(
                function(){
                    var s = systems[entity.id];
                    if(s == null || !s.hasPowder || s.currentAmmoId == null) return "火药";
                    return baseName(s.currentAmmoId);
                },
                function(){
                    return Pal.ammo;
                },
                function(){
                    var s = systems[entity.id];
                    if(s == null || !s.hasPowder || s.currentAmmoId == null) return 0;
                    return Mathf.clamp((s.stored[s.currentAmmoId] || 0) / (s.config.itemCapacity || 60));
                }
            );
        });
    }

    if(needsLiquid){
        block.addBar("powderLiquid", function(entity){
            return new Bar(
                function(){
                    var s = systems[entity.id];
                    if(s == null || !s.hasLiquid || s.currentLiquidId == null) return "液体火药";
                    return baseName(s.currentLiquidId);
                },
                function(){
                    var s = systems[entity.id];
                    if(s == null || s.currentLiquidId == null) return Pal.techBlue;
                    var liq = Vars.content.getByName(Packages.mindustry.ctype.ContentType.liquid, s.currentLiquidId);
                    return liq != null ? liq.color : Pal.techBlue;
                },
                function(){
                    var s = systems[entity.id];
                    if(s == null || !s.hasLiquid || s.currentLiquidId == null) return 0;
                    return Mathf.clamp((s.storedLiquids[s.currentLiquidId] || 0) / (s.config.liquidCapacity || 60));
                }
            );
        });
    }

    return block;
}

function create(baseClass, name, config){
    var block = extend(baseClass, name, {
        setStats: function(){
            this.super$setStats();
            addPowderStats(this.stats, config);
        }
    });
    return apply(block, config);
}

function addPowderStats(stats, config){
    if(config == null || config.ammoTypes == null) return;
    stats.add(new Stat("火药系统", StatCat.function), function(table){
        table.left();
        table.add("");
        table.row();
        for(var k in config.ammoTypes){
            var d = config.ammoTypes[k];
            var modeTag = "[固体]";
            if(d.mode == "liquid") modeTag = "[液体]";
            else if(d.mode == "both") modeTag = "[固体+液体]";
            var txt = "[orange]" + modeTag + k + "[]  ";
            if(d.damage != null){
                txt += "伤害x" + d.damage;
            }
            if(d.range != null) txt += (txt.indexOf("伤害") >= 0 ? "  " : "") + "射程+" + d.range;
            if(d.reload != null) txt += (txt.indexOf("伤害") >= 0 || txt.indexOf("射程") >= 0 ? "  " : "") + "射速x" + d.reload;
            var consume = typeof d.PerShot == "number" ? d.PerShot : config.PerShot;
            if(consume != null && consume > 0) txt += (txt.indexOf("伤害") >= 0 || txt.indexOf("射程") >= 0 || txt.indexOf("射速") >= 0 ? "  " : "") + "消耗 " + consume;
            if(d.selfDamagePercent != null) txt += (txt.indexOf("伤害") >= 0 || txt.indexOf("射程") >= 0 || txt.indexOf("射速") >= 0 || txt.indexOf("消耗") >= 0 ? "  " : "") + "自伤 " + d.selfDamagePercent + "%";
            if(txt.indexOf("伤害") == -1 && txt.indexOf("射程") == -1 && txt.indexOf("射速") == -1 && txt.indexOf("消耗") == -1 && txt.indexOf("自伤") == -1) txt += "无加成";
            table.add("[lightgray]" + txt + "[]").left().padBottom(6);
            table.row();
        }
        return table;
    });
}

module.exports = {
    apply: apply,
    create: create,
    addPowderStats: addPowderStats
};