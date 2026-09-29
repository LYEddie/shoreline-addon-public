package me.lyeddie.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lyeddie.addon.util.PlayerArgumentType;
import me.lyeddie.addon.util.Helpers;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SkinGrab extends Command implements Helpers {

    public SkinGrab() {
        super("skingrab", ".");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerArgumentType.player()).executes(c -> {
            String player = PlayerArgumentType.getPlayer(c, "player");
            String skinTexture = null;
            for (PlayerListEntry playerListEntry : mc.player.networkHandler.getPlayerList()) {
                String playerName = playerListEntry.getProfile().getName();
                if (player.equals(playerName)) {
                    skinTexture = playerListEntry.getSkinTextures().textureUrl();
                    break;
                }
            }
            if (skinTexture == null) {
                error("Failed to find skin texture for " + player);
                return 0;
            }
            try {
                URL url = new URL(skinTexture);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(MeteorClient.FOLDER.toString() + "/" + player + ".png");
                } catch (Exception e) {
                    e.printStackTrace();
                    info("SkinGrab", e.getMessage());
                }
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return 0;

                if (outputStream != null) {
                    outputStream.write(inputStream.readAllBytes());
                    info(player + " skin downloaded to client folder");
                } else {
                    info("SkinGrab", "outputStream is null¿");
                }
            } catch (IOException e) {
                error("Failed to download skin texture for " + player);
            }

            return 1;
        }));
    }
}
