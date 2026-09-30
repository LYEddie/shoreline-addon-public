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
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.time.Instant;
import java.util.BitSet;

import static net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN;

public class TabEvents implements Globals, Helpers {

    public final Timer raytraceTimer = new CacheTimer();
    public float pitch = Float.NaN;

    public TabEvents() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler(priority = 200)
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;

        if (TabConfigs.get().isNCP() && TabConfigs.get().raytraceSpoofConfig.get() && event.packet instanceof PlayerInteractBlockC2SPacket packet && raytraceTimer.passed(250)) {
            BlockHitResult packetResult = packet.getBlockHitResult();
            BlockPos pos = packetResult.getBlockPos();
            BlockHitResult result = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), getDirectionOffsetPos(pos, packetResult.getSide()), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (mc.world.isSpaceEmpty(mc.player.getBoundingBox().stretch(0.0, 0.15, 0.0)) && result != null && result.getType() == HitResult.Type.BLOCK && !result.getBlockPos().equals(pos)) {
                pitch = -75;
                raytraceTimer.reset();
            }
        }

        if (event.packet instanceof PlayerMoveC2SPacket packet && packet.changesLook() && !Float.isNaN(pitch)) {
            ((AccessorPlayerMoveC2SPacket) packet).hookSetPitch(pitch);
            pitch = Float.NaN;
        }

        if (TabConfigs.get().isGrim() && TabConfigs.get().miningFixConfig.get() && event.packet instanceof PlayerActionC2SPacket packet && (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK || packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK || packet.getAction() == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)) {
            if (BlastResistantBlocks.isUnbreakable(packet.getPos())) {
                event.cancel();
                return;
            }

            if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
                Managers.NETWORK.sendQuietPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                Managers.NETWORK.sendQuietPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                Managers.NETWORK.sendQuietPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos(), Direction.UP));
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player != null && mc.world != null) {
            if (TabConfigs.get().isGrim() && TabConfigs.get().noScreenCloseConfig.get() && event.packet instanceof CloseScreenS2CPacket packet && packet.getSyncId() == 0) {
                event.cancel();
            }
        }

        if (event.packet instanceof GameStateChangeS2CPacket packet && packet.getReason() == DEMO_MESSAGE_SHOWN && !mc.isDemo() && TabConfigs.get().demoConfig.get()) {
            info("Anticheat", "Network attempted to use Demo mode features on you!");
            event.cancel();
        }
        if (event.packet instanceof ResourcePackSendS2CPacket && TabConfigs.get().resourcePackConfig.get()) {
            event.cancel();
            Managers.NETWORK.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.DECLINED));
        }

        if (TabConfigs.get().antiCrashConfig.get() && mc.world != null) {
            if (event.packet instanceof PlayerPositionLookS2CPacket packet
                    && (packet.change().position().x > 30000000 || packet.change().position().y > mc.world.getTopYInclusive()
                    || packet.change().position().z > 30000000 || packet.change().position().x < -30000000
                    || packet.change().position().y < mc.world.getBottomY() || packet.change().position().z < -30000000)) {
                event.cancel();
            } else if (event.packet instanceof ExplosionS2CPacket packet
                    && (packet.center().x > 30000000 || packet.center().y > mc.world.getTopYInclusive()
                    || packet.center().z > 30000000 || packet.center().x < -30000000
                    || packet.center().y < mc.world.getBottomY() || packet.center().z < -30000000
                    || packet.playerKnockback().map(velocity -> Math.abs(velocity.x) > 1000
                    || Math.abs(velocity.y) > 1000 || Math.abs(velocity.z) > 1000).orElse(false))) {
                event.cancel();
            } else if (event.packet instanceof EntityVelocityUpdateS2CPacket packet
                    && (Math.abs(packet.getVelocity().x) > 1000 || Math.abs(packet.getVelocity().y) > 1000
                    || Math.abs(packet.getVelocity().z) > 1000)) {
                event.cancel();
            } else if (event.packet instanceof ParticleS2CPacket packet && packet.getCount() > 500) {
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onMenuDisconnect(MenuDisconnectEvent event) {
        if (TabConfigs.get().illegalDisconnectConfig.get()) {
            Managers.NETWORK.sendPacket(new ChatMessageC2SPacket("§", Instant.now(), NetworkEncryptionUtils.SecureRandomUtil.nextLong(), null, new LastSeenMessageList.Acknowledgment(1, new BitSet(2), (byte) 0)));
        }
    }

    @EventHandler
    public void onFriends(FriendAddedEvent event) {
        ChatUtils.sendPlayerMsg("/msg %s %s".formatted(event.friend.getName(), TabConfigs.get().customNotifySetting.get()));
    }

    public static Vec3d getDirectionOffsetPos(BlockPos pos, Direction direction) {
        Vec3d pos1 = pos.toCenterPos();
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
