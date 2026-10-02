package me.lyeddie.addon.mixin;

import net.minecraft.network.protocol.Packet;

public interface IClientPlayNetworkHandler {

    void sendQuietPacket(final Packet<?> packet);
}
