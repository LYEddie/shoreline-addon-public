package me.lyeddie.addon.util.tabs;

import me.lyeddie.addon.events.FriendAddedEvent;
import me.lyeddie.addon.events.MenuDisconnectEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.impl.accessor.AccessorPlayerMoveC2SPacket;
import me.lyeddie.addon.util.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Crypt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.time.Instant;
import java.util.BitSet;

import static net.minecraft.network.protocol.game.ClientboundGameEventPacket.DEMO_EVENT;

public class TabEvents implements Globals, Helpers {

    public final Timer raytraceTimer = new CacheTimer();
    public float pitch = Float.NaN;

    public TabEvents() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler(priority = 200)
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.level == null) return;

        if (TabConfigs.get().isNCP() && TabConfigs.get().raytraceSpoofConfig.get() && event.packet instanceof ServerboundUseItemOnPacket packet && raytraceTimer.passed(250)) {
            BlockHitResult packetResult = packet.getHitResult();
            BlockPos pos = packetResult.getBlockPos();
            BlockHitResult result = mc.level.clip(new ClipContext(mc.player.getEyePosition(), getDirectionOffsetPos(pos, packetResult.getDirection()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (mc.level.noCollision(mc.player.getBoundingBox().expandTowards(0.0, 0.15, 0.0)) && result != null && result.getType() == HitResult.Type.BLOCK && !result.getBlockPos().equals(pos)) {
                pitch = -75;
                raytraceTimer.reset();
            }
        }

        if (event.packet instanceof ServerboundMovePlayerPacket packet && packet.hasRotation() && !Float.isNaN(pitch)) {
            ((AccessorPlayerMoveC2SPacket) packet).hookSetPitch(pitch);
            pitch = Float.NaN;
        }

        if (TabConfigs.get().isGrim() && TabConfigs.get().miningFixConfig.get() && event.packet instanceof ServerboundPlayerActionPacket packet && (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK || packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK || packet.getAction() == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK)) {
            if (BlastResistantBlocks.isUnbreakable(packet.getPos())) {
                event.cancel();
                return;
            }

            if (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                Managers.NETWORK.sendQuietPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player != null && mc.level != null) {
            if (TabConfigs.get().isGrim() && TabConfigs.get().noScreenCloseConfig.get() && event.packet instanceof ClientboundContainerClosePacket packet && packet.getContainerId() == 0) {
                event.cancel();
            }
        }

        if (event.packet instanceof ClientboundGameEventPacket packet && packet.getEvent() == DEMO_EVENT && !mc.isDemo() && TabConfigs.get().demoConfig.get()) {
            info("Anticheat", "Network attempted to use Demo mode features on you!");
            event.cancel();
        }
        if (event.packet instanceof ClientboundResourcePackPushPacket && TabConfigs.get().resourcePackConfig.get()) {
            event.cancel();
            Managers.NETWORK.sendPacket(new ServerboundResourcePackPacket(mc.player.getUUID(), ServerboundResourcePackPacket.Action.DECLINED));
        }

        if (TabConfigs.get().antiCrashConfig.get() && mc.level != null) {
            if (event.packet instanceof ClientboundPlayerPositionPacket packet
                    && (packet.change().position().x > 30000000 || packet.change().position().y > mc.level.getMaxY()
                    || packet.change().position().z > 30000000 || packet.change().position().x < -30000000
                    || packet.change().position().y < mc.level.getMinY() || packet.change().position().z < -30000000)) {
                event.cancel();
            } else if (event.packet instanceof ClientboundExplodePacket packet
                    && (packet.center().x > 30000000 || packet.center().y > mc.level.getMaxY()
                    || packet.center().z > 30000000 || packet.center().x < -30000000
                    || packet.center().y < mc.level.getMinY() || packet.center().z < -30000000
                    || packet.playerKnockback().map(velocity -> Math.abs(velocity.x) > 1000
                    || Math.abs(velocity.y) > 1000 || Math.abs(velocity.z) > 1000).orElse(false))) {
                event.cancel();
            } else if (event.packet instanceof ClientboundSetEntityMotionPacket packet
                    && (Math.abs(packet.getMovement().x) > 1000 || Math.abs(packet.getMovement().y) > 1000
                    || Math.abs(packet.getMovement().z) > 1000)) {
                event.cancel();
            } else if (event.packet instanceof ClientboundLevelParticlesPacket packet && packet.getCount() > 500) {
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onMenuDisconnect(MenuDisconnectEvent event) {
        if (TabConfigs.get().illegalDisconnectConfig.get()) {
            Managers.NETWORK.sendPacket(new ServerboundChatPacket("§", Instant.now(), Crypt.SaltSupplier.getLong(), null, new LastSeenMessages.Update(1, new BitSet(2), (byte) 0)));
        }
    }

    @EventHandler
    public void onFriends(FriendAddedEvent event) {
        ChatUtils.sendPlayerMsg("/msg %s %s".formatted(event.friend.getName(), TabConfigs.get().customNotifySetting.get()));
    }

    public static Vec3 getDirectionOffsetPos(BlockPos pos, Direction direction) {
        Vec3 pos1 = pos.getCenter();
        return switch (direction) {
            case UP -> pos1.add(0.0, 0.5, 0.0);
            case DOWN -> pos1.add(0.0, -0.5, 0.0);
            case NORTH -> pos1.add(0.0, 0.0, -0.5);
            case SOUTH -> pos1.add(0.0, 0.0, 0.5);
            case WEST -> pos1.add(-0.5, 0.0, 0.0);
            case EAST -> pos1.add(0.5, 0.0, 0.0);
        };
    }
}
