package me.lyeddie.addon.managers.impl.util;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Hole implements Position {
    private final List<BlockPos> holeOffsets;
    private final BlockPos origin;
    private final HoleType safety;

    public Hole(BlockPos origin, HoleType safety, BlockPos... holeOffsets) {
        this.origin = origin;
        this.safety = safety;
        this.holeOffsets = Lists.newArrayList(holeOffsets);
        this.holeOffsets.add(origin);
    }

    public double squaredDistanceTo(Entity entity) {
        return entity.getEyePosition().distanceToSqr(getCenter());
    }

    public boolean isStandard() {
        return holeOffsets.size() == 5;
    }

    public boolean isDouble() {
        return holeOffsets.size() == 8;
    }

    public boolean isDoubleX() {
        return isDouble() && holeOffsets.contains(origin.offset(2, 0, 0));
    }

    public boolean isDoubleZ() {
        return isDouble() && holeOffsets.contains(origin.offset(0, 0, 2));
    }

    public boolean isQuad() {
        return holeOffsets.size() == 12;
    }

    public HoleType getSafety() {
        return safety;
    }

    public BlockPos getPos() {
        return origin;
    }

    public List<BlockPos> getHoleOffsets() {
        return holeOffsets;
    }

    public boolean addHoleOffsets(BlockPos... off) {
        return holeOffsets.addAll(Arrays.asList(off));
    }

    public Vec3 getCenter() {
        BlockPos center;
        if (isDoubleX()) {
            center = origin.offset(1, 0, 0);
        } else if (isDoubleZ()) {
            center = origin.offset(0, 0, -1);
        } else if (isQuad()) {
            center = origin.offset(1, 0, -1);
        } else {
            return Vec3.atCenterOf(origin);
        }
        return Vec3.atLowerCornerOf(center);
    }

    public AABB getBoundingBox(double height) {
        AABB render = null;
        if (getSafety() == HoleType.VOID) {
            render = new AABB(x(), y(), z(), x() + 1.0, y() + 1.0, z() + 1.0);
        } else if (isDoubleX()) {
            render = new AABB(x(), y(), z(), x() + 2.0, y() + height, z() + 1.0);
        } else if (isDoubleZ()) {
            render = new AABB(x(), y(), z(), x() + 1.0, y() + height, z() + 2.0);
        } else if (isQuad()) {
            render = new AABB(x(), y(), z(), x() + 2.0, y() + height, z() + 2.0);
        } else if (isStandard()) {
            render = new AABB(x(), y(), z(), x() + 1.0, y() + height, z() + 1.0);
        }
        return render;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Hole hole) {
            return new HashSet<>(hole.getHoleOffsets()).containsAll(holeOffsets);
        }
        return false;
    }

    @Override
    public double x() {
        return origin.getX();
    }

    @Override
    public double y() {
        return origin.getY();
    }

    @Override
    public double z() {
        return origin.getZ();
    }
}
