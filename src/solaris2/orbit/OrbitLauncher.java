package solaris2.orbit;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import solaris2.*;

/**
 * 由 scripts/建筑/轨道/轨道发射器.js 移植而来。
 */
public class OrbitLauncher extends Block{

    // ===== 物品 =====
    public static final Item magnetSteel = SolarisContent.item("磁钢");
    public static final Item uraniumMolybdenum = SolarisContent.item("铀钼合金");
    public static final Item siliconNitride = SolarisContent.item("氮化硅");
    public static final Item chip = SolarisContent.item("芯片");
    public static final Item energyBattery = SolarisContent.item("能量电池");
    public static final Item ferrosilicon = SolarisContent.item("硅铁");
    public static final Item u7xAlloy = SolarisContent.item("U-7X合金");

    // ===== 液体 =====
    public static final Liquid oxygen = SolarisContent.liquid("液氧");
    public static final Liquid hydrogen = SolarisContent.liquid("液氢");
    public static final Liquid cyanogen = Liquids.cyanogen;
    public static final Liquid cryofluid = Liquids.cryofluid;

    // ===== 发射需求 =====
    public static final int CYANOGEN_NEED = 600;
    public static final int CRYOFLUID_NEED = 400;
    public static final int HYDROGEN_NEED = 900;
    public static final int OXYGEN_NEED = 300;

    public static final ItemStack[] ORBITER_COST = ItemStack.with(
        magnetSteel, 1600,
        uraniumMolybdenum, 1070,
        siliconNitride, 1280,
        chip, 70,
        Items.copper, 900,
        energyBattery, 100
    );

    public static final ItemStack[] SHELL_COST = ItemStack.with(
        ferrosilicon, 50,
        u7xAlloy, 100
    );

    // ===== 参数 =====
    public static final float LAUNCH_FRAMES = 1.5f * 60f;
    public static final float FLIGHT_LIFE = 120f;
    public static final float LAUNCH_COOLDOWN = 75f * 60f;

    public static TextureRegion rocketRegion;

    public OrbitLauncher(String name){
        super(name);
        size = 3;
        health = 2000;
        armor = 10f;
        solid = true;
        update = true;
        destructible = true;
        hasLiquids = true;
        liquidCapacity = 2200f;
        hasItems = true;
        itemCapacity = 5100;
        category = Category.crafting;

        buildVisibility = BuildVisibility.shown;
        alwaysUnlocked = true;
        configurable = true;
        saveConfig = true;

        buildType = () -> new OrbitLauncherBuild();
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid");

        float capacity = liquidCapacity;
        Liquid[] liquidList = {cyanogen, cryofluid, hydrogen, oxygen};

        for(Liquid liquid : liquidList){
            addBar("液体" + liquid.name, (OrbitLauncherBuild entity) -> {
                if(entity.mode == 1 && liquid != hydrogen && liquid != oxygen) return null;
                return new Bar(
                    () -> liquid.localizedName,
                    () -> liquid.barColor == null ? liquid.color : liquid.barColor,
                    () -> Mathf.clamp(entity.liquids.get(liquid) / capacity)
                );
            });
        }
    }

    @Override
    public void load(){
        super.load();
        rocketRegion = Core.atlas.find("索拉里斯2.0-轨道打击器-launchpop");
    }

    public static void launchSound(Building entity){
        Sounds.padLaunch.at(entity.x, entity.y, 1f + Mathf.range(0.1f));
        Fx.launchPod.at(entity.x, entity.y);
        Effect.shake(3f, 3f, entity.x, entity.y);
    }

    public class OrbitLauncherBuild extends Building{
        public int mode = 0;
        public boolean launching = false;
        public float progress = 0f;
        public float cooldown = 0f;
        public boolean flying = false;
        public float flightTimer = 0f;
        public float smokeTimer = 0f;

        public int getMode(){
            return mode;
        }

        public void setMode(int value){
            if(!launching) mode = value;
        }

        public boolean isLaunching(){
            return launching;
        }

        public float getProgress(){
            return progress;
        }

        public float getCooldown(){
            return cooldown;
        }

        public int itemAmount(Item item){
            return items == null ? 0 : items.get(item);
        }

        public boolean resourcesEnough(){
            if(liquids == null || items == null) return false;

            ItemStack[] materials = mode == 1 ? SHELL_COST : ORBITER_COST;
            for(ItemStack stack : materials){
                if(items.get(stack.item) < stack.amount) return false;
            }

            if(mode == 1){
                return liquids.get(hydrogen) >= HYDROGEN_NEED
                    && liquids.get(oxygen) >= OXYGEN_NEED;
            }

            return liquids.get(cyanogen) >= CYANOGEN_NEED
                && liquids.get(cryofluid) >= CRYOFLUID_NEED
                && liquids.get(hydrogen) >= HYDROGEN_NEED
                && liquids.get(oxygen) >= OXYGEN_NEED;
        }

        public void consumeResources(){
            ItemStack[] materials = mode == 1 ? SHELL_COST : ORBITER_COST;
            for(ItemStack stack : materials){
                items.remove(stack.item, stack.amount);
            }

            if(mode == 1){
                liquids.remove(hydrogen, HYDROGEN_NEED);
                liquids.remove(oxygen, OXYGEN_NEED);
                return;
            }

            liquids.remove(cyanogen, CYANOGEN_NEED);
            liquids.remove(cryofluid, CRYOFLUID_NEED);
            liquids.remove(hydrogen, HYDROGEN_NEED);
            liquids.remove(oxygen, OXYGEN_NEED);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            // 只接收当前模式配方需要的物品，其余一律拒收
            ItemStack[] materials = mode == 1 ? SHELL_COST : ORBITER_COST;
            for(ItemStack stack : materials){
                if(stack.item == item){
                    if(items == null) return true;
                    return items.get(item) < block.itemCapacity;
                }
            }
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            if(liquid == hydrogen || liquid == cyanogen || liquid == cryofluid || liquid == oxygen){
                if(liquids == null) return true;
                return liquids.get(liquid) < block.liquidCapacity;
            }
            return false;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(cooldown > 0) cooldown -= edelta();

            if(launching){
                progress += edelta() / LAUNCH_FRAMES;
                if(progress >= 1) progress = 1;

                if(progress >= 1){
                    launching = false;
                    progress = 0f;
                    cooldown = LAUNCH_COOLDOWN;

                    if(mode == 1){
                        OrbitFireEffects.launchShell();
                    }else{
                        OrbitFireEffects.launchOrbiter();
                    }

                    launchSound(this);

                    flying = true;
                    flightTimer = 0f;
                    smokeTimer = 0f;
                }
            }else if(cooldown <= 0 && resourcesEnough()){
                consumeResources();
                launching = true;
                progress = 0f;
            }

            if(flying){
                flightTimer += edelta();
                if(flightTimer >= FLIGHT_LIFE){
                    flying = false;
                }else{
                    smokeTimer += edelta();
                    if(smokeTimer >= 4 - payloadT() * 2){
                        smokeTimer = 0f;
                        Fx.rocketSmoke.at(payloadX() + Mathf.range(3f), payloadY() + Mathf.range(3f), payloadT());
                    }
                }
            }
        }

        @Override
        public void draw(){
            super.draw();

            Drawf.shadow(x, y, block.size * 8);

            if(rocketRegion != null && rocketRegion.found()){
                Draw.rect(rocketRegion, x, y);
            }

            Draw.reset();

            if(flying) drawPayload();
        }

        public float payloadT(){
            return Mathf.clamp(flightTimer / FLIGHT_LIFE);
        }

        public float payloadX(){
            return x + Interp.pow2In.apply(payloadT()) * (12 + Mathf.randomSeedRange(pos() + 3, 4));
        }

        public float payloadY(){
            return y + Interp.pow5In.apply(payloadT()) * (100 + Mathf.randomSeedRange(pos() + 2, 30));
        }

        public void drawPayload(){
            if(rocketRegion == null || !rocketRegion.found()) return;

            float t = payloadT();
            float alpha = Interp.pow5Out.apply(1 - t);
            float scale = (1 - alpha) * 1.3f + 1;
            float cx = payloadX(), cy = payloadY();
            float rotation = t * (130 + Mathf.randomSeedRange(pos(), 50));
            float rad = 0.2f + (1 - Math.abs(t * 2 - 1));

            Draw.z(Layer.effect + 0.001f);
            Draw.color(Pal.engine);
            Tmp.c2.set(Pal.engine);
            Tmp.c2.a = alpha;
            Tmp.c1.set(Pal.engine);
            Tmp.c1.a = 0f;
            Fill.light(cx, cy, 10, 25f * (rad + scale - 1), Tmp.c2, Tmp.c1);
            Draw.alpha(alpha);
            for(int i = 0; i < 4; i++){
                Drawf.tri(cx, cy, 6f, 40f * (rad + scale - 1), i * 90 + rotation);
            }
            Draw.color();

            Draw.z(Layer.weather - 1);
            scale *= rocketRegion.scl();
            float rw = rocketRegion.width * scale, rh = rocketRegion.height * scale;

            Draw.alpha(alpha);
            Draw.rect(rocketRegion, cx, cy, rw, rh, rotation);

            Tmp.v1.trns(225f, Interp.pow3In.apply(t) * 250f);

            Draw.z(Layer.flyingUnit + 1);
            Draw.color(0f, 0f, 0f, 0.22f * alpha);
            Draw.rect(rocketRegion, cx + Tmp.v1.x, cy + Tmp.v1.y, rw, rh, rotation);

            Draw.reset();
        }

        @Override
        public void buildConfiguration(Table table){
            table.clear();
            table.defaults().left().pad(4f);

            table.add("[accent]轨道发射设定").center().padBottom(8f).row();

            table.table(bt -> {
                bt.defaults().size(130f, 46f).pad(4f);
                bt.button("设为轨道器", () -> setMode(0));
                bt.button("设为轨道弹", () -> setMode(1));
            }).left().row();

            Table materialTable = new Table();
            materialTable.left();
            table.add(materialTable).left().row();

            final int[] cachedMode = {-1};
            materialTable.update(() -> {
                int current = getMode();
                if(current == cachedMode[0]) return;
                cachedMode[0] = current;

                materialTable.clearChildren();
                materialTable.add("[lightgray]已装填").left().padBottom(4f).row();

                ItemStack[] materials = current == 1 ? SHELL_COST : ORBITER_COST;
                for(ItemStack stack : materials){
                    materialTable.image(stack.item.uiIcon).size(24f).padRight(6f).left();
                    Label text = materialTable.add("").left().get();
                    text.update(() -> text.setText("[white]" + stack.item.localizedName + " [lightgray]" + itemAmount(stack.item) + "/" + stack.amount));
                    materialTable.row();
                }
            });

            table.table(Styles.black6, t -> {
                t.left();

                Label modeRow = t.add("").left().get();
                modeRow.update(() -> {
                    if(getMode() == 1){
                        modeRow.setText("[gold]发射物：轨道弹  [lightgray]每次 +2");
                    }else{
                        modeRow.setText("[gold]发射物：轨道器  [lightgray]每次 +1");
                    }
                });
                t.row();

                Label progressRow = t.add("").left().get();
                progressRow.update(() -> {
                    if(isLaunching()){
                        progressRow.setText("[green]发射中  " + (int)Math.floor(getProgress() * 100) + "%");
                    }else if(getCooldown() > 0){
                        progressRow.setText("[orange]冷却中  " + (int)Math.ceil(getCooldown() / 60) + "s");
                    }else{
                        progressRow.setText("[gray]就绪：等待材料与液体");
                    }
                });
                t.row();

                Label countRow = t.add("").left().get();
                countRow.update(() -> {
                    countRow.setText("[lightgray]轨道器 " + OrbitFireEffects.orbiters + "   轨道弹 " + OrbitFireEffects.shells);
                });
            }).pad(8f).growX().row();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(mode);
            write.bool(launching);
            write.f(progress);
            write.i(OrbitFireEffects.orbiters);
            write.i(OrbitFireEffects.shells);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            mode = read.i();
            launching = read.bool();
            progress = read.f();

            int savedOrbiters = read.i();
            int savedShells = read.i();

            if(savedOrbiters > OrbitFireEffects.orbiters) OrbitFireEffects.orbiters = savedOrbiters;
            if(savedShells > OrbitFireEffects.shells) OrbitFireEffects.shells = savedShells;
        }
    }
}
