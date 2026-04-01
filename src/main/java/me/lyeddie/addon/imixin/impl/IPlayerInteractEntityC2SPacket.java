package me.lyeddie.addon.imixin.impl;

import me.lyeddie.addon.imixin.IMixin;
import net.minecraft.entity.Entity;
import me.lyeddie.addon.util.InteractType;

/**
 *
 */
@IMixin
public interface IPlayerInteractEntityC2SPacket {
    /**
     * @return
     */
    Entity getEntity();

    /**
     * @return
     */
    InteractType getType();
}
