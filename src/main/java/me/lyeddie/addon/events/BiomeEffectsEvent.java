package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.world.biome.BiomeParticleConfig;

public class BiomeEffectsEvent extends Cancellable {

    private BiomeParticleConfig particleConfig;

    public BiomeParticleConfig getParticleConfig() {
        return particleConfig;
    }

    public void setParticleConfig(BiomeParticleConfig particleConfig) {
        this.particleConfig = particleConfig;
    }
}
