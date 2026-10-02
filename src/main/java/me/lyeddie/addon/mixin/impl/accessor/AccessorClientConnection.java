package me.lyeddie.addon.mixin.impl.accessor;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Connection.class)
public interface AccessorClientConnection {

    @Invoker("doSendPacket")
    void hookSendInternal(Packet<?> packet, @Nullable ChannelFutureListener callback, boolean flush);
}
