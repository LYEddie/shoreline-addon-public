package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.BlockCollisionEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockCollisions.class)
public class MixinBlockCollisionSpliterator implements Globals {

    @Redirect(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/CollisionContext;getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape hookGetCollisionShape(CollisionContext instance, BlockState blockState, CollisionGetter collisionView, BlockPos blockPos) {
        VoxelShape voxelShape = instance.getCollisionShape(blockState, collisionView, blockPos);
        if (collisionView != mc.level) {
            return voxelShape;
        }
        BlockCollisionEvent blockCollisionEvent = new BlockCollisionEvent(voxelShape, blockPos, blockState);
        MeteorClient.EVENT_BUS.post(blockCollisionEvent);
        if (blockCollisionEvent.isCancelled()) {
            return blockCollisionEvent.getVoxelShape();
        }
        return voxelShape;
    }
}
