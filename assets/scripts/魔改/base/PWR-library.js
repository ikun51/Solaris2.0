/**
* @author < 冰汐Angel UR-PWR 1.1 >
*/

const 辐射损害 = Vars.content.getByName(ContentType.status, "索拉里斯2.0-辐射损害")
    || Vars.content.getByName(ContentType.status, "辐射损害");


const 辐射区时长 = 60 * 60 * 5;
const 辐射区特效 = new Effect(辐射区时长, 100000, e => {
    var 范围 = Math.min(200, 128 + 0.05 * e.time);
    Draw.color(Color.valueOf("00ff66"));
    Draw.alpha(0.7);
    Fill.poly(e.x, e.y, 12, 范围);
    Draw.color(Color.valueOf("00cc44"));
    Draw.alpha(0.4);
    Fill.poly(e.x, e.y, 12, 范围 * 0.5);
});
辐射区特效.layer = Layer.shields;

const 辐射区列表 = [];

const 开始辐射区 = (x, y) => {
    辐射区列表.push({x: x, y: y, 时间: 辐射区时长});
    辐射区特效.at(x, y);
};

var 辐射区更新 = function() {
    for (var i = 辐射区列表.length - 1; i >= 0; i--) {
        var 区 = 辐射区列表[i];
        var 范围 = Math.min(200, 128 + 0.05 * (辐射区时长 - 区.时间));
        Damage.damage(Team.derelict, 区.x, 区.y, 范围, 1.5 * Time.delta, true);
        Units.nearby(Team.derelict, 区.x, 区.y, 范围, other => {
            other.apply(辐射损害, 180);
        });
        if (Mathf.chance(0.15)) {
            Fx.overdriveWave.at(
                区.x + Mathf.range(范围 * 0.8),
                区.y + Mathf.range(范围 * 0.8),
                Mathf.random(10, 30),
                Color.valueOf("00ff66"),
                null
            );
        }
        区.时间 -= Time.delta;
        if (区.时间 <= 0) {
            Fx.reactorExplosion.at(区.x, 区.y);
            辐射区列表.splice(i, 1);
        }
    }
};
Events.run(EventType.Trigger.update, 辐射区更新);

// ==================== PWR反应堆实体 ====================
function PWRBuildV2() {
    this.FuelRodOrder = function() {
        if (this._FuelRodIn < 0) { return false };
        return true;
    };
    this.CorenotNull = function() {
        if (this._FuelRodAmount == 0) { return false };
        return true;
    };
    this.FuelRodCache = function() {
        if (this._FuelRodIn < 0) { return false } else if (this.items.get(this.block.getFuelRodSupport()[this._FuelRodIn]) > 0) { return true } else { return false }
    };
    this.ReactorCorenotNull = function() {
        if (this._CoreCoolantVolume == 0) { return false };
        return true;
    };
    this.ECnotNull = function() {
        var secCoolant = this.block.getSteamConfig().SecCoolant;
        if (this.liquids.get(secCoolant) == 0) { return false };
        return true;
    };

    this.updateTile = function() {
        var secCoolant = this.block.getSteamConfig().SecCoolant;
        var currentSec = this.liquids.get(secCoolant);
        var targetRate = (currentSec / this.block.liquidCapacity) * 120 * Math.max(0.1, this.getEfficiency());
        this._WaterInRate = this._WaterInRate * 0.9 + targetRate * 0.1;

        if (this._WaterInEnabled) {
            var avail = this.liquids.get(secCoolant);
            var need = this.block.getModel().MaxCoreCoolant - this._CoreCoolantVolume - this._WaterInQueue;
            var reserve = Math.min(10, this.block.liquidCapacity * 0.15);
            if (avail > reserve && need > 0) {
                var transfer = Math.min(avail - reserve, need, 10);
                this.liquids.remove(secCoolant, transfer);
                this._WaterInQueue += transfer;
                this._ReactorCoreHA -= transfer * 4200 * Math.max(0, (this._ReactorCoreTemperature - 30) / 60);
            }
        };

        if (this.FuelRodOrder() && this._FuelRodAmount < this.block.getModel().MaxFuelRod) { this.FuelRodFilling() };
        if (this.power !== null && this._RPS < 180000 && this.block.getModel().needPower) { this._RPS += 14 * this.power.status };

        var needPower = this.block.getModel().needPower;
        this._Running = this.CorenotNull() && (!needPower || this._RPS >= 15);

        if (this.FuelRodOrder()) {
            var rod = this.block.getFuelRods()[this._FuelRodIn];
            var spentRaw = rod.SpentFuel;
            if (typeof spentRaw === "string") {
                var sf = this.block.getSpentFuel()[this._FuelRodIn];
                if (sf != null && this.items.get(sf) > 0) {
                    this.put(sf);
                }
            }
            if (typeof spentRaw === "object" && spentRaw !== null) {
                var hasFull = false;
                for (var si = 0; si < spentRaw.length; si++) {
                    var itemName = spentRaw[si].item;
                    var sfItem = Vars.content.getByName(ContentType.item, itemName);
                    if (sfItem && this.items.get(sfItem) >= this.getMaximumAccepted(sfItem)) {
                        hasFull = true;
                        break;
                    }
                }
                if (hasFull) {
                    return;
                }
            }
        };
        if (this._Running && this._WaterOutEnabled && this.ECnotNull()) {
            this.heatConduction2to();
            this.dumpSteamOutput();
            if (this._CoreSteamVolume > 0) {
                this._CoreSteamVolume -= 0.015;
                if (this._CoreSteamVolume < 0) this._CoreSteamVolume = 0;
            }
        };

        this._ControlRod = this._ControlRodLevel * 2 + (this._ControlRod - this._ControlRodLevel * 2) * Math.exp(-1 / 1800);
        if (this.block.getModel().needPower) { this._RPS -= 2 * (this._ControlRod - this._ControlRodLevel) / 100 };

        if (this.CorenotNull()) { this.heatGenerate() };
        this.processWaterResidence();
        if (this.ReactorCorenotNull()) { this.computeCorePressure() };
        if (this.CorenotNull() && this.ReactorCorenotNull()) { this.heatConduction0to1() };
        if (this._Running && this._CoreCoolantVolume > 50 && this.ECnotNull()) { this.heatConduction1to2() };

        if (this.CorenotNull()) { this.computeCoreTemperature() } else { this._CoreTemperature = 0 };
        if (this.ReactorCorenotNull()) { this.computeReactorCoreTemperature() } else { this._ReactorCoreTemperature = 0 }
        if (this.ECnotNull()) { this.computeECTemperature() } else { this._ECTemperature = 0 }

        var maxP = this.block.getModel().MaxPressure;
        var dangerRatio = Math.max(maxP > 0 ? this._ReactorCoreP / maxP : 0, this._CoreTemperature / 3600);
        if (this._Running && this._CoreTemperature > 1200) {
            var steamChance = Math.min(0.6, (this._CoreTemperature - 1500) / (3600 - 1500) * 0.6);
            if (Mathf.chance(steamChance)) {
                Fx.steam.at(this.x + Mathf.range(10), this.y + Mathf.range(10));
            }
            if (dangerRatio > 0.95 && Math.floor(Time.time * 30) % 2 == 0) {
                Fx.fireSmoke.at(this.x, this.y);
            }
        };

        if (this._ReactorCoreP > maxP || this._CoreTemperature > 3600) {

            Damage.damage(null, this.x, this.y, 120, 5000, true, true, true);
            Fx.reactorExplosion.at(this.x, this.y);
            Fx.blastExplosion.at(this.x, this.y);

            for (var r = 1; r <= 3; r++) {
                Damage.damage(null, this.x + r * 8, this.y, 40, 2000, true, true, true);
                Damage.damage(null, this.x - r * 8, this.y, 40, 2000, true, true, true);
                Damage.damage(null, this.x, this.y + r * 8, 40, 2000, true, true, true);
                Damage.damage(null, this.x, this.y - r * 8, 40, 2000, true, true, true);
            }
            开始辐射区(this.x, this.y);
            for (var i = 0; i < 10; i++) {
                Fx.overdriveWave.at(
                    this.x + Mathf.range(200),
                    this.y + Mathf.range(200),
                    Mathf.random(20, 60),
                    Color.valueOf("00ff66"),
                    null
                );
            }
            Sounds.wreckFallBig.at(this.x, this.y);
            Sounds.explosionReactor2.at(this.x, this.y);

            this.kill();
        };

    };
    this.draw = function() {
        this.super$draw();
        if (this._CoreTemperature > 3400) {
            var t = Math.min(1, (this._CoreTemperature - 3400) / 200);
            var flash = Mathf.absin(Time.time, 3, 1);
            var alpha = (0.35 + flash * 0.65) * t;
            Draw.color(Color.valueOf("ff1d1d"), alpha);
            Draw.rect(this.block.region, this.x, this.y);
            Draw.color();
        }
    };

    this._FillingTimer = 0;
    this.FuelRodFilling = function() {
        var needPower = this.block.getModel().needPower;
        if (needPower && this._RPS < 15) return;
        this._FillingTimer = Math.max(0, this._FillingTimer - 1 / 60);
        if (this.block.getModel().needPower) { this._RPS -= 1 };
        if (this._FillingTimer <= 0) {
            if (this.items.get(this.block.getFuelRodSupport()[this._FuelRodIn]) > 0) {
                this.items.remove(this.block.getFuelRodSupport()[this._FuelRodIn], 1);
                this._FuelRodAmount += 1;
                this._FillingTimer = 3;
            }
        }
    };

    this.ejectFuelRods = function() {
        if (this._FuelRodAmount <= 0 || this._FuelRodIn < 0) return;
        var fuelRodItem = this.block.getFuelRodSupport()[this._FuelRodIn];
        var maxAccept = this.getMaximumAccepted(fuelRodItem);
        var canReturn = Math.min(this._FuelRodAmount, maxAccept - this.items.get(fuelRodItem));
        if (canReturn > 0) {
            this.items.add(fuelRodItem, canReturn);
            this._FuelRodAmount -= canReturn;
            this._FuelRodInUsed = 0;
        }

        if (this._FuelRodAmount <= 0) {
            this._FuelRodIn = -1;
        }
    };

    this.heatGenerate = function() {
        var fuelrods = this.block.getFuelRods()[this._FuelRodIn];

        var rodEffect = this._ControlRod / 100;
        var fuelRodCount = Math.max(1, this._FuelRodAmount);
        var receivedFluxPerRod = this._Flux * rodEffect / fuelRodCount;
        this._CoreHA += receivedFluxPerRod * fuelRodCount * fuelrods.HurtperFlux * 90;
        if (fuelrods.SpentFuel) {
            var spentRaw = fuelrods.SpentFuel;
            var blocked = false;
            if (typeof spentRaw === "string") {
                var sf = this.block.getSpentFuel()[this._FuelRodIn];
                if (sf != null && this.items.get(sf) >= this.getMaximumAccepted(sf)) {
                    blocked = true;
                }
            } else if (typeof spentRaw === "object" && spentRaw !== null) {
                for (var si = 0; si < spentRaw.length; si++) {
                    var item = Vars.content.getByName(ContentType.item, spentRaw[si].item);
                    if (item != null && this.items.get(item) >= this.getMaximumAccepted(item)) {
                        blocked = true;
                        break;
                    }
                }
            }
            if (blocked) {
                this._CoreHA += this._CoreHA * 0.02;
            }
        }
        if (fuelrods.Functions[0] == 0) {
            var releasedFlux = fuelrods.Functions[1] * rodEffect;
        } else if (fuelrods.Functions[0] == 1) {
            var releasedFlux = Math.log10(receivedFluxPerRod / fuelrods.Functions[1] + fuelrods.Functions[2]) * fuelrods.Functions[3];
        } else if (fuelrods.Functions[0] == 2) {
            var releasedFlux = Math.sqrt(receivedFluxPerRod + fuelrods.Functions[1]) * fuelrods.Functions[2];
        } else if (fuelrods.Functions[0] == 3) {
            var releasedFlux = receivedFluxPerRod * fuelrods.Functions[1] + fuelrods.Functions[2];
        }
        var newFlux = (releasedFlux * fuelRodCount) * (1 + this._NeutronSource / 50);
        this._Flux = this._Flux * 0.95 + newFlux * 0.05;
        this._FuelRodInUsed += receivedFluxPerRod * fuelRodCount;
        if (this._FuelRodInUsed > 3000000 * fuelrods.Durable) {
            var spentRaw = fuelrods.SpentFuel;
            var canConsume = true;
            if (typeof spentRaw === "string") {
                var sf = this.block.getSpentFuel()[this._FuelRodIn];
                if (sf != null && this.items.get(sf) >= this.getMaximumAccepted(sf)) {
                    canConsume = false;
                }
            } else if (typeof spentRaw === "object" && spentRaw !== null) {
                for (var si = 0; si < spentRaw.length; si++) {
                    var item = Vars.content.getByName(ContentType.item, spentRaw[si].item);
                    if (item && this.items.get(item) >= this.getMaximumAccepted(item)) {
                        canConsume = false;
                        break;
                    }
                }
            }
            if (!canConsume) {
                this._FuelRodInUsed = 0;
                return;
            }
            this._FuelRodAmount -= 1;
            if (typeof spentRaw === "string") {
                this.items.add(this.block.getSpentFuel()[this._FuelRodIn], 1);
            } else if (typeof spentRaw === "object" && spentRaw !== null) {
                for (var si = 0; si < spentRaw.length; si++) {
                    if (Mathf.chance(spentRaw[si].chance)) {
                        var item = Vars.content.getByName(ContentType.item, spentRaw[si].item);
                        if (item) {
                            this.items.add(item, spentRaw[si].amount || 1);
                        }
                    }
                }
            }
            this._FuelRodInUsed = 0;
        };
    };

    this.processWaterResidence = function() {
        if (this._WaterInQueue <= 0) return;
        var dwellRate = Math.max(0.005, (this._ECTemperature - 30) / 600 * 2);
        var maxCapacity = this.block.getModel().MaxCoreCoolant - this._CoreCoolantVolume;
        var processed = Math.min(this._WaterInQueue, this._WaterInQueue * dwellRate, Math.max(0, maxCapacity));
        this._WaterInQueue -= processed;
        this._CoreCoolantVolume += processed;
    };

    this.computeCorePressure = function() {
        var boilTemp = this.block.getModel().BoilingTemp || 343;
        if (this._ReactorCoreTemperature > boilTemp && this._CoreCoolantVolume > 0) {
            var evap = 2.0;
            this._CoreCoolantVolume -= evap;
            this._ReactorCoreHA = Math.max(0, this._ReactorCoreHA - 4200 * (boilTemp - 30) * 2);
            this._CoreSteamVolume += evap;
        }

        var hasCoolant = this._CoreCoolantVolume > 0 || this._WaterInQueue > 0;
        var baseP = hasCoolant ? 5 : 0;

        var steamRatio = Math.min(1, this._CoreSteamVolume / Math.max(1, this._CoreCoolantVolume));
        var tempFactor = Math.min(1.5, Math.max(0.2,
            (this._ReactorCoreTemperature - 30) / (boilTemp - 30)));
        var steamP = steamRatio * 20 * tempFactor;

        var rawP = Math.min(25, baseP + steamP);

        this._ReactorCoreP += (rawP - this._ReactorCoreP) * 0.05;
    };

    this.heatConduction0to1 = function() {
        var heatConductionNumber = this.block.getModel().Parameter[1] * this.block.getModel().Parameter[2] * this._FuelRodAmount * (this._CoreCoolantVolume / this.block.getModel().MaxCoreCoolant) * (this._CoreTemperature - this._ReactorCoreTemperature) / 15;
        this._ReactorCoreHA += heatConductionNumber;
        var coolantVol = Math.max(1, this._CoreCoolantVolume);
        this._CoreHA -= heatConductionNumber * 1500 / (4200 * coolantVol);
        if (this._CoreHA < 0) this._CoreHA = 0;
    };

    this.heatConduction1to2 = function() {
        var secCoolant = this.block.getSteamConfig().SecCoolant;
        var percentage = this.liquids.get(secCoolant) / this.block.liquidCapacity;
        var heatConductionNumber = this.block.getModel().Parameter[3] * this.block.getModel().Parameter[4] * percentage * (this._ReactorCoreTemperature - this._ECTemperature) / 40;
        var actualConduction = Math.min(heatConductionNumber, this._ReactorCoreHA);
        this._ReactorCoreHA -= actualConduction;
        this._ECHA += actualConduction;
    };

    this.heatConduction2to = function() {
        var cfg = this.block.getSteamConfig();
        if (this._ECTemperature < cfg.BoilingPoint) { this._OutputSpeed = 0; return };
        var secCoolant = cfg.SecCoolant;
        var steamLiquid = cfg.SteamLiquid;
        var calo = this.getFuelCalorific();
        var T = this._ECTemperature;
        var steamMult;
        if (T < 250) {
            steamMult = 0.6 + (T - 100) / 150 * 0.4;
        } else if (T < 400) {
            steamMult = 1.0 + (T - 250) / 150 * 0.7;
        } else {
            steamMult = 1.7;
        }
        var rawOutput = this._WaterInRate * 100 * steamMult * calo;
        var maxOutput = Math.min(rawOutput, this.block.liquidCapacity - this.liquids.get(steamLiquid));
        this._OutputSpeed = Math.floor(maxOutput);
        if (maxOutput > 0) {
            var steamOut = Math.min(maxOutput * 0.05 * calo, 20);
            this.liquids.remove(secCoolant, steamOut * 0.5);
            this.handleLiquid(this, steamLiquid, steamOut);
            this._ECHA -= Math.min(maxOutput * cfg.OutputCoefficient * 120 * steamMult * calo, this._ECHA);
        };
    };

    this.dumpSteamOutput = function() {
        var cfg = this.block.getSteamConfig();
        var liquids = [cfg.SteamLiquid, cfg.SecCoolant];
        for (var i = 0; i < liquids.length; i++) {
            var liq = liquids[i];
            if (this.liquids.get(liq) <= 0) continue;
            var toDump = Math.min(this.liquids.get(liq), 20);
            var fract = this.liquids.get(liq) / this.block.liquidCapacity;
            for (var p = 0; p < this.proximity.size; p++) {
                if (toDump <= 0) break;
                var other = this.proximity.get(p);
                if (other == null || !other.block.hasLiquids || other.liquids == null || !other.acceptLiquid(this, liq)) continue;
                var ofract = other.liquids.get(liq) / other.block.liquidCapacity;
                if (ofract >= fract) continue;
                var flow = Math.min(other.block.liquidCapacity - other.liquids.get(liq), (fract - ofract) * this.block.liquidCapacity);
                flow = Math.min(flow, toDump);
                if (flow > 0) {
                    other.handleLiquid(this, liq, flow);
                    this.liquids.remove(liq, flow);
                    toDump -= flow;
                }
            }
        };
    };

    this._OutputSpeed = 0;
    this.getOutputSpeed = function() {
        return this._OutputSpeed;
    };

    this.getFuelCalorific = function() {
        if (!this.FuelRodOrder()) return 1;
        var fuelrods = this.block.getFuelRods()[this._FuelRodIn];
        return fuelrods.Calorific || 1;
    };
    this.getEfficiency = function() {
        var T = this._ECTemperature;
        if (T <= 120) return 0;
        if (T < 280) return (T - 120) / 160 * 0.3;
        if (T < 430) return 0.3 + (T - 280) / 150 * 0.7;
        return 1;
    };

    this.computeCoreTemperature = function() {
        this._CoreTemperature = this._CoreHA / 1500 + 30;
        if (this._CoreTemperature < 30) { this._CoreTemperature = 30; this._CoreHA = 0 };
    };

    this.computeReactorCoreTemperature = function() {
        var rawTemp = Math.max(30, this._ReactorCoreHA / 4200 / Math.max(1, this._CoreCoolantVolume) + 30);
        this._ReactorCoreTemperature = this._ReactorCoreTemperature * 0.95 + rawTemp * 0.05;
    };

    this.computeECTemperature = function() {
        var cfg = this.block.getSteamConfig();
        var secCoolant = cfg.SecCoolant;
        var liq = Math.max(1, this.liquids.get(secCoolant));
        this._ECLiqSmooth = this._ECLiqSmooth * 0.9 + liq * 0.1;
        var rawTemp = this._ECHA / (cfg.ThermalCapacity || 4.2) / 50 / Math.max(1, this._ECLiqSmooth) + 30;
        this._ECTemperature = this._ECTemperature * 0.98 + rawTemp * 0.02;
    };

    this.buildConfiguration = function(table) {
        var that = this;

        table.table(cons(t => {
            t.background(Styles.black6);
            t.defaults().left().pad(1);

            var fuelCell = t.table().size(20).get();
            t.add("▶").color(Color.lightGray);
            var coreCell = t.add("").color(Color.orange).width(90).get();
            t.add("▶").color(Color.lightGray);
            var wasteCell = t.table().size(20).get();
            t.add("▶").color(Color.lightGray);
            var reactorCell = t.add("").color(Color.orange).width(80).get();
            var pressCell = t.add("").color(Color.lightGray).width(65).padLeft(4).get();
            t.add("▶").color(Color.lightGray);
            var ecCell = t.add("").color(Color.orange).width(80).get();

            t.update(() => {
                fuelCell.clearChildren();
                if (that._FuelRodIn >= 0) {
                    fuelCell.add(Image(that.block.getFuelRodSupport()[that._FuelRodIn].uiIcon)).size(20);
                } else {
                    fuelCell.add(Image(Icon.cancel)).size(20);
                }
                coreCell.setText("核心" + Math.floor(that._CoreTemperature) + "°C");
                wasteCell.clearChildren();
                if (that._FuelRodIn >= 0) {
                    wasteCell.add(Image(that.block.getSpentFuel()[that._FuelRodIn].fullIcon)).size(20);
                } else {
                    wasteCell.add(Image(Icon.cancel)).size(20);
                }
                reactorCell.setText("堆芯" + Math.floor(that._ReactorCoreTemperature) + "°C");
                pressCell.setText(Math.round(that._ReactorCoreP * 10) / 10 + "MPa");
                ecCell.setText("换热" + Math.floor(that._ECTemperature) + "°C");
            });
        })).pad(1).left().row();

        table.row();

        table.add("[accent]燃料棒:").left().row();
        var buttonGroup1 = table.table(Styles.none, cons(buttonTable => {
            var group1 = new ButtonGroup();
            group1.setMinCheckCount(0);
            group1.setMaxCheckCount(1);
            for (var i = 0; i < that.block.getFuelRodSupport().length; i++) {
                (function(i, that2) {
                    var button = buttonTable.button(Tex.pane, 40, () => {
                        if (!that2.CorenotNull()) {
                            that2._FuelRodIn = i;
                        }
                    }).group(group1).get();
                    button.getStyle().up = Styles.black3;
                    button.getStyle().down = Styles.flatOver;
                    button.getStyle().checked = Styles.accentDrawable;
                    button.getStyle().imageUp = new TextureRegionDrawable(that2.block.getFuelRodSupport()[i].uiIcon);
                    button.update(() => {
                        button.setChecked(i == that2._FuelRodIn);
                        button.setDisabled(that2.CorenotNull() || that2.FuelRodCache());
                    });
                })(i, that);
            };
        })).left().get();
        table.row();

        var ejectRow = table.table().left().get();
        ejectRow.add("[accent]退燃料:").left().padRight(4);
        var ejectBtn = ejectRow.button(Tex.pane, Styles.clearTogglei, 40, () => {
            that.ejectFuelRods();
        }).get();
        ejectBtn.update(() => {
            ejectBtn.clearChildren();
            var count = that._FuelRodAmount;
            ejectBtn.setDisabled(count <= 0);
            ejectBtn.add(count > 0 ? "[red]退(" + count + ")" : "[gray]退").center();
        });
        table.row();

        table.add("[accent]控制棒:").left().row();
        var buttonGroup3 = table.table(Styles.none, buttonTable => {
            var group3 = new ButtonGroup();
            group3.setMinCheckCount(0);
            group3.setMaxCheckCount(1);
            for (var i = 0; i < 21; i++) {
                (function(i, that2) {
                    var button = buttonTable.button(Tex.pane, 20, () => {
                        that2._ControlRodLevel = i;
                    }).group(group3).get();
                    button.getStyle().up = Styles.black3;
                    button.getStyle().down = Styles.flatOver;
                    button.getStyle().checked = Styles.accentDrawable;
                    button.getStyle().imageUp = Styles.none;
                    button.update(() => {
                        button.setChecked(i == that2._ControlRodLevel);
                    });
                })(i, that);
            };
        }).left().get();
        table.row();

        var neutronRow = table.table().left().get();
        neutronRow.defaults().padRight(2);
        neutronRow.add("[accent]中子源:").left();
        var NeutronSourceSud = neutronRow.button(Tex.pane, Styles.clearTogglei, 40, () => {
            that._NeutronSource -= 5;
            if (that._NeutronSource < 0) { that._NeutronSource = 0 };
        }).get();
        NeutronSourceSud.update(() => {
            NeutronSourceSud.clearChildren();
            NeutronSourceSud.add(" < ").color(Color.yellow);
        });
        var NeutronSourceButton = neutronRow.button(Tex.pane, Styles.clearTogglei, 40, () => {
            that._NeutronSource += 1;
            if (that._NeutronSource > 50) { that._NeutronSource = 10 };
        }).get();
        NeutronSourceButton.update(() => {
            NeutronSourceButton.clearChildren();
            NeutronSourceButton.add("  " + that._NeutronSource + "  ").color(Color.yellow);
        });
        var NeutronSourceAdd = neutronRow.button(Tex.pane, Styles.clearTogglei, 40, () => {
            that._NeutronSource += 5;
            if (that._NeutronSource > 50) { that._NeutronSource = 50 };
        }).get();
        NeutronSourceAdd.update(() => {
            NeutronSourceAdd.clearChildren();
            NeutronSourceAdd.add(" > ").color(Color.yellow);
        });
        table.row();

        var waterRow = table.table().left().get();
        waterRow.defaults().padRight(4);
        waterRow.add("[accent]进水:").left();
        var waterInBtn = waterRow.button(Tex.pane, Styles.clearTogglei, 40, () => {
            that._WaterInEnabled = !that._WaterInEnabled;
        }).get();
        waterInBtn.update(() => {
            waterInBtn.clearChildren();
            waterInBtn.add(that._WaterInEnabled ? "[#5ba4cf]▼进水" : "[red]⊘关").center();
        });
        waterRow.add().width(10);
        waterRow.add("[accent]出水:").left();
        var waterOutBtn = waterRow.button(Tex.pane, Styles.clearTogglei, 40, () => {
            that._WaterOutEnabled = !that._WaterOutEnabled;
        }).get();
        waterOutBtn.update(() => {
            waterOutBtn.clearChildren();
            waterOutBtn.add(that._WaterOutEnabled ? "[#cf8b5b]▲出水" : "[red]⊘关").center();
        });
        table.row();
    };

    this.acceptItem = function(source, item) {
        if (this.FuelRodOrder()) {
            var needPower = this.block.getModel().needPower;
            if (needPower && this._RPS < 15) return false;
            var fuelRod = this.block.getFuelRodSupport()[this._FuelRodIn];
            if (item !== fuelRod) return false;
            return this.items.get(item) < this.getMaximumAccepted(item);
        };
        return false;
    };

    this.acceptLiquid = function(source, liquid) {
        var secCoolant = this.block.getSteamConfig().SecCoolant;
        if (liquid == secCoolant) {
            var needPower = this.block.getModel().needPower;
            if (needPower && this._RPS < 15) return false;
            if (!this._WaterInEnabled) return false;
            return this.liquids.get(liquid) < this.block.liquidCapacity;
        }
        return false;
    };

    this._Flux = 0;
    this.getFlux = function() {
        return this._Flux;
    };
    this._NeutronSource = 10;
    this.getNeutronSource = function() {
        return this._NeutronSource;
    };
    this._ControlRodLevel = 0;
    this.getControlRodLevel = function() {
        return this._ControlRodLevel;
    };
    this._ControlRod = 0;
    this.getControlRod = function() {
        return this._ControlRod;
    };
    this._FuelRodAmount = 0;
    this.getFuelRodAmoumt = function() {
        return this._FuelRodAmount;
    };
    this._FuelRodInUsed = 0;
    this.FuelRodinUsed = function() {
        return this._FuelRodInUsed;
    };
    this._FuelRodIn = -1;
    this.getFuelRodin = function() {
        return this._FuelRodIn;
    };
    this._CoreHA = 0;
    this.getCoreHA = function() {
        return this._CoreHA;
    };
    this._CoreTemperature = 30;
    this.getCoreTemperature = function() {
        return this._CoreTemperature;
    };
    this._ReactorCoreHA = 0;
    this.getReactorCoreHA = function() {
        return this._ReactorCoreHA;
    };
    this._ReactorCoreTemperature = 30;
    this.getReactorCoreTemperature = function() {
        return this._ReactorCoreTemperature;
    };
    this._ReactorCoreP = 0;
    this.getReactorCoreP = function() {
        return this._ReactorCoreP;
    };
    this._CoreCoolantVolume = 0;
    this.getCoreWaterVolume = function() {
        return this._CoreCoolantVolume;
    };
    this._CoreSteamVolume = 0;
    this.getCoreSteamVolume = function() {
        return this._CoreSteamVolume;
    };
    this._WaterInQueue = 0;
    this.getWaterInQueue = function() {
        return this._WaterInQueue;
    };
    this._WaterInRate = 0;
    this.getWaterInRate = function() {
        return this._WaterInRate;
    };
    this._PrevSecCoolant = 0;
    this.getPrevSecCoolant = function() {
        return this._PrevSecCoolant;
    };
    this._ECHA = 0;
    this.getECHA = function() {
        return this._ECHA;
    };
    this._ECTemperature = 30;
    this.getECTemperature = function() {
        return this._ECTemperature;
    };
    this._ECLiqSmooth = 0;
    this._RPS = 0;
    this.getRPS = function() {
        return this._RPS;
    };
    this._Running = false;
    this.isRunning = function() {
        return this._Running;
    };
    this._WaterInEnabled = true;
    this.isWaterInEnabled = function() {
        return this._WaterInEnabled;
    };
    this._WaterOutEnabled = true;
    this.isWaterOutEnabled = function() {
        return this._WaterOutEnabled;
    };

    this.write = function(write) {
        this.super$write(write);
        write.f(this._Flux);
        write.i(this._NeutronSource);
        write.f(this._ControlRodLevel);
        write.f(this._ControlRod);
        write.i(this._FuelRodAmount);
        write.d(this._FuelRodInUsed);
        write.i(this._FuelRodIn);
        write.f(this._CoreHA);
        write.f(this._CoreTemperature);
        write.f(this._ReactorCoreHA);
        write.f(this._ReactorCoreTemperature);
        write.f(this._ReactorCoreP);
        write.f(this._CoreCoolantVolume);
        write.f(this._CoreSteamVolume);
        write.f(this._ECHA);
        write.f(this._ECTemperature);
        write.i(this._RPS);
        write.i(this._OutputSpeed);
        write.f(this._WaterInRate);
        write.f(this._WaterInQueue);
        write.f(this._PrevSecCoolant);
        write.bool(this._WaterInEnabled);
        write.bool(this._WaterOutEnabled);
    };

    this.read = function(read, revision) {
        this.super$read(read, revision);
        this._Flux = read.f();
        this._NeutronSource = read.i();
        this._ControlRodLevel = read.f();
        this._ControlRod = read.f();
        this._FuelRodAmount = read.i();
        this._FuelRodInUsed = read.d();
        this._FuelRodIn = read.i();
        this._CoreHA = read.f();
        this._CoreTemperature = read.f();
        this._ReactorCoreHA = read.f();
        this._ReactorCoreTemperature = read.f();
        this._ReactorCoreP = read.f();
        this._CoreCoolantVolume = read.f();
        this._CoreSteamVolume = read.f();
        this._ECHA = read.f();
        this._ECTemperature = read.f();
        this._RPS = read.i();
        this._OutputSpeed = read.i();
        this._WaterInRate = read.f();
        this._WaterInQueue = read.f();
        this._PrevSecCoolant = read.f();
        this._WaterInEnabled = read.bool();
        this._WaterOutEnabled = read.bool();
    };
};

// ==================== PWR反应堆方块 ====================
function PWRBlockV2() {
    var Model = {};
    this.getModel = function() { return Model };
    var FuelRods = [];
    this.getFuelRods = function() { return FuelRods };
    var FuelRodSupportSet = [];
    this.getFuelRodSupport = function() { return FuelRodSupportSet };
    var SpentFuelSet = [];
    this.getSpentFuel = function() { return SpentFuelSet };
    var SteamConfig = {};
    this.getSteamConfig = function() { return SteamConfig };

    this._invFrag = extend(BlockInventoryFragment, { _built: false, isBuilt() { return this._built }, visible: false, isShown() { return this.visible }, showFor(t) { this.visible = true;
            this.super$showFor(t) }, hide() { this.visible = false;
            this.super$hide() }, build(parent) { this._built = true;
            this.super$build(parent) } });
    this.getInvFrag = function() { return this._invFrag };

    this.canBreak = function(tile) {
        var entity = tile.build;
        if (entity && typeof entity.getFuelRodAmoumt === "function" && entity.getFuelRodAmoumt() > 0) {
            return false;
        }
        return this.super$canBreak(tile);
    };

    this.init = function() {

        var pwr = this.pwrModel;
        Model = {
            MinPressure: pwr.MinPressure || 0,
            MaxPressure: pwr.MaxPressure || 0,
            MaxFuelRod: pwr.MaxFuelRod || 0,
            MaxCoreCoolant: pwr.MaxCoreCoolant || 0,
            BoilingTemp: pwr.BoilingTemp || 343,
            Parameter: pwr.Parameter || [],
            needPower: pwr.needPower || false,
        };
        SteamConfig = {
            SecCoolant: Vars.content.getByName(ContentType.liquid, pwr.SecCoolant || "water"),
            SteamLiquid: Vars.content.getByName(ContentType.liquid, pwr.SteamOutput || "索拉里斯2.0-蒸汽"),
            BoilingPoint: pwr.BoilingPoint || 150,
            OutputCoefficient: pwr.OutputCoefficient || 0.5,
            ThermalCapacity: pwr.ThermalCapacity || 4.2,
        };
        if (Model.needPower) {
            this.consumesPower = true;
            this.consumeBuilder.add(extend(ConsumePower, {
                requestedPower(entity) {
                    if (entity.getRPS() < 180000) return 14;
                    return 0;
                }
            }));
        }
        for (var i = 0; i < this.pwrFuelRods.length; i++) {
            var pwr = this.pwrFuelRods[i];
            FuelRods[i] = {
                FuelRodSupport: pwr.FuelRodSupport || "",
                SpentFuel: pwr.SpentFuel || "",
                RadiationFunction: pwr.RadiationFunction || "",
                Functions: pwr.Functions || [],
                HurtperFlux: pwr.HurtperFlux || 0,
                Durable: pwr.Durable || 0,
                Calorific: pwr.Calorific || 1,
            };
            FuelRodSupportSet.push(Vars.content.getByName(ContentType.item, FuelRods[i].FuelRodSupport));
            var spentRaw = FuelRods[i].SpentFuel;
            if (typeof spentRaw === "string") {
                SpentFuelSet.push(Vars.content.getByName(ContentType.item, spentRaw));
            } else if (typeof spentRaw === "object" && spentRaw !== null && spentRaw.length > 0) {
                SpentFuelSet.push(Vars.content.getByName(ContentType.item, spentRaw[0].item));
            }
        };
        this.super$init();
        this.outputsLiquid = true;
    };

    this.setStats = function() {
        this.super$setStats();
        if (Model.needPower) { this.stats.add(Stat.powerUse, 600, StatUnit.powerSecond) };
        this.stats.remove(Stat.productionTime);
        this.stats.add(Stat.input, new JavaAdapter(StatValue, {
            display(table) {
                table.table(Styles.none, function(groupTable) {
                    groupTable.table(Styles.grayPanel, function(view) {
                        view.add().size(8).row();
                        view.add("[accent]堆芯参数 ").left().row();
                        view.add().size(12).row();
                        view.add("[lightgrey]  运行压力: " + Model.MinPressure + " 兆帕").left().row();
                        view.add("[lightgrey]  极限压力: " + Model.MaxPressure + " 兆帕").left().row();
                        view.add("[lightgrey]  燃料棒容量: " + Model.MaxFuelRod + " 组").left().row();
                        view.add("[lightgrey]  堆芯水量: " + Model.MaxCoreCoolant + " 吨").left().row();
                        if (Model.needPower) { view.add("[white]  运行需要电力维持").left().row() };
                        view.add("[red]  ⚠ 超压/超温将导致爆炸；供电不足会断水致堆芯升温").left().row();
                        view.add().size(8).row();
                        view.add("[accent]蒸汽输出 ").left().row();
                        view.add("[lightgrey]  二回路冷却剂: ").left().row();
                        view.add(new StatValues.displayLiquid(SteamConfig.SecCoolant, 1, false)).left().row();
                        view.add("[lightgrey]  输出产物: ").left().row();
                        view.add(new StatValues.displayLiquid(SteamConfig.SteamLiquid, 1, false)).left().row();
                        view.add("[lightgrey]  沸点阈值: " + SteamConfig.BoilingPoint + " °C").left().row();
                        view.add("[lightgrey]  输出系数: " + SteamConfig.OutputCoefficient + " /°C").left().row();
                    }).pad(7).left().row();
                    for (var f = 0; f < FuelRods.length; f++) {
                        groupTable.table(Styles.none, function(view) {
                            if (f == 0) {
                                view.add().size(8).row();
                                view.add("[accent]受支持的燃料棒: ").left().row();
                                view.add().size(12).row();
                            };
                            view.table(Styles.grayPanel, function(part) {
                                part.add(new StatValues.displayItem(FuelRodSupportSet[f], 1, true)).left().row();
                                var spentStats = FuelRods[f].SpentFuel;
                                if (typeof spentStats === "string") {
                                    part.add("[lightgrey]  对应乏燃料").left().row();
                                    part.add(new StatValues.displayItem(SpentFuelSet[f], 1, true)).left().row();
                                } else if (typeof spentStats === "object" && spentStats !== null) {
                                    part.add("[lightgrey]  对应乏燃料:").left().row();
                                    for (var ssi = 0; ssi < spentStats.length; ssi++) {
                                        var entry = spentStats[ssi];
                                        var entryItem = Vars.content.getByName(ContentType.item, entry.item);
                                        part.add("    " + Math.floor(entry.chance * 100) + "% x" + (entry.amount || 1)).left().row();
                                        if (entryItem) {
                                            part.add(new StatValues.displayItem(entryItem, 1, true)).left().row();
                                        }
                                    }
                                }
                                part.add("[lightgrey]  函数类型").left().row();
                                part.add("    " + FuelRods[f].RadiationFunction).left().row();
                                if (FuelRods[f].Functions[0] == 0) {
                                    part.add("[grey]    " + FuelRods[f].Functions[1]).left().row();
                                } else if (FuelRods[f].Functions[0] == 1) {
                                    part.add("[grey]    log10(x / " + FuelRods[f].Functions[1] + " + " + FuelRods[f].Functions[2] + ") * " + FuelRods[f].Functions[3]).left().row();
                                } else if (FuelRods[f].Functions[0] == 2) {
                                    part.add("[grey]    sqrt(x + " + FuelRods[f].Functions[1] + ") * " + FuelRods[f].Functions[2]).left().row();
                                } else if (FuelRods[f].Functions[0] == 3) {
                                    part.add("[grey]    x * " + FuelRods[f].Functions[1] + " + " + FuelRods[f].Functions[2]).left().row();
                                };
                                part.add("[lightgrey]  热值产量/接收通量: " + FuelRods[f].HurtperFlux + " KJ").left().row();
                                part.add("[lightgrey]  耐用性: " + FuelRods[f].Durable + " 倍").left().row();
                            })
                        }).pad(7).left().row();
                    };
                })
            }
        }));
    };

    this.setBars = function() {
        this.super$setBars();
        this.removeBar("liquid");
        this.removeBar("items");
        this.removeBar("power");

        if (this.getModel().needPower) {
            this.addBar("power", entity => new Bar(
                () => "反应堆内电源",
                () => Color.valueOf("ddaa33"),
                () => entity.getRPS() / 180000,
            ));
        };

        this.addBar("Running", entity => new Bar(
            () => entity.isRunning() ? "[green]开机运行" : "[red]停机",
            () => entity.isRunning() ? Color.valueOf("12de12") : Color.valueOf("c01818"),
            () => entity.isRunning() ? 1 : 0,
        ));

        this.addBar("WaterIn", entity => new Bar(
            () => entity.isWaterInEnabled() ? "[#5ba4cf]进水开启" : "[red]进水关闭",
            () => entity.isWaterInEnabled() ? Color.valueOf("5ba4cf") : Color.valueOf("c01818"),
            () => entity.isWaterInEnabled() ? 1 : 0,
        ));

        this.addBar("WaterOut", entity => new Bar(
            () => entity.isWaterOutEnabled() ? "[#cf8b5b]出水开启" : "[red]出水关闭",
            () => entity.isWaterOutEnabled() ? Color.valueOf("cf8b5b") : Color.valueOf("c01818"),
            () => entity.isWaterOutEnabled() ? 1 : 0,
        ));

        this.addBar("FuelRod", entity => new Bar(
            () => "燃料棒数量",
            () => Color.valueOf("265626"),
            () => entity.getFuelRodAmoumt() / entity.block.getModel().MaxFuelRod,
        ));

        this.addBar("FuelRodUsed", entity => new Bar(
            () => {
                if (entity.FuelRodOrder()) { return "当前燃料棒消耗: " + Math.floor(entity.FuelRodinUsed() / (3000000 * FuelRods[entity.getFuelRodin()].Durable) * 100) + " %" } else { return "未装载燃料棒" }
            },
            () => Color.valueOf("265626"),
            () => {
                if (entity.FuelRodOrder()) { return 1 - entity.FuelRodinUsed() / (3000000 * FuelRods[entity.getFuelRodin()].Durable) } else { return 0 }
            },
        ));

        this.addBar("ControlRod", entity => new Bar(
            () => "控制棒拨出程度",
            () => Color.valueOf("565656"),
            () => 1 - entity.getControlRod() / 100,
        ));

        this.addBar("Flux", entity => new Bar(
            () => "中子通量: " + Math.floor(entity.getFlux()),
            () => Color.valueOf("e9e9ab"),
            () => entity.getFlux() / 100000,
        ));

        this.addBar("CoreHA", entity => new Bar(
            () => "核心温度: " + Math.floor(entity.getCoreTemperature()) + " 'C",
            () => Color.valueOf("c12626"),
            () => entity.getCoreTemperature() / 3600,
        ));

        this.addBar("ReactorCoreP", entity => new Bar(
            () => "堆芯压力: " + Math.round(entity.getReactorCoreP() * 10) / 10 + " 兆帕",
            () => Color.valueOf("dfdfdf"),
            () => Math.max(0, Math.min(1, entity.getReactorCoreP() / 25)),
        ));

        this.addBar("ReactorCoreHA", entity => new Bar(
            () => "堆芯温度: " + Math.floor(entity.getReactorCoreTemperature()) + " 'C",
            () => Color.valueOf("c12626"),
            () => entity.getReactorCoreTemperature() / 1500,
        ));

    };
}

function cloneObject(obj) {
    var clone = {};
    for (var i in obj) {
        if (typeof obj[i] == "object" && obj[i] != null) clone[i] = cloneObject(obj[i]);
        else clone[i] = obj[i];
    }
    return clone;
}

module.exports = {
    PWRv2(Type, EntityType, name, model, fuelrods, def, ExtraEntityDef) {
        const block = new PWRBlockV2();
        Object.assign(block, def);
        const pwr = extend(Type, name, block);
        pwr.buildType = () => extend(EntityType, pwr, Object.assign(new PWRBuildV2(), typeof ExtraEntityDef == "function" ? new ExtraEntityDef() : cloneObject(ExtraEntityDef)));
        pwr.configurable = true;
        pwr.hasItems = true;
        pwr.hasLiquids = true;
        pwr.hasPower = true;
        pwr.pwrModel = model;
        pwr.pwrFuelRods = fuelrods;
        pwr.saveConfig = true;
        return pwr;
    }
}