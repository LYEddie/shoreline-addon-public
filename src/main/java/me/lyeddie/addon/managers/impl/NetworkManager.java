package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.ConnectScreenEvent;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.imixin.impl.IClientPlayNetworkHandler;
import me.lyeddie.addon.mixin.accessor.AccessorClientWorld;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.PerSecondCounter;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.*;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;

import java.util.HashSet;
import java.util.Set;

public class NetworkManager implements Globals {
    private static final Set<Packet<?>> PACKET_CACHE = new HashSet<>();
    private final PerSecondCounter outgoingCounter = new PerSecondCounter();
    private final PerSecondCounter incomingCounter = new PerSecondCounter();
    private ServerAddress address;
    private ServerInfo info;

    public NetworkManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onConnect(ConnectScreenEvent event) {
        address = event.getAddress();
        info = event.getInfo();
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        PACKET_CACHE.clear();
    }

    @EventHandler
    public void onPacket(PacketEvent.Send event) {
        outgoingCounter.updateCounter();
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        incomingCounter.updateCounter();
    }

    public void sendPacket(final Packet<?> p) {
        if (mc.getNetworkHandler() != null) {
            PACKET_CACHE.add(p);
            mc.getNetworkHandler().sendPacket(p);
        }
    }

    public void sendQuietPacket(final Packet<?> p) {
        if (mc.getNetworkHandler() != null) {
            PACKET_CACHE.add(p);
            ((IClientPlayNetworkHandler) mc.getNetworkHandler()).sendQuietPacket(p);
        }
    }

    public void sendSequencedPacket(final SequencedPacketCreator p) {
        if (mc.world != null) {
            PendingUpdateManager updater =
                ((AccessorClientWorld) mc.world).hookGetPendingUpdateManager().incrementSequence();
            try {
                int i = updater.getSequence();
                Packet<ServerPlayPacketListener> packet = p.predict(i);
                sendPacket(packet);
            } catch (Throwable e) {
                e.printStackTrace();
                if (updater != null) {
                    try {
                        updater.close();
                    } catch (Throwable e1) {
                        e1.printStackTrace();
                        e.addSuppressed(e1);
                    }
                }
                throw e;
            }
            if (updater != null) {
                updater.close();
            }
        }
    }

    public int getClientLatency() {
        if (mc.getNetworkHandler() != null) {
            final PlayerListEntry playerEntry =
                mc.getNetworkHandler().getPlayerListEntry(mc.player.getGameProfile().getId());
            if (playerEntry != null) {
                return playerEntry.getLatency();
            }
        }
        return 0;
    }

    public ServerInfo getInfo() {
        return info;
    }

    public void setInfo(ServerInfo info) {
        this.info = info;
    }

    public boolean isCrystalPvpCC() {
        return getServerIp().contains("crystalpvp.cc");
    }

    public boolean is2b2t() {
        return getServerIp().contains("2b2t.org");
    }

    public String getServerIp() {
        if (info != null) {
            return info.address;
        }
        return "Singleplayer";
    }

    public boolean isCached(Packet<?> p) {
        return PACKET_CACHE.contains(p);
    }
}
