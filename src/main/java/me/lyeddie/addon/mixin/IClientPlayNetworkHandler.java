package me.lyeddie.addon.mixin;

import net.minecraft.network.packet.Packet;

public interface IClientPlayNetworkHandler {

    void sendQuietPacket(final Packet<?> packet);
}
