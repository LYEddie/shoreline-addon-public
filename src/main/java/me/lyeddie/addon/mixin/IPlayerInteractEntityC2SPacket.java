package me.lyeddie.addon.mixin;

import me.lyeddie.addon.util.InteractType;
import net.minecraft.world.entity.Entity;

public interface IPlayerInteractEntityC2SPacket {

    Entity getEntity();
    InteractType getType();
}
