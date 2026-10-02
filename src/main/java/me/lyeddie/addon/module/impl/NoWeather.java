package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.BiomeEffectsEvent;
import me.lyeddie.addon.util.EnumFormatter;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.biome.BiomeParticleConfig;

public class NoWeather extends AddonModule {
    private static NoWeather INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Weather> weatherConfig = sgGeneral.add(new EnumSetting.Builder<Weather>()
        .name("Mode")
        .description("The world weather")
        .defaultValue(Weather.CLEAR)
        .build());

    private Weather weather;

    public NoWeather() {
        super(Shoreline.MAIN, "NoWeather", "Prevents weather rendering");
        INST = this;
    }

    @Override
    public String getInfoString() {
        return EnumFormatter.formatEnum(weatherConfig.get());
    }

    @Override
    public void onActivate() {
        if (mc.world != null) {
            if (mc.world.isThundering()) {
                weather = Weather.THUNDER;
            } else if (mc.world.isRaining()) {
                weather = Weather.RAIN;
            } else {
                weather = Weather.CLEAR;
            }
            setWeather(weatherConfig.get());
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.world != null && weather != null) {
            setWeather(weather);
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        setWeather(weatherConfig.get());
    }

    @EventHandler
    public void onBiomeEffects(BiomeEffectsEvent event) {
        if (weatherConfig.get() == Weather.ASH) {
            event.cancel();
            event.setParticleConfig(new BiomeParticleConfig(ParticleTypes.WHITE_ASH, 0.118093334f));
        }
    }

    private void setWeather(Weather weather) {
        switch (weather) {
            case CLEAR, ASH -> {
                mc.world.getLevelProperties().setRaining(false);
                mc.world.setRainGradient(0.0f);
                mc.world.setThunderGradient(0.0f);
            }
            case RAIN -> {
                mc.world.getLevelProperties().setRaining(true);
                mc.world.setRainGradient(1.0f);
                mc.world.setThunderGradient(0.0f);
            }
            case THUNDER -> {
                mc.world.getLevelProperties().setRaining(true);
                mc.world.setRainGradient(2.0f);
                mc.world.setThunderGradient(1.0f);
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof GameStateChangeS2CPacket packet) {
            if (packet.getReason() == GameStateChangeS2CPacket.RAIN_STARTED
                || packet.getReason() == GameStateChangeS2CPacket.RAIN_STOPPED
                || packet.getReason() == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED
                || packet.getReason() == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED) {
                event.cancel();
            }
        }
    }

    public enum Weather {
        CLEAR,
        RAIN,
        THUNDER,
        ASH
    }

    public static NoWeather getInstance() {
        return INST;
    }
}
