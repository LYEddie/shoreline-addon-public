package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.managers.Managers;
import meteordevelopment.meteorclient.settings.*;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

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
            if (stack.isEmpty()) {
                continue;
            }
            if (noBindingConfig.get() && hasEnchantment(stack, Enchantments.BINDING_CURSE)) {
                continue;
            }
            var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
            if (equippable == null || getArmorValue(stack) <= 0.0) continue;
            int index = equippable.slot().getEntitySlotId();
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
            ItemStack armorStack = getArmorStack(i);
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
        ItemStack stack = getArmorStack(armorSlot);
        armorSlot = 8 - armorSlot;
        Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
        boolean rt = !stack.isEmpty();
        Managers.INVENTORY.pickupSlot(armorSlot);
        if (rt) {
            Managers.INVENTORY.pickupSlot(slot < 9 ? slot + 36 : slot);
        }
    }

    public boolean hasEnchantment(ItemStack armorStack, RegistryKey<Enchantment> enchantment) {
        if (armorStack.getComponents().contains(DataComponentTypes.ENCHANTMENTS)) {
            for (RegistryEntry<Enchantment> entry : armorStack.getComponents()
                .get(DataComponentTypes.ENCHANTMENTS).getEnchantments()) {
                if (entry.getKey().isPresent() && entry.getKey().get().equals(enchantment)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static AutoArmorII getInstance() {
        return INST;
    }

    public enum Priority {
        BLAST_PROTECTION(Enchantments.BLAST_PROTECTION),
        PROTECTION(Enchantments.PROTECTION),
        PROJECTILE_PROTECTION(Enchantments.PROJECTILE_PROTECTION);

        private final RegistryKey<Enchantment> enchant;

        Priority(RegistryKey<Enchantment> enchant) {
            this.enchant = enchant;
        }

        public RegistryKey<Enchantment> getEnchantment() {
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
            int durabilityDiff = Double.compare(getArmorValue(armorStack), getArmorValue(otherStack));
            if (durabilityDiff != 0) {
                return durabilityDiff;
            }
            RegistryKey<Enchantment> enchantment = priorityConfig.get().getEnchantment();
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

    private double getArmorValue(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return 0.0;

        return modifiers.modifiers().stream()
            .filter(entry -> entry.attribute().equals(EntityAttributes.ARMOR))
            .mapToDouble(entry -> entry.modifier().value())
            .sum();
    }

    private ItemStack getArmorStack(int armorSlot) {
        return mc.player.getEquippedStack(switch (armorSlot) {
            case 0 -> net.minecraft.entity.EquipmentSlot.FEET;
            case 1 -> net.minecraft.entity.EquipmentSlot.LEGS;
            case 2 -> net.minecraft.entity.EquipmentSlot.CHEST;
            case 3 -> net.minecraft.entity.EquipmentSlot.HEAD;
            default -> throw new IllegalArgumentException("Invalid armor slot: " + armorSlot);
        });
    }
}
