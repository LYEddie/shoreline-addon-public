package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;

public class TickCounterEvent extends Cancellable {

    private float ticks;

    public float getTicks() {
        return ticks;
    }

    public void setTicks(float ticks) {
        this.ticks = ticks;
    }
}
