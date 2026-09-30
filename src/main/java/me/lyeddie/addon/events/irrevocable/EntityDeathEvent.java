package me.lyeddie.addon.events.irrevocable;

import net.minecraft.world.entity.LivingEntity;

public class EntityDeathEvent {
    private final LivingEntity entity;

    public EntityDeathEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }
}
