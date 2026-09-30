package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class ChestSwapII extends AddonModule {
    private static ChestSwapII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Priority> priorityConfig = sgGeneral.add(new EnumSetting.Builder<Priority>()
        .name("Priority").description("The chestplate material to prioritize")
        .defaultValue(Priority.NETHERITE)
        .build());
    private final Setting<Boolean> autoFireworkConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("AutoFirework").description("Automatically fireworks when swapping to an elytra")
        .defaultValue(false)
        .build());

    public ChestSwapII() {
        super(Shoreline.MAIN, "ChestSwapII", "Automatically swaps chestplate");
        INST = this;
    }

    @Override
    public void onActivate() {
        ItemStack armorStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (isChestArmor(armorStack)) {
            int elytraSlot = getElytraSlot();
            if (elytraSlot != -1) {
                Managers.INVENTORY.pickupSlot(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot);
                Managers.INVENTORY.pickupSlot(6);
                Managers.INVENTORY.pickupSlot(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot);
                if (autoFireworkConfig.get() && !mc.player.isOnGround()) {
                    Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    int slot = -1;
                    for (int i = 0; i < 45; i++) {
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (stack.getItem() == Items.FIREWORK_ROCKET) {
                            slot = i;
                            break;
                        }
                    }
                    if (slot == -1) {
                        return;
                    }
                    if (slot < 9) {
                        Managers.INVENTORY.setSlot(slot);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        Managers.INVENTORY.syncToClient();
                    } else {
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, mc.player.getInventory().getSelectedSlot() + 36, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, mc.player.getInventory().getSelectedSlot() + 36, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                    }
                }
            }
        } else {
            int chestplateSlot = getChestplateSlot();
            if (chestplateSlot != -1) {
                Managers.INVENTORY.pickupSlot(chestplateSlot < 9 ? chestplateSlot + 36 : chestplateSlot);
                Managers.INVENTORY.pickupSlot(6);
                Managers.INVENTORY.pickupSlot(chestplateSlot < 9 ? chestplateSlot + 36 : chestplateSlot);
            }
        }
        toggle();
    }

    private int getChestplateSlot() {
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isChestArmor(stack)) {
                if (stack.isOf(Items.NETHERITE_CHESTPLATE) && priorityConfig.get() == Priority.NETHERITE) {
                    slot = i;
                    break;
                } else if (stack.isOf(Items.DIAMOND_CHESTPLATE) && priorityConfig.get() == Priority.DIAMOND) {
                    slot = i;
                    break;
                } else {
                    slot = i;
                }
            }
        }
        return slot;
    }

    private int getElytraSlot() {
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.ELYTRA)) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private boolean isChestArmor(ItemStack stack) {
        var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST
            && stack.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS) && !stack.isOf(Items.ELYTRA);
    }

    public static ChestSwapII getInstance() {
        return INST;
    }

    private enum Priority {
        NETHERITE,
        DIAMOND
    }
}
