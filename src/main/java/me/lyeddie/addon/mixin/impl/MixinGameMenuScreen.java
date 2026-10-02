package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.MenuDisconnectEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public class MixinGameMenuScreen {

    @Inject(method = "method_72129", at = @At("HEAD"), cancellable = true, remap = false)
    private void hookDisconnect(CallbackInfo ci) {
        MenuDisconnectEvent menuDisconnectEvent = new MenuDisconnectEvent();
        MeteorClient.EVENT_BUS.post(menuDisconnectEvent);
        if (menuDisconnectEvent.isCancelled()) {
            ci.cancel();
        }
    }
}

