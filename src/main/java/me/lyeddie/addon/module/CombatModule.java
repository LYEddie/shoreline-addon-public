package me.lyeddie.addon.module;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import java.util.Comparator;

public class CombatModule extends RotationModule {

    public CombatModule(Category category, String name, String description) {
        super(category, name, description);
    }

    public CombatModule(Category category, String name, String description, int rotationPriority) {
        super(category, name, description, rotationPriority);
    }

    public PlayerEntity getClosestPlayer(double range) {
        return mc.world.getPlayers().stream().filter(e -> !(e instanceof ClientPlayerEntity) && !e.isSpectator())
            .filter(e -> mc.player.squaredDistanceTo(e) <= range * range)
            .filter(e -> !Friends.get().isFriend(e))
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e))).orElse(null);
    }

    public boolean checkMultitask() {
        return checkMultitask(false);
    }

    public boolean checkMultitask(boolean checkOffhand) {
        if (checkOffhand && mc.player.getActiveHand() != Hand.MAIN_HAND) {
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
