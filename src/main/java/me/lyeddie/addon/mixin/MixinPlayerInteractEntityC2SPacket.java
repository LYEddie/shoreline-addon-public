package me.lyeddie.addon.mixin;

import io.netty.buffer.Unpooled;
import me.lyeddie.addon.imixin.impl.IPlayerInteractEntityC2SPacket;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.InteractType;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerInteractEntityC2SPacket.class)
public abstract class MixinPlayerInteractEntityC2SPacket implements IPlayerInteractEntityC2SPacket, Globals {

    @Shadow
    @Final
    private int entityId;

    @Shadow
    public abstract void write(PacketByteBuf buf);

    @Override
    public Entity getEntity() {
        if (mc.world == null) {
            return null;
        }
        return mc.world.getEntityById(entityId);
    }

    @Override
    public InteractType getType() {
        PacketByteBuf packetBuf = new PacketByteBuf(Unpooled.buffer());
        write(packetBuf);
        packetBuf.readVarInt();
        return packetBuf.readEnumConstant(InteractType.class);
    }
}
