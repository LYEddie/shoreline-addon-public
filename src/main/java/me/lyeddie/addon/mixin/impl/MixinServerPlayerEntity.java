package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.irrevocable.LoadWorldEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {

    @Inject(method = "worldChanged", at = @At(value = "HEAD"))
    private void hookMoveToWorld(ServerWorld origin, CallbackInfo ci) {
        LoadWorldEvent loadWorldEvent = new LoadWorldEvent();
        MeteorClient.EVENT_BUS.post(loadWorldEvent);
    }
}
