package me.lyeddie.addon.mixin.impl.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireworkRocketEntity.class)
public interface AccessorFireworkRocketEntity {

    @Accessor("attachedToEntity")
    LivingEntity hookGetShooter();

    @Invoker("isAttachedToEntity")
    boolean hookWasShotByEntity();

}
