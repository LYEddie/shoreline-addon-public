package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.module.impl.PhaseII;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.literal.RayCastUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;

public class PearlManager implements Globals {
    private float[] lastThrownAngles;
    private Box pearlBB;

    public PearlManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || !PhaseII.getInstance().shouldRaytrace()) {
            return;
        }

        if (event.packet instanceof PlayerPositionLookS2CPacket packet && lastThrownAngles != null) {
            BlockHitResult hitResult = (BlockHitResult) RayCastUtil.rayCast(3.0, lastThrownAngles);
            pearlBB = new Box(hitResult.getPos().subtract(0.4, 0.4, 0.4),
                hitResult.getPos().add(0.4, 0.4, 0.4));

            if (mc.world.getBlockState(hitResult.getBlockPos()).isAir()) {
                return;
            }

            if (!pearlBB.contains(packet.change().position())) {
                event.cancel();
                mc.getNetworkHandler().getConnection().send(new TeleportConfirmC2SPacket(packet.teleportId()));
                mc.getNetworkHandler().getConnection().send(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(),
                    mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false, mc.player.horizontalCollision));
            }
            lastThrownAngles = null;
        }
    }

    public void setLastThrownAngles(float[] lastThrownAngles) {
        this.lastThrownAngles = lastThrownAngles;
    }
}
