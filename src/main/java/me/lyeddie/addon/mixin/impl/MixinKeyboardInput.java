package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.staged.PostKeyboardTickEvent;
import me.lyeddie.addon.events.staged.PreKeyboardTickEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void hookTick$Pre(CallbackInfo info) {
        PreKeyboardTickEvent eventA = new PreKeyboardTickEvent((ClientInput) (Object) this);
        MeteorClient.EVENT_BUS.post(eventA);
        if (eventA.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"), cancellable = true)
    private void hookTick$Post(CallbackInfo ci) {
        PostKeyboardTickEvent eventB = new PostKeyboardTickEvent((ClientInput) (Object) this);
        MeteorClient.EVENT_BUS.post(eventB);
        if (eventB.isCancelled()) {
            ci.cancel();
        }
    }
}
