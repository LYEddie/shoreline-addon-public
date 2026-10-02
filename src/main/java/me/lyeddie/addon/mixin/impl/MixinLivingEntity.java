package me.lyeddie.addon.mixin.impl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.lyeddie.addon.events.JumpRotationEvent;
import me.lyeddie.addon.events.LevitationEvent;
import me.lyeddie.addon.events.PlayerClimbEvent;
import me.lyeddie.addon.events.staged.PostPlayerJumpEvent;
import me.lyeddie.addon.events.staged.PrePlayerJumpEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity implements Globals {

    @Shadow
    public abstract float getViewYRot(float tickDelta);

    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
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

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void hookJumpPre(CallbackInfo ci) {
        if ((Object) this != mc.player) return;

        PrePlayerJumpEvent event = new PrePlayerJumpEvent();
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Inject(method = "jumpFromGround", at = @At("RETURN"))
    private void hookJumpPost(CallbackInfo ci) {
        if ((Object) this == mc.player) {
            MeteorClient.EVENT_BUS.post(new PostPlayerJumpEvent());
        }
    }

    @Redirect(method = "travelInAir", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEffect(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"))
    private MobEffectInstance hookGetStatusEffect(LivingEntity instance, Holder<MobEffect> effect) {
        MobEffectInstance statusEffect = instance.getEffect(effect);
        if (instance.equals(mc.player) && effect == MobEffects.LEVITATION) {
            LevitationEvent levitationEvent = new LevitationEvent();
            MeteorClient.EVENT_BUS.post(levitationEvent);
            return levitationEvent.isCancelled() ? null : statusEffect;
        }
        return statusEffect;
    }

    @Inject(method = "onClimbable", at = @At(value = "HEAD"), cancellable = true)
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
