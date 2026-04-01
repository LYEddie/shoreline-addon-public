package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.managers.impl.util.RotationCallback;
import me.lyeddie.addon.module.impl.AirPlaceII;
import me.lyeddie.addon.tabs.TabConfigs;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.literal.MovementUtil;
import me.lyeddie.addon.util.literal.RotationUtil;
import me.lyeddie.addon.util.SneakBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionManager implements Globals {
    private final Map<Integer, Integer> placedOnEntities = new ConcurrentHashMap<>();

    public boolean canPlace(BlockPos pos, Block block) {
        VoxelShape shape = block.getDefaultState().getCollisionShape(mc.world, pos, ShapeContext.absent()).offset(pos.getX(), pos.getY(), pos.getZ());
        if (!shape.isEmpty()) {
            for (Entity entity : mc.world.getOtherEntities(null, shape.getBoundingBox())) {
                if (entity.isRemoved() || !entity.intersectionChecked || !VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND) || entity instanceof EndCrystalEntity && (!placedOnEntities.containsKey(entity.getId()) || placedOnEntities.get(entity.getId()) <= TabConfigs.get().getEntityPlaceThreshold())) continue;
                return false;
            }
        }
        return true;
    }

    public boolean placeBlock(final BlockPos pos, final Block block, final int slot, final boolean strictDirection, final boolean clientSwing, final RotationCallback rotationCallback) {
        return placeBlock(pos, block, slot, strictDirection, clientSwing, rotationCallback, false);
    }

    public boolean placeBlock(BlockPos pos, Block block, int slot, boolean strictDirection, boolean clientSwing, RotationCallback rotationCallback, boolean airPlace) {
        VoxelShape shape = block.getDefaultState().getCollisionShape(mc.world, pos, ShapeContext.absent()).offset(pos.getX(), pos.getY(), pos.getZ());
        boolean isEntityBlockingPlacement = false;
        if (!shape.isEmpty()) {
            for (Entity entity : InteractionManager.mc.world.getOtherEntities(null, shape.getBoundingBox())) {
                if (entity.isRemoved() || !entity.intersectionChecked || !VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND)) continue;
                if (entity instanceof EndCrystalEntity) {
                    placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
                    if (!placedOnEntities.containsKey(entity.getId()) || placedOnEntities.get(entity.getId()) <= TabConfigs.get().getEntityPlaceThreshold()) continue;
                }
                isEntityBlockingPlacement = true;
                break;
            }
        }
        if (isEntityBlockingPlacement) {
            return false;
        }
        Direction direction = getInteractDirectionInternal(pos, strictDirection);
        if (airPlace || AirPlaceII.getInstance().isActive() && direction == null) {
            direction = Direction.DOWN;
            return placeBlock(pos, direction, slot, clientSwing, TabConfigs.get().isGrim(), rotationCallback);
        }
        if (direction == null) {
            return false;
        }
        BlockPos neighbor = pos.offset(direction.getOpposite());
        return placeBlock(neighbor, direction, slot, clientSwing, false, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos, final Block block, final int slot, final boolean strictDirection, final boolean clientSwing, final boolean packet, final RotationCallback rotationCallback) {
        return placeBlock(pos, block, slot, strictDirection, clientSwing, packet, false, rotationCallback);
    }

    public boolean placeBlock(BlockPos pos, Block block, int slot, boolean strictDirection, boolean clientSwing, boolean packet, boolean airPlace, RotationCallback rotationCallback) {
        VoxelShape shape = block.getDefaultState().getCollisionShape(InteractionManager.mc.world, pos, ShapeContext.absent()).offset(pos.getX(), pos.getY(), pos.getZ());
        boolean isEntityBlockingPlacement = false;
        if (!shape.isEmpty()) {
            for (Entity entity : mc.world.getOtherEntities(null, shape.getBoundingBox())) {
                if (entity.isRemoved() || !entity.intersectionChecked || !VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND)) continue;
                if (entity instanceof EndCrystalEntity) {
                    placedOnEntities.compute(entity.getId(), (k, attempts) -> attempts != null ? attempts + 1 : 1);
                    if (!placedOnEntities.containsKey(entity.getId()) || placedOnEntities.get(entity.getId()) <= TabConfigs.get().getEntityPlaceThreshold()) continue;
                }
                isEntityBlockingPlacement = true;
                break;
            }
        }
        if (isEntityBlockingPlacement) {
            return false;
        }
        Direction direction = getInteractDirectionInternal(pos, strictDirection);
        if (airPlace || AirPlaceII.getInstance().isActive() && direction == null) {
            direction = Direction.DOWN;
            return placeBlock(pos, direction, slot, clientSwing, TabConfigs.get().isGrim(), rotationCallback);
        }
        if (direction == null) {
            return false;
        }
        BlockPos neighbor = pos.offset(direction.getOpposite());
        return placeBlock(neighbor, direction, slot, clientSwing, false, packet, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos, final Direction direction, final int slot, final boolean clientSwing, final boolean grimAirPlace, final boolean packet, final RotationCallback rotationCallback) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getUnitVector()).multiply(0.5));
        return placeBlock(new BlockHitResult(hitVec, direction, pos, false),
            slot, clientSwing, grimAirPlace, packet, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos, final Direction direction, final int slot, final boolean clientSwing, final boolean grimAirPlace, final RotationCallback rotationCallback) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(direction.getUnitVector()).multiply(0.5));
        return placeBlock(new BlockHitResult(hitVec, direction, pos, false),
            slot, clientSwing, grimAirPlace, rotationCallback);
    }

    public boolean placeBlock(final BlockHitResult hitResult, final int slot, final boolean clientSwing, final boolean grimAirPlace, final boolean packet, final RotationCallback rotationCallback) {
        final boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
        if (isSpoofing) {
            Managers.INVENTORY.setSlot(slot);
        }

        if (grimAirPlace) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        }

        final boolean isRotating = rotationCallback != null;
        if (isRotating) {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitResult.getPos());
            rotationCallback.handleRotation(true, angles);
        }

        final boolean result = placeBlockImmediately(hitResult, grimAirPlace ? Hand.OFF_HAND : Hand.MAIN_HAND, clientSwing, packet);
        if (isRotating) {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitResult.getPos());
            rotationCallback.handleRotation(false, angles);
        }

        if (grimAirPlace) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        }

        if (isSpoofing) {
            Managers.INVENTORY.syncToClient();
        }

        return result;
    }

    public boolean placeBlock(final BlockHitResult hitResult, final int slot, final boolean clientSwing, final boolean grimAirPlace, final RotationCallback rotationCallback) {
        final boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
        if (isSpoofing) {
            Managers.INVENTORY.setSlot(slot);
        }

        if (grimAirPlace) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        }

        final boolean isRotating = rotationCallback != null;
        if (isRotating) {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitResult.getPos());
            rotationCallback.handleRotation(true, angles);
        }

        final boolean result = placeBlockImmediately(hitResult, grimAirPlace ? Hand.OFF_HAND : Hand.MAIN_HAND, clientSwing, true);
        if (isRotating) {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePos(), hitResult.getPos());
            rotationCallback.handleRotation(false, angles);
        }

        if (grimAirPlace) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        }

        if (isSpoofing) {
            Managers.INVENTORY.syncToClient();
        }

        return result;
    }

    public boolean placeBlockImmediately(final BlockHitResult result, final Hand hand, final boolean clientSwing, final boolean packet) {
        final BlockState state = mc.world.getBlockState(result.getBlockPos());
        final boolean shouldSneak = SneakBlocks.isSneakBlock(state) && !mc.player.isSneaking();
        if (shouldSneak) {
            Managers.MOVEMENT.setPacketSneaking(true);
            MovementUtil.applySneak();
        }
        final ActionResult actionResult = packet ? placeBlockPacket(result, hand) : placeBlockInternally(result, hand);
        if (actionResult.isAccepted() && actionResult.shouldSwingHand()) {
            if (clientSwing) {
                mc.player.swingHand(Hand.MAIN_HAND);
            } else {
                Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }
        if (shouldSneak) {
            Managers.MOVEMENT.setPacketSneaking(false);
        }
        return actionResult.isAccepted();
    }

    private ActionResult placeBlockInternally(final BlockHitResult hitResult, final Hand hand) {
        return mc.interactionManager.interactBlock(mc.player, hand, hitResult);
    }

    public ActionResult placeBlockPacket(final BlockHitResult hitResult, final Hand hand) {
        Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(hand, hitResult, id));
        return ActionResult.SUCCESS;
    }

    public Direction getInteractDirection(final BlockPos blockPos, final boolean strictDirection) {
        Direction direction = getInteractDirectionInternal(blockPos, strictDirection);
        return direction == null ? Direction.UP : direction;
    }

    public Direction getInteractDirectionInternal(final BlockPos blockPos, final boolean strictDirection) {
        Set<Direction> validDirections = getPlaceDirectionsNCP(mc.player.getEyePos(), blockPos.toCenterPos());
        Direction interactDirection = null;
        for (final Direction direction : Direction.values()) {
            final BlockState state = mc.world.getBlockState(blockPos.offset(direction));
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }

            if (state.getBlock() == Blocks.ANVIL || state.getBlock() == Blocks.CHIPPED_ANVIL
                || state.getBlock() == Blocks.DAMAGED_ANVIL) {
                continue;
            }

            if (strictDirection && !validDirections.contains(direction.getOpposite())) {
                continue;
            }
            interactDirection = direction;
            break;
        }
        if (interactDirection == null) {
            return null;
        }
        return interactDirection.getOpposite();
    }

    public Set<Direction> getPlaceDirectionsNCP(Vec3d eyePos, Vec3d blockPos) {
        return getPlaceDirectionsNCP(eyePos.x, eyePos.y, eyePos.z, blockPos.x, blockPos.y, blockPos.z);
    }

    public Set<Direction> getPlaceDirectionsNCP(final double x, final double y, final double z, final double dx, final double dy, final double dz) {
        final double xdiff = x - dx;
        final double ydiff = y - dy;
        final double zdiff = z - dz;
        final Set<Direction> dirs = new HashSet<>(6);
        if (ydiff > 0.5) {
            dirs.add(Direction.UP);
        } else if (ydiff < -0.5) {
            dirs.add(Direction.DOWN);
        } else {
            dirs.add(Direction.UP);
            dirs.add(Direction.DOWN);
        }
        if (xdiff > 0.5) {
            dirs.add(Direction.EAST);
        } else if (xdiff < -0.5) {
            dirs.add(Direction.WEST);
        } else {
            dirs.add(Direction.EAST);
            dirs.add(Direction.WEST);
        }
        if (zdiff > 0.5) {
            dirs.add(Direction.SOUTH);
        } else if (zdiff < -0.5) {
            dirs.add(Direction.NORTH);
        } else {
            dirs.add(Direction.SOUTH);
            dirs.add(Direction.NORTH);
        }
        return dirs;
    }
}
