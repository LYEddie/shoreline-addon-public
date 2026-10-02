package me.lyeddie.addon.events.staged;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.input.Input;

public class PostKeyboardTickEvent extends Cancellable {

    private final Input input;

    public PostKeyboardTickEvent(Input input) {
        this.input = input;
    }

    public Input getInput() {
        return input;
    }
}
