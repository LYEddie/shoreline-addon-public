package me.lyeddie.addon.module;

import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ObsidianPlacerModule extends BlockPlacerModule {
    private static final List<Block> RESISTANT_BLOCKS = new LinkedList<>() {{
        add(Blocks.OBSIDIAN);
        add(Blocks.CRYING_OBSIDIAN);
        add(Blocks.ENDER_CHEST);
    }};

    public ObsidianPlacerModule(Category category, String name, String description) {
        super(category, name, description);
    }

    public ObsidianPlacerModule(Category category, String name, String description, int rotationPriority) {
        super(category, name, description, rotationPriority);
    }

    protected BlockSlot getResistantBlockItem() {
        Set<BlockSlot> blockSlots = new HashSet<>();

        for (Block type : ObsidianPlacerModule.RESISTANT_BLOCKS) {
            int slot = getBlockItemSlot(type);
            if (slot != -1) blockSlots.add(new BlockSlot(type, slot));
        }

        BlockSlot slot2 = blockSlots.stream().filter(b -> b.block() == Blocks.OBSIDIAN).findFirst().orElse(null);
        if (slot2 != null) return slot2;

        BlockSlot slot3 = blockSlots.stream().filter(b -> b.block() == Blocks.CRYING_OBSIDIAN).findFirst().orElse(null);
        if (slot3 != null) return slot3;

        BlockSlot slot4 = blockSlots.stream().filter(b -> b.block() == Blocks.ENDER_CHEST).findFirst().orElse(null);
        if (slot4 != null) return slot4;

        return null;
    }
}
