package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.ShulkerNestedEvent;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;

public class Shulkerception extends AddonModule {
    private static Shulkerception INST;

    public Shulkerception() {
        super(Shoreline.MAIN, "Shulkerception", "Allows you to put shulkers in shulkers");
        INST = this;
    }

    @EventHandler
    public void onShulkerNested(ShulkerNestedEvent event) {
        event.cancel();
    }

    public static Shulkerception getInstance() {
        return INST;
    }
}
