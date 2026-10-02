package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.player.AbstractClientPlayer;

public class RenderPlayerEvent extends Cancellable {

    private final AbstractClientPlayer entity;
    private float yaw;
    private float pitch;

    public RenderPlayerEvent(AbstractClientPlayer entity) {
        this.entity = entity;
    }

    public AbstractClientPlayer getEntity() {
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
