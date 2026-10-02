package me.lyeddie.addon.util.literal;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.Set;

public class EnchantmentUtil {

    public static int getLevel(ItemStack stack, Enchantment enchantment) {
        return EnchantmentHelper.getLevel(enchantment, stack);
    }

    public static boolean isFakeEnchant2b2t(ItemStack itemStack) {
        Set<Map.Entry<Enchantment, Integer>> enchants = EnchantmentHelper.get(itemStack).entrySet();
        if (enchants.size() > 1) {
            return false;
        }
        for (Map.Entry<Enchantment, Integer> e : enchants) {
            Enchantment enchantment = e.getKey();
            int lvl = e.getValue();
            if (lvl == 0 && enchantment == Enchantments.PROTECTION) {
                return true;
            }
        }
        return false;
    }
}
