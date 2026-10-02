package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.managers.Managers;
import meteordevelopment.meteorclient.settings.*;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.PriorityQueue;
import java.util.Queue;

public class AutoArmorII extends AddonModule {
    private static AutoArmorII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Priority> priorityConfig = sgGeneral.add(new EnumSetting.Builder<Priority>()
        .name("Priority").description("Armor enchantment priority")
        .defaultValue(Priority.BLAST_PROTECTION)
        .build());
    public final Setting<Double> minDurabilityConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("MinDurability").description("Durability percent to replace armor")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(20.0)
        .build());
    private final Setting<Boolean> elytraPriorityConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("ElytraPriority").description("Prioritizes existing elytras in the chestplate armor slot")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> blastLeggingsConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Leggings-BlastPriority").description("Prioritizes Blast Protection leggings")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> noBindingConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("NoBinding").description("Avoids armor with the Curse of Binding enchantment")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> inventoryConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("AllowInventory").description("Allows armor to be swapped while in the inventory menu")
        .defaultValue(false)
        .build());

    private final Queue<ArmorSlot> helmet = new PriorityQueue<>();
    private final Queue<ArmorSlot> chestplate = new PriorityQueue<>();
    private final Queue<ArmorSlot> leggings = new PriorityQueue<>();
    private final Queue<ArmorSlot> boots = new PriorityQueue<>();

    public AutoArmorII() {
        super(Shoreline.MAIN, "AutoArmorII", "Automatically replaces armor pieces");
        INST = this;
    }

    @EventHandler
    public void onTick(PlayerTickEvent event) {
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen && inventoryConfig.get())) {
            return;
        }

        helmet.clear();
        chestplate.clear();
        leggings.clear();
        boots.clear();
        for (int j = 0; j < 36; j++) {
            ItemStack stack = mc.player.getInventory().getStack(j);
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) {
                continue;
            }
            if (noBindingConfig.get() && hasEnchantment(stack, Enchantments.BINDING_CURSE)) {
                continue;
            }
            int index = armor.getSlotType().getEntitySlotId();
            float dura = (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage();
            if (dura < minDurabilityConfig.get()) {
                continue;
            }
            ArmorSlot data = new ArmorSlot(index, j, stack);
            switch (index) {
                case 0 -> helmet.add(data);
                case 1 -> chestplate.add(data);
                case 2 -> leggings.add(data);
                case 3 -> boots.add(data);
            }
        }
        for (int i = 0; i < 4; i++) {
            ItemStack armorStack = mc.player.getInventory().getArmorStack(i);
            if (elytraPriorityConfig.get() && armorStack.getItem() == Items.ELYTRA) {
                continue;
            }
            float armorDura = (armorStack.getMaxDamage() - armorStack.getDamage()) / (float) armorStack.getMaxDamage();
            if (!armorStack.isEmpty() || armorDura >= minDurabilityConfig.get()) {
                continue;
            }
            switch (i) {
                case 0 -> {
                    if (!helmet.isEmpty()) {
                        ArmorSlot helmetSlot = helmet.poll();
                        swapArmor(helmetSlot.getType(), helmetSlot.getSlot());
                    }
                }
                case 1 -> {
                    if (!chestplate.isEmpty()) {
                        ArmorSlot chestSlot = chestplate.poll();
                        swapArmor(chestSlot.getType(), chestSlot.getSlot());
                    }
                }
                case 2 -> {
                    if (!leggings.isEmpty()) {
                        ArmorSlot leggingsSlot = leggings.poll();
                        swapArmor(leggingsSlot.getType(), leggingsSlot.getSlot());
                    }
                }
                case 3 -> {
                    if (!boots.isEmpty()) {
                        ArmorSlot bootsSlot = boots.poll();
                        swapArmor(bootsSlot.getType(), bootsSlot.getSlot());
                    }
                }
            }
        }
    }

    public void swapArmor(int armorSlot, int slot) {
        ItemStack stack = mc.player.getInventory().getArmorStack(armorSlot);
        armorSlot = 8 - armorSlot;
        Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
        boolean rt = !stack.isEmpty();
        Managers.INVENTORY.pickupSlot(armorSlot);
        if (rt) {
            Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
        }
    }

    public boolean hasEnchantment(ItemStack armorStack, Enchantment enchantment) {
        return EnchantmentHelper.getLevel(enchantment, armorStack) > 0;
    }

    public static AutoArmorII getInstance() {
        return INST;
    }

    public enum Priority {
        BLAST_PROTECTION(Enchantments.BLAST_PROTECTION),
        PROTECTION(Enchantments.PROTECTION),
        PROJECTILE_PROTECTION(Enchantments.PROJECTILE_PROTECTION);

        private final Enchantment enchant;

        Priority(Enchantment enchant) {
            this.enchant = enchant;
        }

        public Enchantment getEnchantment() {
            return enchant;
        }
    }

    public class ArmorSlot implements Comparable<ArmorSlot> {
        private final int armorType;
        private final int slot;
        private final ItemStack armorStack;

        public ArmorSlot(int armorType, int slot, ItemStack armorStack) {
            this.armorType = armorType;
            this.slot = slot;
            this.armorStack = armorStack;
        }

        @Override
        public int compareTo(ArmorSlot other) {
            if (armorType != other.armorType) {
                return 0;
            }
            final ItemStack otherStack = other.getArmorStack();
            ArmorItem armorItem = (ArmorItem) armorStack.getItem();
            ArmorItem otherItem = (ArmorItem) otherStack.getItem();
            int durabilityDiff = armorItem.getMaterial().getProtection(armorItem.getType())
                - otherItem.getMaterial().getProtection(otherItem.getType());
            if (durabilityDiff != 0) {
                return durabilityDiff;
            }
            Enchantment enchantment = priorityConfig.get().getEnchantment();
            if (blastLeggingsConfig.get() && armorType == 2
                && hasEnchantment(armorStack, Enchantments.BLAST_PROTECTION)) {
                return -1;
            }
            if (hasEnchantment(armorStack, enchantment)) {
                return hasEnchantment(otherStack, enchantment) ? 0 : -1;
            } else {
                return hasEnchantment(otherStack, enchantment) ? 1 : 0;
            }
        }

        public ItemStack getArmorStack() {
            return armorStack;
        }

        public int getType() {
            return armorType;
        }

        public int getSlot() {
            return slot;
        }
    }
}
