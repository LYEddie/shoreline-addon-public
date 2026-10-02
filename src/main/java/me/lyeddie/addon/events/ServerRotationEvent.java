package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;

public class ServerRotationEvent extends Cancellable {

    private float yaw = Float.NaN;
    private float pitch = Float.NaN;

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
