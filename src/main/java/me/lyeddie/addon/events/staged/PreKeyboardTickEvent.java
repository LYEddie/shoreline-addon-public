package me.lyeddie.addon.events.staged;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.client.input.Input;

public class PreKeyboardTickEvent extends Cancellable {

    private final Input input;

    public PreKeyboardTickEvent(Input input) {
        this.input = input;
    }

    public Input getInput() {
        return input;
    }
}
