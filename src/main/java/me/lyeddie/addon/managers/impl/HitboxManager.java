package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.util.math.Box;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HitboxManager implements Globals {
    private final List<Entity> serverCrawling = new CopyOnWriteArrayList<>();

    public HitboxManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (event.packet instanceof EntityTrackerUpdateS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.id());
            if (!(entity instanceof PlayerEntity)) {
                return;
            }

            for (DataTracker.SerializedEntry<?> serializedEntry : packet.trackedValues()) {
                DataTracker.Entry<?> entry = entity.getDataTracker().entries[serializedEntry.id()];
                if (!entry.getData().equals(Entity.POSE)) {
                    continue;
                }

                if (!serializedEntry.value().equals(EntityPose.SWIMMING)) {
                    serverCrawling.remove(entity);
                    continue;
                }

                if (!serverCrawling.contains(entity)) {
                    serverCrawling.add(entity);
                }
            }
        }
    }

    public boolean isServerCrawling(Entity entity) {
        return serverCrawling.contains(entity);
    }

    public Box getCrawlingBoundingBox(Entity entity) {
        return entity.getDimensions(EntityPose.SWIMMING).getBoxAt(entity.getEntityPos());
    }
}
