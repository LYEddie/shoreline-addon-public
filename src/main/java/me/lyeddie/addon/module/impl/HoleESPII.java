package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.managers.impl.util.Hole;
import me.lyeddie.addon.managers.impl.util.HoleType;
import me.lyeddie.addon.util.Animation;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HoleESPII extends AddonModule {
    private static HoleESPII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargets = settings.createGroup("Targets");
    private final SettingGroup sgColor = settings.createGroup("Colors");
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .description("Range to display holes")
        .defaultValue(10.0)
        .min(3.0)
        .sliderMax(25.0)
        .build());
    private final Setting<Boolean> outlineConfig = sgRender.add(new BoolSetting.Builder()
        .name("Outline")
        .description("Renders an outline around the hole")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> crossConfig = sgRender.add(new BoolSetting.Builder()
        .name("Cross")
        .description("Renders a cross on the floor of the hole")
        .defaultValue(false)
        .build());
    public final Setting<Double> heightConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Size")
        .description("Render height of holes")
        .defaultValue(1.00)
        .min(-1.0)
        .sliderMax(1.0)
        .build());
    private final Setting<Boolean> ignoreSelfConfig = sgTargets.add(new BoolSetting.Builder()
        .name("IgnoreSelf")
        .description("Ignores the hole the player is standing in")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> obsidianCheckConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Obsidian")
        .description("Displays obsidian holes")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> obsidianBedrockConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Obsidian-Bedrock")
        .description("Displays mixed obsidian and bedrock holes")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> doubleConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Double")
        .description("Displays double holes where the player can stand in the middle of two blocks to block explosion damage")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> quadConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Quad")
        .description("Displays quad holes where the player can stand in the middle of four blocks to block explosion damage")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> voidConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Void")
        .description("Displays void holes in the world")
        .defaultValue(false)
        .build());
    private final Setting<SettingColor> obsidianConfig = sgColor.add(new ColorSetting.Builder()
        .name("ObsidianColor")
        .description("The color for rendering obsidian holes")
        .defaultValue(new SettingColor(255, 0, 0, 40))
        .visible(obsidianCheckConfig::get)
        .build());
    private final Setting<SettingColor> mixedConfig = sgColor.add(new ColorSetting.Builder()
        .name("Obsidian-BedrockColor")
        .description("The color for rendering mixed holes")
        .defaultValue(new SettingColor(255, 255, 0, 40))
        .visible(obsidianBedrockConfig::get)
        .build());
    private final Setting<SettingColor> bedrockConfig = sgColor.add(new ColorSetting.Builder()
        .name("BedrockColor")
        .description("The color for rendering bedrock holes")
        .defaultValue(new SettingColor(0, 255, 0, 40))
        .build());
    private final Setting<SettingColor> voidColorConfig = sgColor.add(new ColorSetting.Builder()
        .name("VoidColor")
        .description("The color for rendering bedrock holes")
        .defaultValue(new SettingColor(255, 0, 0, 140))
        .visible(voidConfig::get)
        .build());
    public final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("Fade-Time")
        .description("Timer for the fade")
        .defaultValue(300)
        .min(0)
        .sliderMax(1000)
        .build());

    private final Map<Hole, Animation> fadeList = new HashMap<>();

    public HoleESPII() {
        super(Shoreline.MAIN, "HoleESPII", "Displays nearby blast resistant holes");
        INST = this;
    }

    @Override
    public void onDeactivate() {
        fadeList.clear();
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player == null) {
            return;
        }
        for (Hole hole : Managers.HOLE.getHoles()) {
            if (!containsPos(fadeList.keySet(), hole)) {
                Animation anim = new Animation(false, fadeTimeConfig.get());
                fadeList.put(hole, anim);
            }
        }

        for (Map.Entry<Hole, Animation> set : fadeList.entrySet()) {
            Hole hole = set.getKey();
            double dist = hole.squaredDistanceTo(mc.player);

            if (dist > getValueSq(rangeConfig.get())) {
                set.getValue().setState(false);
            } else if (ignoreSelfConfig.get() && mc.player.getBoundingBox().intersects(hole.getBoundingBox(0.5))) {
                set.getValue().setState(false);
            } else if ((hole.isDoubleX() || hole.isDoubleZ()) && !doubleConfig.get()
                || hole.isQuad() && !quadConfig.get()
                || hole.getSafety() == HoleType.VOID && !voidConfig.get()
                || hole.getSafety() == HoleType.OBSIDIAN && !obsidianCheckConfig.get()
                || hole.getSafety() == HoleType.OBSIDIAN_BEDROCK && !obsidianBedrockConfig.get()) {
                set.getValue().setState(false);
            } else {
                set.getValue().setState(containsPos(Managers.HOLE.getHoles(), hole));
            }

            if (set.getValue().getFactor() < 0.01f) {
                continue;
            }
            Color color = getHoleColor(hole.getSafety());
            int boxAlpha = (int) (color.getAlpha() * set.getValue().getFactor());
            int lineAlpha = (int) (100 * set.getValue().getFactor());

            renderHole(event, hole, getHoleColor(hole.getSafety(), boxAlpha),
                getHoleColor(hole.getSafety(), lineAlpha));
        }

    }

    private void renderHole(Render3DEvent event, Hole hole, SettingColor color1, SettingColor color2) {
        Box render;
        if (hole.getSafety() == HoleType.VOID) {
            render = new Box(hole.getPos().getX(), hole.getPos().getY(), hole.getPos().getZ(), hole.getPos().getX() + 1, hole.getPos().getY() + heightConfig.get(), hole.getPos().getZ() + 1);
        } else {
            render = hole.getBoundingBox(heightConfig.get());
        }

        event.renderer.box(render, color1, color1, ShapeMode.Sides, 0);
        if (outlineConfig.get()) {
            event.renderer.box(render, color2, color2, ShapeMode.Lines, 0);
        }
        if (crossConfig.get()) {
            event.renderer.line(render.minX, render.minY, render.minZ, render.maxX, render.minY, render.maxZ, color2);
            event.renderer.line(render.maxX, render.minY, render.minZ, render.minX, render.minY, render.maxZ, color2);
        }
    }

    private boolean containsPos(Set<Hole> set, Hole hole) {
        return set.stream().anyMatch(hole1 ->
        {
            if (hole1.isDoubleX() != hole.isDoubleX() || hole1.isDoubleZ() != hole.isDoubleZ()
                || hole1.isQuad() != hole.isQuad() || hole1.isStandard() != hole.isStandard()) {
                return false;
            }
            if (hole.getSafety() != hole1.getSafety()) {
                return false;
            }
            return hole.equals(hole1);
        });
    }

    private SettingColor getHoleColor(HoleType holeType, int alpha) {
        return switch (holeType) {
            case OBSIDIAN -> getClampColor(obsidianConfig.get(), alpha);
            case OBSIDIAN_BEDROCK -> getClampColor(mixedConfig.get(), alpha);
            case BEDROCK -> getClampColor(bedrockConfig.get(), alpha);
            case VOID -> getClampColor(voidColorConfig.get(), alpha);
        };
    }

    private Color getHoleColor(HoleType holeType) {
        return switch (holeType) {
            case OBSIDIAN -> getSettingColor(obsidianConfig.get());
            case OBSIDIAN_BEDROCK -> getSettingColor(mixedConfig.get());
            case BEDROCK -> getSettingColor(bedrockConfig.get());
            case VOID -> getSettingColor(voidColorConfig.get());
        };
    }

    public double getRange() {
        return rangeConfig.get();
    }

    public static HoleESPII getInstance() {
        return INST;
    }
}
