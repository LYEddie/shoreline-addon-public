package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.systems.friends.Friend;

public class FriendRemovedEvent {
    public final Friend friend;

    public FriendRemovedEvent(Friend friend) {
        this.friend = friend;
    }
}

