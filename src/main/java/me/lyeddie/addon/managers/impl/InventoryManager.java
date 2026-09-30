package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.irrevocable.EntityDeathEvent;
import me.lyeddie.addon.events.ItemDesyncEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.impl.accessor.AccessorBundlePacket;
import me.lyeddie.addon.module.impl.Replenish;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.CacheTimer;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.Timer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
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
        if (event.packet instanceof ServerboundSetCarriedItemPacket packet) {
            final int packetSlot = packet.getSlot();
            if (!Inventory.isHotbarSlot(packetSlot) || slot == packetSlot && TabConfigs.get().invalidSlotTweak.get()) {
                event.setCancelled(true);
                return;
            }
            slot = packetSlot;
        }
    }

    @EventHandler
    public void onPacketInbound(final PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundSetHeldSlotPacket packet) {
            slot = packet.slot();
        }

        if (Replenish.getInstance().isInInventoryScreen() || !TabConfigs.get().isGrim()) {
            return;
        }

        if (event.packet instanceof ClientboundBundlePacket packet) {
            List<Packet<?>> allowedBundle = new ArrayList<>();
            for (Packet<?> packet1 : packet.subPackets()) {
                if (packet1 instanceof ClientboundContainerSetSlotPacket) {
                    continue;
                }
                allowedBundle.add(packet1);
            }
            ((AccessorBundlePacket) packet).setIterable(allowedBundle);
        }

        if (event.packet instanceof ClientboundContainerSetSlotPacket packet) {
            int slot = packet.getSlot() - 36;
            if (slot < 0 || slot > 8) {
                return;
            }

            if (packet.getItem().isEmpty()) {
                return;
            }

            for (PreSwapData data : swapData) {
                if (data.getSlot() != slot && data.getStarting() != slot) {
                    continue;
                }

                ItemStack preStack = data.getPreHolding(slot);
                if (!isEqual(preStack, packet.getItem())) {
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
        if (slot != barSlot && Inventory.isHotbarSlot(barSlot)) {
            setSlotForced(barSlot);

            final ItemStack[] hotbarCopy = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                hotbarCopy[i] = mc.player.getInventory().getItem(i);
            }
            swapData.add(new PreSwapData(hotbarCopy, slot, barSlot));
        }
    }

    public void setSlotAlt(final int barSlot) {
        if (Inventory.isHotbarSlot(barSlot)) {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId,
                barSlot + 36, slot, ClickType.SWAP, mc.player);
        }
    }

    public void setClientSlot(final int barSlot) {
        if (mc.player.getInventory().getSelectedSlot() != barSlot
            && Inventory.isHotbarSlot(barSlot)) {
            mc.player.getInventory().setSelectedSlot(barSlot);
            setSlotForced(barSlot);
        }
    }

    public void setSlotForced(final int barSlot) {
        Managers.NETWORK.sendPacket(new ServerboundSetCarriedItemPacket(barSlot));
    }

    public void syncToClient() {
        if (isDesynced()) {
            setSlotForced(mc.player.getInventory().getSelectedSlot());

            for (PreSwapData swapData : swapData) {
                swapData.beginClear();
            }
        }
    }

    public boolean isDesynced() {
        return mc.player.getInventory().getSelectedSlot() != slot;
    }

    public int pickupSlot(final int slot) {
        return click(slot, 0, ClickType.PICKUP);
    }

    public int click(int slot, int button, ClickType type) {
        if (slot < 0) {
            return -1;
        }
        AbstractContainerMenu screenHandler = mc.player.containerMenu;
        mc.gameMode.handleInventoryMouseClick(screenHandler.containerId, slot, button, type, mc.player);
        return screenHandler.getStateId();
    }

    public int getServerSlot() {
        return slot;
    }

    public ItemStack getServerItem() {
        if (mc.player != null && getServerSlot() != -1) {
            return mc.player.getInventory().getItem(getServerSlot());
        }
        return null;
    }

    private boolean isEqual(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem().equals(stack2.getItem()) && stack1.getHoverName().equals(stack2.getHoverName());
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
