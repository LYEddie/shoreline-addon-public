package me.lyeddie.addon.util.literal;

import java.awt.*;

public class ColorUtil {

    public static Color interpolateColor(float value, Color start, Color end) {
        float sr = start.getRed() / 255.0f;
        float sg = start.getGreen() / 255.0f;
        float sb = start.getBlue() / 255.0f;
        float sa = start.getAlpha() / 255.0f;
        float er = end.getRed() / 255.0f;
        float eg = end.getGreen() / 255.0f;
        float eb = end.getBlue() / 255.0f;
        float ea = end.getAlpha() / 255.0f;
        return new Color(sr * value + er * (1.0f - value),
            sg * value + eg * (1.0f - value),
            sb * value + eb * (1.0f - value),
            sa * value + ea * (1.0f - value));
    }
}
