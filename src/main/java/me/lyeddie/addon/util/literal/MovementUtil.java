package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.mixin.impl.accessor.AccessorInput;
import me.lyeddie.addon.util.Globals;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.MathHelper;

public class MovementUtil implements Globals {

    public static boolean isInputtingMovement() {
        return mc.options.forwardKey.isPressed()
            || mc.options.backKey.isPressed()
            || mc.options.leftKey.isPressed()
            || mc.options.rightKey.isPressed();
    }

    public static boolean isMovingInput() {
        Vec2f movement = mc.player.input.getMovementInput();
        return movement.y != 0.0f || movement.x != 0.0f;
    }

    public static void applySneak() {
        final float modifier = MathHelper.clamp(0.3f + (EnchantmentUtil.getLevel(mc.player.getEquippedStack(EquipmentSlot.FEET), Enchantments.SWIFT_SNEAK) * 0.15F), 0.0f, 1.0f);
        scale(mc.player.input, modifier);
    }

    public static float getForward(Input input) {
        return input.getMovementInput().y;
    }

    public static float getSideways(Input input) {
        return input.getMovementInput().x;
    }

    public static void set(Input input, float sideways, float forward) {
        ((AccessorInput) input).shoreline$setMovementVector(new Vec2f(sideways, forward));
    }

    public static void scale(Input input, float multiplier) {
        Vec2f movement = input.getMovementInput();
        set(input, movement.x * multiplier, movement.y * multiplier);
    }
}
