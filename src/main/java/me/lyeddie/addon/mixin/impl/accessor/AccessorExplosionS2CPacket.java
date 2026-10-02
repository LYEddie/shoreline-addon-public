package me.lyeddie.addon.mixin.impl.accessor;

import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExplosionS2CPacket.class)
public interface AccessorExplosionS2CPacket {

    @Accessor("playerVelocityX")
    @Mutable
    void setPlayerVelocityX(float playerVelocityX);

    @Accessor("playerVelocityY")
    @Mutable
    void setPlayerVelocityY(float playerVelocityY);

    @Accessor("playerVelocityZ")
    @Mutable
    void setPlayerVelocityZ(float playerVelocityZ);
}
