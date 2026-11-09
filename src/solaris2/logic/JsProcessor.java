package solaris2.logic;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * JS 处理器：编辑器专用的世界处理器级方块。
 * 没有图形化逻辑块，配置界面是一个纯文本 JS 编辑器，
 * 代码用 Mindustry 自带的 Rhino 引擎编译，每 tick 执行一次。
 * 权限与世界处理器一致（privileged，可跨队操作、setblock 等）。
 */
public class JsProcessor extends Block{

    public JsProcessor(String name){
        super(name);
        size = 1;
        update = true;
        solid = true;
        destructible = false;
        targetable = false;
        privileged = true;
        configurable = true;
        health = 2000;
        category = Category.logic;
        requirements(Category.logic, BuildVisibility.worldProcessorOnly, ItemStack.with());
        researchCost = ItemStack.with();

        buildType = JsProcessorBuild::new;
    }

    public class JsProcessorBuild extends Building{
        /** 玩家编写的 JS 源码 */
        public String code = "";
        /** 编译后的全局函数名（按建筑 id 唯一） */
        protected String fname;
        /** 是否编译成功 */
        protected boolean compiled;
        /** 最近一次编译/运行错误 */
        protected String error;
        /** 热加载贴图路径（随存档保存） */
        protected String texPath;
        protected TextureRegion tex;
        /** 读档后延迟到 update 再加载贴图，避免在非 GL 线程创建纹理 */
        protected boolean texDirty;
        /** 上次点击"选择图片"的时间，用于防连点 */
        protected long lastPick;

        @Override
        public void created(){
            super.created();
            fname = "__solaris_jsproc_" + id;
        }

        /** 编译代码为全局函数；返回是否成功 */
        public boolean compile(){
            if(fname == null) fname = "__solaris_jsproc_" + id;
            if(code == null || code.isEmpty()){
                compiled = false;
                error = null;
                return false;
            }
            try{
                Scripts s = mods.getScripts();
                s.context.evaluateString(s.scope,
                        fname + " = function(){\n" + code + "\n};",
                        "JS处理器", 1);
                compiled = true;
                error = null;
                return true;
            }catch(Throwable t){
                compiled = false;
                error = t.getClass().getSimpleName() + ": " + t.getMessage();
                return false;
            }
        }

        @Override
        public void update(){
            if(texDirty){
                loadTexture(texPath);
                texDirty = false;
            }

            //读档后 code 还没编译过，这里补一次
            if(!compiled && error == null && code != null && !code.isEmpty()){
                compile();
            }

            if(!compiled) return;
            //与世界处理器一致：服务器规则禁用世界处理器时不执行
            if(state.rules.disableWorldProcessors && !state.rules.editor) return;

            Scripts s = mods.getScripts();
            try{
                s.context.evaluateString(s.scope, fname + "()", "JS处理器-run", 1);
            }catch(Throwable t){
                //运行报错自动停用，避免每 tick 刷异常卡死游戏
                compiled = false;
                error = t.getClass().getSimpleName() + ": " + t.getMessage();
            }
        }

        /** 从绝对路径热加载贴图，失败则回退默认方块贴图 */
        public void loadTexture(String path){
            tex = null;
            if(path == null || path.isEmpty()) return;
            try{
                Fi f = Core.files.absolute(path);
                if(!f.exists()) return;
                Pixmap p = new Pixmap(f);
                tex = new TextureRegion(new Texture(p));
            }catch(Throwable ignored){
            }
        }

        /** 是否允许打开编辑器：与世界处理器一致，仅编辑器/试玩 */
        public boolean accessible(){
            return state.rules.editor || state.playtestingMap != null || state.rules.allowEditWorldProcessors;
        }

        @Override
        public boolean shouldShowConfigure(Player player){
            return accessible();
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.pencil, Styles.cleari, this::showCodeDialog).size(40);
        }

        public void showCodeDialog(){
            BaseDialog d = new BaseDialog("JS 代码编辑器");
            d.addCloseListener();

            TextArea area = new TextArea(code == null ? "" : code);
            area.setPrefRows(18);

            ScrollPane pane = d.cont.pane(area).size(620f, 340f).get();
            pane.setScrollingDisabled(false, false);
            d.cont.row();
            Label status = d.cont.add(error != null ? error : (compiled ? "编译成功，运行中" : "未编译")).left().color(Color.gray).get();
            d.cont.row();
            d.cont.button("编译并保存", () -> {
                code = area.getText().toString();
                if(compile()){
                    status.setText("编译成功，运行中");
                    status.setColor(Color.green);
                }else{
                    status.setText("编译失败: " + error);
                    status.setColor(Color.red);
                }
            }).size(150f, 44f);
            d.cont.button("选择图片", () -> {
                //防止连点：每次点击都会弹一个系统文件窗口，堆叠过多会卡住
                if(System.currentTimeMillis() - lastPick < 2000L){
                    ui.showInfoToast("文件窗口已打开，请勿连续点击", 2f);
                    return;
                }
                lastPick = System.currentTimeMillis();
                FileChooser.open("png", "jpg", "jpeg").submit(f -> {
                    texPath = f.absolutePath();
                    loadTexture(texPath);
                    texDirty = false;
                });
            }).size(150f, 44f).tooltip("选择本地图片作为建筑贴图，请勿连续点击");
            d.cont.button("清除图片", () -> {
                texPath = null;
                tex = null;
            }).size(150f, 44f);
            d.cont.button("@cancel", d::hide).size(120f, 44f);

            d.show();
        }

        @Override
        public void draw(){
            Draw.z(Layer.block);
            if(tex != null){
                Draw.rect(tex, x, y);
            }else{
                Draw.rect(block.name, x, y);
            }
            
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.str(code == null ? "" : code);
            write.str(texPath == null ? "" : texPath);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            code = read.str();
            texPath = read.str();
            compiled = false;
            error = null;
            texDirty = true;
        }
    }
}
