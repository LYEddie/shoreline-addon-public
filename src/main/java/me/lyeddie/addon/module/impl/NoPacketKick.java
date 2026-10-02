package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.DecodePacketEvent;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;

public class NoPacketKick extends AddonModule {
    private static NoPacketKick INST;

    public NoPacketKick() {
        super(Shoreline.MAIN, "NoPacketKick", "Prevents getting kicked by packets");
        INST = this;
    }

    @EventHandler
    public void onDecodePacket(DecodePacketEvent event) {
        event.cancel();
    }

    public static NoPacketKick getInstance() {
        return INST;
    }
}
