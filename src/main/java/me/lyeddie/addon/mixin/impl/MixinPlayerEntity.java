package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.PushFluidsEvent;
import me.lyeddie.addon.events.SprintResetEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerEntity extends LivingEntity implements Globals {

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow
    public abstract void travel(Vec3 movementInput);

    @Inject(method = "isPushedByFluid", at = @At(value = "HEAD"), cancellable = true)
    private void hookIsPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != mc.player) {
            return;
        }
        PushFluidsEvent pushFluidsEvent = new PushFluidsEvent();
        MeteorClient.EVENT_BUS.post(pushFluidsEvent);
        if (pushFluidsEvent.isCancelled()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Redirect(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void hookAttack(Player playerEntity, Vec3 movementInput) {
        if (playerEntity instanceof LocalPlayer) {
            SprintResetEvent sprintResetEvent = new SprintResetEvent();
            MeteorClient.EVENT_BUS.post(sprintResetEvent);
            if (!sprintResetEvent.isCancelled()) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }
        }
    }

    @Redirect(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
    private void hookAttack$1(Player instance, boolean b) {
        if (instance instanceof LocalPlayer) {
            SprintResetEvent sprintResetEvent = new SprintResetEvent();
            MeteorClient.EVENT_BUS.post(sprintResetEvent);
            if (!sprintResetEvent.isCancelled()) {
                mc.player.setSprinting(false);
            }
        }
    }
}
