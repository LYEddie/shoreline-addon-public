package me.lyeddie.addon.mixin;

import me.lyeddie.addon.events.BiomeEffectsEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.BiomeParticleConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BiomeEffects.class)
public class MixinBiomeEffects {

    @Inject(method = "getParticleConfig", at = @At(value = "HEAD"), cancellable = true)
    private void hookGetParticleConfig(CallbackInfoReturnable<Optional<BiomeParticleConfig>> cir) {
        BiomeEffectsEvent biomeEffectsEvent = new BiomeEffectsEvent();
        MeteorClient.EVENT_BUS.post(biomeEffectsEvent);
        if (biomeEffectsEvent.isCancelled()) {
            cir.cancel();
            cir.setReturnValue(Optional.ofNullable(biomeEffectsEvent.getParticleConfig()));
        }
    }
}
