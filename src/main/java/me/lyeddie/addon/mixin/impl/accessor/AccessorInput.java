package me.lyeddie.addon.mixin.impl.accessor;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface AccessorInput {
    @Accessor("moveVector")
    void shoreline$setMovementVector(Vec2 movementVector);
}
