package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.managers.impl.util.RotationCallback;
import me.lyeddie.addon.module.impl.AirPlaceII;
import me.lyeddie.addon.util.tabs.TabConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.literal.MovementUtil;
import me.lyeddie.addon.util.literal.RotationUtil;
import me.lyeddie.addon.util.SneakBlocks;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractionManager implements Globals {
    private final Map<Integer, Integer> placedOnEntities = new ConcurrentHashMap<>();

    public boolean canPlace(BlockPos pos, Block block) {
        VoxelShape shape = block.defaultBlockState().getCollisionShape(mc.level, pos, CollisionContext.empty()).move(pos.getX(), pos.getY(), pos.getZ());
        if (!shape.isEmpty()) {
            for (Entity entity : mc.level.getEntities(null, shape.bounds())) {
                if (entity.isRemoved() || !entity.blocksBuilding || !Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND) || entity instanceof EndCrystal && (!placedOnEntities.containsKey(entity.getId()) || placedOnEntities.get(entity.getId()) <= TabConfigs.get().getEntityPlaceThreshold())) continue;
                return false;
            }
        }
        return true;
    }

    public boolean placeBlock(final BlockPos pos, final Block block, final int slot, final boolean strictDirection, final boolean clientSwing, final RotationCallback rotationCallback) {
        return placeBlock(pos, block, slot, strictDirection, clientSwing, rotationCallback, false);
    }

    public boolean placeBlock(BlockPos pos, Block block, int slot, boolean strictDirection, boolean clientSwing, RotationCallback rotationCallback, boolean airPlace) {
        VoxelShape shape = block.defaultBlockState().getCollisionShape(mc.level, pos, CollisionContext.empty()).move(pos.getX(), pos.getY(), pos.getZ());
        boolean isEntityBlockingPlacement = false;
        if (!shape.isEmpty()) {
            for (Entity entity : InteractionManager.mc.level.getEntities(null, shape.bounds())) {
                if (entity.isRemoved() || !entity.blocksBuilding || !Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) continue;
                if (entity instanceof EndCrystal) {
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
        BlockPos neighbor = pos.relative(direction.getOpposite());
        return placeBlock(neighbor, direction, slot, clientSwing, false, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos, final Block block, final int slot, final boolean strictDirection, final boolean clientSwing, final boolean packet, final RotationCallback rotationCallback) {
        return placeBlock(pos, block, slot, strictDirection, clientSwing, packet, false, rotationCallback);
    }

    public boolean placeBlock(BlockPos pos, Block block, int slot, boolean strictDirection, boolean clientSwing, boolean packet, boolean airPlace, RotationCallback rotationCallback) {
        VoxelShape shape = block.defaultBlockState().getCollisionShape(InteractionManager.mc.level, pos, CollisionContext.empty()).move(pos.getX(), pos.getY(), pos.getZ());
        boolean isEntityBlockingPlacement = false;
        if (!shape.isEmpty()) {
            for (Entity entity : mc.level.getEntities(null, shape.bounds())) {
                if (entity.isRemoved() || !entity.blocksBuilding || !Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) continue;
                if (entity instanceof EndCrystal) {
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
        BlockPos neighbor = pos.relative(direction.getOpposite());
        return placeBlock(neighbor, direction, slot, clientSwing, false, packet, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos, final Direction direction, final int slot, final boolean clientSwing, final boolean grimAirPlace, final boolean packet, final RotationCallback rotationCallback) {
        Vec3 hitVec = pos.getCenter().add(new Vec3(direction.step()).scale(0.5));
        return placeBlock(new BlockHitResult(hitVec, direction, pos, false),
            slot, clientSwing, grimAirPlace, packet, rotationCallback);
    }

    public boolean placeBlock(final BlockPos pos, final Direction direction, final int slot, final boolean clientSwing, final boolean grimAirPlace, final RotationCallback rotationCallback) {
        Vec3 hitVec = pos.getCenter().add(new Vec3(direction.step()).scale(0.5));
        return placeBlock(new BlockHitResult(hitVec, direction, pos, false),
            slot, clientSwing, grimAirPlace, rotationCallback);
    }

    public boolean placeBlock(final BlockHitResult hitResult, final int slot, final boolean clientSwing, final boolean grimAirPlace, final boolean packet, final RotationCallback rotationCallback) {
        final boolean isSpoofing = slot != Managers.INVENTORY.getServerSlot();
        if (isSpoofing) {
            Managers.INVENTORY.setSlot(slot);
        }

        if (grimAirPlace) {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        }

        final boolean isRotating = rotationCallback != null;
        if (isRotating) {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
            rotationCallback.handleRotation(true, angles);
        }

        final boolean result = placeBlockImmediately(hitResult, grimAirPlace ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, clientSwing, packet);
        if (isRotating) {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
            rotationCallback.handleRotation(false, angles);
        }

        if (grimAirPlace) {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
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
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        }

        final boolean isRotating = rotationCallback != null;
        if (isRotating) {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
            rotationCallback.handleRotation(true, angles);
        }

        final boolean result = placeBlockImmediately(hitResult, grimAirPlace ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, clientSwing, true);
        if (isRotating) {
            float[] angles = RotationUtil.getRotationsTo(mc.player.getEyePosition(), hitResult.getLocation());
            rotationCallback.handleRotation(false, angles);
        }

        if (grimAirPlace) {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        }

        if (isSpoofing) {
            Managers.INVENTORY.syncToClient();
        }

        return result;
    }

    public boolean placeBlockImmediately(final BlockHitResult result, final InteractionHand hand, final boolean clientSwing, final boolean packet) {
        final BlockState state = mc.level.getBlockState(result.getBlockPos());
        final boolean shouldSneak = SneakBlocks.isSneakBlock(state) && !mc.player.isShiftKeyDown();
        if (shouldSneak) {
            Managers.MOVEMENT.setPacketSneaking(true);
            MovementUtil.applySneak();
        }
        final InteractionResult actionResult = packet ? placeBlockPacket(result, hand) : placeBlockInternally(result, hand);
        if (actionResult instanceof InteractionResult.Success success
            && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            if (clientSwing) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            } else {
                Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
        if (shouldSneak) {
            Managers.MOVEMENT.setPacketSneaking(false);
        }
        return actionResult.consumesAction();
    }

    private InteractionResult placeBlockInternally(final BlockHitResult hitResult, final InteractionHand hand) {
        return mc.gameMode.useItemOn(mc.player, hand, hitResult);
    }

    public InteractionResult placeBlockPacket(final BlockHitResult hitResult, final InteractionHand hand) {
        Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(hand, hitResult, id));
        return InteractionResult.SUCCESS;
    }

    public Direction getInteractDirection(final BlockPos blockPos, final boolean strictDirection) {
        Direction direction = getInteractDirectionInternal(blockPos, strictDirection);
        return direction == null ? Direction.UP : direction;
    }

    public Direction getInteractDirectionInternal(final BlockPos blockPos, final boolean strictDirection) {
        Set<Direction> validDirections = getPlaceDirectionsNCP(mc.player.getEyePosition(), blockPos.getCenter());
        Direction interactDirection = null;
        for (final Direction direction : Direction.values()) {
            final BlockState state = mc.level.getBlockState(blockPos.relative(direction));
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

    public Set<Direction> getPlaceDirectionsNCP(Vec3 eyePos, Vec3 blockPos) {
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
