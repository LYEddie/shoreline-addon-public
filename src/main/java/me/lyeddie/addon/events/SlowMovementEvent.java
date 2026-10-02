package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.block.BlockState;

public class SlowMovementEvent extends Cancellable {

    private final BlockState state;
    private float multiplier = 1.0f;

    public SlowMovementEvent(BlockState state) {
        this.state = state;
    }

    public BlockState getState() {
        return state;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }
}
