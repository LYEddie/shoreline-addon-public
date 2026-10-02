package me.lyeddie.addon.mixin.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import me.lyeddie.addon.events.DecodePacketEvent;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.staged.OutboundPostPacketEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinClientConnection implements Globals {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "exceptionCaught", at = @At("HEAD"), cancellable = true)
    private void hookExceptionCaught(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
        DecodePacketEvent decodePacketEvent = new DecodePacketEvent();
        MeteorClient.EVENT_BUS.post(decodePacketEvent);
        if (decodePacketEvent.isCancelled()) {
            LOGGER.error("Exception caught on network thread:", ex);
            ci.cancel();
        }
    }

    @Inject(method = "sendPacket", at = @At(value = "TAIL"), cancellable = true)
    private void hookSendImmediately$2(Packet<?> packet, @Nullable ChannelFutureListener callbacks,
                                       boolean flush, CallbackInfo ci) {
        if ((mc.level != null || mc.player != null)) {
            OutboundPostPacketEvent packetOutboundEvent = new OutboundPostPacketEvent(packet);
            MeteorClient.EVENT_BUS.post(packetOutboundEvent);
            if (packetOutboundEvent.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "disconnect", at = @At(value = "HEAD"))
    private void hookDisconnect(Component disconnectReason, CallbackInfo ci) {
        DisconnectEvent disconnectEvent = new DisconnectEvent();
        MeteorClient.EVENT_BUS.post(disconnectEvent);
    }
}
