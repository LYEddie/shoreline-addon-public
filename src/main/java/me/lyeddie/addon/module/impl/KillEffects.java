package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.EntityDeathEvent;
import me.lyeddie.addon.tabs.TabConfigs;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.particle.ParticleTypes;

import java.util.HashMap;
import java.util.Map;

import static me.lyeddie.addon.util.Globals.RANDOM;

public class KillEffects extends AddonModule {
    private static KillEffects INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<KillEffect> killEffectConfig = sgGeneral.add(new EnumSetting.Builder<KillEffect>()
        .name("Effect")
        .description("The kill effect to apply")
        .defaultValue(KillEffect.LIGHTNING)
        .build());
    public final Setting<Integer> strikesConfig = sgGeneral.add(new IntSetting.Builder()
        .name("Strikes")
        .description("The number of lightning strikes")
        .defaultValue(1)
        .min(1)
        .sliderMax(5)
        .visible(() -> killEffectConfig.get() == KillEffect.LIGHTNING)
        .build());

    private final Map<Entity, Long> lastAttackedEntities = new HashMap<>();

    public KillEffects() {
        super(Shoreline.MAIN, "KillEffects", "Adds effects to player deaths");
        INST = this;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() == mc.player || !(event.getEntity() instanceof PlayerEntity player) || !wasLastAttackedByPlayer(player)) {
            return;
        }
        switch (killEffectConfig.get()) {
            case LIGHTNING -> {
                for (int i = 0; i < strikesConfig.get(); i++) {
                    LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
                    lightningEntity.setPos(player.getX(), player.getY(), player.getZ());
                    mc.world.addEntity(lightningEntity);
                }
            }
            case FIREWORK -> fireworkExplode(player.getX(), player.getY(), player.getZ(), 0.5, 4);
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        if (event.packet instanceof EntityDamageS2CPacket packet && packet.sourceCauseId() == mc.player.getId()) {
            lastAttackedEntities.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue() > 5000);
            Entity entity = mc.world.getEntityById(packet.entityId());
            if (entity == null) {
                return;
            }
            lastAttackedEntities.put(entity, System.currentTimeMillis());
        }
    }

    private void fireworkExplode(double x, double y, double z, double size, int amount) {
        double d = x;
        double e = y;
        double f = z;
        for (int i = -amount; i <= amount; ++i) {
            for (int j = -amount; j <= amount; ++j) {
                for (int k = -amount; k <= amount; ++k) {
                    double g = (double) j + (RANDOM.nextDouble() - RANDOM.nextDouble()) * 0.5;
                    double h = (double) i + (RANDOM.nextDouble() - RANDOM.nextDouble()) * 0.5;
                    double l = (double) k + (RANDOM.nextDouble() - RANDOM.nextDouble()) * 0.5;
                    double m = Math.sqrt(g * g + h * h + l * l) / size + RANDOM.nextGaussian() * 0.05;
                    addExplosionParticle(d, e, f, g / m, h / m, l / m);
                    if (i == -amount || i == amount || j == -amount || j == amount) continue;
                    k += amount * 2 - 1;
                }
            }
        }
    }

    private void addExplosionParticle(double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        if (mc.particleManager == null || mc.world == null) {
            return;
        }
        FireworksSparkParticle.Explosion explosion = (FireworksSparkParticle.Explosion) mc.particleManager.addParticle(ParticleTypes.FIREWORK, x, y, z, velocityX, velocityY, velocityZ);
        if (explosion == null) {
            return;
        }
        explosion.setTrail(false);
        explosion.setFlicker(false);
        explosion.setColor(TabConfigs.get().getColorRGB());
    }

    private boolean wasLastAttackedByPlayer(Entity entity) {
        Long lastAttackedTime = lastAttackedEntities.get(entity);
        return lastAttackedTime != null && (System.currentTimeMillis() - lastAttackedTime) < 5000;
    }

    public enum KillEffect {
        LIGHTNING,
        FIREWORK
    }

    public static KillEffects getInstance() {
        return INST;
    }
}
