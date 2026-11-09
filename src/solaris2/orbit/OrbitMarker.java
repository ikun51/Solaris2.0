package solaris2.orbit;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;
import solaris2.*;

import static mindustry.Vars.*;

/**
 * 由 scripts/建筑/轨道/轨道标记.js 移植而来。
 */
public class OrbitMarker extends ItemTurret{

    /** 每次打击后的冷却（60 秒 = 3600 帧）。 */
    public static final float ATTACK_COOLDOWN = 60f * 60f;

    // ===== 选点状态 =====
    /** 正在等待玩家在地图上点选目标的建筑。 */
    public static OrbitMarkerBuild selectedBuild = null;
    /** 第一次点击锁定的格子坐标，-1 表示还没点第一次。 */
    public static int previewX = -1;
    public static int previewY = -1;

    public OrbitMarker(String name){
        super(name);
        size = 3;
        health = 800;
        armor = 5f;
        reload = 720f;
        range = 1600f;
        inaccuracy = 0f;
        recoil = 0f;
        targetAir = true;
        targetGround = true;
        category = Category.turret;

        requirements = ItemStack.with(
            SolarisContent.item("磁钢"), 400,
            SolarisContent.item("铀钼合金"), 300,
            SolarisContent.item("氮化硅"), 300,
            SolarisContent.item("芯片"), 30
        );

        buildVisibility = BuildVisibility.shown;
        alwaysUnlocked = true;
        configurable = true;
        saveConfig = true;

        buildType = () -> new OrbitMarkerBuild();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("ammo");

        addBar("轨道弹", (OrbitMarkerBuild entity) -> new Bar(
            () -> "轨道弹: " + OrbitFireEffects.shells,
            () -> Pal.ammo,
            () -> Mathf.clamp(OrbitFireEffects.shells / 10f)
        ));
    }

    public class OrbitMarkerBuild extends ItemTurretBuild{
        public boolean boundFlag = false;
        public boolean registered = false;
        public int targetX = -1;
        public int targetY = -1;
        public float cooldown = 0f;
        public boolean autoAim = false;

        @Override
        public void updateTile(){
            if(!registered){
                registered = true;
                OrbitFireEffects.markers += 1;
            }

            if(!boundFlag && OrbitFireEffects.bound < OrbitFireEffects.orbiters){
                boundFlag = true;
                OrbitFireEffects.bound += 1;
            }

            float multiplier = 1f;
            if(OrbitFireEffects.markers > 0 && OrbitFireEffects.orbiters > OrbitFireEffects.markers){
                multiplier = (float)OrbitFireEffects.orbiters / OrbitFireEffects.markers;
            }

            efficiency = multiplier;

            if(cooldown > 0) cooldown -= delta();

            super.updateTile();

            if(autoAim) autoAttack();
        }

        @Override
        public void remove(){
            if(selectedBuild == this){
                selectedBuild = null;
                previewX = -1;
                previewY = -1;
            }
            if(registered){
                registered = false;
                OrbitFireEffects.markers -= 1;
                if(OrbitFireEffects.markers < 0) OrbitFireEffects.markers = 0;
            }
            if(boundFlag){
                boundFlag = false;
                if(OrbitFireEffects.bound > 0) OrbitFireEffects.bound -= 1;
            }
            super.remove();
        }

        @Override
        public boolean canConsume(){
            return true;
        }

        public int getTargetX(){
            return targetX;
        }

        public int getTargetY(){
            return targetY;
        }

        public void setTarget(int x, int y){
            targetX = x;
            targetY = y;
        }

        public boolean isBound(){
            return boundFlag;
        }

        public float getCooldown(){
            return cooldown;
        }

        public boolean isAutoAim(){
            return autoAim;
        }

        public void toggleAutoAim(){
            autoAim = !autoAim;
        }

        public void autoAttack(){
            if(cooldown > 0) return;
            if(!boundFlag) return;
            if(OrbitFireEffects.shells <= 0) return;

            Teamc enemy = Units.closestTarget(team, x, y, ((BaseTurret)block).range);
            if(enemy == null) return;
            if(enemy instanceof Healthc h && !h.isValid()) return;

            OrbitFireEffects.consumeShell();
            cooldown = ATTACK_COOLDOWN;
            OrbitFireEffects.havocOrbitalStrike.create(this, team, enemy.x(), enemy.y(), 0f);
        }

        public void manualAttack(){
            if(cooldown > 0) return;
            if(!boundFlag) return;
            if(targetX < 0 || targetY < 0) return;
            if(OrbitFireEffects.shells <= 0) return;

            OrbitFireEffects.consumeShell();
            cooldown = ATTACK_COOLDOWN;

            float worldX = targetX * tilesize + tilesize / 2f;
            float worldY = targetY * tilesize + tilesize / 2f;
            OrbitFireEffects.havocOrbitalStrike.create(this, team, worldX, worldY, 0f);
        }

        @Override
        public void buildConfiguration(Table table){
            table.clear();
            table.defaults().left().pad(4f);

            table.add("[accent]═══ 轨道打击标记 ═══").row();

            Label coordLabel = table.add("").left().get();
            coordLabel.update(() -> {
                if(getTargetX() < 0 || getTargetY() < 0){
                    coordLabel.setText("[lightgray]打击坐标：[scarlet]未设定");
                }else{
                    coordLabel.setText("[lightgray]打击坐标：[gold]" + getTargetX() + "[lightgray] , [gold]" + getTargetY());
                }
            });
            table.row();

            Label statusLabel = table.add("").left().get();
            statusLabel.update(() -> {
                float cold = getCooldown();
                statusLabel.setText("[lightgray]轨道器：" + OrbitFireEffects.orbiters
                    + "   轨道弹：" + OrbitFireEffects.shells
                    + "\n[lightgray]供给：" + (isBound() ? "[green]已绑定" : "[scarlet]未绑定")
                    + "   [lightgray]冷却：" + (cold > 0 ? "[scarlet]" + (int)Math.ceil(cold / 60) + "s" : "[green]就绪")
                    + "\n[lightgray]瞄准：" + (isAutoAim() ? "[green]自动" : "[gold]手动"));
            });
            table.row();

            table.table(bt -> {
                bt.button("配置打击位置", () -> {
                    selectedBuild = this;
                    previewX = -1;
                    previewY = -1;
                    control.input.config.hideConfig();
                }).size(160f, 46f).pad(4f);

                bt.button("发射", () -> manualAttack()).size(120f, 46f).pad(4f);

                bt.button("自动瞄准", () -> toggleAutoAim()).size(120f, 46f).pad(4f);
            }).padTop(8f).row();
        }

        @Override
        public boolean hasAmmo(){
            return false;
        }

        @Override
        public BulletType peekAmmo(){
            return OrbitFireEffects.havocBoostAttack;
        }

        @Override
        public BulletType useAmmo(){
            OrbitFireEffects.consumeShell();
            return OrbitFireEffects.havocBoostAttack;
        }

        @Override
        protected void shoot(BulletType type){
            super.shoot(OrbitFireEffects.havocBoostAttack);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(boundFlag);
            write.i(targetX);
            write.i(targetY);
            write.f(cooldown);
            write.bool(autoAim);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            boolean savedBound = read.bool();
            if(savedBound && !boundFlag && OrbitFireEffects.bound < OrbitFireEffects.orbiters){
                boundFlag = true;
                OrbitFireEffects.bound += 1;
            }

            targetX = read.i();
            targetY = read.i();
            cooldown = read.f();
            autoAim = read.bool();
        }
    }

    static{
        Events.on(EventType.TapEvent.class, e -> {
            if(selectedBuild == null) return;

            // 第一次点击：锁定预览位置，十字轴停在这里
            if(previewX < 0){
                previewX = e.tile.x;
                previewY = e.tile.y;
                return;
            }

            // 第二次点击：确认打击位置
            OrbitMarkerBuild target = selectedBuild;
            selectedBuild = null;
            target.setTarget(previewX, previewY);
            previewX = -1;
            previewY = -1;
        });

        Events.run(EventType.Trigger.draw, () -> {
            if(selectedBuild == null) return;

            int gridX = previewX;
            int gridY = previewY;
            if(gridX < 0){
                gridX = Mathf.floor(Core.input.mouseWorldX() / tilesize);
                gridY = Mathf.floor(Core.input.mouseWorldY() / tilesize);
            }
            if(gridX < 0 || gridY < 0 || gridX >= world.width() || gridY >= world.height()) return;

            float worldX = gridX * tilesize + tilesize / 2f;
            float worldY = gridY * tilesize + tilesize / 2f;
            float mapWidth = world.width() * tilesize;
            float mapHeight = world.height() * tilesize;

            Draw.z(Layer.overlayUI);

            // 贯穿全图的十字坐标轴
            Draw.color(Pal.accent);
            Lines.stroke(1.5f);
            Lines.line(0f, worldY, mapWidth, worldY);
            Lines.line(worldX, 0f, worldX, mapHeight);

            // 目标格高亮方框
            Lines.stroke(2f);
            Lines.rect(worldX - tilesize / 2f, worldY - tilesize / 2f, tilesize, tilesize);

            // 坐标数值
            Drawf.text(gridX + ", " + gridY, worldX + 10f, worldY + 14f, Pal.accent);

            Draw.reset();
        });
    }
}
