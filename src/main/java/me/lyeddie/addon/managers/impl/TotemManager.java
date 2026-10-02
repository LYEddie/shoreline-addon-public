package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.irrevocable.EntityDeathEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TotemManager implements Globals {
    private final ConcurrentMap<UUID, TotemData> totems = new ConcurrentHashMap<>();

    public TotemManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.world == null) {
            return;
        }
        if (event.packet instanceof EntityStatusS2CPacket packet
            && packet.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
            Entity entity = packet.getEntity(mc.world);
            if (entity != null && entity.isAlive()) {
                if (totems.containsKey(entity.getUuid())) {
                    totems.replace(entity.getUuid(), new TotemData(System.currentTimeMillis(),
                        totems.get(entity.getUuid()).getPops() + 1));
                } else {
                    totems.put(entity.getUuid(), new TotemData(System.currentTimeMillis(), 1));
                }
            }
        }
    }

    @EventHandler(priority = -200)
    public void onRemoveEntity(EntityDeathEvent event) {
        totems.remove(event.getEntity().getUuid());
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        totems.clear();
    }

    public long getLastPopTime(Entity entity) {
        return totems.getOrDefault(entity.getUuid(), new TotemData(-1, 0)).getLastPopTime();
    }

    public static class TotemData {
        private final long lastPopTime;
        private final int pops;

        public TotemData(long lastPopTime, int pops) {
            this.lastPopTime = lastPopTime;
            this.pops = pops;
        }

        public int getPops() {
            return pops;
        }

        public long getLastPopTime() {
            return lastPopTime;
        }
    }
}
