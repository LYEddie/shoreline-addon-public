package me.lyeddie.addon.mixin.impl.accessor;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface AccessorPlayerMoveC2SPacket {

    @Accessor("onGround")
    @Mutable
    void hookSetOnGround(boolean onGround);

    @Accessor("x")
    @Mutable
    void hookSetX(double x);

    @Accessor("y")
    @Mutable
    void hookSetY(double y);

    @Accessor("z")
    @Mutable
    void hookSetZ(double z);

    @Accessor("yRot")
    @Mutable
    void hookSetYaw(float yaw);

    @Accessor("xRot")
    @Mutable
    void hookSetPitch(float pitch);
}
