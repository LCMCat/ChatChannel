package com.chatchannel.listener;

import com.chatchannel.service.ChatService;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;

import java.util.UUID;

public class PlayerListener {

    private final ChatService service;

    public PlayerListener(ChatService service) {
        this.service = service;
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        service.removePlayer(uuid);
    }
}
