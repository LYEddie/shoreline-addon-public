package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.BlockPlacerModule;
import me.lyeddie.addon.tabs.TabConfigs;
import me.lyeddie.addon.util.Animation;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoWeb extends BlockPlacerModule {
    private static AutoWeb INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> multitaskConfig = addMultitaskConfig(sgGeneral);
    private final Setting<Boolean> strictDirectionConfig = addStrictDirectionConfig(sgGeneral);
    private final Setting<Boolean> stopMotionConfig = addStopMotionConfig(sgGeneral);

    public final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("PlaceRange").description("The range to fill nearby holes")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .build());
    public final Setting<Double> enemyRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("EnemyRange").description("The maximum range of targets")
        .defaultValue(10.0)
        .min(0.1)
        .sliderMax(15.0)
        .build());
    private final Setting<Boolean> rotateConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate").description("Rotates to block before placing")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> coverHeadConfig = sgMisc.add(new BoolSetting.Builder()
        .name("CoverHead").description("Places webs on the targets head")
        .defaultValue(false)
        .build());
    public final Setting<Integer> shiftTicksConfig = sgGeneral.add(new IntSetting.Builder()
        .name("ShiftTicks").description("The number of blocks to place per tick")
        .defaultValue(2)
        .min(1)
        .sliderMax(5)
        .build());
    public final Setting<Integer> shiftDelayConfig = sgGeneral.add(new IntSetting.Builder()
        .name("ShiftDelay").description("The delay between each block placement interval")
        .defaultValue(1)
        .min(0)
        .sliderMax(5)
        .build());
    private final Setting<Boolean> renderConfig = sgRender.add(new BoolSetting.Builder()
        .name("Render").description("Renders web placements")
        .defaultValue(false)
        .build());
    public final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("Fade-Time").description("Time to fade")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .build());

    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private int shiftDelay;
    private List<BlockPos> webs = new ArrayList<>();

    public AutoWeb() {
        super(Shoreline.MAIN, "AutoWeb", "Automatically traps nearby entities in webs");
        INST = this;
    }

    @Override
    public void onDeactivate() {
        fadeList.clear();
        webs.clear();
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        toggle();
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {
        if ((!multitaskConfig.get() && checkMultitask()) || (stopMotionConfig.get() && !mc.player.isOnGround())) {
            webs.clear();
            return;
        }

        int blocksPlaced = 0;
        int slot = getBlockItemSlot(Blocks.COBWEB);
        if (slot == -1) {
            webs.clear();
            return;
        }

        if (shiftDelay < shiftDelayConfig.get()) {
            shiftDelay++;
            return;
        }
        List<BlockPos> webPlacements = new ArrayList<>();
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player || Friends.get().isFriend(entity)) {
                continue;
            }
            double d = mc.player.distanceTo(entity);
            if (d > enemyRangeConfig.get()) {
                continue;
            }
            BlockPos feetPos = entity.getBlockPos();
            double dist = mc.player.getEyePos().squaredDistanceTo(feetPos.toCenterPos());
            if (mc.world.getBlockState(feetPos).isAir() && dist <= getValueSq(rangeConfig.get())) {
                if (!Managers.INTERACT.canPlace(feetPos, Blocks.COBWEB)) {
                    continue;
                }
                webPlacements.add(feetPos);
            }
            if (coverHeadConfig.get()) {
                BlockPos headPos = feetPos.up();
                double dist2 = mc.player.getEyePos().squaredDistanceTo(headPos.toCenterPos());
                if (mc.world.getBlockState(headPos).isAir() && dist2 <= getValueSq(rangeConfig.get()) && Managers.INTERACT.canPlace(headPos, Blocks.COBWEB)) {
                    webPlacements.add(headPos);
                }
            }
        }
        webs = webPlacements;
        if (webs.isEmpty()) {
            return;
        }
        Vec3d prevMotion = mc.player.getVelocity();
        if (stopMotionConfig.get()) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
        while (blocksPlaced < shiftTicksConfig.get()) {
            if (blocksPlaced >= webs.size()) {
                break;
            }
            BlockPos targetPos = webs.get(blocksPlaced);
            blocksPlaced++;
            shiftDelay = 0;
            placeWeb(targetPos, slot);
        }

        if (rotateConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }

        if (this.stopMotionConfig.get()) {
            mc.player.setVelocity(prevMotion);
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (renderConfig.get()) {
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet()) {
                set.getValue().setState(false);
                int lineAlpha = (int) (120 * set.getValue().getFactor());
                BlockPos blockPos = set.getKey();
                double x1 = blockPos.getX();
                double y1 = blockPos.getY();
                double z1 = blockPos.getZ();
                double x2 = blockPos.getX() + 1.0;
                double y2 = blockPos.getY() + 1.0;
                double z2 = blockPos.getZ() + 1.0;
                event.renderer.quadVertical(x1, y1, z1, x2, y2, z2, TabConfigs.get().getClampColor(lineAlpha));
                event.renderer.quadVertical(x2, y1, z1, x1, y2, z2, TabConfigs.get().getClampColor(lineAlpha));
            }

            if (webs.isEmpty()) {
                return;
            }

            for (BlockPos pos : webs) {
                Animation animation = new Animation(true, fadeTimeConfig.get());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }

    private void placeWeb(BlockPos pos, int slot) {
        Managers.INTERACT.placeBlock(pos, Blocks.COBWEB, slot, strictDirectionConfig.get(), false, (state, angles) -> {
            if (rotateConfig.get() && state) {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
    }

    public boolean isPlacing() {
        return !webs.isEmpty();
    }

    public static AutoWeb getInstance() {
        return INST;
    }
}
