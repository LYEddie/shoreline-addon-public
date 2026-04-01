package me.lyeddie.addon.managers.impl;

import com.google.common.collect.Lists;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.TickCounterEvent;
import me.lyeddie.addon.managers.impl.util.TickSync;
import me.lyeddie.addon.util.EvictingQueue;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.util.ArrayList;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Queue;

public class TickManager implements Globals {
    private final Deque<Float> ticks = new EvictingQueue<>(20);
    private long time;
    private float clientTick = 1.0f;

    public TickManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        ticks.clear();
    }

    @EventHandler
    public void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.packet instanceof WorldTimeUpdateS2CPacket) {
            float last = 20000.0f / (System.currentTimeMillis() - time);
            ticks.addFirst(last);
            time = System.currentTimeMillis();
        }
    }

    public void setClientTick(float ticks) {
        clientTick = ticks;
    }

    @EventHandler
    public void onTickCounter(TickCounterEvent event) {
        if (clientTick != 1.0f) {
            event.cancel();
            event.setTicks(clientTick);
        }
    }

    public Queue<Float> getTicks() {
        return ticks;
    }

    public float getTpsAverage() {
        float avg = 0.0f;
        try {
            ArrayList<Float> ticksCopy = Lists.newArrayList(ticks);
            if (!ticksCopy.isEmpty()) {
                for (float t : ticksCopy) {
                    avg += t;
                }
                avg /= Math.max(ticksCopy.size(), 1.0f);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return Math.min(100.0f, avg);
    }

    public float getTpsCurrent() {
        try {
            if (!ticks.isEmpty()) {
                return Math.min(100.0f, ticks.getFirst());
            }
        } catch (NoSuchElementException ignored) {

        }
        return 20.0f;
    }

    public float getTpsMin() {
        float min = 20.0f;
        try {
            for (float t : ticks) {
                if (t < min) {
                    min = t;
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return min;
    }

    public float getTickSync(TickSync tps) {
        return switch (tps) {
            case AVERAGE -> getTpsAverage();
            case CURRENT -> getTpsCurrent();
            case MINIMAL -> getTpsMin();
            case NONE -> 20.0f;
        };
    }
}
