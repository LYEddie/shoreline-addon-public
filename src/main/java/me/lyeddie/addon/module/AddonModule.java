package me.lyeddie.addon.module;

import com.mojang.brigadier.StringReader;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

// idc bout formatting
public class AddonModule extends Module {
    private final String prefix = "§7[" + "§9Shoreline" + "§7]";

    public AddonModule(Category category, String name, String description) {
        super(category, name, description);
    }

    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback && mc.level != null) {
            ChatUtils.forceNextPrefixClass(getClass());
            String msg = prefix + " §f" + name + (isActive() ? " §aon" : " §coff");
            sendMessage(Component.nullToEmpty(msg), hashCode());
        }
    }

    @Override
    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        MutableComponent mutVal = formatMsg(String.format(message, args), ChatFormatting.GRAY);
        MutableComponent last = Component.empty();
        last.append(prefix + " §7[§d" + title + "§7] ");
        last.append(mutVal);
        ((IChatHud) mc.gui.getChat()).meteor$add(last, (Config.get().deleteChatFeedback.get() ? 0 : 1));
    }

    public void sendMessage(Component text, int id) {
        ((IChatHud) mc.gui.getChat()).meteor$add(text, id);
    }

/*    public Color getClampColor(SettingColor set, int alpha) {
        return new Color(set.r, set.g, set.b, MathHelper.clamp(alpha, 0, 255));
    }*/

    public SettingColor getClampColor(SettingColor set, int alpha) {
        return new SettingColor(set.r, set.g, set.b, Mth.clamp(alpha, 0, 255));
    }

    public Color getSettingColor(SettingColor set) {
        return new Color(set.r, set.g, set.b, set.a);
    }

    public float toFloat(double db) {
        return (float) db;
    }

    public double getValueSq(double inp) {
        return inp * inp;
    }
    // :nerd_exploding_skull:
    public double getValueSq(float inp) {
        return inp * inp;
    }

    public static double round(double value, int places) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static SettingColor interpolateColor(float value, SettingColor start, SettingColor end) {
        float sr = start.r / 255.0f;
        float sg = start.g / 255.0f;
        float sb = start.b / 255.0f;
        float sa = start.a / 255.0f;
        float er = end.r / 255.0f;
        float eg = end.g / 255.0f;
        float eb = end.b / 255.0f;
        float ea = end.a / 255.0f;
        return new SettingColor(sr * value + er * (1.0f - value),
                sg * value + eg * (1.0f - value),
                sb * value + eb * (1.0f - value),
                sa * value + ea * (1.0f - value));
    }

    private MutableComponent formatMsg(String message, ChatFormatting defaultColor) {
        StringReader reader = new StringReader(message);
        MutableComponent text = Component.empty();
        Style style = Style.EMPTY.applyFormat(defaultColor);
        StringBuilder result = new StringBuilder();
        boolean formatting = false;
        while (reader.canRead()) {
            char c = reader.read();
            if (c == '(') {
                text.append(Component.literal(result.toString()).setStyle(style));
                result.setLength(0);
                result.append(c);
                formatting = true;
            } else {
                result.append(c);
                if (formatting && c == ')') {
                    switch (result.toString()) {
                        case "(default)" -> {
                            style = style.applyFormat(defaultColor);
                            result.setLength(0);
                        }
                        case "(highlight)" -> {
                            style = style.applyFormat(ChatFormatting.WHITE);
                            result.setLength(0);
                        }
                        case "(underline)" -> {
                            style = style.applyFormat(ChatFormatting.UNDERLINE);
                            result.setLength(0);
                        }
                        case "(bold)" -> {
                            style = style.applyFormat(ChatFormatting.BOLD);
                            result.setLength(0);
                        }
                    }
                    formatting = false;
                }
            }
        }
        if (!result.isEmpty()) text.append(Component.literal(result.toString()).setStyle(style));
        return text;
    }
}
