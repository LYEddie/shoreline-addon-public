package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.world.attribute.AmbientParticle;

import java.util.List;

public class BiomeEffectsEvent extends Cancellable {

    private List<AmbientParticle> particles;

    public List<AmbientParticle> getParticles() {
        return particles;
    }

    public void setParticles(List<AmbientParticle> particles) {
        this.particles = particles;
    }
}
