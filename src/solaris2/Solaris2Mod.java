package solaris2;

import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.mod.Mod;
import solaris2.daynight.DayNight;
import solaris2.daynight.DayNightBlocks;
import solaris2.heat.HeatBlocks;
import solaris2.logic.JsProcessor;
import solaris2.industry.IndustryBlocks;
import solaris2.liquid.LiquidBlocks;
import solaris2.orbit.OrbitBlocks;
import solaris2.power.PowerBlocks;
import solaris2.defense.WallBlocks;
import solaris2.production.ProductionBlocks;
import solaris2.storage.StorageBlocks;
import solaris2.transport.TransportBlocks;
import solaris2.turret.TurretBlocks;
import solaris2.units.UnitBlocks;

/**
 * Solaris2.0（索拉里斯 2.0）Java 主类。
 * 指定建筑改为 Java 实现，其余内容仍由 scripts 下的 JS 加载。
 */
public class Solaris2Mod extends Mod{

    public static JsProcessor jsProcessor;

    public Solaris2Mod(){

    }

    @Override
    public void loadContent(){
        //物品/液体/状态/天气/单位由 scripts/main.js 先行加载（脚本在 mod.loadContent() 之前执行），
        //这里再调用一次会重复注册同名内容并抛出异常，导致后面的 Java 建筑全部加载失败

        HeatCategory.install();
        DayNight.init();

        HeatBlocks.load();
        LiquidBlocks.load();
        IndustryBlocks.load();
        TransportBlocks.load();
        PowerBlocks.load();
        DayNightBlocks.load();
        WallBlocks.load();
        StorageBlocks.load();
        ProductionBlocks.load();
        TurretBlocks.load();
        UnitBlocks.load();
        OrbitBlocks.load();

        //JS 代码编辑器方块：世界处理器权限，仅编辑器可放置
        jsProcessor = new JsProcessor("JS处理器");
        jsProcessor.description = "编写并运行JavaScript";
    }

    @Override
    public void init(){
        //Mindustry 会给 mod 内容自动加 "索拉里斯2.0-" 前缀，这里去掉，UI 只显示名字本身
        String prefix = "索拉里斯2.0-";
        for(ContentType type : ContentType.values()){
            for(Content c : Vars.content.<Content>getBy(type)){
                if(c instanceof UnlockableContent){
                    UnlockableContent u = (UnlockableContent)c;
                    if(u.name.startsWith(prefix) && (u.localizedName == null || u.localizedName.equals(u.name))){
                        u.localizedName = u.name.substring(prefix.length());
                    }
                }
            }
        }
    }
}
