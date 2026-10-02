package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.managers.impl.util.SetbackData;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.Helpers;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import java.util.Arrays;

public final class AntiCheatManager implements Globals, Helpers {
    private final int[] transactions = new int[4];
    private SetbackData lastSetback;
    private int index;
    private boolean isGrim;

    public AntiCheatManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
        Arrays.fill(transactions, -1);
    }

    @EventHandler
    public void onPacketInbound(final PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundPingPacket packet) {
            if (index > 3) {
                return;
            }
            final int uid = packet.getId();
            transactions[index] = uid;
            ++index;
            if (index == 4) {
                grimCheck();
            }
        } else if (event.packet instanceof ClientboundPlayerPositionPacket packet) {
            lastSetback = new SetbackData(packet.change().position(),
                System.currentTimeMillis(), packet.id());
        }
    }

    @EventHandler
    public void onDisconnect(final DisconnectEvent event) {
        Arrays.fill(transactions, -1);
        index = 0;
        isGrim = false;
    }

    private void grimCheck() {
        for (int i = 0; i < 4; ++i) {
            if (transactions[i] != -i) {
                break;
            }
        }
        isGrim = true;
        info("AntiCheatManager", "GlobalServer is running GrimAC.");
    }

    public boolean isGrim() {
        return isGrim;
    }

    public boolean hasPassed(final long timeMS) {
        return lastSetback != null && lastSetback.timeSince() >= timeMS;
    }
}
