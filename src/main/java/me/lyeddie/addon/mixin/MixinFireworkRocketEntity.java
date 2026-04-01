package me.lyeddie.addon.mixin;

import me.lyeddie.addon.events.FireworkVelocityEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity implements Globals {

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void hookSetVelocity(LivingEntity entity, Vec3d velocity) {
        if (entity instanceof ClientPlayerEntity) {
            FireworkVelocityEvent fireworkVelocityEvent = new FireworkVelocityEvent();
            MeteorClient.EVENT_BUS.post(fireworkVelocityEvent);
            if (!fireworkVelocityEvent.isCancelled()) {
                entity.setVelocity(velocity);
            }
        }
    }
}

