package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.util.Interpolation;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.irrevocable.RemoveEntityEvent;
import me.lyeddie.addon.util.tabs.TabConfigs;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BreadcrumbsII extends AddonModule {
    private static BreadcrumbsII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Targets");

    private final Setting<Boolean> infiniteConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Infinite").description("Renders breadcrumbs for all positions since toggle")
        .defaultValue(true)
        .build());
    public final Setting<Double> maxTimeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("MaxPosition").description("The maximum time for a given position")
        .defaultValue(2.0)
        .min(1.0)
        .sliderMax(20.0)
        .build());
    public final Setting<Integer> fadeTimeConfig = sgGeneral.add(new IntSetting.Builder()
        .name("Fade-Time").description("Timer for the fade")
        .defaultValue(1000)
        .min(0)
        .sliderMax(5000)
        .build());
    private final Setting<Boolean> selfConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Self").description("Renders breadcrumbs on player")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> playersConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Players").description("Renders breadcrumbs on other players")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> pearlsConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Pearls").description("Renders breadcrumbs on thrown pearls")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> arrowsConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Arrows").description("Renders breadcrumbs on arrows")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> xpBottlesConfig = sgTargets.add(new BoolSetting.Builder()
        .name("XPBottles").description("Renders breadcrumbs on thrown experience bottles")
        .defaultValue(false)
        .build());

    private final Map<Integer, List<TimedPosition>> positions = new ConcurrentHashMap<>();

    public BreadcrumbsII() {
        super(Shoreline.MAIN, "BreadcrumbsII", "Renders a line connecting all previous positions");
        INST = this;
    }

    @Override
    public void onDeactivate() {
        positions.clear();
    }

    @EventHandler
    public void onPlayerUpdate(PlayerTickEvent event) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!checkEntity(entity)) {
                continue;
            }
            final Vec3 pos = Interpolation.getInterpolatedPosition(entity, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
            if (positions.containsKey(entity.getId())) {
                positions.get(entity.getId()).add(new TimedPosition(pos, System.currentTimeMillis()));
            } else {
                List<TimedPosition> timedPositions = new CopyOnWriteArrayList<>();
                timedPositions.add(new TimedPosition(pos, System.currentTimeMillis()));
                positions.put(entity.getId(), timedPositions);
            }
        }
        if (!infiniteConfig.get()) {
            for (Map.Entry<Integer, List<TimedPosition>> entry : positions.entrySet()) {
                for (TimedPosition timedPosition : entry.getValue()) {
                    if (System.currentTimeMillis() - timedPosition.time() > maxTimeConfig.get() * 1000.0f) {
                        positions.get(entry.getKey()).remove(timedPosition);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(RemoveEntityEvent event) {
        if (infiniteConfig.get()) {
            positions.remove(event.getEntity().getId());
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        for (Map.Entry<Integer, List<TimedPosition>> entry : positions.entrySet()) {
            List<TimedPosition> timedPositions = entry.getValue();
            for (int i = 0; i < timedPositions.size(); i++) {
                TimedPosition timedPosition = timedPositions.get(i);
                SettingColor lineCol;
                if (!infiniteConfig.get()) {
                    float fade = 1.0f - Mth.clamp((System.currentTimeMillis() - timedPosition.time()) / (float) fadeTimeConfig.get(), 0.0f, 1.0f);
                    lineCol = TabConfigs.get().getClampColor((int) (fade * 255.0f));
                } else {
                    lineCol = TabConfigs.get().colorConfig.get();
                }
                if (i > 1) {
                    Vec3 vec3d = timedPositions.get(i - 1).pos();
                    Vec3 vec3d2 = timedPosition.pos();
                    event.renderer.line(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z, lineCol);
                }
            }
        }
    }

    public boolean checkEntity(Entity entity) {
        if (entity instanceof Player) {
            return playersConfig.get() || entity == mc.player && selfConfig.get();
        }
        return entity instanceof ThrownEnderpearl && pearlsConfig.get()
            || entity instanceof Arrow && arrowsConfig.get()
            || entity instanceof ThrownExperienceBottle && xpBottlesConfig.get();
    }

    private record TimedPosition(Vec3 pos, long time) {
    }

    public static BreadcrumbsII getInstance() {
        return INST;
    }
}
