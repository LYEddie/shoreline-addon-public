package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.PacketSneakingEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import static net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY;
import static net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;

public class MovementManager implements Globals {
    private boolean packetSneaking;

    public MovementManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void setMotionY(double y) {
        mc.player.setVelocity(mc.player.getVelocity().getX(), y, mc.player.getVelocity().getZ());
    }

    public void setPacketSneaking(final boolean packetSneaking) {
        this.packetSneaking = packetSneaking;
        if (packetSneaking) {
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, PRESS_SHIFT_KEY));
        } else {
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, RELEASE_SHIFT_KEY));
        }
    }

    @EventHandler
    public void onPacketSneak(PacketSneakingEvent event) {
        event.setCancelled(packetSneaking);
    }
}
