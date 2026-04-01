package me.lyeddie.addon.module;

import com.mojang.brigadier.StringReader;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import java.awt.*;

// idc bout formatting
public class AddonModule extends Module {
    private final String prefix = "§7[" + "§9Shoreline" + "§7]";

    public AddonModule(Category category, String name, String description) {
        super(category, name, description);
    }

    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback && mc.world != null) {
            ChatUtils.forceNextPrefixClass(getClass());
            String msg = prefix + " §f" + name + (isActive() ? " §aon" : " §coff");
            sendMessage(Text.of(msg), hashCode());
        }
    }

    @Override
    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        MutableText mutVal = formatMsg(String.format(message, args), Formatting.GRAY);
        MutableText last = Text.empty();
        last.append(prefix + " §7[§d" + title + "§7] ");
        last.append(mutVal);
        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(last, (Config.get().deleteChatFeedback.get() ? 0 : 1));
    }

    public void sendMessage(Text text, int id) {
        ((IChatHud) mc.inGameHud.getChatHud()).meteor$add(text, id);
    }

    public Color getClampColor(SettingColor set, int alpha) {
        return new Color(set.r, set.g, set.b, MathHelper.clamp(alpha, 0, 255));
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

    private MutableText formatMsg(String message, Formatting defaultColor) {
        StringReader reader = new StringReader(message);
        MutableText text = Text.empty();
        Style style = Style.EMPTY.withFormatting(defaultColor);
        StringBuilder result = new StringBuilder();
        boolean formatting = false;
        while (reader.canRead()) {
            char c = reader.read();
            if (c == '(') {
                text.append(Text.literal(result.toString()).setStyle(style));
                result.setLength(0);
                result.append(c);
                formatting = true;
            } else {
                result.append(c);
                if (formatting && c == ')') {
                    switch (result.toString()) {
                        case "(default)" -> {
                            style = style.withFormatting(defaultColor);
                            result.setLength(0);
                        }
                        case "(highlight)" -> {
                            style = style.withFormatting(Formatting.WHITE);
                            result.setLength(0);
                        }
                        case "(underline)" -> {
                            style = style.withFormatting(Formatting.UNDERLINE);
                            result.setLength(0);
                        }
                        case "(bold)" -> {
                            style = style.withFormatting(Formatting.BOLD);
                            result.setLength(0);
                        }
                    }
                    formatting = false;
                }
            }
        }
        if (!result.isEmpty()) text.append(Text.literal(result.toString()).setStyle(style));
        return text;
    }
}
