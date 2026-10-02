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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

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
                float yaw = mc.player.getYaw();
                float pitch = mc.player.getPitch();
                if (Managers.ROTATION.isRotating()) {
                    yaw = Managers.ROTATION.getRotationYaw();
                    pitch = Managers.ROTATION.getRotationPitch();
                }
                Managers.NETWORK.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN));
            }
            cancelVelocity = false;
        }
        concealVelocity = false;
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (event.packet instanceof PlayerPositionLookS2CPacket && concealConfig.get()) {
            concealVelocity = true;
        }

        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet && knockbackConfig.get()) {
            if (packet.getEntityId() != mc.player.getId()) {
                return;
            }

            if (concealVelocity && packet.getVelocityX() == 0 && packet.getVelocityZ() == 0 && packet.getVelocityZ() == 0) {
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
                    ((AccessorEntityVelocityUpdateS2CPacket) packet).setVelocityX((int) (packet.getVelocityX()
                        * (horizontalConfig.get() / 100.0f)));
                    ((AccessorEntityVelocityUpdateS2CPacket) packet).setVelocityY((int) (packet.getVelocityY()
                        * (verticalConfig.get() / 100.0f)));
                    ((AccessorEntityVelocityUpdateS2CPacket) packet).setVelocityZ((int) (packet.getVelocityZ()
                        * (horizontalConfig.get() / 100.0f)));
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
        } else if (event.packet instanceof ExplosionS2CPacket packet && explosionConfig.get()) {
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
                mc.executeSync(() -> ((AccessorClientWorld) mc.world).hookPlaySound(packet.center().x, packet.center().y, packet.center().z,
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS,
                    4.0f, (1.0f + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2f) * 0.7f, false, RANDOM.nextLong()));
            }
        } else if (event.packet instanceof BundleS2CPacket packet) {
            List<Packet<?>> allowedBundle = new ArrayList<>();

            for (Packet<?> packet1 : packet.getPackets()) {
                if (packet1 instanceof ExplosionS2CPacket packet2 && explosionConfig.get()) {
                    mc.executeSync(() -> ((AccessorClientWorld) mc.world).hookPlaySound(packet2.center().x, packet2.center().y, packet2.center().z,
                        SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS,
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
                } else if (packet1 instanceof EntityVelocityUpdateS2CPacket packet2 && knockbackConfig.get()) {
                    if (packet2.getEntityId() != mc.player.getId()) {
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
                                ((AccessorEntityVelocityUpdateS2CPacket) packet2).setVelocityX((int) (packet2.getVelocityX()
                                    * (horizontalConfig.get() / 100.0f)));
                                ((AccessorEntityVelocityUpdateS2CPacket) packet2).setVelocityY((int) (packet2.getVelocityY()
                                    * (verticalConfig.get() / 100.0f)));
                                ((AccessorEntityVelocityUpdateS2CPacket) packet2).setVelocityZ((int) (packet2.getVelocityZ()
                                    * (horizontalConfig.get() / 100.0f)));
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
        } else if (event.packet instanceof EntityDamageS2CPacket packet
            && packet.entityId() == mc.player.getId()
            && modeConfig.get() == VelocityMode.GRIM_V3 && isPhased()) {
            Managers.NETWORK.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, mc.player.horizontalCollision));
            Managers.NETWORK.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
        } else if (event.packet instanceof EntityStatusS2CPacket packet
            && packet.getStatus() == EntityStatuses.PULL_HOOKED_ENTITY && pushFishhookConfig.get()) {
            Entity entity = packet.getEntity(mc.world);
            if (entity instanceof FishingBobberEntity hook && hook.getHookedEntity() == mc.player) {
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
                Managers.NETWORK.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(),
                    mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                    mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(), Direction.DOWN));
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
        BlockPos headPos = mc.player.getBlockPos().up(mc.player.isCrawling() ? 1 : 2);
        if (mc.world.getBlockState(headPos).isReplaceable()) {
            return false;
        }

        return SurroundII.getInstance().getSurroundNoDown(mc.player).stream().noneMatch(
            blockPos -> mc.world.getBlockState(mc.player.isCrawling() ? blockPos : blockPos.up()).isReplaceable());
    }

    private void scaleExplosionKnockback(ExplosionS2CPacket packet) {
        packet.playerKnockback().ifPresent(knockback -> {
            Vec3d scaled = new Vec3d(
                knockback.x * (horizontalConfig.get() / 100.0f),
                knockback.y * (verticalConfig.get() / 100.0f),
                knockback.z * (horizontalConfig.get() / 100.0f)
            );
            ((AccessorExplosionS2CPacket) (Object) packet).setPlayerKnockback(Optional.of(scaled));
        });
    }

    private boolean isPhased() {
        return PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream()
            .anyMatch(blockPos -> !mc.world.getBlockState(blockPos).isReplaceable());
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
