package me.lyeddie.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lyeddie.addon.util.PlayerArgumentType;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Helpers;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class LookUp extends Command implements Helpers {

    public LookUp() {
        super("lookup", ".");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            info("no args");
            return 1;
        });
        builder.then(argument("player", PlayerArgumentType.player()).executes(c -> {
            String playerName = PlayerArgumentType.getPlayer(c, "player");
            String[] inp = c.getInput().split(" ");
            info("LookUp", "Checking history of \"" + inp[inp.length - 1] + "\""); // ugh

            try {
                Executors.newSingleThreadExecutor().execute(() -> {
                    UUID uuid = Managers.LOOKUP.getUUIDFromName(playerName);
                    if (uuid == null) {
                        error("Could not find player UUID!");
                        return;
                    }
                    Map<String, String> nameHistory = Managers.LOOKUP.getNameHistoryFromUUID(uuid);
                    if (nameHistory == null) {
                        error("Could not find player name history!");
                        return;
                    }
                    ArrayList<String> nameHistoryList = new ArrayList<>();
                    for (Map.Entry<String, String> entry : nameHistory.entrySet()) {
                        nameHistoryList.add(entry.getValue() + " - " + entry.getKey().substring(0, 10));
                    }
                    if (nameHistoryList.isEmpty()) {
                        error("No player name history!");
                        return;
                    }
                    info("§7History: §f" + String.join(", ", nameHistoryList));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 1;
        }));
    }
}
