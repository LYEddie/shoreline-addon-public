package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AttackBlockEvent extends Cancellable {

    private final BlockPos pos;
    private final BlockState state;
    private final Direction direction;

    public AttackBlockEvent(BlockPos pos, BlockState state, Direction direction) {
        this.pos = pos;
        this.state = state;
        this.direction = direction;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    public Direction getDirection() {
        return direction;
    }
}
