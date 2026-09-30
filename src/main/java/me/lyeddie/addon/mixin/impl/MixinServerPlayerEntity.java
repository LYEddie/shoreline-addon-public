package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.irrevocable.LoadWorldEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class MixinServerPlayerEntity {

    @Inject(method = "triggerDimensionChangeTriggers", at = @At(value = "HEAD"))
    private void hookMoveToWorld(ServerLevel origin, CallbackInfo ci) {
        LoadWorldEvent loadWorldEvent = new LoadWorldEvent();
        MeteorClient.EVENT_BUS.post(loadWorldEvent);
    }
}
