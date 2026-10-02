package me.lyeddie.addon.events;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public class ConnectScreenEvent {

    private final ServerAddress address;
    private final ServerData info;

    public ConnectScreenEvent(ServerAddress address, ServerData info) {
        this.address = address;
        this.info = info;
    }

    public ServerAddress getAddress() {
        return address;
    }

    public ServerData getInfo() {
        return info;
    }
}
