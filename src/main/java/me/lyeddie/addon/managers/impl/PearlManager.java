package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.module.impl.PhaseII;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.literal.RayCastUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public class PearlManager implements Globals {
    private float[] lastThrownAngles;
    private AABB pearlBB;

    public PearlManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || !PhaseII.getInstance().shouldRaytrace()) {
            return;
        }

        if (event.packet instanceof ClientboundPlayerPositionPacket packet && lastThrownAngles != null) {
            BlockHitResult hitResult = (BlockHitResult) RayCastUtil.rayCast(3.0, lastThrownAngles);
            pearlBB = new AABB(hitResult.getLocation().subtract(0.4, 0.4, 0.4),
                hitResult.getLocation().add(0.4, 0.4, 0.4));

            if (mc.level.getBlockState(hitResult.getBlockPos()).isAir()) {
                return;
            }

            if (!pearlBB.contains(packet.change().position())) {
                event.cancel();
                mc.getConnection().getConnection().send(new ServerboundAcceptTeleportationPacket(packet.id()));
                mc.getConnection().getConnection().send(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(),
                    mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision));
            }
            lastThrownAngles = null;
        }
    }

    public void setLastThrownAngles(float[] lastThrownAngles) {
        this.lastThrownAngles = lastThrownAngles;
    }
}
