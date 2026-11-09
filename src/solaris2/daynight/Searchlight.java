package solaris2.daynight;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;

/**
 * 探照灯：通电后在夜晚照亮一片区域，圈内的本模组炮台恢复白天射程。
 */
public class Searchlight extends Block{
    /** 当前世界里已建成的探照灯，交给 DayNight 判定与绘制。 */
    public static final Seq<Building> lights = new Seq<>();

    /** 照明半径（世界单位，8 单位 = 1 格，168 约等于 21 格）。 */
    public float radius = 168f;

    public Searchlight(String name){
        super(name);
        solid = true;
        update = true;
        sync = true;
        hasPower = true;
        consumePower(3f);
        requirements(Category.defense, ItemStack.with(Items.silicon, 40, Items.lead, 30));
        buildType = SearchlightBuild::new;
    }

    public class SearchlightBuild extends Building{
        private boolean registered;

        @Override
        public void updateTile(){
            if(!registered){
                registered = true;
                lights.add(this);
            }
        }

        @Override
        public void onDestroyed(){
            lights.remove(this);
            super.onDestroyed();
        }

        @Override
        public void onRemoved(){
            lights.remove(this);
            super.onRemoved();
        }
    }
}
