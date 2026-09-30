package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.RenderPlayerEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class MixinPlayerEntityRenderer {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void hookUpdateRenderState(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }
        RenderPlayerEvent renderPlayerEvent = new RenderPlayerEvent(clientPlayer);
        MeteorClient.EVENT_BUS.post(renderPlayerEvent);
        if (renderPlayerEvent.isCancelled()) {
            state.bodyRot = renderPlayerEvent.getYaw();
            state.yRot = 0.0f;
            state.xRot = renderPlayerEvent.getPitch();
        }
    }
}
