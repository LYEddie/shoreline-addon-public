package me.lyeddie.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lyeddie.addon.api.PlayerArgumentType;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Helpers;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import java.util.Map;
import java.util.concurrent.Executors;

public class Stats extends Command implements Helpers {

    public Stats() {
        super("2bstats", ".");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerArgumentType.player()).executes(c -> {
            String playerName = PlayerArgumentType.getPlayer(c, "player");

            String[] inp = c.getInput().split(" ");
            info("Stats", "Checking stats of \"" + inp[inp.length - 1] + "\"");

            try {
                Executors.newSingleThreadExecutor().execute(() -> {
                    Map<String, String> stats = Managers.LOOKUP.getPlayerStats2b2t(playerName);
                    if (stats == null) {
                        error("Could not find player 2b2t stats!");
                        return;
                    }
                    int id = -9957204;
                    info(playerName + "'s Stats", id--);
                    for (Map.Entry<String, String> entry : stats.entrySet()) {
                        info("§7" + entry.getKey() + ": §f" + entry.getValue(), id--);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 1;
        }));
    }
}
