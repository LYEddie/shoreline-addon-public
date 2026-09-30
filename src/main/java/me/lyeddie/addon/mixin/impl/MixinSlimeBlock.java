package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.SteppedOnSlimeBlockEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public class MixinSlimeBlock implements Globals {

    @Inject(method = "stepOn", at = @At(value = "HEAD"), cancellable = true)
    private void hookOnSteppedOn(Level world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        SteppedOnSlimeBlockEvent steppedOnSlimeBlockEvent = new SteppedOnSlimeBlockEvent();
        MeteorClient.EVENT_BUS.post(steppedOnSlimeBlockEvent);
        if (steppedOnSlimeBlockEvent.isCancelled() && entity == mc.player) {
            ci.cancel();
        }
    }
}
