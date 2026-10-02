package me.lyeddie.addon.mixin.impl;

import com.llamalad7.mixinextras.sugar.Local;
import me.lyeddie.addon.events.PushEntityEvent;
import me.lyeddie.addon.events.SlowMovementEvent;
import me.lyeddie.addon.events.VelocityMultiplierEvent;
import me.lyeddie.addon.events.staged.UpdateVelocityEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity implements Globals {

    @Shadow
    public double fallDistance;
    @Shadow
    protected Vec3 stuckSpeedMultiplier;

    @Shadow
    private static Vec3 getInputVector(Vec3 movementInput, float speed, float yaw) {
        return null;
    }

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract void setDeltaMovement(Vec3 velocity);

    @Inject(method = "makeStuckInBlock", at = @At(value = "HEAD"), cancellable = true)
    private void hookSlowMovement(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if ((Object) this != mc.player) {
            return;
        }
        SlowMovementEvent slowMovementEvent = new SlowMovementEvent(state);
        MeteorClient.EVENT_BUS.post(slowMovementEvent);
        if (slowMovementEvent.isCancelled()) {
            ci.cancel();
            this.fallDistance = 0.0f;
            this.stuckSpeedMultiplier = multiplier.scale(slowMovementEvent.getMultiplier());
        }
    }

    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void onGetVelocityMultiplier(CallbackInfoReturnable<Float> cir, @Local BlockState blockState) {
        if ((Object) this != mc.player) return;
        float original = cir.getReturnValueF();
        VelocityMultiplierEvent event = new VelocityMultiplierEvent(blockState);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) {
            cir.setReturnValue(1.0f); // 0.3000iq play
        } else {
            cir.setReturnValue(event.getBlock().getSpeedFactor());
        }
    }

    @Inject(method = "moveRelative", at = @At(value = "HEAD"), cancellable = true)
    private void hookUpdateVelocity(float speed, Vec3 movementInput, CallbackInfo ci) {
        if ((Object) this == mc.player) {
            UpdateVelocityEvent updateVelocityEvent = new UpdateVelocityEvent(movementInput, speed, mc.player.getYRot(), getInputVector(movementInput, speed, mc.player.getYRot()));
            MeteorClient.EVENT_BUS.post(updateVelocityEvent);
            if (updateVelocityEvent.isCancelled()) {
                ci.cancel();
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(updateVelocityEvent.getVelocity()));
            }
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "HEAD"), cancellable = true)
    private void hookPushAwayFrom(Entity entity, CallbackInfo ci) {
        PushEntityEvent pushEntityEvent = new PushEntityEvent((Entity) (Object) this, entity);
        MeteorClient.EVENT_BUS.post(pushEntityEvent);
        if (pushEntityEvent.isCancelled()) {
            ci.cancel();
        }
    }
}
