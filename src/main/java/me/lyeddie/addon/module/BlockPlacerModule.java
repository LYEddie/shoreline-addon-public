package me.lyeddie.addon.module;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class BlockPlacerModule extends CombatModule {

    public BlockPlacerModule(Category category, String name, String description) {
        super(category, name, description);
    }

    public BlockPlacerModule(Category category, String name, String description, int rotationPriority) {
        super(category, name, description, rotationPriority);
    }

    protected int getBlockItemSlot(final Block block) {
        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() == block) {
                return i;
            }
        }
        return -1;
    }

    public record BlockSlot(Block block, int slot) {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof BlockSlot b && b.block() == block;
        }
    }

    public Setting<Boolean> addStrictDirectionConfig(SettingGroup group) {
        return group.add(new BoolSetting.Builder()
            .name("StrictDirection")
            .description("Places on visible sides only")
            .defaultValue(false)
            .build()
        );
    }

    public Setting<Boolean> addRotateConfig(SettingGroup group) {
        return group.add(new BoolSetting.Builder()
            .name("Rotate")
            .description("Rotates to block before placing")
            .defaultValue(false)
            .build()
        );
    }

    public Setting<Boolean> addStopMotionConfig(SettingGroup group) {
        return group.add(new BoolSetting.Builder()
            .name("StopMotion")
            .description("Stops movement before placing")
            .defaultValue(false)
            .build()
        );
    }
}
