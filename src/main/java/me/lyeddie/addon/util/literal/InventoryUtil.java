package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.util.Globals;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class InventoryUtil implements Globals {

    public static boolean isHolding32k() {
        return isHolding32k(1000);
    }

    public static boolean isHolding32k(int lvl) {
        final ItemStack mainhand = mc.player.getMainHandStack();
        return EnchantmentUtil.getLevel(mainhand, Enchantments.SHARPNESS) >= lvl;
    }

    public static boolean hasItemInInventory(final Item item, final boolean hotbar) {
        final int startSlot = hotbar ? 0 : 9;
        for (int i = startSlot; i < 36; ++i) {
            final ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (!itemStack.isEmpty() && itemStack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    public static int count(Item item) {
        if (mc.player == null) {
            return 0;
        }
        ItemStack offhandStack = mc.player.getOffHandStack();
        int itemCount = offhandStack.getItem() == item ? offhandStack.getCount() : 0;
        for (int i = 0; i < 36; i++) {
            ItemStack slot = mc.player.getInventory().getStack(i);
            if (slot.getItem() == item) {
                itemCount += slot.getCount();
            }
        }
        return itemCount;
    }
}
