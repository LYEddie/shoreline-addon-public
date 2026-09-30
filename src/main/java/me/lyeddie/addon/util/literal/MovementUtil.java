package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.mixin.impl.accessor.AccessorInput;
import me.lyeddie.addon.util.Globals;
import net.minecraft.client.player.ClientInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec2;

public class MovementUtil implements Globals {

    public static boolean isInputtingMovement() {
        return mc.options.keyUp.isDown()
            || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown()
            || mc.options.keyRight.isDown();
    }

    public static boolean isMovingInput() {
        Vec2 movement = mc.player.input.getMoveVector();
        return movement.y != 0.0f || movement.x != 0.0f;
    }

    public static void applySneak() {
        final float modifier = Mth.clamp(0.3f + (EnchantmentUtil.getLevel(mc.player.getItemBySlot(EquipmentSlot.FEET), Enchantments.SWIFT_SNEAK) * 0.15F), 0.0f, 1.0f);
        scale(mc.player.input, modifier);
    }

    public static float getForward(ClientInput input) {
        return input.getMoveVector().y;
    }

    public static float getSideways(ClientInput input) {
        return input.getMoveVector().x;
    }

    public static void set(ClientInput input, float sideways, float forward) {
        ((AccessorInput) input).shoreline$setMovementVector(new Vec2(sideways, forward));
    }

    public static void scale(ClientInput input, float multiplier) {
        Vec2 movement = input.getMoveVector();
        set(input, movement.x * multiplier, movement.y * multiplier);
    }
}
