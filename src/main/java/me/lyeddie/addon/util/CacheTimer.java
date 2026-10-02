package me.lyeddie.addon.util;

import java.util.concurrent.TimeUnit;

public class CacheTimer implements Timer {
    private long time;
    private long lastResetTime;

    public CacheTimer() {
        this.time = System.nanoTime();
    }

    @Override
    public boolean passed(Number time) {
        if (time.longValue() <= 0) {
            return true;
        }
        return getElapsedTime() > time.longValue();
    }

    public boolean passed(Number time, TimeUnit unit) {
        return passed(unit.toMillis(time.longValue()));
    }

    @Override
    public long getElapsedTime() {
        return toMillis(System.nanoTime() - time);
    }

    @Override
    public void setElapsedTime(Number time) {
        this.time = time.longValue() == MAX_TIME ? 0 :
            System.nanoTime() - time.longValue();
    }

    public void setDelay(Number delay) {
        this.time += delay.longValue();
    }

    public long getLastResetTime() {
        return lastResetTime;
    }

    @Override
    public void reset() {
        long time = System.nanoTime();
        lastResetTime = time - this.time;

        this.time = time;
    }

    private long toMillis(long nanos) {
        return nanos / 1000000;
    }
}
