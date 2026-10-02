package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.LevitationEvent;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;

public class AntiLevitation extends AddonModule {
    private static AntiLevitation INST;

    public AntiLevitation() {
        super(Shoreline.MAIN, "AntiLevitation", "Prevents the player from being levitated");
        INST = this;
    }

    @EventHandler
    public void onLevitation(LevitationEvent event) {
        event.cancel();
    }

    public static AntiLevitation getInstance() {
        return INST;
    }
}
