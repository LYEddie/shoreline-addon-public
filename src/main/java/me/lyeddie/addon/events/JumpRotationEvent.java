package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;

public class JumpRotationEvent extends Cancellable {

    private float yaw;

    public JumpRotationEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
