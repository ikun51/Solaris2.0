package solaris2;

import mindustry.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;

/**
 * 内容查找工具。
 * 脚本内容与 Java 内容都会被加上 "<模组名>-" 前缀，
 * 这里统一按 “模组名 + 中文名” 查找，避免各处硬编码前缀。
 */
public class SolarisContent{

    public static final String MOD_NAME = "索拉里斯2.0";

    public static Item item(String name){
        return (Item)get(ContentType.item, name);
    }

    public static Liquid liquid(String name){
        return (Liquid)get(ContentType.liquid, name);
    }

    public static UnitType unit(String name){
        return (UnitType)get(ContentType.unit, name);
    }

    public static Weather weather(String name){
        return (Weather)get(ContentType.weather, name);
    }

    public static Block block(String name){
        return (Block)get(ContentType.block, name);
    }

    public static StatusEffect status(String name){
        return (StatusEffect)get(ContentType.status, name);
    }

    public static Content get(ContentType type, String name){
        Content c = Vars.content.getByName(type, MOD_NAME + "-" + name);
        if(c == null) c = Vars.content.getByName(type, name);
        return c;
    }
}
