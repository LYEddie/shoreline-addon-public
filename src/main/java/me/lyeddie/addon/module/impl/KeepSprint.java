package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.SprintResetEvent;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;

public class KeepSprint extends AddonModule {
    private static KeepSprint INST;

    public KeepSprint() {
        super(Shoreline.MAIN, "KeepSprint", "Removes sprint reset when attacking");
        INST = this;
    }

    @EventHandler
    public void onSprintReset(SprintResetEvent event) {
        event.cancel();
    }

    public static KeepSprint getInstance() {
        return INST;
    }
}
