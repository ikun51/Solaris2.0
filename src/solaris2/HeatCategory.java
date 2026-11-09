package solaris2;

import arc.*;
import arc.graphics.g2d.*;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.*;
import java.lang.reflect.*;
import java.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.gen.Icon;
import mindustry.type.*;
import mindustry.world.*;
import sun.misc.Unsafe;

/**
 * 由 scripts/lib/HeatCategory.js 移植而来。
 * 通过 Unsafe 向 Category 枚举注入 heat 分类，并在内容加载完成后归类热量建筑。
 */
public class HeatCategory{

    public static boolean available = false;
    public static Category heatCategory;

    public static final String[] heatBlockNames = {
        "索拉里斯2.0-铝热反应炉",
        "索拉里斯2.0-电热器",
        "索拉里斯2.0-热管"
    };

    private static Unsafe getUnsafe(){
        try{
            Class<?> c = Class.forName("sun.misc.Unsafe");
            try{
                Field f = c.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return (Unsafe)f.get(null);
            }catch(Throwable e1){
                Field[] fields = c.getDeclaredFields();
                for(Field field : fields){
                    if(Modifier.isStatic(field.getModifiers())){
                        try{
                            field.setAccessible(true);
                            Object v = field.get(null);
                            if(v != null) return (Unsafe)v;
                        }catch(Throwable e2){
                        }
                    }
                }
            }
        }catch(Throwable e){
        }
        return null;
    }

    public static void install(){
        try{
            Field valuesField = Category.class.getDeclaredField("$VALUES");
            valuesField.setAccessible(true);
            Category[] oldArr = (Category[])valuesField.get(null);

            for(Category value : oldArr){
                if(value.name().equals("heat")){
                    available = true;
                    return;
                }
            }

            Unsafe unsafe = getUnsafe();
            if(unsafe == null) throw new RuntimeException("无法获取 sun.misc.Unsafe");
            Category heat = (Category)unsafe.allocateInstance(Category.class);

            Field nameField = Enum.class.getDeclaredField("name");
            unsafe.putObject(heat, unsafe.objectFieldOffset(nameField), "heat");
            Field ordinalField = Enum.class.getDeclaredField("ordinal");
            unsafe.putInt(heat, unsafe.objectFieldOffset(ordinalField), oldArr.length);

            Category[] newArr = Arrays.copyOf(oldArr, oldArr.length + 1);
            newArr[oldArr.length] = heat;
            unsafe.putObject(unsafe.staticFieldBase(valuesField), unsafe.staticFieldOffset(valuesField), newArr);

            Field allField = Category.class.getDeclaredField("all");
            allField.setAccessible(true);
            unsafe.putObject(unsafe.staticFieldBase(allField), unsafe.staticFieldOffset(allField), newArr);

            try{
                ObjectMap<String, TextureRegionDrawable> icons = Icon.icons;
                if(icons != null && icons.containsKey("heat") && icons.get("heat") == null){
                    icons.remove("heat");
                }
            }catch(Throwable e){
            }

            heatCategory = heat;
            available = true;

            try{
                Events.on(ClientLoadEvent.class, e -> {
                    try{
                        ObjectMap<String, TextureRegionDrawable> icons = Icon.icons;
                        if(icons != null && icons.get("heat") == null){
                            TextureRegionDrawable d = Core.atlas.getDrawable("heat");
                            if(d != null) icons.put("heat", d);
                        }
                    }catch(Throwable ec){
                    }
                });
            }catch(Throwable ec){
            }
        }catch(Throwable e){
            available = false;
        }

        try{
            Events.on(ModContentLoadEvent.class, e -> {
                if(!available) return;
                try{
                    for(String name : heatBlockNames){
                        Content c = Vars.content.getByName(ContentType.block, name);
                        if(c instanceof Block b) b.category = heatCategory;
                    }
                }catch(Throwable ec){
                }
            });
        }catch(Throwable ec){
        }
    }
}
