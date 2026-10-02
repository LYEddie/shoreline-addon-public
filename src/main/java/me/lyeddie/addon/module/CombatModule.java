package me.lyeddie.addon.module;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import java.util.Comparator;

public class CombatModule extends RotationModule {

    public CombatModule(Category category, String name, String description) {
        super(category, name, description);
    }

    public CombatModule(Category category, String name, String description, int rotationPriority) {
        super(category, name, description, rotationPriority);
    }

    public Player getClosestPlayer(double range) {
        return mc.level.players().stream().filter(e -> !(e instanceof LocalPlayer) && !e.isSpectator())
            .filter(e -> mc.player.distanceToSqr(e) <= range * range)
            .filter(e -> !Friends.get().isFriend(e))
            .min(Comparator.comparingDouble(e -> mc.player.distanceToSqr(e))).orElse(null);
    }

    public boolean checkMultitask() {
        return checkMultitask(false);
    }

    public boolean checkMultitask(boolean checkOffhand) {
        if (checkOffhand && mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        return mc.player.isUsingItem();
    }

    public Setting<Boolean> addMultitaskConfig(SettingGroup group) {
        return group.add(new BoolSetting.Builder()
            .name("Multitask")
            .description("Allows actions while using items")
            .defaultValue(false)
            .build()
        );
    }
}
