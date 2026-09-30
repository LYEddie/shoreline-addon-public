package me.lyeddie.addon.events.staged;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.player.ClientInput;

public class PostKeyboardTickEvent extends Cancellable {

    private final ClientInput input;

    public PostKeyboardTickEvent(ClientInput input) {
        this.input = input;
    }

    public ClientInput getInput() {
        return input;
    }
}
