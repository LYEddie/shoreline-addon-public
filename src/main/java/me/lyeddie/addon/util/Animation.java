package me.lyeddie.addon.util;

public class Animation {

    private final Easing easing;
    private float length;
    private long last = 0L;
    private boolean state;

    public Animation(float length) {
        this(false, length);
    }

    public Animation(boolean initial, float length) {
        this(initial, length, Easing.LINEAR);
    }

    public Animation(boolean initial, float length, Easing easing) {
        this.length = length;
        this.state = initial;
        this.easing = easing;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        last = (long) (!state ? System.currentTimeMillis() - ((1 - getFactor()) * length) : System.currentTimeMillis() - (getFactor() * length));
        this.state = state;
    }

    public double getFactor() {
        return easing.ease(getLinearFactor());
    }

    public double getLinearFactor() {
        return state ? clamp(((System.currentTimeMillis() - last) / length)) : clamp((1 - (System.currentTimeMillis() - last) / length));
    }

    private double clamp(double in) {
        return in < 0 ? 0 : Math.min(in, 1);
    }

    public void reset() {
        last = System.currentTimeMillis();
    }
}
