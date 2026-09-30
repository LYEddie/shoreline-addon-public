package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.List;

public class Renders2D implements Globals {
    private static final double DEFAULT_SCALE = 2.0;

    private final List<Sign> signs = new ArrayList<>();
    private final List<Sign> renderSigns = new ArrayList<>();

    public Renders2D() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void renderSign(String text, Vec3 pos, int color) {
        renderSign(text, pos, color, DEFAULT_SCALE);
    }

    public void renderSign(String text, Vec3 pos, int color, double scale) {
        if (text == null || pos == null) return;

        signs.add(new Sign(
                text,
                new Vector3d(pos.x, pos.y, pos.z),
                parseColor(color),
                scale
        ));
    }

    public void renderSign(String text, Vec3 pos, Color color) {
        renderSign(text, pos, color, DEFAULT_SCALE);
    }

    public void renderSign(String text, Vec3 pos, Color color, double scale) {
        if (text == null || pos == null || color == null) return;

        signs.add(new Sign(
                text,
                new Vector3d(pos.x, pos.y, pos.z),
                color,
                scale
        ));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.level == null) {
            signs.clear();
            return;
        }

        if (signs.isEmpty()) return;

        renderSigns.clear();
        renderSigns.addAll(signs);
        signs.clear();

        for (Sign sign : renderSigns) {
            if (!NametagUtils.to2D(sign.pos, sign.scale)) continue;

            NametagUtils.begin(sign.pos, event.graphics);
            TextRenderer.get().begin(event.graphics, 1, false, true);

            double width = TextRenderer.get().getWidth(sign.text) / 2.0;
            TextRenderer.get().render(sign.text, -width, 0, sign.color, true);

            TextRenderer.get().end();
            NametagUtils.end(event.graphics);
        }

        renderSigns.clear();
    }

    private Color parseColor(int color) {
        if (color == -1) return Color.WHITE;

        int a = color >>> 24;
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;

        if (a == 0) a = 255;

        return new Color(r, g, b, a);
    }

    private record Sign(String text, Vector3d pos, Color color, double scale) {}
}
