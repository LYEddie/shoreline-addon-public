package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.staged.PostKeyboardTickEvent;
import me.lyeddie.addon.events.staged.PreKeyboardTickEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void hookTick$Pre(boolean slowDown, float slowDownFactor, CallbackInfo info) {
        PreKeyboardTickEvent eventA = new PreKeyboardTickEvent((Input) (Object) this);
        MeteorClient.EVENT_BUS.post(eventA);
        if (eventA.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/" +
        "client/input/KeyboardInput;sneaking:Z", shift = At.Shift.BEFORE), cancellable = true)
    private void hookTick$Post(boolean slowDown, float f, CallbackInfo ci) {
        PostKeyboardTickEvent eventB = new PostKeyboardTickEvent((Input) (Object) this);
        MeteorClient.EVENT_BUS.post(eventB);
        if (eventB.isCancelled()) {
            ci.cancel();
        }
    }
}
