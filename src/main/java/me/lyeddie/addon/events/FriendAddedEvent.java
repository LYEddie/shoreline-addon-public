package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.systems.friends.Friend;

public class FriendAddedEvent {
    public final Friend friend;

    public FriendAddedEvent(Friend friend) {
        this.friend = friend;
    }
}

