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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.world.attribute.AmbientParticle;

import java.util.List;

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
        if (mc.level != null) {
            if (mc.level.isThundering()) {
                weather = Weather.THUNDER;
            } else if (mc.level.isRaining()) {
                weather = Weather.RAIN;
            } else {
                weather = Weather.CLEAR;
            }
            setWeather(weatherConfig.get());
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.level != null && weather != null) {
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
            event.setParticles(List.of(new AmbientParticle(ParticleTypes.WHITE_ASH, 0.118093334f)));
        }
    }

    private void setWeather(Weather weather) {
        switch (weather) {
            case CLEAR, ASH -> {
                mc.level.setRainLevel(0.0f);
                mc.level.setThunderLevel(0.0f);
            }
            case RAIN -> {
                mc.level.setRainLevel(1.0f);
                mc.level.setThunderLevel(0.0f);
            }
            case THUNDER -> {
                mc.level.setRainLevel(2.0f);
                mc.level.setThunderLevel(1.0f);
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundGameEventPacket packet) {
            if (packet.getEvent() == ClientboundGameEventPacket.START_RAINING
                || packet.getEvent() == ClientboundGameEventPacket.STOP_RAINING
                || packet.getEvent() == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE
                || packet.getEvent() == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
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
