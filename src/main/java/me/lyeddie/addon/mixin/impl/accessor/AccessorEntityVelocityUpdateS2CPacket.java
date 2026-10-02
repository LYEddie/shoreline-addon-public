package me.lyeddie.addon.mixin.impl.accessor;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetEntityMotionPacket.class)
public interface AccessorEntityVelocityUpdateS2CPacket {

    @Accessor("movement")
    @Mutable
    void setVelocity(Vec3 velocity);
}

