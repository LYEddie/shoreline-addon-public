package me.lyeddie.addon.events.staged;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.player.ClientInput;

public class PreKeyboardTickEvent extends Cancellable {

    private final ClientInput input;

    public PreKeyboardTickEvent(ClientInput input) {
        this.input = input;
    }

    public ClientInput getInput() {
        return input;
    }
}
