package me.lyeddie.addon.mixin.impl.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;

@Mixin(ClientboundExplodePacket.class)
public interface AccessorExplosionS2CPacket {

    @Accessor("playerKnockback")
    @Mutable
    void setPlayerKnockback(Optional<Vec3> playerKnockback);
}
