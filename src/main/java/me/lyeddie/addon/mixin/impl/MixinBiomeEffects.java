package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.BiomeEffectsEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeSystem.class)
public class MixinBiomeEffects {

    @Inject(method = "getDimensionValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
    private void hookGetAttributeValue(EnvironmentAttribute<?> attribute, CallbackInfoReturnable<Object> cir) {
        if (attribute != EnvironmentAttributes.AMBIENT_PARTICLES) {
            return;
        }
        BiomeEffectsEvent biomeEffectsEvent = new BiomeEffectsEvent();
        MeteorClient.EVENT_BUS.post(biomeEffectsEvent);
        if (biomeEffectsEvent.isCancelled()) {
            cir.setReturnValue(biomeEffectsEvent.getParticles());
        }
    }
}
