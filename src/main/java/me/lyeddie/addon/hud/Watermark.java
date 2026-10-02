package me.lyeddie.addon.hud;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.util.BuildConfig;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;

public class Watermark extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> wmStr = sgGeneral.add(new StringSetting.Builder()
        .name("value").description(".")
        .defaultValue("[Shoreline Addon]")
        .build());
    private final Setting<Type> displayType = sgGeneral.add(new EnumSetting.Builder<Type>()
        .name("display-type").description(".")
        .defaultValue(Type.CLASSIC)
        .build());
    private final Setting<Boolean> bgBool = sgGeneral.add(new BoolSetting.Builder()
        .name("background").description(".")
        .defaultValue(false)
        .build());
    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color").description(".")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build());
    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color").description(".")
        .defaultValue(new SettingColor(0, 0, 0, 64))
        .visible(bgBool::get)
        .build());

    private final Identifier id = Identifier.fromNamespaceAndPath("shornselines", "icon.png");

    public static final HudElementInfo<Watermark> INFO = new HudElementInfo<>(
        Shoreline.HUD, "watermark", "cool non-pasted ai slop watermark viewer, yea", Watermark::new);

    public Watermark() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        String text = wmStr.get().isEmpty() ? BuildConfig.BASE_NAME : wmStr.get();

        switch (displayType.get()) {
            case CLASSIC -> text += " %s (%s-%s-%s)".formatted(BuildConfig.VERSION, BuildConfig.BUILD_IDENTIFIER, BuildConfig.BUILD_NUMBER, BuildConfig.HASH);
            case MIO_LIKE -> text += " v%s-%s %s".formatted(BuildConfig.VERSION.substring(0, 1), BuildConfig.BUILD_IDENTIFIER, BuildConfig.BUILD_TIME);
            case FUTURE_LIKE -> text += " v%s-mc%s-%s+%s.%s".formatted(BuildConfig.VERSION, SharedConstants.getCurrentVersion().name(), BuildConfig.BUILD_IDENTIFIER, BuildConfig.BUILD_NUMBER, BuildConfig.HASH);
        }

        String last = "    %s%s".formatted(Hud.get().hasCustomFont() ? "  " : "", text);
        setSize(renderer.textWidth(last, true), renderer.textHeight(true));

        if (bgBool.get()) renderer.quad(x, y, getWidth(), getHeight(), bgColor.get());
        renderer.text(last, x, y, textColor.get(), true);
        renderer.post(() -> handlePost(renderer));
    }

    private void handlePost(HudRenderer renderer) {
        renderer.texture(id, x, y -3, getHeight() + 3, getHeight() + 3, Color.WHITE);
    }

    public enum Type {
        CLASSIC, MIO_LIKE, FUTURE_LIKE
    }
}
