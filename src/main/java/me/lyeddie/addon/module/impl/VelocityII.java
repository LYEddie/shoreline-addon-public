package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.PushEntityEvent;
import me.lyeddie.addon.events.PushFluidsEvent;
import me.lyeddie.addon.events.PushOutOfBlocksEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.impl.accessor.AccessorBundlePacket;
import me.lyeddie.addon.mixin.impl.accessor.AccessorClientWorld;
import me.lyeddie.addon.mixin.impl.accessor.AccessorEntityVelocityUpdateS2CPacket;
import me.lyeddie.addon.mixin.impl.accessor.AccessorExplosionS2CPacket;
import me.lyeddie.addon.util.EnumFormatter;
import me.lyeddie.addon.util.literal.PositionUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.lyeddie.addon.util.Globals.RANDOM;

public class VelocityII extends AddonModule {
    private static VelocityII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgChecks = settings.createGroup("Checks");

    private final Setting<Boolean> knockbackConfig = sgChecks.add(new BoolSetting.Builder()
        .name("Knockback")
        .description("Removes player knockback velocity")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> explosionConfig = sgChecks.add(new BoolSetting.Builder()
        .name("Explosion")
        .description("Removes player explosion velocity")
        .defaultValue(true)
        .build());
    public final Setting<VelocityMode> modeConfig = sgGeneral.add(new EnumSetting.Builder<VelocityMode>()
        .name("Mode").description("The mode for velocity")
        .defaultValue(VelocityMode.NORMAL)
        .build());
    public final Setting<Double> horizontalConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Horizontal").description("How much horizontal knock-back to take")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(100.0)
        .visible(() -> modeConfig.get() == VelocityMode.NORMAL || modeConfig.get() == VelocityMode.WALLS)
        .build());
    public final Setting<Double> verticalConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Vertical").description("How much vertical knock-back to take")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(100.0)
        .visible(() -> modeConfig.get() == VelocityMode.NORMAL || modeConfig.get() == VelocityMode.WALLS)
        .build());
    private final Setting<Boolean> concealConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Conceal").description("Fixes velocity on servers with excessive setbacks")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> wallsAirConfig = sgMisc.add(new BoolSetting.Builder()
        .name("GroundOnly").description("Only applies velocity in walls while on ground")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == VelocityMode.WALLS)
        .build());
    private final Setting<Boolean> wallsTrappedConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Trapped").description("Applies velocity while player head is trapped")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == VelocityMode.WALLS)
        .build());
    private final Setting<Boolean> pushEntitiesConfig = sgChecks.add(new BoolSetting.Builder()
        .name("NoPush-Entities").description("Prevents being pushed away from entities")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> pushBlocksConfig = sgChecks.add(new BoolSetting.Builder()
        .name("NoPush-Blocks").description("Prevents being pushed out of blocks")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> pushLiquidsConfig = sgChecks.add(new BoolSetting.Builder()
        .name("NoPush-Liquids").description("Prevents being pushed by flowing liquids")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> pushFishhookConfig = sgChecks.add(new BoolSetting.Builder()
        .name("NoPush-Fishhook").description("Prevents being pulled by fishing rod hooks")
        .defaultValue(true)
        .build());

    private boolean cancelVelocity;
    private boolean concealVelocity;

    public VelocityII() {
        super(Shoreline.MAIN, "VelocityII", "Reduces the amount of player knockback velocity");
        INST = this;
    }

    @Override
    public String getInfoString() {
        if (modeConfig.get() == VelocityMode.NORMAL) {
            DecimalFormat decimal = new DecimalFormat("0.0");
            return String.format("H:%s%%, V:%s%%",
                decimal.format(horizontalConfig.get()),
                decimal.format(verticalConfig.get()));
        }
        return EnumFormatter.formatEnum(modeConfig.get());
    }

    @Override
    public void onActivate() {
        cancelVelocity = false;
    }

    @Override
    public void onDeactivate() {
        if (cancelVelocity) {
            if (modeConfig.get() == VelocityMode.GRIM) {
                float yaw = mc.player.getYRot();
                float pitch = mc.player.getXRot();
                if (Managers.ROTATION.isRotating()) {
                    yaw = Managers.ROTATION.getRotationYaw();
                    pitch = Managers.ROTATION.getRotationPitch();
                }
                Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, mc.player.isVisuallyCrawling() ? mc.player.blockPosition() : mc.player.blockPosition().above(), Direction.DOWN));
            }
            cancelVelocity = false;
        }
        concealVelocity = false;
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (event.packet instanceof ClientboundPlayerPositionPacket && concealConfig.get()) {
            concealVelocity = true;
        }

        if (event.packet instanceof ClientboundSetEntityMotionPacket packet && knockbackConfig.get()) {
            if (packet.getId() != mc.player.getId()) {
                return;
            }

            Vec3 velocity = packet.getMovement();
            if (concealVelocity && velocity.x == 0 && velocity.y == 0 && velocity.z == 0) {
                concealVelocity = false;
                return;
            }

            if (modeConfig.get() == VelocityMode.WALLS) {
                if (!isPhased() && (!wallsTrappedConfig.get() || !isWallsTrapped())) {
                    return;
                }

                if (wallsAirConfig.get() && !Managers.POSITION.isOnGround()) {
                    return;
                }
            }

            switch (modeConfig.get()) {
                case NORMAL, WALLS -> {
                    if (horizontalConfig.get() == 0.0f && verticalConfig.get() == 0.0f) {
                        event.cancel();
                        return;
                    }
                    ((AccessorEntityVelocityUpdateS2CPacket) packet).setVelocity(new Vec3(
                        velocity.x * (horizontalConfig.get() / 100.0f),
                        velocity.y * (verticalConfig.get() / 100.0f),
                        velocity.z * (horizontalConfig.get() / 100.0f)));
                }
                case GRIM -> {
                    if (!Managers.ANTICHEAT.hasPassed(100)) {
                        return;
                    }
                    event.cancel();
                    cancelVelocity = true;
                }

                case GRIM_V3 -> event.setCancelled(isPhased());
            }
        } else if (event.packet instanceof ClientboundExplodePacket packet && explosionConfig.get()) {
            if (modeConfig.get() == VelocityMode.WALLS && !isPhased()) {
                return;
            }

            switch (modeConfig.get()) {
                case NORMAL, WALLS -> {
                    if (horizontalConfig.get() == 0.0f && verticalConfig.get() == 0.0f) {
                        event.cancel();
                    } else {
                        scaleExplosionKnockback(packet);
                    }
                }
                case GRIM -> {
                    if (!Managers.ANTICHEAT.hasPassed(100)) {
                        return;
                    }
                    event.cancel();
                    cancelVelocity = true;
                }

                case GRIM_V3 -> event.setCancelled(isPhased());
            }

            if (event.isCancelled()) {
                mc.executeIfPossible(() -> ((AccessorClientWorld) mc.level).hookPlaySound(packet.center().x, packet.center().y, packet.center().z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                    4.0f, (1.0f + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2f) * 0.7f, false, RANDOM.nextLong()));
            }
        } else if (event.packet instanceof ClientboundBundlePacket packet) {
            List<Packet<?>> allowedBundle = new ArrayList<>();

            for (Packet<?> packet1 : packet.subPackets()) {
                if (packet1 instanceof ClientboundExplodePacket packet2 && explosionConfig.get()) {
                    mc.executeIfPossible(() -> ((AccessorClientWorld) mc.level).hookPlaySound(packet2.center().x, packet2.center().y, packet2.center().z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                        4.0f, (1.0f + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2f) * 0.7f, false, RANDOM.nextLong()));

                    if (modeConfig.get() == VelocityMode.WALLS && !isPhased()) {
                        allowedBundle.add(packet1);
                        continue;
                    }

                    switch (modeConfig.get()) {
                        case NORMAL, WALLS -> {
                            if (horizontalConfig.get() == 0.0f && verticalConfig.get() == 0.0f) {
                                continue;
                            } else {
                                scaleExplosionKnockback(packet2);
                            }
                        }
                        case GRIM -> {
                            if (Managers.ANTICHEAT.hasPassed(100)) {
                                allowedBundle.add(packet1);
                                continue;
                            }

                            cancelVelocity = true;
                            continue;
                        }
                        case GRIM_V3 -> {
                            if (isPhased()) {
                                continue;
                            }
                        }
                    }
                } else if (packet1 instanceof ClientboundSetEntityMotionPacket packet2 && knockbackConfig.get()) {
                    if (packet2.getId() != mc.player.getId()) {
                        allowedBundle.add(packet1);
                        continue;
                    }

                    if (modeConfig.get() == VelocityMode.WALLS) {
                        if (!isPhased() && (!wallsTrappedConfig.get() || !isWallsTrapped())) {
                            allowedBundle.add(packet1);
                            return;
                        }

                        if (wallsAirConfig.get() && !Managers.POSITION.isOnGround()) {
                            allowedBundle.add(packet1);
                            continue;
                        }
                    }

                    switch (modeConfig.get()) {
                        case NORMAL, WALLS -> {
                            if (horizontalConfig.get() == 0.0f && verticalConfig.get() == 0.0f) {
                                continue;
                            } else {
                                Vec3 velocity = packet2.getMovement();
                                ((AccessorEntityVelocityUpdateS2CPacket) packet2).setVelocity(new Vec3(
                                    velocity.x * (horizontalConfig.get() / 100.0f),
                                    velocity.y * (verticalConfig.get() / 100.0f),
                                    velocity.z * (horizontalConfig.get() / 100.0f)));
                            }
                        }
                        case GRIM -> {
                            if (!Managers.ANTICHEAT.hasPassed(100)) {
                                allowedBundle.add(packet1);
                                continue;
                            }

                            cancelVelocity = true;
                            continue;
                        }
                        case GRIM_V3 -> {
                            if (isPhased()) {
                                continue;
                            }
                        }
                    }
                }

                allowedBundle.add(packet1);
            }

            ((AccessorBundlePacket) packet).setIterable(allowedBundle);
        } else if (event.packet instanceof ClientboundDamageEventPacket packet
            && packet.entityId() == mc.player.getId()
            && modeConfig.get() == VelocityMode.GRIM_V3 && isPhased()) {
            Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
            Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
        } else if (event.packet instanceof ClientboundEntityEventPacket packet
            && packet.getEventId() == EntityEvent.FISHING_ROD_REEL_IN && pushFishhookConfig.get()) {
            Entity entity = packet.getEntity(mc.level);
            if (entity instanceof FishingHook hook && hook.getHookedIn() == mc.player) {
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {
        concealVelocity = false;

        if (cancelVelocity) {
            if (modeConfig.get() == VelocityMode.GRIM) {
                float yaw = Managers.ROTATION.getServerYaw();
                float pitch = Managers.ROTATION.getServerPitch();
                if (Managers.ROTATION.isRotating()) {
                    yaw = Managers.ROTATION.getRotationYaw();
                    pitch = Managers.ROTATION.getRotationPitch();
                }
                Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(),
                    mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    mc.player.isVisuallyCrawling() ? mc.player.blockPosition() : mc.player.blockPosition().above(), Direction.DOWN));
            }
            cancelVelocity = false;
        }
    }

    @EventHandler
    public void onPushEntity(PushEntityEvent event) {
        if (pushEntitiesConfig.get() && event.getPushed().equals(mc.player)) {
            event.cancel();
        }
    }

    @EventHandler
    public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        if (pushBlocksConfig.get()) {
            event.cancel();
        }
    }

    @EventHandler
    public void onPushFluid(PushFluidsEvent event) {
        if (pushLiquidsConfig.get()) {
            event.cancel();
        }
    }

    private boolean isWallsTrapped() {
        BlockPos headPos = mc.player.blockPosition().above(mc.player.isVisuallyCrawling() ? 1 : 2);
        if (mc.level.getBlockState(headPos).canBeReplaced()) {
            return false;
        }

        return SurroundII.getInstance().getSurroundNoDown(mc.player).stream().noneMatch(
            blockPos -> mc.level.getBlockState(mc.player.isVisuallyCrawling() ? blockPos : blockPos.above()).canBeReplaced());
    }

    private void scaleExplosionKnockback(ClientboundExplodePacket packet) {
        packet.playerKnockback().ifPresent(knockback -> {
            Vec3 scaled = new Vec3(
                knockback.x * (horizontalConfig.get() / 100.0f),
                knockback.y * (verticalConfig.get() / 100.0f),
                knockback.z * (horizontalConfig.get() / 100.0f)
            );
            ((AccessorExplosionS2CPacket) (Object) packet).setPlayerKnockback(Optional.of(scaled));
        });
    }

    private boolean isPhased() {
        return PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream()
            .anyMatch(blockPos -> !mc.level.getBlockState(blockPos).canBeReplaced());
    }

    public static VelocityII getInstance() {
        return INST;
    }

    public enum VelocityMode {
        NORMAL,
        WALLS,
        GRIM,
        GRIM_V3
    }
}
