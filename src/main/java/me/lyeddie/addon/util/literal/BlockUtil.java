package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.util.Globals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.ChunkSource;

public class BlockUtil implements Globals {

    public static boolean isBlockAccessible(BlockPos pos) {
        return mc.level.isEmptyBlock(pos) && !mc.level.isEmptyBlock(pos.offset(0, -1, 0))
            && mc.level.isEmptyBlock(pos.offset(0, 1, 0)) && mc.level.isEmptyBlock(pos.offset(0, 2, 0));
    }

    public static boolean isBlockLoaded(double x, double z) {
        ChunkSource chunkManager = mc.level.getChunkSource();
        if (chunkManager != null) {
            return chunkManager.hasChunk(SectionPos.posToSectionCoord(x),
                SectionPos.posToSectionCoord(z));
        }
        return false;
    }
}
