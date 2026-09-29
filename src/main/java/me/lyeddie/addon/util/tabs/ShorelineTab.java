package me.lyeddie.addon.util.tabs;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.screen.Screen;

public class ShorelineTab extends Tab {

    public ShorelineTab() {
        super("Shoreline");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new AddonTabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof AddonTabScreen;
    }

    private static class AddonTabScreen extends WindowTabScreen {
        private final Settings settings;

        public AddonTabScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
            settings = TabConfigs.get().settings;
            settings.onActivated();
        }

        @Override
        public void initWidgets() {
            add(theme.settings(settings)).expandX();
        }

        @Override
        public void tick() {
            super.tick();
            settings.tick(window, theme);
        }

        // config copying/pasting thing
        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(TabConfigs.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(TabConfigs.get());
        }
    }
}
