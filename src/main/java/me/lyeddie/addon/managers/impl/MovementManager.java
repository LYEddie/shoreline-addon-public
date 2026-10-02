package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.PacketSneakingEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;

public class MovementManager implements Globals {
    private boolean packetSneaking;

    public MovementManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void setMotionY(double y) {
        mc.player.setDeltaMovement(mc.player.getDeltaMovement().x(), y, mc.player.getDeltaMovement().z());
    }

    public void setPacketSneaking(final boolean packetSneaking) {
        this.packetSneaking = packetSneaking;
        sendSneaking(packetSneaking);
    }

    public void sendSneaking(boolean sneaking) {
        Input input = mc.player.input.keyPresses;
        Managers.NETWORK.sendPacket(new ServerboundPlayerInputPacket(new Input(
            input.forward(), input.backward(), input.left(), input.right(), input.jump(), sneaking, input.sprint())));
    }

    @EventHandler
    public void onPacketSneak(PacketSneakingEvent event) {
        event.setCancelled(packetSneaking);
    }
}
