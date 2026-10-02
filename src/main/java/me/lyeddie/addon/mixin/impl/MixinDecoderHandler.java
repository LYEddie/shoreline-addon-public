package me.lyeddie.addon.mixin.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import me.lyeddie.addon.events.DecodePacketEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.network.handler.DecoderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DecoderHandler.class)
public class MixinDecoderHandler {

    @Inject(method = "decode", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkPhase;getId()Ljava/lang/String;", shift = At.Shift.AFTER), cancellable = true)
    private void hookDecode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> objects, CallbackInfo ci) {
        DecodePacketEvent decodePacketEvent = new DecodePacketEvent();
        MeteorClient.EVENT_BUS.post(decodePacketEvent);
        if (decodePacketEvent.isCancelled()) {
            ci.cancel();
        }
    }
}
