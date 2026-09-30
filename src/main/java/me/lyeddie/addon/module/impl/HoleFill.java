package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.managers.impl.util.Hole;
import me.lyeddie.addon.managers.impl.util.HoleType;
import me.lyeddie.addon.module.ObsidianPlacerModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.Animation;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HoleFill extends ObsidianPlacerModule {
    private static HoleFill INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgTarget = settings.createGroup("Targets");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> multitaskConfig = addMultitaskConfig(sgGeneral);
    private final Setting<Boolean> strictDirectionConfig = addStrictDirectionConfig(sgGeneral);
    private final Setting<Boolean> stopMotionConfig = addStopMotionConfig(sgGeneral);

    private final Setting<Boolean> obsidianConfig = sgTarget.add(new BoolSetting.Builder()
        .name("Obsidian").description("Fills obsidian holes")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> doublesConfig = sgTarget.add(new BoolSetting.Builder()
        .name("Doubles").description("Fills double holes")
        .defaultValue(false)
        .build());
    public final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("PlaceRange").description("The range to fill nearby holes")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .build());
    private final Setting<Boolean> websConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Webs").description("Fills holes with webs")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> autoConfig = sgTarget.add(new BoolSetting.Builder()
        .name("Auto").description("Fills holes when enemies are within a certain range")
        .defaultValue(false)
        .build());
    public final Setting<Double> targetRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("TargetRange").description("The range from the target to the hole")
        .defaultValue(3.0)
        .min(0.5)
        .sliderMax(5.0)
        .visible(autoConfig::get)
        .build());
    public final Setting<Double> enemyRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("EnemyRange").description("The maximum range of targets")
        .defaultValue(10.0)
        .min(0.1)
        .sliderMax(15.0)
        .visible(autoConfig::get)
        .build());
    private final Setting<Boolean> attackConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Attack").description("Attacks crystals in the way of hole fill")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> rotateConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate").description("Rotates to block before placing")
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
    private final Setting<Boolean> autoDisableConfig = sgMisc.add(new BoolSetting.Builder()
        .name("AutoDisable").description("Disables after filling all holes")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> renderConfig = sgRender.add(new BoolSetting.Builder()
        .name("Render").description("Renders where blocks are being filled")
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
    private List<BlockPos> fills = new ArrayList<>();

    public HoleFill() {
        super(Shoreline.MAIN, "HoleFill", "Fills in nearby holes with blocks");
        INST = this;
    }

    @Override
    public void onDeactivate() {
        fadeList.clear();
        fills.clear();
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        toggle();
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {
        int blocksPlaced = 0;

        if ((!multitaskConfig.get() && checkMultitask()) || (stopMotionConfig.get() && !mc.player.onGround())) {
            fills.clear();
            return;
        }

        BlockSlot blockItem = websConfig.get() ? new BlockSlot(Blocks.COBWEB, getBlockItemSlot(Blocks.COBWEB)) : getResistantBlockItem();
        if (blockItem == null || blockItem.slot() == -1) {
            fills.clear();
            return;
        }

        if (shiftDelayConfig.get() > 0 && shiftDelay < shiftDelayConfig.get()) {
            shiftDelay++;
            return;
        }
        List<BlockPos> holes = new ArrayList<>();
        for (Hole hole : Managers.HOLE.getHoles()) {
            if (hole.isQuad() || hole.isDouble() && !doublesConfig.get() || hole.getSafety() == HoleType.OBSIDIAN && !obsidianConfig.get()) {
                continue;
            }
            if (hole.squaredDistanceTo(mc.player) > getValueSq(rangeConfig.get())) {
                continue;
            }

            if (!Managers.INTERACT.canPlace(hole.getPos(), blockItem.block())) {
                continue;
            }

            if (autoConfig.get()) {
                for (Player entity : mc.level.players()) {
                    if (entity == mc.player || Friends.get().isFriend(entity)) {
                        continue;
                    }
                    double dist = mc.player.distanceTo(entity);
                    if (dist > enemyRangeConfig.get()) {
                        continue;
                    }
                    if (entity.getY() >= hole.y() &&
                        hole.squaredDistanceTo(entity) > getValueSq(targetRangeConfig.get())) {
                        continue;
                    }
                    holes.add(hole.getPos());
                    break;
                }
            } else {
                holes.add(hole.getPos());
            }
        }
        fills = holes;
        if (fills.isEmpty()) {
            if (autoDisableConfig.get()) {
                toggle();
            }
            return;
        }

        if (attackConfig.get()) {
            attackBlockingCrystals(fills);
        }

        Vec3 prevMotion = mc.player.getDeltaMovement();
        if (stopMotionConfig.get()) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
        }

        while (blocksPlaced < shiftTicksConfig.get()) {
            if (blocksPlaced >= fills.size()) {
                break;
            }
            BlockPos targetPos = fills.get(blocksPlaced);
            blocksPlaced++;
            shiftDelay = 0;
            placeBlock(targetPos, blockItem);
        }

        if (rotateConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }

        if (stopMotionConfig.get()) {
            mc.player.setDeltaMovement(prevMotion);
        }
    }

    public void attackBlockingCrystals(List<BlockPos> posList) {
        for (BlockPos pos : posList) {
            Entity crystalEntity = mc.level.getEntities(null, new AABB(pos)).stream()
                .filter(e -> e instanceof EndCrystal).findFirst().orElse(null);
            if (crystalEntity == null) {
                continue;
            }
            Managers.NETWORK.sendPacket(ServerboundInteractPacket.createAttackPacket(crystalEntity, mc.player.isShiftKeyDown()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            return;
        }
    }

    private void placeBlock(BlockPos targetPos, BlockSlot blockItem) {
        Managers.INTERACT.placeBlock(targetPos, blockItem.block(), blockItem.slot(), strictDirectionConfig.get(), false, (state, angles) -> {
            if (rotateConfig.get() && state) {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (renderConfig.get()) {
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet()) {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());
                event.renderer.box(set.getKey(), TabConfigs.get().getClampColor(boxAlpha), TabConfigs.get().getClampColor(lineAlpha), ShapeMode.Both, 0);
            }

            if (fills.isEmpty()) {
                return;
            }

            for (BlockPos pos : fills) {
                Animation animation = new Animation(true, fadeTimeConfig.get());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }

    public boolean isPlacing() {
        return !fills.isEmpty();
    }

    public static HoleFill getInstance() {
        return INST;
    }
}
