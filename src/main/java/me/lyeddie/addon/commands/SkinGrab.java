package me.lyeddie.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lyeddie.addon.util.PlayerArgumentType;
import me.lyeddie.addon.util.Helpers;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.MinecraftClient;
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
            String playerNameArg = PlayerArgumentType.getPlayer(c, "player");
            String skinTexture = null;
            for (PlayerListEntry playerListEntry : MinecraftClient.getInstance().player.networkHandler.getPlayerList()) {
                String playerName = playerListEntry.getProfile().getName();
                if (playerNameArg.equals(playerName)) {
                    skinTexture = playerListEntry.getSkinTextures().textureUrl();
                    break;
                }
            }
            if (skinTexture == null) {
                error("Failed to find skin texture for " + playerNameArg);
                return 0;
            }
            try {
                URL url = new URL(skinTexture);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(MeteorClient.FOLDER.toString() + "/" + playerNameArg + ".png");
                } catch (Exception e) {
                    e.printStackTrace();
                    info("SkinGrab", e.getMessage());
                }
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return 0;

                if (outputStream != null) {
                    outputStream.write(inputStream.readAllBytes());
                    info(playerNameArg + " skin downloaded to client folder");
                } else {
                    info("SkinGrab", "outputStream is null¿");
                }
            } catch (IOException e) {
                error("Failed to download skin texture for " + playerNameArg);
            }

            return 1;
        }));
    }
}
