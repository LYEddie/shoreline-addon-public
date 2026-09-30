package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.ObsidianPlacerModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.Animation;
import me.lyeddie.addon.util.BlastResistantBlocks;
import me.lyeddie.addon.util.literal.PositionUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public class SurroundII extends ObsidianPlacerModule {
    private static SurroundII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> multitaskConfig = addMultitaskConfig(sgGeneral);
    private final Setting<Boolean> strictDirectionConfig = addStrictDirectionConfig(sgGeneral);
    private final Setting<Boolean> rotateConfig = addRotateConfig(sgGeneral);
    private final Setting<Boolean> stopMotionConfig = addStopMotionConfig(sgGeneral);

    public final Setting<Timing> timingConfig = sgMisc.add(new EnumSetting.Builder<Timing>()
        .name("Timing").description("Timing for replacing blocks")
        .defaultValue(Timing.VANILLA)
        .build());
    public final Setting<ReplaceMode> replaceConfig = sgGeneral.add(new EnumSetting.Builder<ReplaceMode>()
        .name("Replace").description("Pre places before explosions")
        .defaultValue(ReplaceMode.NORMAL)
        .visible(() -> timingConfig.get() == Timing.SEQUENTIAL)
        .build());
    public final Setting<Double> placeRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("PlaceRange").description("The placement range for surround")
        .defaultValue(4.0)
        .min(0.0)
        .sliderMax(6.0)
        .build());
    private final Setting<Boolean> attackConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Attack").description("Attacks crystals in the way of surround")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> extendConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Extend").description("Extends surround if the player is not in the center of a block")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> headConfig = sgMisc.add(new BoolSetting.Builder()
        .name("CoverHead").description("Place a block at your head")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> mineExtendConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("MineExtend").description("Extends surround if the block is being mined")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> supportConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Support").description("Creates a floor for the surround if there is none")
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
    private final Setting<Boolean> jumpDisableConfig = sgMisc.add(new BoolSetting.Builder()
        .name("AutoDisable").description("Disables after moving out of the hole")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> renderConfig = sgRender.add(new BoolSetting.Builder()
        .name("Render").description("Renders where scaffold is placing blocks")
        .defaultValue(false)
        .build());
    public final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("Fade-Time").description("Time to fade")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .build());

    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private int blocksPlaced;
    private List<BlockPos> surround = new ArrayList<>();
    private List<BlockPos> placements = new ArrayList<>();
    private double prevY;

    public SurroundII() {
        super(Shoreline.MAIN, "SurroundII", "Surrounds feet with obsidian", 950);
        INST = this;
    }

    @Override
    public void onActivate() {
        if (mc.player == null) {
            return;
        }
        prevY = mc.player.getY();
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
        if (SelfTrapII.getInstance().isActive()) {
            return;
        }

        if (jumpDisableConfig.get() && (mc.player.getY() - prevY > 0.5 || mc.player.fallDistance > 1.5f)) {
            toggle();
            return;
        }

        if ((!multitaskConfig.get() && checkMultitask()) || (stopMotionConfig.get() && !mc.player.onGround())) {
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

        surround = getSurround(mc.player);
        if (surround.isEmpty()) {
            return;
        }

        if (attackConfig.get()) {
            attackBlockingCrystals(surround);
        }
        placements = getPlacementsFromSurround(surround, blockItem.block());
        if (placements.isEmpty()) {
            return;
        }

        if (supportConfig.get()) {
            for (BlockPos block : new ArrayList<>(placements)) {
                if (block.getY() > mc.player.getBlockY() + 1.0) {
                    continue;
                }
                Direction direction = Managers.INTERACT.getInteractDirectionInternal(block, strictDirectionConfig.get());
                if (direction == null) {
                    placements.add(block.below());
                }
            }
        }
        placements.sort(Comparator.comparingInt(Vec3i::getY));

        Vec3 prevMotion = mc.player.getDeltaMovement();
        if (stopMotionConfig.get()) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
        }

        while (blocksPlaced < shiftTicksConfig.get()) {
            if (blocksPlaced >= placements.size()) {
                break;
            }
            BlockPos targetPos = placements.get(blocksPlaced);
            placeBlock(targetPos, blockItem);
            blocksPlaced++;
        }

        if (rotateConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }
        if (stopMotionConfig.get()) mc.player.setDeltaMovement(prevMotion);
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (event.packet instanceof ClientboundBundlePacket packet) {
            for (Packet<?> packet1 : packet.subPackets()) {
                handlePackets(packet1);
            }
        } else {
            handlePackets(event.packet);
        }
    }

    private void handlePackets(Packet<?> serverPacket) {
        if (timingConfig.get() != Timing.SEQUENTIAL) {
            return;
        }

        if (serverPacket instanceof ClientboundBlockUpdatePacket packet) {
            final BlockState blockState = packet.getBlockState();
            final BlockPos targetPos = packet.getPos();
            if (surround.contains(targetPos)) {
                if (blockState.canBeReplaced()) {
                    BlockSlot blockItem = getResistantBlockItem();
                    if (blockItem == null) return;
                    placeBlock(targetPos, blockItem);
                } else if (BlastResistantBlocks.isBlastResistant(blockState)) {
                    packets.remove(targetPos);
                }
            }
        }

        if (serverPacket instanceof ClientboundExplodePacket packet && replaceConfig.get() == ReplaceMode.FAST) {
            BlockPos pos = BlockPos.containing(packet.center());
            if (surround.contains(pos)) {
                BlockSlot blockItem = getResistantBlockItem();
                if (blockItem == null) return;
                placeBlock(pos, blockItem);
            }
        }

        if (serverPacket instanceof ClientboundRemoveEntitiesPacket packetxx && replaceConfig.get() == ReplaceMode.NORMAL) {
            for (int id : packetxx.getEntityIds()) {
                Entity entity = mc.level.getEntity(id);
                if (entity instanceof EndCrystal) {
                    BlockPos targetPos = entity.blockPosition();
                    if (surround.contains(targetPos)) {
                        BlockSlot blockItem = getResistantBlockItem();
                        if (blockItem == null) return;
                        placeBlock(targetPos, blockItem);
                    }
                }
            }
        }

        if (serverPacket instanceof ClientboundAddEntityPacket packet && packet.getType().equals(EntityType.END_CRYSTAL) && replaceConfig.get() == ReplaceMode.STRICT) {
            for (BlockPos pos : surround) {
                if (!pos.equals(BlockPos.containing(packet.getX(), packet.getY(), packet.getZ()))) {
                    continue;
                }

                BlockSlot blockItem = getResistantBlockItem();
                if (blockItem == null) return;
                placeBlock(pos, blockItem);
                break;
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

    public List<BlockPos> getPlacementsFromSurround(List<BlockPos> surround, Block block) {
        List<BlockPos> placements = new ArrayList<>();
        for (BlockPos surroundPos : surround) {
            Long placed = packets.get(surroundPos);
            if (shiftDelayConfig.get() > 0.0f && placed != null && System.currentTimeMillis() - placed < shiftDelayConfig.get() * 50.0f) {
                continue;
            }
            if (!mc.level.getBlockState(surroundPos).canBeReplaced()) {
                continue;
            }
            double dist = mc.player.distanceToSqr(surroundPos.getCenter());
            if (dist > getValueSq(placeRangeConfig.get())) {
                continue;
            }

            if (Managers.INTERACT.canPlace(surroundPos, block)) {
                placements.add(surroundPos);
            }
        }
        return placements;
    }

    public List<BlockPos> getSurround(Player player) {
        List<BlockPos> surroundBlocks = getSurroundNoDown(player);
        List<BlockPos> playerBlocks = getPlayerBlocks(player);
        for (BlockPos playerPos : playerBlocks) {
            if (playerPos.equals(player.blockPosition())) {
                continue;
            }
            surroundBlocks.add(playerPos.below());
        }
        if (mineExtendConfig.get()) {
            for (BlockPos surroundPos : new ArrayList<>(surroundBlocks)) {
                if (!Managers.BLOCK.isPassed(surroundPos, 0.7f)) {
                    continue;
                }
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN) {
                        continue;
                    }
                    BlockPos blockerPos = surroundPos.relative(direction);
                    if (playerBlocks.contains(blockerPos) || AutoMine.getInstance().getMiningBlock() == blockerPos) {
                        continue;
                    }
                    surroundBlocks.add(blockerPos);
                }
            }
        }

        if (AirPlaceII.getInstance().isActive() && headConfig.get()) {
            surroundBlocks.add(mc.player.blockPosition().above(2));
        }
        return surroundBlocks;
    }

    public List<BlockPos> getSurroundNoDown(Player player) {
        return getSurroundNoDown(player, 0.0f);
    }

    public List<BlockPos> getSurroundNoDown(Player player, float range) {
        List<BlockPos> surroundBlocks = new ArrayList<>();
        List<BlockPos> playerBlocks = getPlayerBlocks(player);
        for (BlockPos pos : playerBlocks) {
            if (range > 0.0f && mc.player.getEyePosition().distanceToSqr(pos.getCenter()) > range * range) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                if (!dir.getAxis().isHorizontal()) {
                    continue;
                }
                BlockPos pos1 = pos.relative(dir);
                if (surroundBlocks.contains(pos1) || playerBlocks.contains(pos1)) {
                    continue;
                }
                surroundBlocks.add(pos1);
            }
        }
        return surroundBlocks;
    }

    public List<BlockPos> getPlayerBlocks(Player entity) {
        BlockPos playerPos = PositionUtil.getRoundedBlockPos(entity.getX(), entity.getY(), entity.getZ());
        final List<BlockPos> playerBlocks = new ArrayList<>();
        if (extendConfig.get()) {
            playerBlocks.addAll(PositionUtil.getAllInBox(entity.getBoundingBox(), playerPos));
        } else {
            playerBlocks.add(playerPos);
        }
        return playerBlocks;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (SelfTrapII.getInstance().isActive()) {
            return;
        }
        if (renderConfig.get()) {
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet()) {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());
                event.renderer.box(set.getKey(), TabConfigs.get().getClampColor(boxAlpha), TabConfigs.get().getClampColor(lineAlpha), ShapeMode.Both, 0);
            }

            if (placements.isEmpty()) {
                return;
            }

            for (BlockPos pos : placements) {
                Animation animation = new Animation(true, fadeTimeConfig.get());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }

    public boolean isPlacing() {
        return !placements.isEmpty();
    }

    public static SurroundII getInstance() {
        return INST;
    }

    public enum Timing {
        VANILLA,
        SEQUENTIAL
    }

    public enum ReplaceMode {
        NORMAL,
        STRICT,
        FAST
    }
}
