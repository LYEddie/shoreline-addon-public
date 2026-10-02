package me.lyeddie.addon.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;

/**
 * tech?
 */
public interface Globals {
    Minecraft mc = Minecraft.getInstance();
    Random RANDOM = ThreadLocalRandom.current();
}

