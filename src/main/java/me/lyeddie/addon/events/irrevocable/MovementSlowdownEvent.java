package me.lyeddie.addon.events.irrevocable;

import net.minecraft.client.player.ClientInput;

public class MovementSlowdownEvent {

    public final ClientInput input;

    public MovementSlowdownEvent(ClientInput input) {
        this.input = input;
    }

    public ClientInput getInput() {
        return input;
    }
}
