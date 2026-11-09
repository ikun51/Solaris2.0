// 物品索引 - 显示物品的生产/用途配方

function getByName(name, config, contentType){
    if (config == 0){
        var k = Vars.content.getByName(ContentType.block, name);
        if (k != null){
            return k;
        }
        k = Vars.content.getByName(ContentType.item, name);
        if (k != null){
            return k;
        }
        k = Vars.content.getByName(ContentType.liquid, name);
        return k;
    }
    if (config == 1){
        if (contentType == "block"){
            return Vars.content.getByName(ContentType.block, name);
        }
        if (contentType == "item"){
            return Vars.content.getByName(ContentType.item, name);
        }
        if (contentType == "liquid"){
            return Vars.content.getByName(ContentType.liquid, name);
        }
    }
    if (config == 2){
        var k = Vars.content.getByName(ContentType.block, name);
        if (k != null && contentType != "block"){
            return k;
        }
        k = Vars.content.getByName(ContentType.item, name);
        if (k != null && contentType != "item"){
            return k;
        }
        k = Vars.content.getByName(ContentType.liquid, name);
        return k;
    }
}

function typeByName(name){
    var k = Vars.content.getByName(ContentType.item, name);
    if (k != null){
        return "item";
    }
    k = Vars.content.getByName(ContentType.liquid, name);
    if (k != null){
        return "liquid";
    }
    return "block";
}

// 收集所有 MultiCrafter 的配方
function parseLibraryRecipes(block, allRecipes){
    var recipes = block.getRecipes();
    for (var i = 0; i < recipes.length; i++){
        var recipe = recipes[i];
        var recipeData = {
            input: [],
            output: [],
            area: block.name,
            config: []
        };
        
        // 处理输入物品
        if (recipe.input && recipe.input.items){
            for (var j = 0; j < recipe.input.items.length; j++){
                var stack = recipe.input.items[j];
                // stack 是 ItemStack: item, amount
                var fullName = stack.item.name;
                recipeData.input.push(fullName);
                recipeData.input.push(stack.amount);
            }
        }
        // 处理输入液体
        if (recipe.input && recipe.input.liquids){
            for (var j = 0; j < recipe.input.liquids.length; j++){
                var stack = recipe.input.liquids[j];
                var fullName = stack.liquid.name;
                recipeData.input.push(fullName);
                recipeData.input.push(stack.amount * 60);
            }
        }
        // 输入热量
        if (recipe.input && recipe.input.heat && recipe.input.heat > 0){
            recipeData.input.push("heat");
            recipeData.input.push(recipe.input.heat);
        }
        // 输入电力
        if (recipe.input && recipe.input.power && recipe.input.power > 0){
            recipeData.input.push("power");
            recipeData.input.push(recipe.input.power);
        }
        
        // 处理输出物品
        if (recipe.output && recipe.output.items){
            for (var j = 0; j < recipe.output.items.length; j++){
                var stack = recipe.output.items[j];
                var fullName = stack.item.name;
                recipeData.output.push(fullName);
                recipeData.output.push(stack.amount);
            }
        }
        // 处理输出液体
        if (recipe.output && recipe.output.liquids){
            for (var j = 0; j < recipe.output.liquids.length; j++){
                var stack = recipe.output.liquids[j];
                var fullName = stack.liquid.name;
                recipeData.output.push(fullName);
                recipeData.output.push(stack.amount * 60);
            }
        }
        // 处理产物（带概率）
        if (recipe.output && recipe.output.result){
            for (var j = 0; j < recipe.output.result.length; j++){
                var result = recipe.output.result[j];
                if (result.item == null) continue; // 液体概率产物无item字段，跳过
                var fullName = result.item.name;
                recipeData.output.push(fullName);
                recipeData.output.push(result.amount);
            }
        }
        // 输出热量
        if (recipe.output && recipe.output.heat && recipe.output.heat > 0){
            recipeData.output.push("heat");
            recipeData.output.push(recipe.output.heat);
        }
        // 输出电力
        if (recipe.output && recipe.output.power && recipe.output.power > 0){
            recipeData.output.push("power");
            recipeData.output.push(recipe.output.power);
        }
        
        allRecipes.push(recipeData);
    }
}

// 通用扫描
function parseGenericRecipes(block, allRecipes){
    var inputs = [];
    var outputs = [];
    var consumers = block.consumers;
    if (consumers != null){
        for (var j = 0; j < consumers.length; j++){
            var c = consumers[j];
            if (c == null) continue;
            // 多物品消耗
            if (c.items != null){
                for (var i = 0; i < c.items.length; i++){
                    var it = c.items[i];
                    inputs.push(it.item.name);
                    inputs.push(it.amount);
                }
            }
            // 多液体消耗
            if (c.liquids != null){
                for (var i = 0; i < c.liquids.length; i++){
                    var li = c.liquids[i];
                    inputs.push(li.liquid.name);
                    inputs.push(li.amount * 60);
                }
            }
            // 单液体消耗
            if (c.liquid != null){
                inputs.push(c.liquid.name);
                inputs.push(c.amount * 60);
            }
        }
    }
    // 热量需求(HeatCrafter等)
    if (block.heatRequirement != null && block.heatRequirement > 0){
        inputs.push("heat");
        inputs.push(block.heatRequirement);
    }
    // 物品产物
    if (block.outputItems != null){
        for (var i = 0; i < block.outputItems.length; i++){
            var it = block.outputItems[i];
            outputs.push(it.item.name);
            outputs.push(it.amount);
        }
    }
    // 概率产物(Separator等)
    if (block.results != null){
        for (var i = 0; i < block.results.length; i++){
            var it = block.results[i];
            outputs.push(it.item.name);
            outputs.push(it.amount);
        }
    }
    // 液体产物
    if (block.outputLiquids != null){
        for (var i = 0; i < block.outputLiquids.length; i++){
            var li = block.outputLiquids[i];
            outputs.push(li.liquid.name);
            outputs.push(li.amount * 60);
        }
    } else if (block.outputLiquid != null){
        outputs.push(block.outputLiquid.liquid.name);
        outputs.push(block.outputLiquid.amount * 60);
    }
    // 电力/热量产出
    if (block.powerProduction > 0){
        outputs.push("power");
        outputs.push(block.powerProduction);
    }
    if (block.heatOutput > 0){
        outputs.push("heat");
        outputs.push(block.heatOutput);
    }
    if (inputs.length > 0 || outputs.length > 0){
        allRecipes.push({input: inputs, output: outputs, area: block.name, config: []});
    }
}

// 单独适配：索拉里斯的 PWR 核反应堆
// 它的输入输出全是自定义逻辑（acceptItem/acceptLiquid/dumpSteamOutput，没有标准 consumer
// 和 outputItem 字段），通用扫描读不到。这里按配置手动还原成
// 燃料棒 + 冷却剂 + 电力 → 蒸汽 + 乏燃料 的配方喵~
function parsePWRRecipes(block, allRecipes){
    var fuelRods = block.getFuelRods();
    var cfg = block.getSteamConfig();
    if (fuelRods == null || cfg == null) return;
    var waterName = cfg.SecCoolant == null ? null : cfg.SecCoolant.name;
    var steamName = cfg.SteamLiquid == null ? null : cfg.SteamLiquid.name;
    var needPower = block.getModel() != null && block.getModel().needPower;
    for (var i = 0; i < fuelRods.length; i++){
        var rod = fuelRods[i];
        var recipeData = {input: [], output: [], area: block.name, config: []};
        // 输入：燃料棒
        if (rod.FuelRodSupport){
            recipeData.input.push(rod.FuelRodSupport);
            recipeData.input.push(1);
        }
        // 输入：二回路冷却剂（水）
        if (waterName){
            recipeData.input.push(waterName);
            recipeData.input.push(120 * 60);
        }
        // 输入：运行电力（维持循环泵）
        if (needPower){
            recipeData.input.push("power");
            recipeData.input.push(600);
        }
        // 输出：蒸汽
        if (steamName){
            recipeData.output.push(steamName);
            recipeData.output.push(120 * 60);
        }
        // 输出：乏燃料（铀钼合金会额外产出钼）
        var spent = rod.SpentFuel;
        if (typeof spent === "string"){
            recipeData.output.push(spent);
            recipeData.output.push(1);
        } else if (spent != null && spent.length > 0){
            for (var j = 0; j < spent.length; j++){
                var s = spent[j];
                if (s != null && s.item){
                    recipeData.output.push(s.item);
                    recipeData.output.push(s.amount || 1);
                }
            }
        }
        allRecipes.push(recipeData);
    }
}

// 收集全部方块配方：自家多配方系统，其余走通用扫描，核反应堆单独适配
function collectAllRecipes(){
    var allRecipes = [];
    Vars.content.blocks().each(function(block){
        if (typeof block.getRecipes === "function"){
            parseLibraryRecipes(block, allRecipes);
        } else if (typeof block.getFuelRods === "function"){
            parsePWRRecipes(block, allRecipes);
        } else {
            parseGenericRecipes(block, allRecipes);
        }
    });
    return allRecipes;
}

function lookForRecipe(target, allRecipes){
    var aim = target.name;
    var findInput = [];
    var findOutput = [];
    var recipe;
    var input;
    var output;
    
    for (var i = 0; i < allRecipes.length; i++){
        recipe = allRecipes[i];
        input = recipe.input;
        output = recipe.output;
        
        var hasIn = false;
        for (var j = 0; j < input.length; j += 2){
            if (input[j] == aim){
                hasIn = true;
                break;
            }
        }
        if (hasIn){
            findInput.push(recipe);
        }
        
        var hasOut = false;
        for (var j = 0; j < output.length; j += 2){
            if (output[j] == aim){
                hasOut = true;
                break;
            }
        }
        if (hasOut){
            findOutput.push(recipe);
        }
    }
    
    return [findInput, findOutput];
}

function display(recipe, table){
    var input = recipe.input;
    var output = recipe.output;
    var area = recipe.area;
    var config = recipe.config;
    var dis;
    var amo;
    
    for (var i = 0; i < input.length / 2; i++){
        var name = input[i * 2];
        amo = input[(i * 2) + 1];
        if (name == "heat"){
            table.add(Image(Icon.wavesSmall)).padRight(4);
            table.add("[orange]" + amo).padRight(8);
        } else if (name == "power"){
            amo = Math.floor(amo * 60);
            table.add(Image(Icon.power)).padRight(4);
            table.add("[accent]" + amo).padRight(8);
        } else {
            dis = getByName(name, 2, "block");
            if (dis != null){
                if (dis.contentType == ContentType.item){
                    table.add(StatValues.displayItem(dis, amo, true)).padRight(8);
                } else if (dis.contentType == ContentType.liquid){
                    table.add(StatValues.displayLiquid(dis, amo, true)).padRight(8);
                }
            }
        }
    }
    table.add(" ").padRight(4);
    table.add(Image(Icon.right)).padRight(4);
    table.add(" ").padRight(4);
    for (var i = 0; i < output.length / 2; i++){
        var name = output[i * 2];
        amo = output[(i * 2) + 1];
        if (name == "heat"){
            table.add(Image(Icon.wavesSmall)).padRight(4);
            table.add("[orange]" + amo).padRight(8);
        } else if (name == "power"){
            amo = Math.floor(amo * 60);
            table.add(Image(Icon.power)).padRight(4);
            table.add("[accent]" + amo).padRight(8);
        } else {
            dis = getByName(name, 2, "block");
            if (dis != null){
                if (dis.contentType == ContentType.item){
                    table.add(StatValues.displayItem(dis, amo, true)).padRight(8);
                } else if (dis.contentType == ContentType.liquid){
                    table.add(StatValues.displayLiquid(dis, amo, true)).padRight(8);
                }
            }
        }
    }
    for (var i = 0; i < config.length / 2; i++){
        // 配置项暂时不处理，预留接口喵~
    }
    table.add("  ");
    var block = getByName(area, 1, "block");
    if (block != null){
        table.add(Image(block.uiIcon).setScaling(Scaling.fit)).size(30).padRight(4);
        table.add(" " + block.localizedName);
    }
}

function execute(mat, table, type){
    var makeRow = function(idx){
        return function(part){
            part.add("[accent]" + (type == "input" ? Core.bundle.get("text-用途", "用途") : Core.bundle.get("text-生产", "生产"))).expandX().left().row();
            part.table(cons(function(row){
                display(mat[idx], row);
            })).left().row();
        };
    };
    for (var i = 0; i < mat.length; i++){
        table.table(Core.scene.getStyle(Button.ButtonStyle).up, makeRow(i))
            .color(type == "input" ? Color.valueOf("96c8fa") : Color.valueOf("ffffff"))
            .left().growX().row();
    }
}

function createStatFunction(item, allRecipes){
    return new JavaAdapter(StatValue, {
        display: function(table){
                    if (item.contentType == ContentType.item) {
                        table.add(StatValues.displayItem(item, 0, true)).padRight(8);
                    } else if (item.contentType == ContentType.liquid) {
                        table.add(StatValues.displayLiquid(item, 0, true)).padRight(8);
                    }   
            table.button("", Icon.book, run(function(){
                var dialog = new BaseDialog(Core.bundle.get("text-生产/用途", "生产/用途"));
                var getrecipe = lookForRecipe(item, allRecipes);
                dialog.cont.pane(function(t){
                    if (getrecipe[1].length > 0){
                        t.add("[accent]" + Core.bundle.get("text-生产", "生产")).left().row();
                        execute(getrecipe[1], t, "output");
                        if (getrecipe[0].length > 0){
                            t.row();
                            t.add(" ").row();
                        }
                    }
                    if (getrecipe[0].length > 0){
                        t.add("[accent]" + Core.bundle.get("text-用途", "用途")).left().row();
                        execute(getrecipe[0], t, "input");
                    }
                    if (getrecipe[0].length == 0 && getrecipe[1].length == 0){
                        t.add("[lightgray]" + Core.bundle.get("text-无配方", "没有找到相关配方")).row();
                    }
                });
                dialog.buttons.button("@close", function(){dialog.hide()}).size(400, 128);
                dialog.show();
            })).size(50, 40);
        }
    });
}

// 兜底：沿整条bundle链写入stat文案，缺bundle时标题也不会是"???stat.索拉里斯索引???"
function ensureStatTranslation(){
    try{
        var key = "stat.索拉里斯索引";
        var bundle = Core.bundle;
        while(bundle != null){
            var props = bundle.getProperties();
            if(props != null && props.get(key) == null){
                var loc = bundle.getLocale() == null ? "" : bundle.getLocale().toString();
                props.put(key, loc.indexOf("zh") == 0 ? "查看生产/用途" : "View production/usage");
            }
            bundle = bundle.getParent();
        }
    }catch(e){}
}

Events.on(EventType.ClientLoadEvent, function(){
    try{
        var allRecipes = collectAllRecipes();
        Log.info("物品索引: collectAllRecipes 完成, 配方数=" + (allRecipes == null ? "null" : allRecipes.length));

        ensureStatTranslation();

        // 160版本起说明框走 content.computeStats()，每次 new Stats() 会清掉挂载的 stat，
        // 所以改为代理信息框，在说明内容下方给物品/液体追加索引按钮。
        // 注意：Rhino 的 JavaAdapter 重写方法按名字匹配，会覆盖 show 的所有重载，
        // 必须按参数个数区分 show(UnlockableContent)、show(Scene, Action)、show() 三种情况，
        // 否则基础版内部的无参 show() 会回调回来导致死循环/报错喵~
        Vars.ui.content = new JavaAdapter(ContentInfoDialog, {
            show: function(a, b){
                if(arguments.length == 2){         // show(Scene, Action) 内部调用
                    this.super$show(a, b);
                }else if(arguments.length == 0){   // show() 内部调用
                    this.super$show();
                }else{                             // show(UnlockableContent)
                    var content = a;
                    this.super$show(content);
                    if(content instanceof Item || content instanceof Liquid){
                        this.cont.row();
                        this.cont.table(cons(function(t){
                            t.left();
                            createStatFunction(content, allRecipes).display(t);
                        })).left().padTop(6).row();
                    }
                }
            }
        });
        Log.info("物品索引: 信息框代理替换成功");
    }catch(e){
        Log.err("物品索引: 出错 " + e, e);
    }
});
