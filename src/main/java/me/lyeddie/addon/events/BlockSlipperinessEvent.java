package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.block.Block;

public class BlockSlipperinessEvent extends Cancellable {

    private final Block block;
    private float slipperiness;

    public BlockSlipperinessEvent(Block block, float slipperiness) {
        this.block = block;
        this.slipperiness = slipperiness;
    }

    public Block getBlock() {
        return block;
    }

    public float getSlipperiness() {
        return slipperiness;
    }

    public void setSlipperiness(float slipperiness) {
        this.slipperiness = slipperiness;
    }
}
