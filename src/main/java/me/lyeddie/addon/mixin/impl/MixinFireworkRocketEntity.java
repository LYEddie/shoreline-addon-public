package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.FireworkVelocityEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity implements Globals {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void hookSetVelocity(LivingEntity entity, Vec3 velocity) {
        if (entity instanceof LocalPlayer) {
            FireworkVelocityEvent fireworkVelocityEvent = new FireworkVelocityEvent();
            MeteorClient.EVENT_BUS.post(fireworkVelocityEvent);
            if (!fireworkVelocityEvent.isCancelled()) {
                entity.setDeltaMovement(velocity);
            }
        }
    }
}

