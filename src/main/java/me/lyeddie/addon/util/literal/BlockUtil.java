package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.util.Globals;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkManager;

public class BlockUtil implements Globals {

    public static boolean isBlockAccessible(BlockPos pos) {
        return mc.world.isAir(pos) && !mc.world.isAir(pos.add(0, -1, 0))
            && mc.world.isAir(pos.add(0, 1, 0)) && mc.world.isAir(pos.add(0, 2, 0));
    }

    public static boolean isBlockLoaded(double x, double z) {
        ChunkManager chunkManager = mc.world.getChunkManager();
        if (chunkManager != null) {
            return chunkManager.isChunkLoaded(ChunkSectionPos.getSectionCoord(x),
                ChunkSectionPos.getSectionCoord(z));
        }
        return false;
    }
}
