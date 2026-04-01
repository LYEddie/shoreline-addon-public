package me.lyeddie.addon.imixin.impl;

import me.lyeddie.addon.imixin.IMixin;
import net.minecraft.network.packet.Packet;

@IMixin
public interface IClientPlayNetworkHandler {
    void sendQuietPacket(final Packet<?> packet);
}
