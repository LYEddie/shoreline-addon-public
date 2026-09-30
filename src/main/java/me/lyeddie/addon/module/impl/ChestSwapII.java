package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        ItemStack armorStack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (isChestArmor(armorStack)) {
            int elytraSlot = getElytraSlot();
            if (elytraSlot != -1) {
                Managers.INVENTORY.pickupSlot(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot);
                Managers.INVENTORY.pickupSlot(6);
                Managers.INVENTORY.pickupSlot(elytraSlot < 9 ? elytraSlot + 36 : elytraSlot);
                if (autoFireworkConfig.get() && !mc.player.onGround()) {
                    Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    int slot = -1;
                    for (int i = 0; i < 45; i++) {
                        ItemStack stack = mc.player.getInventory().getItem(i);
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
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        Managers.INVENTORY.syncToClient();
                    } else {
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().getSelectedSlot() + 36, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, mc.player.getInventory().getSelectedSlot() + 36, 0, ClickType.PICKUP, mc.player);
                        mc.gameMode.handleInventoryMouseClick(0, slot, 0, ClickType.PICKUP, mc.player);
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
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (isChestArmor(stack)) {
                if (stack.is(Items.NETHERITE_CHESTPLATE) && priorityConfig.get() == Priority.NETHERITE) {
                    slot = i;
                    break;
                } else if (stack.is(Items.DIAMOND_CHESTPLATE) && priorityConfig.get() == Priority.DIAMOND) {
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
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.ELYTRA)) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private boolean isChestArmor(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST
            && stack.has(DataComponents.ATTRIBUTE_MODIFIERS) && !stack.is(Items.ELYTRA);
    }

    public static ChestSwapII getInstance() {
        return INST;
    }

    private enum Priority {
        NETHERITE,
        DIAMOND
    }
}
