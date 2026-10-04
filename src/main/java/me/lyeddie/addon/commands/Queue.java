package me.lyeddie.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lyeddie.addon.managers.Managers;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import java.util.concurrent.Executors;

public class Queue extends Command {

    private String queue = "";

    public Queue() {
        super("queue", "Displays the current 2b2t queue size.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(c -> {
            try {
                Executors.newSingleThreadExecutor().execute(() -> {
                    queue = Managers.LOOKUP.get2b2tQueueSize();
                    if (queue.isEmpty()) {
                        error("Could not fetch 2b2t queue size!");
                    } else {
                        info(queue);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 1;
        });
    }
}
