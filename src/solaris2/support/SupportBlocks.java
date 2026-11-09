package solaris2.support;

import arc.graphics.Color;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import solaris2.SolarisContent;

/** 辅助类方块：冷冻力场、超频投影。 */
public class SupportBlocks{

    public static CryoField cryoField;
    public static OverdriveProjection overdriveProjection;

    public static void load(){
        // 冷冻力场
        cryoField = new CryoField("冷冻力场");
        cryoField.description = "投射低温力场，使大范围内的火焰难以燃烧，使范围内的单位冻结";
        cryoField.health = 320;
        cryoField.size = 3;
        cryoField.requirements(Category.effect,
                ItemStack.with(Items.tungsten, 50, Items.silicon, 30,
                        SolarisContent.item("硅铁"), 50, SolarisContent.item("铝"), 80,
                        SolarisContent.item("铀钼合金"), 20));
        research(cryoField, "force-projector");

        // 超频投影
        overdriveProjection = new OverdriveProjection("超频投影");
        overdriveProjection.description = "通过微观反应能量在一定范围内提供超速场，需要氢气。使用能量电池可以提升加速倍率。";
        overdriveProjection.health = 1040;
        overdriveProjection.size = 3;
        overdriveProjection.reload = 90f;
        overdriveProjection.squareSprite = false;
        overdriveProjection.itemCapacity = 3;
        overdriveProjection.phaseRangeBoost = 0f;
        overdriveProjection.speedBoostPhase = 0.9f;
        overdriveProjection.speedBoost = 1.5f;
        overdriveProjection.hasBoost = true;
        overdriveProjection.consumePower(6f);
        overdriveProjection.consumeLiquid(Liquids.cyanogen, 0.07f);
        overdriveProjection.consumeItems(ItemStack.with(SolarisContent.item("能量电池"), 1)).optional(true, true);
        overdriveProjection.destroyBullet = new BasicBulletType(){{
            damage = 180f;
            absorbable = false;
            instantDisappear = true;
            hitEffect = Fx.none;
            despawnEffect = new MultiEffect(
                    new WaveEffect(){{
                        lifetime = 10f;
                        sizeFrom = 0f;
                        sizeTo = 60f;
                        strokeFrom = 9f;
                        strokeTo = 0f;
                        colorFrom = Color.valueOf("EEC591FF");
                        colorTo = Color.valueOf("EEC591FF");
                    }});
            hitSound = mindustry.Vars.tree.loadSound("explosionbig");
            width = 125f;
            height = 125f;
            backColor = Color.valueOf("EEC591");
            frontColor = Color.valueOf("EEC591");
            sprite = "索拉里斯2.0-六边形";
            spin = 0.88f;
            speed = 0f;
            lifetime = 0f;
            splashDamageRadius = 60f;
            splashDamage = 200f;
            lightningDamage = 23f;
            lightning = 8;
            lightningLength = 20;
            hitShake = 10f;
            fragLifeMin = 0.3f;
            fragBullets = 30;
            fragBullet = new PointBulletType(){{
                trailEffect = new ParticleEffect(){{
                    particles = 1;
                    lifetime = 6f;
                    line = true;
                    randLength = false;
                    length = 0f;
                    baseLength = 1f;
                    lenFrom = 7f;
                    lenTo = 7f;
                    strokeFrom = 3f;
                    strokeTo = 0f;
                    colorFrom = Color.valueOf("FEEBB3FF");
                    colorTo = Color.valueOf("FEEBB3FF");
                    cone = 0f;
                }};
                trailSpacing = 6f;
                hitEffect = new MultiEffect(
                        new ParticleEffect(){{
                            particles = 4;
                            baseLength = 0f;
                            length = 16f;
                            region = "索拉里斯2.0-菱形";
                            lifetime = 23f;
                            sizeFrom = 4f;
                            sizeTo = 0f;
                            colorFrom = Color.valueOf("EEC591");
                            colorTo = Color.valueOf("EEC591");
                            cone = 360f;
                        }},
                        new ParticleEffect(){{
                            particles = 1;
                            baseLength = 0f;
                            length = 0f;
                            lifetime = 25f;
                            sizeFrom = 8f;
                            sizeTo = 0f;
                            colorFrom = Color.valueOf("EEC591");
                            colorTo = Color.valueOf("EEC591");
                            cone = 360f;
                        }});
                despawnEffect = new WaveEffect(){{
                    lifetime = 15f;
                    sizeFrom = 0f;
                    sizeTo = 20f;
                    strokeFrom = 3f;
                    strokeTo = 0f;
                    colorFrom = Color.valueOf("EEC591");
                    colorTo = Color.valueOf("EEC591");
                }};
                hitSound = Sounds.explosion;
                splashDamageRadius = 20f;
                splashDamage = 35f;
                lifetime = 20f;
                speed = 8f;
                status = StatusEffects.shocked;
                statusDuration = 10f;
            }};
        }};
        overdriveProjection.requirements(Category.effect,
                ItemStack.with(Items.silicon, 100, SolarisContent.item("芯片"), 10,
                        SolarisContent.item("磁钢"), 80, SolarisContent.item("U-7X合金"), 60,
                        Items.tungsten, 90, SolarisContent.item("银"), 35));
        research(overdriveProjection, "regen-projector");
    }

    private static void research(Block block, String parentName){
        research(block, parentName, block.researchRequirements());
    }

    private static void research(Block block, String parentName, ItemStack[] requirements){
        Block parent = SolarisContent.block(parentName);
        if(parent == null) return;
        TechTree.TechNode parentNode = TechTree.all.find(t -> t.content == parent);
        if(parentNode == null) return;
        TechTree.TechNode node = new TechTree.TechNode(null, block, requirements);
        node.parent = parentNode;
        parentNode.children.add(node);
    }
}
