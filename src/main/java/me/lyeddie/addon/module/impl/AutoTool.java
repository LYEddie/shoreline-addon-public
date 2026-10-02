package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.AttackBlockEvent;
import me.lyeddie.addon.util.literal.EnchantmentUtil;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;

public class AutoTool extends AddonModule {
    private static AutoTool INST;

    public AutoTool() {
        super(Shoreline.MAIN, "AutoTool", "Automatically switches to a tool before mining");
        INST = this;
    }

    @EventHandler
    public void onBreakBlock(final AttackBlockEvent event) {
        final BlockState state = mc.world.getBlockState(event.getPos());
        final int blockSlot = getBestToolNoFallback(state);
        if (blockSlot != -1) {
            mc.player.getInventory().selectedSlot = blockSlot;
        }
    }

    public int getBestTool(final BlockState state) {
        int slot = getBestToolNoFallback(state);
        if (slot != -1) {
            return slot;
        }
        return mc.player.getInventory().selectedSlot;
    }

    public int getBestToolNoFallback(final BlockState state) {
        if (state.getBlock() == Blocks.COBWEB) {
            for (int i = 0; i < 9; i++) {
                final ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) {
                    continue;
                }
                return i;
            }
        }
        int slot = -1;
        float bestTool = 0.0f;
        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ToolItem)) {
                continue;
            }
            float speed = stack.getMiningSpeedMultiplier(state);
            final int efficiency = EnchantmentUtil.getLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0) {
                speed += efficiency * efficiency + 1.0f;
            }
            if (speed > bestTool) {
                bestTool = speed;
                slot = i;
            }
        }
        return slot;
    }

    public static AutoTool getInstance() {
        return INST;
    }
}
