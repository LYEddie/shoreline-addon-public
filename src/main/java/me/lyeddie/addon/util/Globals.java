package me.lyeddie.addon.util;

import net.minecraft.client.MinecraftClient;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * tech?
 */
public interface Globals {
    MinecraftClient mc = MinecraftClient.getInstance();
    Random RANDOM = ThreadLocalRandom.current();
}

