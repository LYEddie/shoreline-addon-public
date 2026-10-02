package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.AttackBlockEvent;
import me.lyeddie.addon.util.literal.EnchantmentUtil;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AutoTool extends AddonModule {
    private static AutoTool INST;

    public AutoTool() {
        super(Shoreline.MAIN, "AutoTool", "Automatically switches to a tool before mining");
        INST = this;
    }

    @EventHandler
    public void onBreakBlock(final AttackBlockEvent event) {
        final BlockState state = mc.level.getBlockState(event.getPos());
        final int blockSlot = getBestToolNoFallback(state);
        if (blockSlot != -1) {
            mc.player.getInventory().setSelectedSlot(blockSlot);
        }
    }

    public int getBestTool(final BlockState state) {
        int slot = getBestToolNoFallback(state);
        if (slot != -1) {
            return slot;
        }
        return mc.player.getInventory().getSelectedSlot();
    }

    public int getBestToolNoFallback(final BlockState state) {
        if (state.getBlock() == Blocks.COBWEB) {
            for (int i = 0; i < 9; i++) {
                final ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty() || !stack.has(DataComponents.WEAPON)) {
                    continue;
                }
                return i;
            }
        }
        int slot = -1;
        float bestTool = 0.0f;
        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.has(DataComponents.TOOL)) {
                continue;
            }
            float speed = stack.getDestroySpeed(state);
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
