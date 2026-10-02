package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.BiomeEffectsEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldEnvironmentAttributeAccess.class)
public class MixinBiomeEffects {

    @Inject(method = "getAttributeValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
    private void hookGetAttributeValue(EnvironmentAttribute<?> attribute, CallbackInfoReturnable<Object> cir) {
        if (attribute != EnvironmentAttributes.AMBIENT_PARTICLES_VISUAL) {
            return;
        }
        BiomeEffectsEvent biomeEffectsEvent = new BiomeEffectsEvent();
        MeteorClient.EVENT_BUS.post(biomeEffectsEvent);
        if (biomeEffectsEvent.isCancelled()) {
            cir.setReturnValue(biomeEffectsEvent.getParticles());
        }
    }
}
