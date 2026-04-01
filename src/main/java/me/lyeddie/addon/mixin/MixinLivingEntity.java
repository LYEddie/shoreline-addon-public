package me.lyeddie.addon.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.lyeddie.addon.events.JumpRotationEvent;
import me.lyeddie.addon.events.LevitationEvent;
import me.lyeddie.addon.events.PlayerClimbEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity implements Globals {

    @Shadow
    public abstract float getYaw(float tickDelta);

    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> par1);

    @ModifyExpressionValue(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float hookJump$getYaw(float original) {
        if ((Object) this != mc.player) {
            return original;
        }

        JumpRotationEvent jumpRotationEvent = new JumpRotationEvent(original);
        MeteorClient.EVENT_BUS.post(jumpRotationEvent);
        if (jumpRotationEvent.isCancelled()) {
            return jumpRotationEvent.getYaw();
        }

        return original;
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    private boolean hookHasStatusEffect(LivingEntity instance, RegistryEntry<StatusEffect> effect) {
        if (instance.equals(mc.player) && effect == StatusEffects.LEVITATION) {
            LevitationEvent levitationEvent = new LevitationEvent();
            MeteorClient.EVENT_BUS.post(levitationEvent);
            return !levitationEvent.isCancelled() && hasStatusEffect(effect);
        }
        return hasStatusEffect(effect);
    }

    @Inject(method = "isClimbing", at = @At(value = "HEAD"), cancellable = true)
    private void hookIsClimbing(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != mc.player) {
            return;
        }

        PlayerClimbEvent playerClimbEvent = new PlayerClimbEvent();
        MeteorClient.EVENT_BUS.post(playerClimbEvent);
        if (playerClimbEvent.isCancelled()) {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }
}
