package me.lyeddie.addon.mixin;

import me.lyeddie.addon.events.AddEntityEvent;
import me.lyeddie.addon.events.irrevocable.RemoveEntityEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld {

    @Shadow
    protected abstract EntityLookup<Entity> getEntityLookup();

    @Inject(method = "addEntity", at = @At(value = "HEAD"))
    private void hookAddEntity(Entity entity, CallbackInfo ci) {
        AddEntityEvent addEntityEvent = new AddEntityEvent(entity);
        MeteorClient.EVENT_BUS.post(addEntityEvent);
    }

    @Inject(method = "removeEntity", at = @At(value = "HEAD"))
    private void hookRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        Entity entity = getEntityLookup().get(entityId);
        if (entity == null) {
            return;
        }
        RemoveEntityEvent removeEntityEvent = new RemoveEntityEvent(entity, removalReason);
        MeteorClient.EVENT_BUS.post(removeEntityEvent);
    }
}
