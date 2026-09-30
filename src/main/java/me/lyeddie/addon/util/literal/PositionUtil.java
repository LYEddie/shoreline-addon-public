package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.util.BlastResistantBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;

public class PositionUtil {

    public static BlockPos getRoundedBlockPos(final double x, final double y, final double z) {
        final int flooredX = Mth.floor(x);
        final int flooredY = (int) Math.round(y);
        final int flooredZ = Mth.floor(z);
        return new BlockPos(flooredX, flooredY, flooredZ);
    }

    public static boolean isBedrock(AABB box, BlockPos pos) {
        return getAllInBox(box, pos).stream().anyMatch(BlastResistantBlocks::isUnbreakable);
    }

    public static List<BlockPos> getAllInBox(AABB box, BlockPos pos) {
        final List<BlockPos> intersections = new ArrayList<>();
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                intersections.add(new BlockPos(x, pos.getY(), z));
            }
        }
        return intersections;
    }

    public static List<BlockPos> getAllInBox(AABB box) {
        final List<BlockPos> intersections = new ArrayList<>();
        for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                    intersections.add(new BlockPos(x, y, z));
                }
            }
        }
        return intersections;
    }
}
