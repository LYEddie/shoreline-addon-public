package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.util.Globals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class EntityUtil implements Globals {
    public static List<ItemStack> getArmorItems(LivingEntity entity) {
        return List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD).stream()
            .map(entity::getItemBySlot)
            .filter(stack -> !stack.isEmpty())
            .toList();
    }

    public static BlockPos getRoundedBlockPos(Entity entity) {
        return new BlockPos(entity.getBlockX(), (int) Math.round(entity.getY()), entity.getBlockZ());
    }

    public static float getHealth(Entity entity) {
        if (entity instanceof LivingEntity e) {
            return e.getHealth() + e.getAbsorptionAmount();
        }
        return 0.0f;
    }

    public static boolean isMonster(Entity e) {
        return e instanceof Enemy && !isNeutralInternal(e);
    }

    private static boolean isNeutralInternal(Entity e) {
        return e instanceof EnderMan enderman && !enderman.isAggressive()
            || e instanceof ZombifiedPiglin piglin && !piglin.isAggressive()
            || e instanceof Wolf wolf && !wolf.isAggressive()
            || e instanceof IronGolem ironGolem && !ironGolem.isAggressive()
            || e instanceof Bee bee && !bee.isAggressive();
    }

    public static boolean isNeutral(Entity e) {
        return e instanceof EnderMan || e instanceof ZombifiedPiglin || e instanceof Wolf || e instanceof IronGolem;
    }

    public static boolean isPassive(Entity e) {
        return e instanceof AgeableMob || e instanceof AmbientCreature || e instanceof Squid;
    }

    public static boolean isVehicle(Entity e) {
        return e instanceof Boat || e instanceof Minecart
            || e instanceof MinecartFurnace
            || e instanceof MinecartChest;
    }
}
