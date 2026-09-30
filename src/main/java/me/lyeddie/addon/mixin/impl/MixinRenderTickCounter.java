package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.TickCounterEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public class MixinRenderTickCounter {

    @Shadow
    private float deltaTicks;

    @Shadow
    private float deltaTickResidual;

    @Shadow
    private long lastMs;

    @Final
    @Shadow
    private float msPerTick;

    @Inject(method = "advanceGameTime(J)I", at = @At(value = "HEAD"), cancellable = true)
    private void hookBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        TickCounterEvent tickCounterEvent = new TickCounterEvent();
        MeteorClient.EVENT_BUS.post(tickCounterEvent);
        if (tickCounterEvent.isCancelled()) {
            deltaTicks = ((timeMillis - lastMs) / msPerTick) * tickCounterEvent.getTicks();
            lastMs = timeMillis;
            deltaTickResidual += deltaTicks;
            int i = (int) deltaTickResidual;
            deltaTickResidual -= i;
            cir.setReturnValue(i);
        }
    }
}
