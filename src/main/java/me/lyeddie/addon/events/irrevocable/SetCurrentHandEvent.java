package me.lyeddie.addon.events.irrevocable;

import me.lyeddie.addon.util.Globals;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class SetCurrentHandEvent implements Globals {

    private final InteractionHand hand;

    public SetCurrentHandEvent(InteractionHand hand) {
        this.hand = hand;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getStackInHand() {
        return mc.player.getItemInHand(hand);
    }
}
