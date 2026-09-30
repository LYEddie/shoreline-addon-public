package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.util.Globals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Set;
import java.util.function.BiFunction;

public class ExplosionUtil implements Globals {

    public static double getDamageTo(final Entity entity, final Vec3 explosion, boolean assumeBestArmor) {
        return getDamageTo(entity, explosion, false, assumeBestArmor);
    }

    public static double getDamageTo(final Entity entity, final Vec3 explosion, final boolean ignoreTerrain, boolean assumeBestArmor) {
        return getDamageTo(entity, explosion, ignoreTerrain, 12.0f, 0, assumeBestArmor);
    }

    public static double getDamageTo(final Entity entity, final Vec3 explosion, final boolean ignoreTerrain, int extrapolationTicks, boolean assumeBestArmor) {
        return getDamageTo(entity, explosion, ignoreTerrain, 12.0f, extrapolationTicks, assumeBestArmor);
    }

    public static double getDamageTo(final Entity entity, final Vec3 explosion, final boolean ignoreTerrain, final Set<BlockPos> ignoreBlocks, int extrapolationTicks, boolean assumeBestArmor) {
        return getDamageTo(entity, explosion, ignoreTerrain, 12.0f, ignoreBlocks, extrapolationTicks, assumeBestArmor);
    }

    public static double getDamageTo(final Entity entity, final Vec3 explosion, final boolean ignoreTerrain, float power, final Set<BlockPos> ignoreBlocks, int extrapolationTicks, boolean assumeBestArmor) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        Vec3 vec3d2 = Vec3.ZERO;
        if (extrapolationTicks != 0) {
            double ox = (x - entity.xo) * extrapolationTicks;
            double oy = (y - entity.yo) * extrapolationTicks * 0.3;
            double oz = (z - entity.zo) * extrapolationTicks;
            x += ox;
            y += oy;
            z += oz;
            vec3d2 = new Vec3(ox, oy, oz);
        }

        Vec3 vec3d = new Vec3(x, y, z);
        double d = Math.sqrt(vec3d.distanceToSqr(explosion));
        double ab = getExposure(explosion, entity.getBoundingBox().move(vec3d2), ignoreTerrain ? IgnoreTerrain.BLAST : IgnoreTerrain.NONE, ignoreBlocks);
        double w = d / power;
        double ac = (1.0 - w) * ab;
        double dmg = (float) ((int) ((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0));
        dmg = getReduction(entity, mc.level.damageSources().explosion(null), dmg, assumeBestArmor);
        return Math.max(0.0, dmg);
    }

    public static double getDamageTo(final Entity entity, final Vec3 explosion, final boolean ignoreTerrain, float power, int extrapolationTicks, boolean assumeBestArmor) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        Vec3 vec3d2 = Vec3.ZERO;
        if (extrapolationTicks != 0) {
            double ox = (x - entity.xo) * extrapolationTicks;
            double oy = (y - entity.yo) * extrapolationTicks * 0.3;
            double oz = (z - entity.zo) * extrapolationTicks;
            x += ox;
            y += oy;
            z += oz;
            vec3d2 = new Vec3(ox, oy, oz);
        }

        Vec3 vec3d = new Vec3(x, y, z);
        double d = Math.sqrt(vec3d.distanceToSqr(explosion));
        double ab = getExposure(explosion, entity.getBoundingBox().move(vec3d2), ignoreTerrain ? IgnoreTerrain.BLAST : IgnoreTerrain.NONE);
        double w = d / power;
        double ac = (1.0 - w) * ab;
        double dmg = (float) ((int) ((ac * ac + ac) / 2.0 * 7.0 * 12.0 + 1.0));
        dmg = getReduction(entity, mc.level.damageSources().explosion(null), dmg, assumeBestArmor);
        return Math.max(0.0, dmg);
    }

    private static double getReduction(Entity entity, DamageSource damageSource, double damage, boolean assumeBestArmor) {
        if (damageSource.scalesWithDifficulty()) {
            switch (mc.level.getDifficulty()) {
                case EASY -> damage = Math.min(damage / 2 + 1, damage);
                case HARD -> damage *= 1.5f;
            }
        }

        if (entity instanceof LivingEntity livingEntity) {
            damage = CombatRules.getDamageAfterAbsorb(livingEntity, (float) damage, damageSource, getArmor(livingEntity), (float) livingEntity.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            damage = getResistanceReduction(livingEntity, damage);
            damage = getProtectionReduction(livingEntity, damage, damageSource, assumeBestArmor);
        }

        return Math.max(damage, 0);
    }

    private static float getArmor(LivingEntity entity) {
        return (float) Math.floor(entity.getAttributeValue(Attributes.ARMOR));
    }

    private static float getProtectionReduction(Entity player, double damage, DamageSource source, boolean assumeBestArmor) {
        if (player instanceof LivingEntity livingEntity) {
            float protLevel = getProtectionAmount(EntityUtil.getArmorItems(livingEntity), assumeBestArmor);
            return CombatRules.getDamageAfterMagicAbsorb((float) damage, protLevel);
        }
        return 0.0f;
    }

    private static float getProtectionAmount(Iterable<ItemStack> equipment, boolean assumeBestArmor) {
        MutableInt mutableInt = new MutableInt();
        equipment.forEach(i -> {
            if (assumeBestArmor && EnchantmentUtil.isFakeEnchant2b2t(i)) {
                var equippable = i.get(DataComponents.EQUIPPABLE);
                mutableInt.add(equippable != null && equippable.slot() == EquipmentSlot.LEGS ? 8 : 4);
            } else {
                int modifierBlast = EnchantmentUtil.getLevel(i, Enchantments.BLAST_PROTECTION);
                int modifier = EnchantmentUtil.getLevel(i, Enchantments.PROTECTION);
                mutableInt.add(modifierBlast * 2 + modifier);
            }
        });
        return mutableInt.intValue();
    }

    private static double getResistanceReduction(LivingEntity player, double damage) {
        MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
        if (resistance != null) {
            int lvl = resistance.getAmplifier() + 1;
            damage *= (1.0f - (lvl * 0.2f));
        }

        return Math.max(damage, 0.0f);
    }

    private static float getExposure(final Vec3 source, final AABB box, final IgnoreTerrain ignoreTerrain, final Set<BlockPos> ignoreBlocks) {
        RaycastFactory raycastFactory = getRaycastFactory(ignoreTerrain, ignoreBlocks);
        return getExposure(source, box, raycastFactory);
    }

    private static float getExposure(final Vec3 source, final AABB box, final IgnoreTerrain ignoreTerrain) {
        RaycastFactory raycastFactory = getRaycastFactory(ignoreTerrain);
        return getExposure(source, box, raycastFactory);
    }

    private static float getExposure(final Vec3 source, final AABB box, final RaycastFactory raycastFactory) {
        double xDiff = box.maxX - box.minX;
        double yDiff = box.maxY - box.minY;
        double zDiff = box.maxZ - box.minZ;

        double xStep = 1 / (xDiff * 2 + 1);
        double yStep = 1 / (yDiff * 2 + 1);
        double zStep = 1 / (zDiff * 2 + 1);

        if (xStep > 0 && yStep > 0 && zStep > 0) {
            int misses = 0;
            int hits = 0;

            double xOffset = (1 - Math.floor(1 / xStep) * xStep) * 0.5;
            double zOffset = (1 - Math.floor(1 / zStep) * zStep) * 0.5;

            xStep = xStep * xDiff;
            yStep = yStep * yDiff;
            zStep = zStep * zDiff;

            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        Vec3 position = new Vec3(x, y, z);

                        if (raycast(new ExposureRaycastContext(position, source), raycastFactory) == null) misses++;

                        hits++;
                    }
                }
            }
            return (float) misses / hits;
        }
        return 0f;
    }

    private static RaycastFactory getRaycastFactory(IgnoreTerrain ignoreTerrain, Set<BlockPos> ignoreBlocks) {
        if (ignoreTerrain == IgnoreTerrain.BLAST) {
            return (context, blockPos) -> {
                if (ignoreBlocks.contains(blockPos)) {
                    return null;
                }
                BlockState blockState = mc.level.getBlockState(blockPos);
                if (blockState.getBlock().getExplosionResistance() < 600) return null;

                return blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
            };
        } else if (ignoreTerrain == IgnoreTerrain.ALL) {
            return (context, blockPos) -> null;
        } else {
            return (context, blockPos) ->
            {
                if (ignoreBlocks.contains(blockPos)) {
                    return null;
                }
                BlockState blockState = mc.level.getBlockState(blockPos);
                return blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
            };
        }
    }

    private static RaycastFactory getRaycastFactory(IgnoreTerrain ignoreTerrain) {
        if (ignoreTerrain == IgnoreTerrain.BLAST) {
            return (context, blockPos) -> {
                BlockState blockState = mc.level.getBlockState(blockPos);
                if (blockState.getBlock().getExplosionResistance() < 600) return null;

                return blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
            };
        } else if (ignoreTerrain == IgnoreTerrain.ALL) {
            return (context, blockPos) -> null;
        } else {
            return (context, blockPos) ->
            {
                BlockState blockState = mc.level.getBlockState(blockPos);
                return blockState.getCollisionShape(mc.level, blockPos).clip(context.start(), context.end(), blockPos);
            };
        }
    }

    private static BlockHitResult raycast(ExposureRaycastContext context, RaycastFactory raycastFactory) {
        return BlockGetter.traverseBlocks(context.start, context.end, context, raycastFactory, ctx -> null);
    }

    public enum IgnoreTerrain {
        ALL,
        BLAST,
        NONE
    }

    @FunctionalInterface
    public interface RaycastFactory extends BiFunction<ExposureRaycastContext, BlockPos, BlockHitResult> {
    }

    public record ExposureRaycastContext(Vec3 start, Vec3 end) {
    }
}
