package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.SteppedOnSlimeBlockEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public class MixinSlimeBlock implements Globals {

    @Inject(method = "onSteppedOn", at = @At(value = "HEAD"), cancellable = true)
    private void hookOnSteppedOn(World world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        SteppedOnSlimeBlockEvent steppedOnSlimeBlockEvent = new SteppedOnSlimeBlockEvent();
        MeteorClient.EVENT_BUS.post(steppedOnSlimeBlockEvent);
        if (steppedOnSlimeBlockEvent.isCancelled() && entity == mc.player) {
            ci.cancel();
        }
    }
}
