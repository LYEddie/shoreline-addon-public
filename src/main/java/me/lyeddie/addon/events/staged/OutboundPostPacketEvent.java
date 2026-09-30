package me.lyeddie.addon.events.staged;

import me.lyeddie.addon.managers.Managers;
import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.network.protocol.Packet;

public class OutboundPostPacketEvent extends Cancellable {

    private final Packet<?> packet;
    private final boolean cached;

    public OutboundPostPacketEvent(Packet<?> packet) {
        this.packet = packet;
        this.cached = Managers.NETWORK.isCached(packet); // check if works
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public boolean isClientPacket() {
        return cached;
    }
}
