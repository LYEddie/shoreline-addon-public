package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.PacketSneakingEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.PlayerInput;

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
        sendSneaking(packetSneaking);
    }

    public void sendSneaking(boolean sneaking) {
        PlayerInput input = mc.player.input.playerInput;
        Managers.NETWORK.sendPacket(new PlayerInputC2SPacket(new PlayerInput(
            input.forward(), input.backward(), input.left(), input.right(), input.jump(), sneaking, input.sprint())));
    }

    @EventHandler
    public void onPacketSneak(PacketSneakingEvent event) {
        event.setCancelled(packetSneaking);
    }
}
