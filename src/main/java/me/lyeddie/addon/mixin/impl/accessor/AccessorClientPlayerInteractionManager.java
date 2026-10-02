package me.lyeddie.addon.mixin.impl.accessor;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface AccessorClientPlayerInteractionManager {

    @Invoker("syncSelectedSlot")
    void hookSyncSelectedSlot();

    @Invoker("interactBlockInternal")
    ActionResult hookInteractBlockInternal(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult);

    @Accessor("currentBreakingProgress")
    float hookGetCurrentBreakingProgress();

    @Accessor("currentBreakingProgress")
    void hookSetCurrentBreakingProgress(float currentBreakingProgress);
}
