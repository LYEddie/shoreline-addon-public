package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.RenderPlayerEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void hookUpdateRenderState(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!(player instanceof AbstractClientPlayerEntity clientPlayer)) {
            return;
        }
        RenderPlayerEvent renderPlayerEvent = new RenderPlayerEvent(clientPlayer);
        MeteorClient.EVENT_BUS.post(renderPlayerEvent);
        if (renderPlayerEvent.isCancelled()) {
            state.bodyYaw = renderPlayerEvent.getYaw();
            state.relativeHeadYaw = 0.0f;
            state.pitch = renderPlayerEvent.getPitch();
        }
    }
}
