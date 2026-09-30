package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HitboxManager implements Globals {
    private final List<Entity> serverCrawling = new CopyOnWriteArrayList<>();

    public HitboxManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (event.packet instanceof ClientboundSetEntityDataPacket packet) {
            Entity entity = mc.level.getEntity(packet.id());
            if (!(entity instanceof Player)) {
                return;
            }

            for (SynchedEntityData.DataValue<?> serializedEntry : packet.packedItems()) {
                SynchedEntityData.DataItem<?> entry = entity.getEntityData().itemsById[serializedEntry.id()];
                if (!entry.getAccessor().equals(Entity.DATA_POSE)) {
                    continue;
                }

                if (!serializedEntry.value().equals(Pose.SWIMMING)) {
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

    public AABB getCrawlingBoundingBox(Entity entity) {
        return entity.getDimensions(Pose.SWIMMING).makeBoundingBox(entity.position());
    }
}
