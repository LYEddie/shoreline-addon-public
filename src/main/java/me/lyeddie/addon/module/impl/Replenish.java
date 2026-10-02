package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.irrevocable.EntityDeathEvent;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.util.CacheTimer;
import me.lyeddie.addon.util.literal.InventoryUtil;
import me.lyeddie.addon.util.Timer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Replenish extends AddonModule {
    private static Replenish INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> percentConfig = sgGeneral.add(new IntSetting.Builder()
        .name("Percent").description("The minimum percent of total stack before replenishing")
        .defaultValue(25)
        .min(1)
        .sliderMax(80)
        .build());
    private final Setting<Boolean> resistantConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("AllowResistant").description("Refills obsidian with other types of resistant blocks")
        .defaultValue(false)
        .build());

    private final Map<Integer, ItemStack> hotbarCache = new ConcurrentHashMap<>();
    private final Timer lastDroppedTimer = new CacheTimer();

    public Replenish() {
        super(Shoreline.MAIN, "Replenish", "Automatically replaces items in your hotbar");
        INST = this;
    }

    @Override
    public void onDeactivate() {
        hotbarCache.clear();
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        hotbarCache.clear();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof ClientPlayerEntity) {
            hotbarCache.clear();
        }
    }

    @EventHandler
    public void onTick(PlayerTickEvent event) {
        if (mc.options.dropKey.isPressed()) {
            lastDroppedTimer.reset();
        }

        boolean pauseReplenish = isInInventoryScreen() || !lastDroppedTimer.passed(100);

        if (!pauseReplenish) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty()) {
                    ItemStack cachedStack = hotbarCache.getOrDefault(i, null);
                    if (cachedStack != null && !cachedStack.isEmpty()) {
                        replenishStack(i, cachedStack);
                        break;
                    }
                    continue;
                }

                if (!stack.isStackable()) {
                    continue;
                }

                double percentage = ((double) stack.getCount() / stack.getMaxCount()) * 100.0;
                if (percentage <= percentConfig.get()) {
                    replenishStack(i, stack);
                    break;
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() && !pauseReplenish) {
                continue;
            }

            if (hotbarCache.containsKey(i)) {
                hotbarCache.replace(i, stack.copy());
            } else {
                hotbarCache.put(i, stack.copy());
            }
        }
    }

    public boolean isInInventoryScreen() {
        return mc.currentScreen instanceof GenericContainerScreen || mc.currentScreen instanceof ShulkerBoxScreen || mc.currentScreen instanceof InventoryScreen;
    }

    private void replenishStack(int slot, ItemStack stack) {
        int slot1 = -1;
        boolean outOfObsidian = stack.getItem() == Items.OBSIDIAN && InventoryUtil.count(Items.OBSIDIAN) <= 1;
        for (int i = 9; i < 36; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (itemStack.isEmpty()) {
                continue;
            }

            if (!isSame(stack, itemStack, outOfObsidian) || !itemStack.isStackable()) {
                continue;
            }

            slot1 = i;
        }

        if (slot1 != -1) {
            mc.interactionManager.clickSlot(0, slot1, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, slot + 36, 0, SlotActionType.PICKUP, mc.player);
            if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(0, slot1, 0, SlotActionType.PICKUP, mc.player);
            }
        }
    }

    public boolean isSame(ItemStack stack1, ItemStack stack2, boolean outOfObsidian) {
        if (resistantConfig.get() && stack1.getItem() == Items.OBSIDIAN && outOfObsidian) {
            return stack2.getItem() == Items.ENDER_CHEST || stack2.getItem() == Items.CRYING_OBSIDIAN;
        } else if (stack1.getItem() instanceof BlockItem blockItem
            && (!(stack2.getItem() instanceof BlockItem blockItem1) || blockItem.getBlock() != blockItem1.getBlock())) {
            return false;
        } else if (!stack1.getName().getString().equals(stack2.getName().getString())) {
            return false;
        }

        return stack1.getItem().equals(stack2.getItem());
    }

    public static Replenish getInstance() {
        return INST;
    }
}
