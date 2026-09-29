package me.lyeddie.addon.imixin.impl;

import net.minecraft.network.packet.Packet;

public interface IClientPlayNetworkHandler {

    void sendQuietPacket(final Packet<?> packet);
}
