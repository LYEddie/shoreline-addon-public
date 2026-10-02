package me.lyeddie.addon.mixin.impl;

import com.llamalad7.mixinextras.sugar.Local;
import me.lyeddie.addon.events.PushEntityEvent;
import me.lyeddie.addon.events.SlowMovementEvent;
import me.lyeddie.addon.events.VelocityMultiplierEvent;
import me.lyeddie.addon.events.staged.UpdateVelocityEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
    protected Vec3d movementMultiplier;

    @Shadow
    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        return null;
    }

    @Shadow
    public abstract Box getBoundingBox();

    @Shadow
    public abstract Vec3d getVelocity();

    @Shadow
    public abstract void setVelocity(Vec3d velocity);

    @Inject(method = "slowMovement", at = @At(value = "HEAD"), cancellable = true)
    private void hookSlowMovement(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        if ((Object) this != mc.player) {
            return;
        }
        SlowMovementEvent slowMovementEvent = new SlowMovementEvent(state);
        MeteorClient.EVENT_BUS.post(slowMovementEvent);
        if (slowMovementEvent.isCancelled()) {
            ci.cancel();
            this.fallDistance = 0.0f;
            this.movementMultiplier = multiplier.multiply(slowMovementEvent.getMultiplier());
        }
    }

    @Inject(method = "getVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void onGetVelocityMultiplier(CallbackInfoReturnable<Float> cir, @Local BlockState blockState) {
        if ((Object) this != mc.player) return;
        float original = cir.getReturnValueF();
        VelocityMultiplierEvent event = new VelocityMultiplierEvent(blockState);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) {
            cir.setReturnValue(1.0f); // 0.3000iq play
        } else {
            cir.setReturnValue(event.getBlock().getVelocityMultiplier());
        }
    }

    @Inject(method = "updateVelocity", at = @At(value = "HEAD"), cancellable = true)
    private void hookUpdateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this == mc.player) {
            UpdateVelocityEvent updateVelocityEvent = new UpdateVelocityEvent(movementInput, speed, mc.player.getYaw(), movementInputToVelocity(movementInput, speed, mc.player.getYaw()));
            MeteorClient.EVENT_BUS.post(updateVelocityEvent);
            if (updateVelocityEvent.isCancelled()) {
                ci.cancel();
                mc.player.setVelocity(mc.player.getVelocity().add(updateVelocityEvent.getVelocity()));
            }
        }
    }

    @Inject(method = "pushAwayFrom", at = @At(value = "HEAD"), cancellable = true)
    private void hookPushAwayFrom(Entity entity, CallbackInfo ci) {
        PushEntityEvent pushEntityEvent = new PushEntityEvent((Entity) (Object) this, entity);
        MeteorClient.EVENT_BUS.post(pushEntityEvent);
        if (pushEntityEvent.isCancelled()) {
            ci.cancel();
        }
    }
}
