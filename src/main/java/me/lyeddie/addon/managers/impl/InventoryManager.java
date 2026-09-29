package me.lyeddie.addon.managers.impl;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.lyeddie.addon.events.irrevocable.EntityDeathEvent;
import me.lyeddie.addon.events.ItemDesyncEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.impl.accessor.AccessorBundlePacket;
import me.lyeddie.addon.module.impl.Replenish;
import me.lyeddie.addon.tabs.TabConfigs;
import me.lyeddie.addon.util.CacheTimer;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.Timer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InventoryManager implements Globals {
    private final List<PreSwapData> swapData = new CopyOnWriteArrayList<>();
    private int slot;

    public InventoryManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onPacketOutBound(final PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
            final int packetSlot = packet.getSelectedSlot();
            if (!PlayerInventory.isValidHotbarIndex(packetSlot) || slot == packetSlot && TabConfigs.get().invalidSlotTweak.get()) {
                event.setCancelled(true);
                return;
            }
            slot = packetSlot;
        }
    }

    @EventHandler
    public void onPacketInbound(final PacketEvent.Receive event) {
        if (event.packet instanceof UpdateSelectedSlotS2CPacket packet) {
            slot = packet.getSlot();
        }

        if (Replenish.getInstance().isInInventoryScreen() || !TabConfigs.get().isGrim()) {
            return;
        }

        if (event.packet instanceof BundleS2CPacket packet) {
            List<Packet<?>> allowedBundle = new ArrayList<>();
            for (Packet<?> packet1 : packet.getPackets()) {
                if (packet1 instanceof ScreenHandlerSlotUpdateS2CPacket) {
                    continue;
                }
                allowedBundle.add(packet1);
            }
            ((AccessorBundlePacket) packet).setIterable(allowedBundle);
        }

        if (event.packet instanceof ScreenHandlerSlotUpdateS2CPacket packet) {
            int slot = packet.getSlot() - 36;
            if (slot < 0 || slot > 8) {
                return;
            }

            if (packet.getStack().isEmpty()) {
                return;
            }

            for (PreSwapData data : swapData) {
                if (data.getSlot() != slot && data.getStarting() != slot) {
                    continue;
                }

                ItemStack preStack = data.getPreHolding(slot);
                if (!isEqual(preStack, packet.getStack())) {
                    event.cancel();
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onItemDesync(ItemDesyncEvent event) {
        if (isDesynced()) {
            event.cancel();
            event.setStack(getServerItem());
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() == mc.player) {
            syncToClient();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) { // ...
        swapData.removeIf(PreSwapData::isPassedClearTime);
    }

    public void setSlot(final int barSlot) {
        if (slot != barSlot && PlayerInventory.isValidHotbarIndex(barSlot)) {
            setSlotForced(barSlot);

            final ItemStack[] hotbarCopy = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                hotbarCopy[i] = mc.player.getInventory().getStack(i);
            }
            swapData.add(new PreSwapData(hotbarCopy, slot, barSlot));
        }
    }

    public void setSlotAlt(final int barSlot) {
        if (PlayerInventory.isValidHotbarIndex(barSlot)) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                barSlot + 36, slot, SlotActionType.SWAP, mc.player);
        }
    }

    public void setClientSlot(final int barSlot) {
        if (mc.player.getInventory().selectedSlot != barSlot
            && PlayerInventory.isValidHotbarIndex(barSlot)) {
            mc.player.getInventory().selectedSlot = barSlot;
            setSlotForced(barSlot);
        }
    }

    public void setSlotForced(final int barSlot) {
        Managers.NETWORK.sendPacket(new UpdateSelectedSlotC2SPacket(barSlot));
    }

    public void syncToClient() {
        if (isDesynced()) {
            setSlotForced(mc.player.getInventory().selectedSlot);

            for (PreSwapData swapData : swapData) {
                swapData.beginClear();
            }
        }
    }

    public boolean isDesynced() {
        return mc.player.getInventory().selectedSlot != slot;
    }

    public int pickupSlot(final int slot) {
        return click(slot, 0, SlotActionType.PICKUP);
    }

    public int click(int slot, int button, SlotActionType type) {
        if (slot < 0) {
            return -1;
        }
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        DefaultedList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);
        for (Slot slot1 : defaultedList) {
            list.add(slot1.getStack().copy());
        }
        screenHandler.onSlotClick(slot, button, type, mc.player);
        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();
        for (int j = 0; j < i; ++j) {
            ItemStack itemStack2;
            ItemStack itemStack = list.get(j);
            if (ItemStack.areEqual(itemStack, itemStack2 = defaultedList.get(j).getStack())) continue;
            int2ObjectMap.put(j, itemStack2.copy());
        }
        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), slot, button, type, screenHandler.getCursorStack().copy(), int2ObjectMap));
        return screenHandler.getRevision();
    }

    public int getServerSlot() {
        return slot;
    }

    public ItemStack getServerItem() {
        if (mc.player != null && getServerSlot() != -1) {
            return mc.player.getInventory().getStack(getServerSlot());
        }
        return null;
    }

    private boolean isEqual(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem().equals(stack2.getItem()) && stack1.getName().equals(stack2.getName());
    }

    public static class PreSwapData {
        private final ItemStack[] preHotbar;

        private final int starting;
        private final int swapTo;

        private Timer clearTime;

        public PreSwapData(ItemStack[] preHotbar, int start, int swapTo) {
            this.preHotbar = preHotbar;
            this.starting = start;
            this.swapTo = swapTo;
        }

        public void beginClear() {
            clearTime = new CacheTimer();
            clearTime.reset();
        }

        public boolean isPassedClearTime() {
            return clearTime != null && clearTime.passed(300);
        }

        public ItemStack getPreHolding(int i) {
            return preHotbar[i];
        }

        public int getStarting() {
            return starting;
        }

        public int getSlot() {
            return swapTo;
        }
    }
}
