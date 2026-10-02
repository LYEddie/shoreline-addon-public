package me.lyeddie.addon;

import com.mojang.logging.LogUtils;
import me.lyeddie.addon.commands.LookUp;
import me.lyeddie.addon.commands.Queue;
import me.lyeddie.addon.commands.SkinGrab;
import me.lyeddie.addon.commands.Stats;
import me.lyeddie.addon.hud.Logo;
import me.lyeddie.addon.hud.Watermark;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.impl.*;
import me.lyeddie.addon.util.tabs.ShorelineTab;
import me.lyeddie.addon.util.BuildConfig;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

public class Shoreline extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static Category MAIN = new Category("Shoreline", () -> Items.HEART_OF_THE_SEA.getDefaultInstance());
    public static HudGroup HUD = new HudGroup("Shoreline");

    @Override
    public void onInitialize() {
        LOG.info("initializing shornselines addon!! [%s]".formatted(BuildConfig.HASH));

        Managers.init();
        registerModules(Modules.get());
        registerWidgets(Hud.get());
        registerCommands();
        Tabs.add(new ShorelineTab());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(MAIN);
    }

    @Override
    public String getPackage() {
        return "me.lyeddie.addon";
    }

    @Override
    public String getWebsite() {
        return "https://vicoredevelopment.com/";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("LYEddie", "shoreline-addon-public");
    }

    private void registerModules(Modules mods) {
        mods.add(new AirPlaceII());
        mods.add(new AntiInteract());
        mods.add(new AntiLevitation());
        mods.add(new Aura());
        mods.add(new AutoArmorII());
        mods.add(new CrawlTrap());
        mods.add(new AutoCrystal());
        mods.add(new AutoMine());
        mods.add(new AutoTool());
        mods.add(new AutoTotem());
        mods.add(new AutoTrapII());
        mods.add(new AutoWeb());
        mods.add(new AutoXP());
        mods.add(new Avoid());
        mods.add(new BasePlace());
        mods.add(new BreadcrumbsII());
        mods.add(new BreakHighlight());
        mods.add(new ChestSwapII());
        mods.add(new ChorusControl());
        mods.add(new CriticalsII());
        mods.add(new Disabler());
        mods.add(new FastLatency());
        mods.add(new FastPlaceII());
        mods.add(new HoleESPII());
        mods.add(new HoleFill());
        mods.add(new KeepSprint());
        mods.add(new KillEffects());
        mods.add(new MiddleClickII());
        mods.add(new NoPacketKick());
        mods.add(new NoRotateII());
        mods.add(new NoSlowDown());
        mods.add(new NoSoundLag());
        mods.add(new NoWeather());
        mods.add(new PacketLogger());
        mods.add(new PhaseII());
        mods.add(new Replenish());
        mods.add(new ScaffoldII());
        mods.add(new SelfTrapII());
        mods.add(new Shulkerception());
        mods.add(new SpeedII());
        mods.add(new SpeedMineII());
        mods.add(new SprintII());
        mods.add(new SurroundII());
        mods.add(new TimerII());
        mods.add(new VelocityII());
    }

    private void registerWidgets(Hud hud) {
        hud.register(Logo.INFO);
        hud.register(Watermark.INFO);
    }

    private void registerCommands() {
        reg(new LookUp());
        reg(new Queue());
        reg(new SkinGrab());
        reg(new Stats());
    }

    private void reg(Command cc) {
        Commands.add(cc);
    }
}
