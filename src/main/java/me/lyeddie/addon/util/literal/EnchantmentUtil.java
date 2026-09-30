package me.lyeddie.addon.util.literal;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class EnchantmentUtil {

    public static int getLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentRegistryKey) {
        if (!stack.getComponents().has(DataComponents.ENCHANTMENTS)) {
            return 0;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> e : stack.getComponents()
            .get(DataComponents.ENCHANTMENTS).entrySet()) {
            if (e.getKey().unwrapKey().isPresent() && e.getKey().unwrapKey().get().equals(enchantmentRegistryKey)) {
                return e.getIntValue();
            }
        }
        return 0;
    }

    public static boolean isFakeEnchant2b2t(ItemStack itemStack) {
        Set<Object2IntMap.Entry<Holder<Enchantment>>> enchants = EnchantmentHelper.getEnchantmentsForCrafting(itemStack).entrySet();
        if (enchants.size() > 1) {
            return false;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> e : enchants) {
            Holder<Enchantment> enchantment = e.getKey();
            int lvl = e.getIntValue();
            if (lvl == 0 && enchantment.unwrapKey().isPresent() && enchantment.unwrapKey().get() == Enchantments.PROTECTION) {
                return true;
            }
        }
        return false;
    }
}
