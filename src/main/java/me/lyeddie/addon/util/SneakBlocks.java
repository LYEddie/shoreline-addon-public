package me.lyeddie.addon.util;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SneakBlocks {
    private static final Set<Block> SNEAK_BLOCKS;

    static {
        Set<Block> blocks = new HashSet<>(Set.of(
            Blocks.CHEST,
            Blocks.ENDER_CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.CRAFTING_TABLE,
            Blocks.FURNACE,
            Blocks.BLAST_FURNACE,
            Blocks.FLETCHING_TABLE,
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.ENCHANTING_TABLE,
            Blocks.SMITHING_TABLE,
            Blocks.STONECUTTER,
            Blocks.JUKEBOX,
            Blocks.NOTE_BLOCK,
            Blocks.SHULKER_BOX,
            Blocks.ACACIA_TRAPDOOR,
            Blocks.BAMBOO_TRAPDOOR,
            Blocks.BIRCH_TRAPDOOR,
            Blocks.CHERRY_TRAPDOOR,
            Blocks.SPRUCE_TRAPDOOR,
            Blocks.WARPED_TRAPDOOR,
            Blocks.IRON_TRAPDOOR,
            Blocks.DARK_OAK_TRAPDOOR,
            Blocks.JUNGLE_TRAPDOOR,
            Blocks.MANGROVE_TRAPDOOR,
            Blocks.OAK_TRAPDOOR,
            Blocks.CRIMSON_TRAPDOOR
        ));
        blocks.addAll(Blocks.DYED_SHULKER_BOX.asList());
        blocks.addAll(Blocks.COPPER_TRAPDOOR.asList());
        SNEAK_BLOCKS = Set.copyOf(blocks);
    }

    public static boolean isSneakBlock(BlockState state) {
        return isSneakBlock(state.getBlock());
    }

    public static boolean isSneakBlock(Block block) {
        return SNEAK_BLOCKS.contains(block);
    }
}
