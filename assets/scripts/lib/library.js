/**
* v4
* 没啥，就是适配了136的更新
*/

/**
* v7
* 第二个版本主要优化ui和适配热量，修复了一些bug和优化逻辑。
*如有侵权，立即删除
*/

/**
* @author < (main) younggam 6.0 : v1 >
* @author < guiY 7.0 adapter&remake : v2 >
* @author < 晓伟 136 adapter : v3 >
* @readme <
    Version 6.0 To version 7.0 Not available due to changes in game source code,
    for example, NumberValue becomes StatValues.number, This means that some parameters and internal functions are changed,
    i just change some parameters and functions, to make it can adapt ver 7.0(and StatValue can be extended[line 554])
    Item display has been fixed
    v8: 支持 result 三段式 "物品或液体/概率/数量"
    v9: 支持可选输入 oritem / orliquid
>
*/

var colorStat = Pal.accent;

function makeItemImage(region, amount){
    var stack = new Packages.arc.scene.ui.layout.Stack();
    stack.add(new Packages.arc.scene.ui.Image(region));
    if(amount > 1){
        var t = new Packages.arc.scene.ui.layout.Table();
        t.bottom().right();
        t.add(String(amount)).style(Styles.outlineLabel);
        t.pack();
        stack.add(t);
    }
    return stack;
}

function parseRecipeStr(str, type, label){
    if(typeof str != "string") throw "It is not string at " + label;
    var words = str.split("/");
    if(words.length != 2 && words.length != 3) throw "Malform at " + label;
    if(isNaN(words[1])) throw "Invalid amount at " + label;
    if(words.length == 3 && isNaN(words[2])) throw "Invalid count at " + label;
    var content = type == "item" ? Vars.content.getByName(ContentType.item, words[0]) : Vars.content.getByName(ContentType.liquid, words[0]);
    if(content == null) throw "Invalid " + type + " at " + label;
    return {
        content: content,
        amount: words[1] * 1,
        count: words.length == 3 ? words[2] * 1 : 1
    };
}

function MultiCrafterBuild() {
    this.acceptItem = function(source, item) {
        if(typeof this.block["getInputItemSet"] !== "function") return false;
        if(this.items.get(item) >= this.getMaximumAccepted(item)) return false;
        if(!this.block.getInputItemSet().contains(item)) return false;
        // 拒绝接受当前配方的纯输出物品，防止堵住
        var current = this._toggle;
        if(current >= 0) {
            var recipe = this.block.getRecipes()[current];
            if(recipe.output) {
                var isOutput = false;
                if(recipe.output.items) {
                    for(var i = 0, len = recipe.output.items.length; i < len; i++) {
                        if(recipe.output.items[i].item == item){ isOutput = true; break; }
                    }
                };
                if(!isOutput && recipe.output.result) {
                    for(var i = 0, len = recipe.output.result.length; i < len; i++) {
                        if(recipe.output.result[i].item == item){ isOutput = true; break; }
                    }
                };
                if(isOutput) {
                    var isInput = false;
                    if(recipe.input && recipe.input.items) {
                        for(var i = 0, len = recipe.input.items.length; i < len; i++) {
                            if(recipe.input.items[i].item == item){ isInput = true; break; }
                        }
                    };
                    // 检查可选输入物品
                    if(!isInput && recipe.input && recipe.input.oritems) {
                        for(var i = 0, len = recipe.input.oritems.length; i < len; i++) {
                            if(recipe.input.oritems[i].item == item){ isInput = true; break; }
                        }
                    };
                    if(!isInput) return false;
                };
            };
        };
        return true;
    };
    this.acceptLiquid = function(source, liquid) {
        if(typeof this.block["getInputLiquidSet"] !== "function") return false;
        if(!this.block.getInputLiquidSet().contains(liquid)) return false;
        // 拒绝接受当前配方的纯输出液体，防止串液
        var current = this._toggle;
        if(current >= 0) {
            var recipe = this.block.getRecipes()[current];
            if(recipe.output && recipe.output.liquids) {
                var isOutput = false;
                for(var i = 0, len = recipe.output.liquids.length; i < len; i++) {
                    if(recipe.output.liquids[i].liquid == liquid){ isOutput = true; break; }
                };
                if(isOutput) {
                    var isInput = false;
                    if(recipe.input && recipe.input.liquids) {
                        for(var i = 0, len = recipe.input.liquids.length; i < len; i++) {
                            if(recipe.input.liquids[i].liquid == liquid){ isInput = true; break; }
                        }
                    };
                    // 检查可选输入液体
                    if(!isInput && recipe.input && recipe.input.orliquids) {
                        for(var i = 0, len = recipe.input.orliquids.length; i < len; i++) {
                            if(recipe.input.orliquids[i].liquid == liquid){ isInput = true; break; }
                        }
                    };
                    if(!isInput) return false;
                };
            };
        };
        return true;
    };
    this.removeStack = function(item, amount) {
        var ret = this.super$removeStack(item, amount);
        if(!this.items.has(item)) this.toOutputItemSet.remove(item);
        return ret;
    };
    this._isDumpOutput = function(item) {
        if(typeof this.block["getRecipes"] !== "function") return false;
        var current = this._toggle;
        var isOutput = this.block.doDumpToggle()
            ? current > -1 && this.block.getRecipes()[current].output.items.some(a => a.item == item)
            : this.block.getOutputItemSet().contains(item);
        return isOutput && !this.block.getInputItemSet().contains(item) && !this.items.has(item);
    };
    this.handleItem = function(source, item) {
        if(this._isDumpOutput(item)) this.toOutputItemSet.add(item);
        this.items.add(item, 1);
    };
    this.handleStack = function(item, amount, tile, source) {
        if(this._isDumpOutput(item)) this.toOutputItemSet.add(item);
        this.items.add(item, amount);
    };
    this.displayConsumption = function(table) {
        if(typeof this.block["getRecipes"] !== "function") return;
        const recs = this.block.getRecipes();
        table.left();
        var flow = new Packages.arc.scene.ui.layout.Table();
        var lastIdx = -1;
        flow.update(() => {
            var cur = this._toggle;
            if(cur < 0 || cur >= recs.length) cur = 0;
            if(cur != lastIdx){
                lastIdx = cur;
                flow.clearChildren();
                var rec = recs[cur];
                var inputItems = rec.input.items;
                var inputLiquids = rec.input.liquids;
                var orItems = rec.input.oritems;
                var orLiquids = rec.input.orliquids;
                var outputItems = rec.output.items;
                var outputLiquids = rec.output.liquids;
                var outputResults = rec.output.result;
                var inputPower = rec.input.power;
                var outputPower = rec.output.power;
                flow.left();
                for(var l = 0, len = inputItems.length; l < len; l++) {
                    (function(that, stack) {
                        flow.add(new Packages.mindustry.ui.ReqImage(makeItemImage(stack.item.uiIcon, stack.amount), () => that.items != null && that.items.has(stack.item, stack.amount))).size(8 * 4).padRight(5);
                    })(this, inputItems[l]);
                }
                for(var l = 0, len = inputLiquids.length; l < len; l++) {
                    (function(that, stack) {
                        flow.add(new Packages.mindustry.ui.ReqImage(makeItemImage(stack.liquid.uiIcon, stack.amount), () => that.liquids != null && that.liquids.get(stack.liquid) > stack.amount)).size(8 * 4).padRight(5);
                    })(this, inputLiquids[l]);
                }
                // 可选输入物品
                if(orItems && orItems.length > 0){
                    for(var l = 0, len = orItems.length; l < len; l++) {
                        if(l >= 1) flow.add(" [white]/ ").padRight(5);
                        (function(that, stack) {
                            flow.add(new Packages.mindustry.ui.ReqImage(makeItemImage(stack.item.uiIcon, stack.amount), () => that.items != null && that.items.has(stack.item, stack.amount))).size(8 * 4).padRight(5);
                        })(this, orItems[l]);
                    }
                }
                // 可选输入液体
                if(orLiquids && orLiquids.length > 0){
                    for(var l = 0, len = orLiquids.length; l < len; l++) {
                        if(l >= 1) flow.add(" [white]/ ").padRight(5);
                        (function(that, stack) {
                            flow.add(new Packages.mindustry.ui.ReqImage(makeItemImage(stack.liquid.uiIcon, stack.amount), () => that.liquids != null && that.liquids.get(stack.liquid) > stack.amount)).size(8 * 4).padRight(5);
                        })(this, orLiquids[l]);
                    }
                }
                if(inputPower > 0) flow.add(Image(Icon.power)).size(8 * 4).padRight(5);
                flow.add(Image(Icon.right)).size(8 * 4).padRight(5);
                for(var jj = 0, len = outputItems.length; jj < len; jj++) {
                    flow.add(makeItemImage(outputItems[jj].item.uiIcon, outputItems[jj].amount)).size(8 * 4).padRight(5);
                }
                // 概率产出，仅显示图标（兼容液体）
                for(var jj = 0, len = outputResults.length; jj < len; jj++) {
                    flow.add(makeItemImage((outputResults[jj].liquid != null ? outputResults[jj].liquid : outputResults[jj].item).uiIcon, 0)).size(8 * 4).padRight(5);
                }
                for(var jj = 0, len = outputLiquids.length; jj < len; jj++) {
                    flow.add(makeItemImage(outputLiquids[jj].liquid.uiIcon, outputLiquids[jj].amount)).size(8 * 4).padRight(5);
                }
                if(outputPower > 0) flow.add(Image(Icon.power)).size(8 * 4).padRight(5);
                flow.pack();
            }
        });
        table.add(flow).left();
    };
    this.getPowerProduction = function() {
        var i = this._toggle;
        if(i < 0 || typeof this.block["getRecipes"] !== "function") return 0;
        var oPower = this.block.getRecipes()[i].output.power;
        if(oPower > 0 && this._cond) {
            if(this.block.getRecipes()[i].input.power > 0) {
                this._powerStat = this.efficiency();
                return oPower * this.efficiency();
            } else {
                this._powerStat = 1;
                return oPower;
            };
        }
        this._powerStat = 0;
        return 0;
    };
    this.getProgressIncreaseA = function(i, baseTime) {
        if(typeof this.block["getRecipes"] !== "function" || this.block.getRecipes()[i].input.power > 0) return this.getProgressIncrease(baseTime);
        else return 1 / baseTime * this.delta();
    };
    this.checkinput = function(i) {
        const recs = this.block.getRecipes();
        var items = recs[i].input.items;
        var liquids = recs[i].input.liquids;
        var orItems = recs[i].input.oritems;
        var orLiquids = recs[i].input.orliquids;
        // 基础输入必须满足
        if(!this.items.has(items)) return true;
        for(var j = 0, len = liquids.length; j < len; j++) {
            if(this.liquids.get(liquids[j].liquid) < liquids[j].amount) return true;
        };
        // 可选输入物品：至少要有一个满足
        if(orItems && orItems.length > 0) {
            var hasOrItem = false;
            for(var k = 0; k < orItems.length; k++) {
                if(this.items.has(orItems[k].item, orItems[k].amount)) { hasOrItem = true; break; }
            }
            if(!hasOrItem) return true;
        }
        // 可选输入液体：至少要有一个满足
        if(orLiquids && orLiquids.length > 0) {
            var hasOrLiquid = false;
            for(var k = 0; k < orLiquids.length; k++) {
                if(this.liquids.get(orLiquids[k].liquid) >= orLiquids[k].amount) { hasOrLiquid = true; break; }
            }
            if(!hasOrLiquid) return true;
        }
        return false;
    };
    this.checkoutput = function(i) {
        const recs = this.block.getRecipes();
        var items = recs[i].output.items;
        var liquids = recs[i].output.liquids;
        for(var j = 0, len = items.length; j < len; j++) {
            if(this.items.get(items[j].item) + items[j].amount > this.getMaximumAccepted(items[j].item)) return true;
        };
        if(liquids.length > 0) {
            var allFull = true;
            for(var j = 0, len = liquids.length; j < len; j++) {
                if(this.liquids.get(liquids[j].liquid) + liquids[j].amount <= this.block.liquidCapacity) {
                    allFull = false;
                    break;
                }
            };
            if(allFull) return true;
        };
        return false;
    };
    this.checkCond = function(i) {
        if(this.block.getRecipes()[i].input.power > 0 && this.power.status <= 0) {
            this._condValid = false;
            this._cond = false;
            return false;
        } else if(this.block.getRecipes()[i].input.heat > 0 && (this._heat || 0) <= 0) {
            // 需要热量的配方在没有热量时不应该工作
            this._condValid = false;
            this._cond = false;
            return false;
        } else if(this.checkinput(i)) {
            this._condValid = false;
            this._cond = false;
            return false;
        } else if(this.checkoutput(i)) {
            this._condValid = true;
            this._cond = false;
            return false;
        };
        this._condValid = true;
        this._cond = true;
        return true;
    };
    this.customCons = function(i) {
        const recs = this.block.getRecipes();
        if(this.checkCond(i)) {
            if(this.progressArr[i] != 0 && this.progressArr[i] != null) {
                this.progress = this.progressArr[i];
                this.progressArr[i] = 0;
            };
            this.progress += this.getProgressIncreaseA(i, recs[i].craftTime);
            this.totalProgress += this.delta();
            this.warmup = Mathf.lerpDelta(this.warmup, 1, 0.02);
            if(Mathf.chance(Time.delta * this.updateEffectChance)) Effects.effect(this.updateEffect, this.x + Mathf.range(this.size * 4), this.y + Mathf.range(this.size * 4));
        } else this.warmup = Mathf.lerp(this.warmup, 0, 0.02);
    };
    this.customProd = function(i) {
        const recs = this.block.getRecipes();
        var inputItems = recs[i].input.items;
        var inputLiquids = recs[i].input.liquids;
        var orItems = recs[i].input.oritems;
        var orLiquids = recs[i].input.orliquids;
        var outputItems = recs[i].output.items;
        var outputLiquids = recs[i].output.liquids;
        var outputResults = recs[i].output.result;
        var eItems = this.items;
        var eLiquids = this.liquids;
        // 消耗基础输入
        for(var k = 0, len = inputItems.length; k < len; k++) eItems.remove(inputItems[k]);
        for(var j = 0, len = inputLiquids.length; j < len; j++) eLiquids.remove(inputLiquids[j].liquid, inputLiquids[j].amount);
        // 消耗可选输入物品（只消耗第一个满足的）
        if(orItems && orItems.length > 0) {
            for(var k = 0; k < orItems.length; k++) {
                if(eItems.has(orItems[k].item, orItems[k].amount)) {
                    eItems.remove(orItems[k].item, orItems[k].amount);
                    break;
                }
            }
        }
        // 消耗可选输入液体（只消耗第一个满足的）
        if(orLiquids && orLiquids.length > 0) {
            for(var k = 0; k < orLiquids.length; k++) {
                if(eLiquids.get(orLiquids[k].liquid) >= orLiquids[k].amount) {
                    eLiquids.remove(orLiquids[k].liquid, orLiquids[k].amount);
                    break;
                }
            }
        }
        // 产出必定物品
        for(var a = 0, len = outputItems.length; a < len; a++) {
            for(var aa = 0, amount = outputItems[a].amount; aa < amount; aa++) {
                var oItem = outputItems[a].item
                if(!this.put(oItem)) {
                    if(!eItems.has(oItem)) this.toOutputItemSet.add(oItem);
                    eItems.add(oItem, 1);
                };
            };
        };
        // 概率产出（支持数量，兼容液体）
        for(var r = 0, rlen = outputResults.length; r < rlen; r++) {
            if(Mathf.chance(outputResults[r].amount)) {
                if(outputResults[r].liquid != null) {
                    var rLiquid = outputResults[r].liquid;
                    if(eLiquids.get(rLiquid) <= 0.001) this.toOutputLiquidset.add(rLiquid);
                    this.handleLiquid(this, rLiquid, outputResults[r].count || 1);
                } else {
                    var rItem = outputResults[r].item;
                    var rCount = outputResults[r].count || 1;
                    for(var c = 0; c < rCount; c++) {
                        if(!this.put(rItem)) {
                            if(!eItems.has(rItem)) this.toOutputItemSet.add(rItem);
                            eItems.add(rItem, 1);
                        };
                    };
                }
            };
        };
        // 产出液体
        for(var j = 0, len = outputLiquids.length; j < len; j++) {
            var oLiquid = outputLiquids[j].liquid;
            if(eLiquids.get(oLiquid) <= 0.001) this.toOutputLiquidset.add(oLiquid);
            this.handleLiquid(this, oLiquid, outputLiquids[j].amount);
        };
        this.block.craftEffect.at(this.x, this.y);
        this.progress = 0;
    };
    this.updateTile = function() {
        if(typeof this.block["getRecipes"] !== "function") return;
        if(this.timer.get(1, 60)) {
            this.itemHas = 0;
            this.items.each(item => this.itemHas++);
        };
        if(!Vars.headless) this.block.invFrag.hide();
        const recs = this.block.getRecipes();
        var recLen = recs.length;
        var current = this._toggle;
        if(typeof this["customUpdate"] === "function") this.customUpdate();
        if(current >= 0) {
            this.customCons(current);
            if(this.progress >= 1) this.customProd(current);
        };
        var eItems = this.items;
        var eLiquids = this.liquids;
        if(this.block.doDumpToggle() && current == -1) return;
        var que = this.toOutputItemSet.orderedItems(),
            len = que.size,
            itemEntry = this.dumpItemEntry;
        if(this.timer.get(this.block.dumpTime) && len > 0) {
            for(var i = 0; i < len; i++) {
                var candidate = que.get((i + itemEntry) % len);
                if(this.put(candidate)) {
                    eItems.remove(candidate, 1);
                    if(!eItems.has(candidate)) this.toOutputItemSet.remove(candidate);
                    break;
                };
            };
            if(i != len) this.dumpItemEntry = (i + itemEntry) % len;
        };
        var queL = this.toOutputLiquidset.orderedItems(),
            lenL = queL.size,
            liquidEntry = this.dumpLiquidEntry;
        if(this.timer.get(this.block.dumpTime) && lenL > 0) {
            for(var j = 0; j < lenL; j++) {
                var candidate = queL.get((j + liquidEntry) % lenL);
                this.dumpLiquid(candidate);
                if(eLiquids.get(candidate) <= 0.001) {
                    this.toOutputLiquidset.remove(candidate);
                    this.dumpLiquidEntry = (j + liquidEntry) % lenL;
                    break;
                }
            };
        };
        // 直接倾倒当前配方输出液体，用独立计数器控制频率
        if(current >= 0) {
            this._liquidDumpTimer += this.delta();
            if(this._liquidDumpTimer >= this.block.dumpTime) {
                this._liquidDumpTimer = 0;
                var outLiqs = recs[current].output.liquids;
                for(var j = 0, len = outLiqs.length; j < len; j++) {
                    this.dumpLiquid(outLiqs[j].liquid);
                };
            };
        };
    };
    this.shouldConsume = function() {
        return this._condValid && this.productionValid();
    };
    this.productionValid = function() {
        return this._cond && this.enabled;
    };
    this.buildConfiguration = function(table) {
        if(typeof this.block["getRecipes"] !== "function") return;
        const recs = this.block.getRecipes(),
            invFrag = this.block.getInvFrag();
        // 只有一个配方时不需要渲染配方选择按钮
        if(recs.length <= 1) return;
        if(!invFrag.isBuilt()) invFrag.build(table.parent);
        if(invFrag.isShown()) {
            invFrag.hide();
            Vars.control.input.frag.config.hideConfig();
            return;
        };
        var group = new Packages.arc.scene.ui.ButtonGroup();
        group.setMinCheckCount(0);
        group.setMaxCheckCount(1);
        var recLen = recs.length;
        for(var i = 0; i < recLen; i++) {
            (function(i, that) {
                var output = recs[i].output;
                var button = table.button(Tex.pane,40,() => that.configure(button.isChecked() ? i : -1)).group(group).pad(2).get();
                button.getStyle().up = Styles.black3;
                button.getStyle().down = Styles.flatOver;
                button.getStyle().checked = Styles.accentDrawable;
                button.getStyle().imageUp = new TextureRegionDrawable(output.items.length > 0 ? output.items[0].item.uiIcon : output.liquids.length > 0 ? output.liquids[0].liquid.uiIcon : output.power > 0 ? Icon.power : Icon.cancel);
                button.update(() => button.setChecked(i==that._toggle));
            })(i, this);
        };
    };
    this.configured = function(player, value) {
        if(isNaN(value) || typeof value != "number") {
            this._toggle = -1;
            this._cond = false;
            this._condValid = false;
            return;
        };
        var current = this._toggle;
        if(current >= 0) this.progressArr[current] = this.progress;
        if(value == -1) {
            this._condValid = false;
            this._cond = false;
        };
        this.toOutputItemSet.clear();
        this.toOutputLiquidset.clear();
        if(this.block.doDumpToggle() && value > -1) {
            var oItems = this.block.getRecipes()[value].output.items;
            var oLiquids = this.block.getRecipes()[value].output.liquids;
            for(var i = 0, len = oItems.length; i < len; i++) {
                var item = oItems[i].item;
                if(this.items.has(item)) this.toOutputItemSet.add(item);
            };
            for(var i = 0, len = oLiquids.length; i < len; i++) {
                var liquid = oLiquids[i].liquid;
                if(this.liquids.get(liquid) > 0.001) this.toOutputLiquidset.add(liquid);
            };
        };
        this.progress = 0;
        this._toggle = value;
    };
    this.onConfigureTileTapped = function(other) {
        return this.items.total() > 0 ? true : this != other;
    };
    this.getToggle = function() {
        return this._toggle;
    };
    this._toggle = 0;
    this.progressArr = [];
    this.getCond = function() {
        return this._cond;
    };
    this._cond = false;
    this._condValid = false;
    this.getCondValid = function() {
        return this._condValid;
    };
    this.getPowerStat = function() {
        return this._powerStat;
    };
    this._powerStat = 0;
    this.toOutputItemSet = new OrderedSet();
    this.toOutputLiquidset = new OrderedSet();
    this.dumpItemEntry = 0;
    this.dumpLiquidEntry = 0;
    this.itemHas = 0;
    this._liquidDumpTimer = 0;
    this.config = function() {
        return this._toggle;
    };
    this.write = function(write) {
        this.super$write(write);
        write.s(this._toggle);
        var queItem = this.toOutputItemSet.orderedItems(),
            len = queItem.size;
        write.s(len);
        for(var i = 0; i < len; i++) write.s(queItem.get(i).id);
        var queLiquid = this.toOutputLiquidset.orderedItems(),
            len = queLiquid.size;
        write.s(len);
        for(var i = 0; i < len; i++) write.s(queLiquid.get(i).id);
    };
    this.read = function(read, revision) {
        this.super$read(read, revision);
        this._toggle = read.s();
        this.toOutputItemSet.clear();
        this.toOutputLiquidset.clear();
        var len = read.s(),
            vc = Vars.content,
            ci = ContentType.item,
            cl = ContentType.liquid;
        for(var i = 0; i < len; i++) this.toOutputItemSet.add(vc.getByID(ci, read.s()));
        var len = read.s();
        for(var i = 0; i < len; i++) this.toOutputLiquidset.add(vc.getByID(cl, read.s()));
    };
};

function MultiCrafterBlock() {
    this.tmpRecs = [];
    var recs = [];
    var infoStyle = null;
    this.getRecipes = function() {
        return recs;
    };
    this._liquidSet = new ObjectSet();
    this.getLiquidSet = function() {
        return this._liquidSet;
    };
    this.hasOutputItem = false;
    this._inputItemSet = new ObjectSet();
    this.getInputItemSet = function() {
        return this._inputItemSet;
    };
    this._inputLiquidSet = new ObjectSet();
    this.getInputLiquidSet = function() {
        return this._inputLiquidSet;
    };
    this._outputItemSet = new ObjectSet();
    this.getOutputItemSet = function() {
        return this._outputItemSet;
    };
    this._outputLiquidSet = new ObjectSet();
    this.getOutputLiquidSet = function() {
        return this._outputLiquidSet;
    };
    this.dumpToggle = false;
    this.doDumpToggle = function() {
        return this.dumpToggle;
    };
    this.powerBarI = false;
    this.powerBarO = false;
    this._invFrag = extend(BlockInventoryFragment, {
        _built: false,
        isBuilt() {
            return this._built;
        },
        visible: false,
        isShown() {
            return this.visible;
        },
        showFor(t) {
            this.visible = true;
            this.super$showFor(t);
        },
        hide() {
            this.visible = false;
            this.super$hide();
        },
        build(parent) {
            this._built = true;
            this.super$build(parent);
        }
    });
    this.getInvFrag = function() {
        return this._invFrag;
    };
    this.init = function() {
        for(var i = 0; i < this.tmpRecs.length; i++) {
            var tmp = this.tmpRecs[i];
            var isInputExist = tmp.input != null,
                isOutputExist = tmp.output != null;
            var tmpInput = tmp.input;
            var tmpOutput = tmp.output;
            if(isInputExist && tmpInput.power > 0) this.powerBarI = true;
            if(isOutputExist && tmpOutput.power > 0) this.powerBarO = true;
            recs[i] = {
                input: {
                    items: [],
                    liquids: [],
                    oritems: [],      // ← 新增：可选输入物品
                    orliquids: [],    // ← 新增：可选输入液体
                    power: isInputExist ? typeof tmpInput.power == "number" ? tmpInput.power : 0 : 0,
                    heat: isInputExist && typeof tmpInput.heat == "number" ? tmpInput.heat : 0
                },
                output: {
                    items: [],
                    liquids: [],
                    result: [],
                    power: isOutputExist ? typeof tmpOutput.power == "number" ? tmpOutput.power : 0 : 0,
                    heat: isOutputExist && typeof tmpOutput.heat == "number" ? tmpOutput.heat : 0
                },
                craftTime: typeof tmp.craftTime == "number" ? tmp.craftTime : 80,
                maxEfficiency: typeof tmp.maxEfficiency == "number" ? tmp.maxEfficiency : 0
            };
            var realInput = recs[i].input;
            var realOutput = recs[i].output;
            if(isInputExist) {
                if(tmpInput.items != null) {
                    for(var j = 0, len = tmpInput.items.length; j < len; j++) {
                        var parsed = parseRecipeStr(tmpInput.items[j], "item", j + "th input item in " + i + "th recipe");
                        this._inputItemSet.add(parsed.content);
                        realInput.items[j] = new ItemStack(parsed.content, parsed.amount);
                    };
                };
                if(tmpInput.liquids != null) {
                    for(var j = 0, len = tmpInput.liquids.length; j < len; j++) {
                        var parsed = parseRecipeStr(tmpInput.liquids[j], "liquid", j + "th input liquid in " + i + "th recipe");
                        this._inputLiquidSet.add(parsed.content);
                        this._liquidSet.add(parsed.content);
                        realInput.liquids[j] = new LiquidStack(parsed.content, parsed.amount);
                    };
                };
                // === 新增：可选输入物品 ===
                if(tmpInput.oritem != null) {
                    for(var j = 0, len = tmpInput.oritem.length; j < len; j++) {
                        var parsed = parseRecipeStr(tmpInput.oritem[j], "item", j + "th oritem in " + i + "th recipe");
                        this._inputItemSet.add(parsed.content);
                        realInput.oritems[j] = new ItemStack(parsed.content, parsed.amount);
                    };
                };
                // === 新增：可选输入液体 ===
                if(tmpInput.orliquid != null) {
                    for(var j = 0, len = tmpInput.orliquid.length; j < len; j++) {
                        var parsed = parseRecipeStr(tmpInput.orliquid[j], "liquid", j + "th orliquid in " + i + "th recipe");
                        this._inputLiquidSet.add(parsed.content);
                        this._liquidSet.add(parsed.content);
                        realInput.orliquids[j] = new LiquidStack(parsed.content, parsed.amount);
                    };
                };
            };
            if(isOutputExist) {
                if(tmpOutput.items != null) {
                    for(var j = 0, len = tmpOutput.items.length; j < len; j++) {
                        var parsed = parseRecipeStr(tmpOutput.items[j], "item", j + "th output item in " + i + "th recipe");
                        this._outputItemSet.add(parsed.content);
                        realOutput.items[j] = new ItemStack(parsed.content, parsed.amount);
                    };
                    if(j != 0) this.hasOutputItem = true;
                };
                if(tmpOutput.result != null) {
                    for(var j = 0, len = tmpOutput.result.length; j < len; j++) {
                        // 先按物品解析，找不到同名物品时回退为液体（兼容液体概率产出）
                        var resultName = String(tmpOutput.result[j]).split("/")[0];
                        if(Vars.content.getByName(ContentType.item, resultName) != null) {
                            var parsed = parseRecipeStr(tmpOutput.result[j], "item", j + "th result item in " + i + "th recipe");
                            this._outputItemSet.add(parsed.content);
                            realOutput.result[j] = {item: parsed.content, amount: parsed.amount, count: parsed.count};
                            this.hasOutputItem = true;
                        } else {
                            var parsedL = parseRecipeStr(tmpOutput.result[j], "liquid", j + "th result liquid in " + i + "th recipe");
                            this._outputLiquidSet.add(parsedL.content);
                            this._liquidSet.add(parsedL.content);
                            realOutput.result[j] = {liquid: parsedL.content, amount: parsedL.amount, count: parsedL.count};
                        }
                    };
                };
                if(tmpOutput.liquids != null) {
                    for(var j = 0, len = tmpOutput.liquids.length; j < len; j++) {
                        var parsed = parseRecipeStr(tmpOutput.liquids[j], "liquid", j + "th output liquid in " + i + "th recipe");
                        this._outputLiquidSet.add(parsed.content);
                        this._liquidSet.add(parsed.content);
                        realOutput.liquids[j] = new LiquidStack(parsed.content, parsed.amount);
                    };
                };
            };
        };
        this.hasPower = this.powerBarI || this.powerBarO;
        if(this.powerBarI) this.consumeBuilder.add(extend(ConsumePower, {
            requestedPower(entity) {
                if(typeof entity["getToggle"] !== "function") return 0;
                var i = entity.getToggle();
                if(i < 0) return 0;
                var input = entity.block.getRecipes()[i].input.power;
                if(input > 0 && entity.getCond()) return input;
                return 0;
            }
        }));
        this.consumesPower = this.powerBarI;
        this.outputsPower = this.powerBarO;
        this.super$init();
        if(!this._outputLiquidSet.isEmpty()) this.outputsLiquid = true;
        this.timers++;
        if(!Vars.headless) infoStyle = Core.scene.getStyle(Button.ButtonStyle);
    };
    this.setStats = function() {
        this.super$setStats();
        if(this.powerBarI) this.stats.remove(Stat.powerUse);
        this.stats.remove(Stat.productionTime);
        this.stats.add(Stat.input, new JavaAdapter(StatValue, {
        display(table){
            table.row();
            var recLen = recs.length;
            for(var i = 0; i < recLen; i++) {
                var rec = recs[i];
                var outputItems = rec.output.items,
                    inputItems = rec.input.items;
                var outputLiquids = rec.output.liquids,
                    inputLiquids = rec.input.liquids;
                var orItems = rec.input.oritems,
                    orLiquids = rec.input.orliquids;
                var inputPower = rec.input.power,
                    outputPower = rec.output.power;
                table.table(infoStyle.up, part => {
                    part.add("[accent]" + Stat.input.localized()).expandX().left().row();
                    part.table(cons(row => {
                        for(var l = 0, len = inputItems.length; l < len; l++) row.add(StatValues.displayItem(inputItems[l].item, inputItems[l].amount, true)).padRight(5);
                    })).left().row();
                    part.table(cons(row => {
                        for(var l = 0, len = inputLiquids.length; l < len; l++) row.add(StatValues.displayLiquid(inputLiquids[l].liquid, inputLiquids[l].amount, false));
                    })).left().row();
                    // === 新增：可选输入显示 ===
                    if((orItems && orItems.length > 0) || (orLiquids && orLiquids.length > 0)){
                        part.table(cons(row => {
                            row.add("[lightgray]可选输入: ").padRight(4);
                            var first = true;
                            for(var l = 0, len = orItems.length; l < len; l++){
                                if(!first) row.add(" [white]/ ");
                                row.add(StatValues.displayItem(orItems[l].item, orItems[l].amount, true)).padRight(5);
                                first = false;
                            }
                            for(var l = 0, len = orLiquids.length; l < len; l++){
                                if(!first) row.add(" [white]/ ");
                                row.add(StatValues.displayLiquid(orLiquids[l].liquid, orLiquids[l].amount, false));
                                first = false;
                            }
                        })).left().row();
                    }
                    if(inputPower > 0) {
                        part.table(cons(row => {
                            row.add("[lightgray]" + Stat.powerUse.localized() + ":[]").padRight(4);
                            (StatValues.number(inputPower * 60, StatUnit.powerSecond)).display(row);
                        })).left().row();
                    }
                    if(rec.input.heat > 0) {
                        part.table(cons(row => {
                            row.add("[lightgray]" + Stat.input.localized() + ":[]").padRight(4);
                            (StatValues.number(rec.input.heat, StatUnit.heatUnits)).display(row);
                        })).left().row();
                    }
                    part.add("[accent]" + Stat.output.localized()).left().row();
                    part.table(cons(row => {
                        for(var jj = 0, len = outputItems.length; jj < len; jj++) row.add(StatValues.displayItem(outputItems[jj].item, outputItems[jj].amount, true)).padRight(5);
                    })).left().row();
                    if(rec.output.result.length > 0) {
                        part.table(cons(row => {
                            row.add("[lightgray]" + Stat.output.localized() + "(" + Core.bundle.get("stat.result", "产物") + "):[]").padRight(4);
                            for(var jj = 0, len = rec.output.result.length; jj < len; jj++) {
                                if(jj > 0) row.add(" [white]/ ");
                                // 数量像非概率配方一样显示在图标角标上（兼容液体）
                                var rr = rec.output.result[jj];
                                if(rr.liquid != null) {
                                    row.add(StatValues.displayLiquid(rr.liquid, rr.count || 1, false));
                                } else {
                                    row.add(StatValues.displayItem(rr.item, rr.count || 1, true));
                                }
                                row.add(" [white]" + (Math.round(rr.amount * 10000) / 100) + "%");
                            };
                        })).left().row();
                    }
                    part.table(cons(row => {
                        for(var jj = 0, len = outputLiquids.length; jj < len; jj++) row.add(StatValues.displayLiquid(outputLiquids[jj].liquid, outputLiquids[jj].amount, false));
                    })).left().row();
                    if(outputPower > 0) {
                        part.table(cons(row => {
                            row.add("[lightgray]" + Stat.basePowerGeneration.localized() + ":[]").padRight(4);
                            (StatValues.number(outputPower * 60, StatUnit.powerSecond)).display(row);
                        })).left().row();
                    }
                    if(rec.output.heat > 0) {
                        part.table(cons(row => {
                            row.add("[lightgray]" + Stat.output.localized() + ":[]").padRight(4);
                            (StatValues.number(rec.output.heat, StatUnit.heatUnits)).display(row);
                        })).left().row();
                    }
                    part.table(cons(row => {
                        row.add("[lightgray]" + Stat.productionTime.localized() + ":[]").padRight(4);
                        (StatValues.number(rec.craftTime / 60, StatUnit.seconds)).display(row);
                    })).left().row();
                    if(typeof this["customDisplay"] === "function") this.customDisplay(part, i);
                }).color(colorStat).left().growX();
                table.add().size(18).row();
            }
        },
        }));
    };
    this.setBars = function() {
        this.super$setBars();
        this.removeBar("liquid");
        this.removeBar("items");
        this.removeBar("power");
        this.removeBar("poweroutput");
        this.addBar("power", entity => {
            var recs = entity.block.getRecipes();
            var toggle = typeof entity.getToggle === "function" ? entity.getToggle() : -1;
            if(toggle >= 0 && toggle < recs.length && recs[toggle].input.power > 0){
                return new Bar(() => Core.bundle.format("bar.power", Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale, 1)), () => Pal.powerBar, () => entity.power.status);
            }
            return null;
        });
        if(this.powerBarO){
            this.addBar("poweroutput", entity => {
                var recs = entity.block.getRecipes();
                var toggle = typeof entity.getToggle === "function" ? entity.getToggle() : -1;
                if(toggle >= 0 && toggle < recs.length && recs[toggle].output.power > 0){
                    return new Bar(() => Core.bundle.format("bar.poweroutput", entity.getPowerProduction() * 60 * entity.timeScale), () => Pal.powerBar, () => typeof entity["getPowerStat"] === "function" ? entity.getPowerStat() : 0);
                }
                return null;
            });
        }
        var i = 0;
        if(!this._liquidSet.isEmpty()) {
            this._liquidSet.each(k => {
                this.addBar("liquid" + i, entity => {
                    var recs = entity.block.getRecipes();
                    var toggle = typeof entity.getToggle === "function" ? entity.getToggle() : -1;
                    var used = false;
                    if(toggle >= 0 && toggle < recs.length){
                        var recipe = recs[toggle];
                        if(recipe.input && recipe.input.liquids){
                            for(var j = 0; j < recipe.input.liquids.length; j++){
                                if(recipe.input.liquids[j].liquid == k){
                                    used = true;
                                    break;
                                }
                            }
                        }
                        // === 新增：可选输入液体也参与显示判断 ===
                        if(!used && recipe.input && recipe.input.orliquids){
                            for(var j = 0; j < recipe.input.orliquids.length; j++){
                                if(recipe.input.orliquids[j].liquid == k){
                                    used = true;
                                    break;
                                }
                            }
                        }
                        if(!used && recipe.output && recipe.output.liquids){
                            for(var j = 0; j < recipe.output.liquids.length; j++){
                                if(recipe.output.liquids[j].liquid == k){
                                    used = true;
                                    break;
                                }
                            }
                        }
                    }
                    if(used){
                        return new Bar(() => k.localizedName, () => k.barColor == null ? k.color : k.barColor, () => entity.liquids.get(k) / this.liquidCapacity);
                    }
                    return null;
                });
                i++;
            });
        }
    }
    this.outputsItems = function() {
        return this.hasOutputItem;
    }
}

function cloneObject(obj) {
    var clone = {};
    for(var i in obj) {
        if(typeof obj[i] == "object" && obj[i] != null) clone[i] = cloneObject(obj[i]);
        else clone[i] = obj[i];
    }
    return clone;
}

module.exports = {
    MultiCrafter(Type, EntityType, name, recipes, def, ExtraEntityDef, heat) {
        const block = new MultiCrafterBlock();
        Object.assign(block, def);
        const multi = extend(Type, name, block);
        var hasIn = false, hasOut = false;
        for (var ri = 0; ri < recipes.length; ri++) {
            var rc = recipes[ri];
            if (rc.input != null && typeof rc.input.heat == "number" && rc.input.heat > 0) hasIn = true;
            if (rc.output != null && typeof rc.output.heat == "number" && rc.output.heat > 0) hasOut = true;
        }
        heat = heat != null ? heat : {};
        heat.isHeatSource = hasOut;
        if (heat.isHeatSink == null) heat.isHeatSink = hasIn;
        var heatEnabled = heat.isHeatSource || heat.isHeatSink;
        if (heatEnabled) {
            if (heat.isHeatSource) {
                multi.rotate = true;
                multi.rotateDraw = false;
                multi.drawArrow = true;
            }
            multi.addBar("heat", function(e) {
                var recs = e.block.getRecipes();
                var i = typeof e.getToggle === "function" ? e.getToggle() : -1;
                var inH = (i >= 0 && i < recs.length) ? recs[i].input.heat : 0;
                var outH = (i >= 0 && i < recs.length) ? recs[i].output.heat : 0;
                // 当前配方不消耗也不产出热量时，不显示热量条，与其他 bar 保持一致
                if(inH <= 0 && outH <= 0) return null;
                var req = inH > 0 ? inH : (outH > 0 ? outH : (heat.heatRequirement != null ? heat.heatRequirement : 10));
                return new Bar(
                    function() {
                        if (inH > 0) {
                            var eff = typeof e.efficiencyScale === "function" ? e.efficiencyScale() : 0;
                            return Core.bundle.format("bar.heatpercent", Math.floor(e.heat() + 0.01), Math.floor(eff * 100 + 0.01));
                        }
                        return Core.bundle.format("bar.heatamount", Math.floor(e.heat() + 0.01));
                    },
                    function() { return Pal.lightOrange; },
                    function() { return req > 0 ? e.heat() / req : 0; }
                );
            });
        }
        multi.buildType = () => {
            var props = Object.assign(new MultiCrafterBuild(), typeof ExtraEntityDef == "function" ? new ExtraEntityDef() : cloneObject(ExtraEntityDef));
            var heatIfaces = [];
            if (heatEnabled) {
                if (heat.isHeatSource) heatIfaces.push(HeatBlock);
                if (heat.isHeatSink) heatIfaces.push(HeatConsumer);
                props.heat = function() { return this._heat || 0; };
                props.customUpdate = function() {
                    var recs = this.block.getRecipes();
                    var i = this.getToggle();
                    var inH = 0, outH = 0;
                    if (i >= 0 && i < recs.length) {
                        inH = recs[i].input.heat;
                        outH = recs[i].output.heat;
                    }
                    if (heat.isHeatSink && inH > 0) {
                        if (this._sideHeat == null) this._sideHeat = java.lang.reflect.Array.newInstance(java.lang.Float.TYPE, 4);
                        this._heat = this.calculateHeat(this._sideHeat);
                    } else if (heat.isHeatSource && outH > 0) {
                        var hOut = outH * this.efficiency;
                        this._heat = Mathf.approachDelta(this._heat || 0, hOut, (heat.warmupRate != null ? heat.warmupRate : 0.15) * this.delta());
                    }
                };
                if (heat.isHeatSource) {
                    props.heatFrac = function() {
                        var recs = this.block.getRecipes();
                        var i = this.getToggle();
                        var max = (i >= 0 && i < recs.length) ? recs[i].output.heat : 0;
                        return max > 0 ? (this._heat || 0) / max : 0;
                    };
                }
                if (heat.isHeatSink) {
                    props.calculateHeat = function(sideHeat) {
                        if (sideHeat == null) sideHeat = this._sideHeat;
                        if (sideHeat == null) return 0;
                        for (var i = 0; i < 4; i++) sideHeat[i] = 0;
                        var sum = 0;
                        var prox = this.proximity;
                        for (var pi = 0; pi < prox.size; pi++) {
                            var b = prox.get(pi);
                            if (b != null && b.team == this.team && b instanceof HeatBlock) {
                                var h = typeof b.heat == "function" ? b.heat() : (b.heat || 0);
                                if (h > 0) {
                                    var dir = this.relativeTo(b);
                                    if (dir >= 0 && dir < 4) {
                                        sideHeat[dir] += h;
                                    } else {
                                        sideHeat[0] += h;
                                    }
                                    sum += h;
                                }
                            }
                        }
                        return sum;
                    };
                    props.sideHeat = function() {
                        if (this._sideHeat == null) this._sideHeat = java.lang.reflect.Array.newInstance(java.lang.Float.TYPE, 4);
                        return this._sideHeat;
                    };
                    props.heatRequirement = function() {
                        var recs = this.block.getRecipes();
                        var i = this.getToggle();
                        var rHeat = (i >= 0 && i < recs.length) ? recs[i].input.heat : 0;
                        // 配方不耗热且未显式配置全局热量需求时，热量需求为 0，避免卡死无热量配方
                        return rHeat > 0 ? rHeat : (heat.heatRequirement != null ? heat.heatRequirement : (heat.heatOutput != null ? heat.heatOutput : 0));
                    };
                    props.efficiencyScale = function() {
                        var recs = this.block.getRecipes();
                        var i = this.getToggle();
                        // 优先用当前配方的 maxEfficiency，其次方块级 heat 参数，最后默认 4
                        var rMe = (i >= 0 && i < recs.length) ? recs[i].maxEfficiency : 0;
                        var cap = rMe > 0 ? rMe : (heat.maxEfficiency != null ? heat.maxEfficiency : 4);
                        var req = this.heatRequirement();
                        if (req <= 0) return 1;
                        var h = this._heat || 0;
                        var over = Math.max(h - req, 0);
                        return Math.min(Mathf.clamp(h / req) + (over / req) * (heat.overheatScale != null ? heat.overheatScale : 1), cap);
                    };
                    props.shouldConsume = function() {
                        var req = this.heatRequirement();
                        return (req <= 0 || (this._heat || 0) > 0) && this.canConsume();
                    };
                    props.warmupTarget = function() {
                        var req = this.heatRequirement();
                        return req <= 0 ? 1 : Mathf.clamp((this._heat || 0) / req);
                    };
                    var baseGPA = props.getProgressIncreaseA;
                    props.getProgressIncreaseA = function(i, baseTime) {
                        var base = baseGPA.call(this, i, baseTime);
                        var recs = this.block.getRecipes();
                        var inH = (i >= 0 && i < recs.length) ? recs[i].input.heat : 0;
                        return inH > 0 ? base * this.efficiencyScale() : base;
                    };
                    var basePP = props.getPowerProduction;
                    props.getPowerProduction = function() {
                        var base = basePP.call(this);
                        var recs = this.block.getRecipes();
                        var i = this.getToggle();
                        var inH = (i >= 0 && i < recs.length) ? recs[i].input.heat : 0;
                        return inH > 0 ? base * this.efficiencyScale() : base;
                    };
                }
                var baseWrite = props.write;
                props.write = function(w) {
                    if (typeof baseWrite === "function") baseWrite.call(this, w);
                    w.f(this._heat || 0);
                };
                var baseRead = props.read;
                props.read = function(r, revision) {
                    if (typeof baseRead === "function") baseRead.call(this, r, revision);
                    this._heat = r.f();
                };
            }
            if (heatIfaces.length > 0) {
                var heatInst = JavaAdapter.apply(null, [EntityType].concat(heatIfaces).concat([props, multi]));
                for (var fi in props) {
                    if (typeof props[fi] != "function") heatInst[fi] = props[fi];
                }
                return heatInst;
            }
            return extend(EntityType, multi, props);
        };
        // 只有一个配方时不需要配方选择界面，点击方块不做任何事
        multi.configurable = recipes.length > 1;
        multi.hasItems = true;
        multi.hasLiquids = true;
        multi.hasPower = false;
        multi.tmpRecs = recipes;
        multi.saveConfig = true;
        return multi;
    }
}