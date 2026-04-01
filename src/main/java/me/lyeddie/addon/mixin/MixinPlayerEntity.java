package me.lyeddie.addon.mixin;

import me.lyeddie.addon.events.PushFluidsEvent;
import me.lyeddie.addon.events.SprintResetEvent;
import me.lyeddie.addon.events.staged.PostPlayerJumpEvent;
import me.lyeddie.addon.events.staged.PrePlayerJumpEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements Globals {

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    public abstract void travel(Vec3d movementInput);

    @Inject(method = "isPushedByFluids", at = @At(value = "HEAD"), cancellable = true)
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

    @Inject(method = "jump", at = @At(value = "HEAD"), cancellable = true)
    private void hookJumpPre(CallbackInfo ci) {
        if ((Object) this != mc.player) {
            return;
        }
        PrePlayerJumpEvent playerJumpEvent = new PrePlayerJumpEvent();
        MeteorClient.EVENT_BUS.post(playerJumpEvent);
        if (playerJumpEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "jump", at = @At(value = "RETURN"), cancellable = true)
    private void hookJumpPost(CallbackInfo ci) {
        if ((Object) this != mc.player) {
            return;
        }
        PostPlayerJumpEvent playerJumpEvent = new PostPlayerJumpEvent();
        MeteorClient.EVENT_BUS.post(playerJumpEvent);
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void hookAttack(PlayerEntity playerEntity, Vec3d movementInput) {
        if (playerEntity instanceof ClientPlayerEntity) {
            SprintResetEvent sprintResetEvent = new SprintResetEvent();
            MeteorClient.EVENT_BUS.post(sprintResetEvent);
            if (!sprintResetEvent.isCancelled()) {
                mc.player.setVelocity(mc.player.getVelocity().multiply(0.6, 1.0, 0.6));
            }
        }
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"))
    private void hookAttack$1(PlayerEntity instance, boolean b) {
        if (instance instanceof ClientPlayerEntity) {
            SprintResetEvent sprintResetEvent = new SprintResetEvent();
            MeteorClient.EVENT_BUS.post(sprintResetEvent);
            if (!sprintResetEvent.isCancelled()) {
                mc.player.setSprinting(false);
            }
        }
    }
}
