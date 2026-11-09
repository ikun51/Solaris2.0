package solaris2.industry;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import mindustry.content.*;
import mindustry.entities.effect.*;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.*;
import mindustry.world.meta.BuildVisibility;
import solaris2.SolarisContent;

/**
 * 合金编织器：将铀和钼编织成超合金。
 * 由 scripts/建筑/工厂/合金编织器.js 与 content/blocks/工厂/合金编织器.json 移植而来。
 */
public class AlloyWeaver extends GenericCrafter{

    private static final Color itemColor = Color.valueOf("9B9DCFFF");

    public AlloyWeaver(String name){
        super(name);

        description = "将铀和钼编织成超合金。";
        health = 130;
        size = 3;
        hasItems = true;
        hasLiquids = true;
        itemCapacity = 50;
        liquidCapacity = 30f;
        craftTime = 260f;
        squareSprite = true;
        outputItem = new ItemStack(SolarisContent.item("铀钼合金"), 2);
        outputLiquids = LiquidStack.with(Liquids.hydrogen, 0.3f);
        craftEffect = new SoundEffect(Sounds.none, new WrapEffect(Fx.dynamicSpikes, Color.valueOf("FFD59EFF"), 20f));
        ambientSound = Sounds.loopTech;
        ambientSoundVolume = 1f;
        buildVisibility = BuildVisibility.shown;
        hasPower = true;
        consumePower(4f);
        consumeItems(ItemStack.with(
                SolarisContent.item("铀"), 4, SolarisContent.item("钼"), 6, Items.copper, 10));
        consumeLiquid(SolarisContent.liquid("合成氨"), 0.5f);
        requirements(Category.crafting, ItemStack.with(
                Items.tungsten, 130, SolarisContent.item("钢"), 150,
                SolarisContent.item("硅铁"), 85, SolarisContent.item("气凝胶"), 80,
                Items.copper, 100));
        drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawDefault(),
                new DrawGlowRegion("-glow"){{
                    blending = Blending.additive;
                    color = Color.valueOf("9B9DCFFF");
                    alpha = 0.9f;
                }},
                new DrawLiquidRegion(SolarisContent.liquid("合成氨")){{
                    suffix = "-liquid";
                }});
        buildType = AlloyWeaverBuild::new;
    }

    public class AlloyWeaverBuild extends GenericCrafterBuild{

        @Override
        public void draw(){
            DrawBlock[] drawers = ((DrawMulti)((GenericCrafter)block).drawer).drawers;
            float total = totalProgress;

            // 0. JSON 第一层：底座
            drawers[0].draw(this);

            // 1. 旋转层（随进度旋转）
            Draw.rect(Core.atlas.find(block.name + "-s"), x, y, total * 3f);

            // 2. 物品层（透明度随进度）
            Draw.color(itemColor);
            Draw.alpha(progress);
            Draw.rect(Core.atlas.find(block.name + "-item"), x, y);
            Draw.color();

            // 3. 摆动线条（暖机时）
            float craft = ((GenericCrafter)block).craftTime;
            Draw.color(Pal.accent);
            Draw.alpha(warmup);
            float sin = Mathf.sin(total, craft / 4f, 7f);
            for(int i = -1; i <= 1; i++){
                if(i == 0) continue;
                Lines.lineAngleCenter(x + i * sin, y, 90f, 12f);
                Lines.lineAngleCenter(x, y + i * sin, 0f, 12f);
            }
            Draw.reset();

            // 4. 垂直摆动线
            Draw.color(Pal.accent);
            Draw.alpha(warmup);
            Lines.lineAngleCenter(x + Mathf.sin(total, 5f, 7f), y, 90f, block.size * 8f - 4f);
            Draw.reset();

            // 5. JSON 其余层：本体 / 发光 / 液体
            for(int j = 1; j < drawers.length; j++){
                drawers[j].draw(this);
            }
        }
    }
}
