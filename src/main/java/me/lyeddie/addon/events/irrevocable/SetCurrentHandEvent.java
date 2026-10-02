package me.lyeddie.addon.events.irrevocable;

import me.lyeddie.addon.util.Globals;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class SetCurrentHandEvent implements Globals {

    private final Hand hand;

    public SetCurrentHandEvent(Hand hand) {
        this.hand = hand;
    }

    public Hand getHand() {
        return hand;
    }

    public ItemStack getStackInHand() {
        return mc.player.getStackInHand(hand);
    }
}
