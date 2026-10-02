package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.TickCounterEvent;
import me.lyeddie.addon.managers.Managers;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;

import java.text.DecimalFormat;

public class TimerII extends AddonModule {
    private static TimerII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> ticksConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Ticks")
        .description("The game tick speed")
        .defaultValue(2.0)
        .min(0.1)
        .sliderMax(50.0)
        .build());
    private final Setting<Boolean> tpsSyncConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("TPSSync")
        .description("Syncs game tick speed to server tick speed")
        .defaultValue(false)
        .build());

    private float prevTimer = -1.0f;
    private float timer = 1.0f;

    public TimerII() {
        super(Shoreline.MAIN, "TimerII", "Changes the client tick speed");
        INST = this;
    }

    @Override
    public String getInfoString() {
        DecimalFormat decimal = new DecimalFormat("0.0#");
        return decimal.format(timer);
    }

    @Override // ¿
    public void toggle() {
        SpeedII.getInstance().setPrevTimer();
        if (SpeedII.getInstance().isUsingTimer()) {
            return;
        }
        super.toggle();
    }

    @Override
    public void onDeactivate() {
        Managers.TICK.setClientTick(1.0f);
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (SpeedII.getInstance().isUsingTimer()) {
            return;
        }
        if (tpsSyncConfig.get()) {
            timer = Math.max(Managers.TICK.getTpsCurrent() / 20.0f, 0.1f);
            return;
        }
        timer = toFloat(ticksConfig.get());
    }

    @EventHandler
    public void onTickCounter(TickCounterEvent event) {
        if (timer != 1.0f) {
            event.cancel();
            event.setTicks(timer);
        }
    }

    public float getTimer() {
        return timer;
    }

    public void setTimer(float timer) {
        prevTimer = this.timer;
        this.timer = timer;
    }

    public void resetTimer() {
        if (prevTimer > 0.0f) {
            this.timer = prevTimer;
            prevTimer = -1.0f;
        }
    }

    public static TimerII getInstance() {
        return INST;
    }
}
