package me.lyeddie.addon.hud;

import me.lyeddie.addon.Shoreline;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.resources.Identifier;

public class Logo extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> widgetBorder = sgGeneral.add(new IntSetting.Builder()
        .name("widget-border").description(".")
        .min(0)
        .defaultValue(5)
        .sliderMax(64)
        .build());
    private final Setting<Double> logoScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("logo-scale").description(".")
        .min(0.0)
        .defaultValue(1.0)
        .sliderRange(0, 2.0)
        .build());
    private final Setting<Integer> scaleDiv = sgGeneral.add(new IntSetting.Builder()
        .name("scale-division").description(".")
        .min(1)
        .defaultValue(2)
        .max(5)
        .build());
    private final Setting<SideMode> side = sgGeneral.add(new EnumSetting.Builder<SideMode>()
        .name("side").description(".")
        .defaultValue(SideMode.Right)
        .build());
    private final Setting<SettingColor> sampleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("color-test").description(".")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build());

    private final Identifier logoFile = Identifier.fromNamespaceAndPath("shornselines", "icon.png");

    public static final HudElementInfo<Logo> INFO = new HudElementInfo<>(
        Shoreline.HUD, "logo", ".", Logo::new);

    public Logo() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        int width = 1080;
        int height = 1080;

        double scale = logoScale.get();
        double w = width * scale / scaleDiv.get();
        double h = height * scale / scaleDiv.get();
        double finalX = x;
        double finalW = w;

        if (side.get() == SideMode.Left) {
            finalX += w;
            finalW = -w;
        }

        setSize(Math.abs(finalW) + widgetBorder.get(), h + widgetBorder.get());
        float border = widgetBorder.get() > 0 ? (widgetBorder.get() / 2) : 0;
        renderer.texture(logoFile, border + finalX, border + y, finalW, h, sampleColor.get());
    }

    public enum SideMode {
        Right,
        Left
    }
}
