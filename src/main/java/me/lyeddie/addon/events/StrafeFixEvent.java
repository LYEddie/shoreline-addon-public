package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;

public class StrafeFixEvent extends Cancellable {

    private float yaw, pitch;

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
