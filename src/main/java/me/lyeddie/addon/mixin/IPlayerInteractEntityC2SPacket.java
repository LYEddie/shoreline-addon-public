package me.lyeddie.addon.mixin;

import net.minecraft.entity.Entity;
import me.lyeddie.addon.util.InteractType;

public interface IPlayerInteractEntityC2SPacket {

    Entity getEntity();
    InteractType getType();
}
