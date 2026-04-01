package me.lyeddie.addon.module.impl;

import com.google.common.collect.Lists;
import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.api.RenderBuffers;
import me.lyeddie.addon.api.RenderManager;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.irrevocable.RenderWorldEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.ObsidianPlacerModule;
import me.lyeddie.addon.tabs.TabConfigs;
import me.lyeddie.addon.util.Animation;
import me.lyeddie.addon.util.BlastResistantBlocks;
import me.lyeddie.addon.util.literal.PositionUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AutoTrapII extends ObsidianPlacerModule {
    private static AutoTrapII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> strictDirectionConfig = addStrictDirectionConfig(sgGeneral);
    private final Setting<Boolean> stopMotionConfig = addStopMotionConfig(sgGeneral);

    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> multitaskConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Multitask").description("Allows placing while eating")
        .defaultValue(true)
        .build());
    public final Setting<Double> placeRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("PlaceRange").description("The placement range for trap")
        .defaultValue(4.0)
        .min(0.0)
        .sliderMax(6.0)
        .build());
    private final Setting<Boolean> rotateConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Rotate").description("Rotates to block before placing")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> attackConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Attack").description("Attacks crystals in the way of trap ")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> extendConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Extend").description("Extends trap if the player is not in the center of a block")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> supportConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Support").description("Creates a floor for the trap if there is none")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> headConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Head").description("Place a block at targets head")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> antiStepConfig = sgMisc.add(new BoolSetting.Builder()
        .name("PreventStep").description("Prevents target from stepping out of the trap")
        .defaultValue(false)
        .build());
    public final Setting<Integer> shiftTicksConfig = sgGeneral.add(new IntSetting.Builder()
        .name("ShiftTicks").description("The number of blocks to place per tick")
        .defaultValue(2)
        .min(1)
        .sliderMax(10)
        .build());
    public final Setting<Double> shiftDelayConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("ShiftDelay").description("The delay between each block placement interval")
        .defaultValue(1.0)
        .min(0.0)
        .sliderMax(5.0)
        .build());
    private final Setting<Boolean> autoDisableConfig = sgMisc.add(new BoolSetting.Builder()
        .name("AutoDisable").description("Disables after placing the blocks")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> renderConfig = sgRender.add(new BoolSetting.Builder()
        .name("Render").description("Renders where trap is placing blocks")
        .defaultValue(false)
        .build());
    public final Setting<Double> fadeTimeConfig = sgRender.add(new DoubleSetting.Builder()
        .name("Fade-Time").description("Time to fade")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .build());

    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private List<BlockPos> surround = new ArrayList<>();
    private List<BlockPos> placements = new ArrayList<>();
    private int blocksPlaced;

    public AutoTrapII() {
        super(Shoreline.MAIN, "AutoTrapII", "Fully traps enemies with blocks", 900);
        INST = this;
    }

    @Override
    public void onDeactivate() {
        surround.clear();
        placements.clear();
        packets.clear();
        fadeList.clear();
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {
        blocksPlaced = 0;

        if ((!multitaskConfig.get() && checkMultitask()) || (stopMotionConfig.get() && !mc.player.isOnGround())) {
            surround.clear();
            placements.clear();
            return;
        }

        BlockSlot blockItem = getResistantBlockItem();
        if (blockItem == null) {
            surround.clear();
            placements.clear();
            return;
        }
        PlayerEntity trapTarget = getTrapTarget();
        if (trapTarget == null) {
            surround.clear();
            placements.clear();
            return;
        }

        BlockPos targetBlockPos = PositionUtil.getRoundedBlockPos(trapTarget.getX(), trapTarget.getY(), trapTarget.getZ());
        surround = getSurround(targetBlockPos, trapTarget);
        if (surround.isEmpty()) {
            return;
        }
        if (attackConfig.get()) {
            attackBlockingCrystals(surround);
        }
        placements = getPlacementsFromSurround(surround, blockItem);
        if (placements.isEmpty()) {
            if (autoDisableConfig.get()) {
                toggle();
            }
            return;
        }
        if (supportConfig.get()) {
            for (BlockPos block : new ArrayList<>(placements)) {
                if (block.getY() <= targetBlockPos.getY()) {
                    Direction direction = Managers.INTERACT.getInteractDirectionInternal(block, strictDirectionConfig.get());
                    if (direction == null) placements.add(block.down());
                }
            }
        }
        placements.sort(Comparator.comparingInt(Vec3i::getY));

        Vec3d prevMotion = mc.player.getVelocity();
        if (stopMotionConfig.get()) mc.player.setVelocity(0.0, 0.0, 0.0);

        while (blocksPlaced < shiftTicksConfig.get()) {
            if (blocksPlaced >= placements.size()) {
                break;
            }
            BlockPos targetPos = placements.get(blocksPlaced);
            blocksPlaced++;
            placeBlock(targetPos, blockItem);
        }

        if (rotateConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }
        if (stopMotionConfig.get()) mc.player.setVelocity(prevMotion);
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.packet instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) {
                handlePackets(packet1);
            }
        } else {
            handlePackets(event.packet);
        }
    }

    private void handlePackets(Packet<?> serverPacket) {
        if (serverPacket instanceof BlockUpdateS2CPacket packet) {
            final BlockState blockState = packet.getState();
            final BlockPos targetPos = packet.getPos();
            if (surround.contains(targetPos)) {
                if (blockState.isReplaceable()) {
                    BlockSlot blockItem = getResistantBlockItem();
                    if (blockItem == null) return;
                    placeBlock(targetPos, blockItem);
                } else if (BlastResistantBlocks.isBlastResistant(blockState)) {
                    packets.remove(targetPos);
                }
            }
        }
    }

    private void placeBlock(BlockPos pos, BlockSlot blockItem) {
        Managers.INTERACT.placeBlock(pos, blockItem.block(), blockItem.slot(), strictDirectionConfig.get(), false, true, (state, angles) -> {
            if (rotateConfig.get() && state) {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
        packets.put(pos, System.currentTimeMillis());
    }

    private PlayerEntity getTrapTarget() {
        final List<Entity> entities = Lists.newArrayList(mc.world.getEntities());
        return (PlayerEntity) entities.stream()
            .filter(e -> e instanceof PlayerEntity pent && e.isAlive() && mc.player != e && !(Friends.get().isFriend(pent)))
            .filter(e -> mc.player.squaredDistanceTo(e) <= getValueSq(placeRangeConfig.get()))
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
            .orElse(null);
    }

    public void attackBlockingCrystals(List<BlockPos> posList) {
        for (BlockPos pos : posList) {
            Entity crystalEntity = mc.world.getOtherEntities(null, new Box(pos)).stream()
                .filter(e -> e instanceof EndCrystalEntity).findFirst().orElse(null);
            if (crystalEntity == null) {
                continue;
            }
            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(crystalEntity, mc.player.isSneaking()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return;
        }
    }

    public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround, BlockSlot blockItem) {
        List<BlockPos> placements = new ArrayList<>();
        for (BlockPos surroundPos : surround) {
            Long placed = packets.get(surroundPos);
            if (shiftDelayConfig.get() > 0.0f && placed != null && System.currentTimeMillis() - placed < shiftDelayConfig.get() * 50.0f) {
                continue;
            }
            if (!mc.world.getBlockState(surroundPos).isReplaceable()) {
                continue;
            }
            double dist = mc.player.squaredDistanceTo(surroundPos.toCenterPos());
            if (dist > getValueSq(placeRangeConfig.get())) {
                continue;
            }

            if (Managers.INTERACT.canPlace(surroundPos, blockItem.block())) {
                placements.add(surroundPos);
            }
        }
        return placements;
    }

    public List<BlockPos> getSurround(BlockPos playerPos, PlayerEntity player) {
        List<BlockPos> surroundBlocks = new ArrayList<>();
        List<BlockPos> playerBlocks = getPlayerBlocks(playerPos, player);
        for (BlockPos pos : playerBlocks) {
            for (Direction dir : Direction.values()) {
                if (!dir.getAxis().isHorizontal()) {
                    continue;
                }
                BlockPos pos1 = pos.offset(dir);
                if (surroundBlocks.contains(pos1) || playerBlocks.contains(pos1)) {
                    continue;
                }

                surroundBlocks.add(pos1);
                surroundBlocks.add(pos1.up());
            }
        }
        if (headConfig.get()) {
            boolean support = false;
            final List<BlockPos> headBlocks = new ArrayList<>();
            for (BlockPos pos : playerBlocks) {
                BlockPos headPos = pos.offset(Direction.UP, 2);
                if (!mc.world.getBlockState(headPos).isReplaceable()) {
                    support = true;
                }
                headBlocks.add(headPos);
                if (antiStepConfig.get()) {
                    BlockPos antiStepPos = pos.offset(Direction.UP, 3);
                    headBlocks.add(antiStepPos);
                }
            }
            if (!AirPlaceII.getInstance().isActive()) {
                BlockPos supportingPos = null;
                double min = Double.MAX_VALUE;
                for (BlockPos pos : surroundBlocks) {
                    BlockPos pos1 = pos.offset(Direction.UP, 2);
                    if (!mc.world.getBlockState(pos1).isReplaceable()) {
                        support = true;
                        break;
                    }
                    double dist = mc.player.squaredDistanceTo(pos1.toCenterPos());
                    if (dist < min) {
                        supportingPos = pos1;
                        min = dist;
                    }
                }
                if (supportingPos != null && !support) {
                    surroundBlocks.add(supportingPos);
                }
            }
            surroundBlocks.addAll(headBlocks);
        }
        return surroundBlocks;
    }

    public List<BlockPos> getPlayerBlocks(BlockPos playerPos, PlayerEntity entity) {
        final List<BlockPos> playerBlocks = new ArrayList<>();
        if (extendConfig.get()) {
            playerBlocks.addAll(PositionUtil.getAllInBox(entity.getBoundingBox(), playerPos));
        } else {
            playerBlocks.add(playerPos);
        }
        return playerBlocks;
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (renderConfig.get()) {
            RenderBuffers.preRender();
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet()) {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());
                Color boxColor = TabConfigs.get().getClampColor(boxAlpha);
                Color lineColor = TabConfigs.get().getClampColor(lineAlpha);
                RenderManager.renderBox(event.getMatrices(), set.getKey(), boxColor.getRGB());
                RenderManager.renderBoundingBox(event.getMatrices(), set.getKey(), 1.5f, lineColor.getRGB());
            }
            RenderBuffers.postRender();

            if (placements.isEmpty()) {
                return;
            }

            for (BlockPos pos : placements) {
                Animation animation = new Animation(true, toFloat(fadeTimeConfig.get()));
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }

    public boolean isPlacing() {
        return !placements.isEmpty();
    }

    public static AutoTrapII getInstance() {
        return INST;
    }
}
