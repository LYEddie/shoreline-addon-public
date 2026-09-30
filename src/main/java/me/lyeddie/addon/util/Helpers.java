package me.lyeddie.addon.util;

import me.lyeddie.addon.Shoreline;
import net.minecraft.network.chat.Component;

import static me.lyeddie.addon.util.Globals.mc;

/**
 * "ahh" ahh
 */
public interface Helpers {

    default void info(Class<?> klass, String par) {
        String out = "(" + klass.getSimpleName() + ") " + par;
        if (mc.player != null) mc.player.displayClientMessage(Component.nullToEmpty(out), false);
        else Shoreline.LOG.info(out);
    }

    default void info(String str, String par) {
        String out = "(%s) ".formatted(str) + par;
        if (mc.player != null) mc.player.displayClientMessage(Component.nullToEmpty(out), false);
        else Shoreline.LOG.info(out);
    }
}
