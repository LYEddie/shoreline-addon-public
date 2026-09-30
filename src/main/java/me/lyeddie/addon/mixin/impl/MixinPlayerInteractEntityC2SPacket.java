package me.lyeddie.addon.mixin.impl;

import io.netty.buffer.Unpooled;
import me.lyeddie.addon.mixin.IPlayerInteractEntityC2SPacket;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.InteractType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerboundInteractPacket.class)
public abstract class MixinPlayerInteractEntityC2SPacket implements IPlayerInteractEntityC2SPacket, Globals {

    @Shadow
    @Final
    private int entityId;

    @Shadow
    public abstract void write(FriendlyByteBuf buf);

    @Override
    public Entity getEntity() {
        if (mc.level == null) {
            return null;
        }
        return mc.level.getEntity(entityId);
    }

    @Override
    public InteractType getType() {
        FriendlyByteBuf packetBuf = new FriendlyByteBuf(Unpooled.buffer());
        write(packetBuf);
        packetBuf.readVarInt();
        return packetBuf.readEnum(InteractType.class);
    }
}
