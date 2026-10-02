package me.lyeddie.addon.util;

public interface Timer {
    long MAX_TIME = -0xff;
    boolean passed(Number time);
    void reset();
    long getElapsedTime();
    void setElapsedTime(Number time);
}
