package me.lyeddie.addon.module.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.api.Interpolation;
import me.lyeddie.addon.api.RenderBuffers;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.irrevocable.RemoveEntityEvent;
import me.lyeddie.addon.events.irrevocable.RenderWorldEvent;
import me.lyeddie.addon.tabs.TabConfigs;
import meteordevelopment.meteorclient.settings.*;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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
    public final Setting<Double> widthConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Width").description("The line width of the path")
        .defaultValue(1.0)
        .min(1.0)
        .sliderMax(5.0)
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
        for (Entity entity : mc.world.getEntities()) {
            if (!checkEntity(entity)) {
                continue;
            }
            final Vec3d pos = Interpolation.getInterpolatedPosition(entity, mc.getRenderTickCounter().getTickDelta(true));
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
    public void onRenderWorld(RenderWorldEvent event) {
        event.getMatrices().push();
        RenderBuffers.preRender();
        RenderSystem.lineWidth(toFloat(widthConfig.get()));
        RenderBuffers.LINES.begin(event.getMatrices());
        for (Map.Entry<Integer, List<TimedPosition>> entry : positions.entrySet()) {
            List<TimedPosition> timedPositions = entry.getValue();
            for (int i = 0; i < timedPositions.size(); i++) {
                TimedPosition timedPosition = timedPositions.get(i);
                if (!infiniteConfig.get()) {
                    float fade = 1.0f - MathHelper.clamp((System.currentTimeMillis() - timedPosition.time()) / (float) fadeTimeConfig.get(), 0.0f, 1.0f);
                    RenderBuffers.LINES.color(TabConfigs.get().getClampColor((int) (fade * 255.0f)).getRGB());
                } else {
                    RenderBuffers.LINES.color(TabConfigs.get().getColorRGB());
                }
                if (i > 1) {
                    Vec3d vec3d = timedPositions.get(i - 1).pos();
                    Vec3d vec3d2 = timedPosition.pos();
                    RenderBuffers.LINES.vertexLine(vec3d.x, vec3d.y, vec3d.z, vec3d2.x, vec3d2.y, vec3d2.z);
                }
            }
        }
        RenderBuffers.LINES.end();
        RenderBuffers.postRender();
        event.getMatrices().pop();
    }

    public boolean checkEntity(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return playersConfig.get() || entity == mc.player && selfConfig.get();
        }
        return entity instanceof EnderPearlEntity && pearlsConfig.get()
            || entity instanceof ArrowEntity && arrowsConfig.get()
            || entity instanceof ExperienceBottleEntity && xpBottlesConfig.get();
    }

    private record TimedPosition(Vec3d pos, long time) {
    }

    public static BreadcrumbsII getInstance() {
        return INST;
    }
}
