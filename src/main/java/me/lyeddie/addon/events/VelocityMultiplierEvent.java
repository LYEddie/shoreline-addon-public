package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class VelocityMultiplierEvent extends Cancellable {

    private final BlockState state;

    public VelocityMultiplierEvent(BlockState state) {
        this.state = state;
    }

    public Block getBlock() {
        return state.getBlock();
    }
}
