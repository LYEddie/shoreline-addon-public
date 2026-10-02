package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class RenderPlayerEvent extends Cancellable {

    private final AbstractClientPlayerEntity entity;
    private float yaw;
    private float pitch;

    public RenderPlayerEvent(AbstractClientPlayerEntity entity) {
        this.entity = entity;
    }

    public AbstractClientPlayerEntity getEntity() {
        return entity;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
