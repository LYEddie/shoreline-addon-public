package me.lyeddie.addon.events.irrevocable;

import net.minecraft.client.input.Input;

public class MovementSlowdownEvent {

    public final Input input;

    public MovementSlowdownEvent(Input input) {
        this.input = input;
    }

    public Input getInput() {
        return input;
    }
}
