package me.lyeddie.addon.hud;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.util.BuildConfig;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class Watermark extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> wmStr = sgGeneral.add(new StringSetting.Builder()
        .name("value").description(".")
        .defaultValue("Shoreline Addon")
        .build());
    private final Setting<Info> infMode = sgGeneral.add(new EnumSetting.Builder<Info>()
        .name("info").description(".")
        .defaultValue(Info.NONE)
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

    public static final HudElementInfo<Watermark> INFO = new HudElementInfo<>(
        Shoreline.HUD, "watermark", ".", Watermark::new);

    public Watermark() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        String text = wmStr.get().isEmpty() ? "cooltechaddon" : wmStr.get();
        if (infMode.get() == Info.HASH || infMode.get() == Info.BOTH) text += "+" + BuildConfig.HASH;
        if (infMode.get() == Info.BUILD || infMode.get() == Info.BOTH) text += "-beta" + BuildConfig.BUILD_NUMBER;

        setSize(renderer.textWidth(text, true), renderer.textHeight(true));

        if (bgBool.get()) renderer.quad(x, y, getWidth(), getHeight(), bgColor.get());
        renderer.text(text, x, y, textColor.get(), true);
    }

    public enum Info {
        NONE, BUILD, HASH, BOTH
    }
}
