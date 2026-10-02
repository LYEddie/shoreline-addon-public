package me.lyeddie.addon.events;

import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public class ConnectScreenEvent {

    private final ServerAddress address;
    private final ServerInfo info;

    public ConnectScreenEvent(ServerAddress address, ServerInfo info) {
        this.address = address;
        this.info = info;
    }

    public ServerAddress getAddress() {
        return address;
    }

    public ServerInfo getInfo() {
        return info;
    }
}
