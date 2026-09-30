package me.lyeddie.addon.events.irrevocable;

import me.lyeddie.addon.util.Globals;
import net.minecraft.world.entity.Entity;

public class RemoveEntityEvent implements Globals {
    private final Entity entity;
    private final Entity.RemovalReason removalReason;

    public RemoveEntityEvent(Entity entity, Entity.RemovalReason removalReason) {
        this.entity = entity;
        this.removalReason = removalReason;
    }

    public Entity getEntity() {
        return entity;
    }

    public Entity.RemovalReason getRemovalReason() {
        return removalReason;
    }
}
