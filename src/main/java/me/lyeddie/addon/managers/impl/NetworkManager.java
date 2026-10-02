package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.ConnectScreenEvent;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.mixin.IClientPlayNetworkHandler;
import me.lyeddie.addon.mixin.impl.accessor.AccessorClientWorld;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.PerSecondCounter;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import java.util.HashSet;
import java.util.Set;

public class NetworkManager implements Globals {
    private static final Set<Packet<?>> PACKET_CACHE = new HashSet<>();
    private final PerSecondCounter outgoingCounter = new PerSecondCounter();
    private final PerSecondCounter incomingCounter = new PerSecondCounter();
    private ServerAddress address;
    private ServerData info;

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
        if (mc.getConnection() != null) {
            PACKET_CACHE.add(p);
            mc.getConnection().send(p);
        }
    }

    public void sendQuietPacket(final Packet<?> p) {
        if (mc.getConnection() != null) {
            PACKET_CACHE.add(p);
            ((IClientPlayNetworkHandler) mc.getConnection()).sendQuietPacket(p);
        }
    }

    public void sendSequencedPacket(final PredictiveAction p) {
        if (mc.level != null) {
            BlockStatePredictionHandler updater =
                ((AccessorClientWorld) mc.level).hookGetPendingUpdateManager().startPredicting();
            try {
                int i = updater.currentSequence();
                Packet<ServerGamePacketListener> packet = p.predict(i);
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
        if (mc.getConnection() != null) {
            final PlayerInfo playerEntry =
                mc.getConnection().getPlayerInfo(mc.player.getGameProfile().id());
            if (playerEntry != null) {
                return playerEntry.getLatency();
            }
        }
        return 0;
    }

    public ServerData getInfo() {
        return info;
    }

    public void setInfo(ServerData info) {
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
            return info.ip;
        }
        return "Singleplayer";
    }

    public boolean isCached(Packet<?> p) {
        return PACKET_CACHE.contains(p);
    }
}
