package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.world.item.ItemStack;

public class ItemDesyncEvent extends Cancellable {

    private ItemStack stack;

    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getServerItem() {
        return stack;
    }
}
