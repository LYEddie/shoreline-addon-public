package me.lyeddie.addon.managers.impl.util;

import me.lyeddie.addon.util.Timer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

public class TickTimer implements Timer {
    private long ticks;

    public TickTimer() {
        ticks = 0;
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler(priority = 200)
    public void onTick(TickEvent.Pre event) {
        ++ticks;
    }

    @Override
    public boolean passed(Number time) {
        return ticks >= time.longValue();
    }

    @Override
    public void reset() {
        setElapsedTime(0);
    }

    @Override
    public long getElapsedTime() {
        return ticks;
    }

    @Override
    public void setElapsedTime(Number time) {
        ticks = time.longValue();
    }
}
