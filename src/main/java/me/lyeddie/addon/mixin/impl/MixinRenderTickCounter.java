package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.TickCounterEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class MixinRenderTickCounter {

    @Shadow
    private float dynamicDeltaTicks;

    @Shadow
    private float tickProgress;

    @Shadow
    private long lastTimeMillis;

    @Final
    @Shadow
    private float tickTime;

    @Inject(method = "beginRenderTick(J)I", at = @At(value = "HEAD"), cancellable = true)
    private void hookBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        TickCounterEvent tickCounterEvent = new TickCounterEvent();
        MeteorClient.EVENT_BUS.post(tickCounterEvent);
        if (tickCounterEvent.isCancelled()) {
            dynamicDeltaTicks = ((timeMillis - lastTimeMillis) / tickTime) * tickCounterEvent.getTicks();
            lastTimeMillis = timeMillis;
            tickProgress += dynamicDeltaTicks;
            int i = (int) tickProgress;
            tickProgress -= i;
            cir.setReturnValue(i);
        }
    }
}
