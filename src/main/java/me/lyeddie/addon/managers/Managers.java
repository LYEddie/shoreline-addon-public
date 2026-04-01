package me.lyeddie.addon.managers;

import me.lyeddie.addon.managers.impl.*;
import me.lyeddie.addon.tabs.TabEvents;
import me.lyeddie.addon.util.BuildConfig;
import meteordevelopment.meteorclient.utils.render.prompts.OkPrompt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class Managers {
    private static boolean initialized;
    private final File oneTimeFile = new File(FabricLoader.getInstance().getGameDir().toFile(), "SHORELINE_ADDON.NOT_FOR_SALE");
    private final boolean isLangMexicanOalgo = Locale.getDefault().getLanguage().equalsIgnoreCase("es");
    private static final Managers INSTANCE = new Managers();

    public static AntiCheatManager ANTICHEAT;
    public static BlockManager BLOCK;
    public static HitboxManager HITBOX;
    public static HoleManager HOLE;
    public static InteractionManager INTERACT;
    public static InventoryManager INVENTORY;
    public static LookupManager LOOKUP;
    public static MovementManager MOVEMENT;
    public static NetworkManager NETWORK;
    public static PearlManager PEARL;
    public static PositionManager POSITION;
    public static RotationManager ROTATION;
    public static TickManager TICK;
    public static TotemManager TOTEM;

    public static TabEvents TAB_EVENTS; // ¿

    public static void init() {
        if (!isInitialized()) {
            ANTICHEAT = new AntiCheatManager();
            BLOCK = new BlockManager();
            HITBOX = new HitboxManager();
            HOLE = new HoleManager();
            INTERACT = new InteractionManager();
            INVENTORY = new InventoryManager();
            LOOKUP = new LookupManager();
            MOVEMENT = new MovementManager();
            NETWORK = new NetworkManager();
            PEARL = new PearlManager();
            POSITION = new PositionManager();
            ROTATION = new RotationManager();
            TICK = new TickManager();
            TOTEM = new TotemManager();

            TAB_EVENTS = new TabEvents();

            initialized = true;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static Managers INST() {
        return INSTANCE;
    }

    public void info(TitleScreen scr) {
        if (!oneTimeFile.exists()) {
            OkPrompt.create()
                .title("Shoreline Addon-%s %s".formatted(BuildConfig.HASH, BuildConfig.BUILD_TIME))
                .message(isLangMexicanOalgo ? "Hola, este addon es un paste feo del \"Shoreline Client\" (sitio web: shoreline.dev)" : "Hello, this addon is a horrible paste from \"Shoreline Client\" (website: shoreline.dev)")
                .message(isLangMexicanOalgo ? "Si pagaste por esto te estafaron." : "If you paid for this, you have been scammed.")
                .onOk(() -> {
                    scr.close();
                    try {
                        final boolean did = oneTimeFile.createNewFile();
                    } catch (IOException e) {
                        log(e);
                    }
                })
                .dontShowAgainCheckboxVisible(false)
                .show();
        }
    }

    public void log(Exception ex) {
        System.out.println("<?> " + ex.getMessage());
    }

    public void log(String str) {
        System.out.println("<?> " + str);
    }
}
