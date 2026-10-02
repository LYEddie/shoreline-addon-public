package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.CacheTimer;
import me.lyeddie.addon.util.Timer;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;

public class FastLatency extends AddonModule {
    private static FastLatency INST;

    private final Timer lastRequest = new CacheTimer();
    private final Timer requestTimer = new CacheTimer();
    private long requestTime;
    private long latency;

    public FastLatency() {
        super(Shoreline.MAIN, "FastLatency", "Calculates server ping");
        INST = this;
    }

    @Override
    public String getInfoString() {
        return String.format("%dms", latency);
    }

    @Override
    public void onActivate() {
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        latency = 0;
        requestTime = 0;
    }

    @EventHandler // stage¿
    public void onTick(TickEvent.Post event) {
        if (lastRequest.passed(5000) && requestTimer.passed(500)) {
            Managers.NETWORK.sendPacket(new RequestCommandCompletionsC2SPacket(1000, "w "));
            requestTimer.reset();
            lastRequest.reset();
            requestTime = System.currentTimeMillis();
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof CommandSuggestionsS2CPacket packet && packet.getCompletionId() == 1000) {
            latency = System.currentTimeMillis() - requestTime;
            lastRequest.setElapsedTime(Timer.MAX_TIME);
        }
    }

    public long getLatency() {
        return latency;
    }

    public static FastLatency getInstance() {
        return INST;
    }
}
