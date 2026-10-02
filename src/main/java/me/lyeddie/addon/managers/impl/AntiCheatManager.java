package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.managers.impl.util.SetbackData;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.Helpers;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;
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
        if (event.packet instanceof CommonPingS2CPacket packet) {
            if (index > 3) {
                return;
            }
            final int uid = packet.getParameter();
            transactions[index] = uid;
            ++index;
            if (index == 4) {
                grimCheck();
            }
        } else if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            lastSetback = new SetbackData(new Vec3d(packet.getX(), packet.getY(), packet.getZ()),
                System.currentTimeMillis(), packet.getTeleportId());
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
