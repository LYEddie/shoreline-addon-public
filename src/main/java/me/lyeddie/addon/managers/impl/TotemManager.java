package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.irrevocable.EntityDeathEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
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
        if (mc.level == null) {
            return;
        }
        if (event.packet instanceof ClientboundEntityEventPacket packet
            && packet.getEventId() == EntityEvent.PROTECTED_FROM_DEATH) {
            Entity entity = packet.getEntity(mc.level);
            if (entity != null && entity.isAlive()) {
                if (totems.containsKey(entity.getUUID())) {
                    totems.replace(entity.getUUID(), new TotemData(System.currentTimeMillis(),
                        totems.get(entity.getUUID()).getPops() + 1));
                } else {
                    totems.put(entity.getUUID(), new TotemData(System.currentTimeMillis(), 1));
                }
            }
        }
    }

    @EventHandler(priority = -200)
    public void onRemoveEntity(EntityDeathEvent event) {
        totems.remove(event.getEntity().getUUID());
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        totems.clear();
    }

    public long getLastPopTime(Entity entity) {
        return totems.getOrDefault(entity.getUUID(), new TotemData(-1, 0)).getLastPopTime();
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
