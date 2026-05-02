package com.chatchannel.listener;

import com.chatchannel.channel.ChannelType;
import com.chatchannel.config.ChatChannelConfig;
import com.chatchannel.service.ChatService;
import com.chatchannel.util.MessageUtil;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;

public class ChatListener implements EventHandler<PlayerChatEvent> {

    private final ChatService service;
    private final ChatChannelConfig config;

    public ChatListener(ChatService service, ChatChannelConfig config) {
        this.service = service;
        this.config = config;
    }

    @Override
    public void execute(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        if (message.isEmpty()) {
            return;
        }

        ChannelType currentChannel = service.getPlayerChannel(player.getUniqueId());

        if (!service.hasChannelPermission(player, currentChannel)) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("channel-no-permission", "channel", currentChannel.getDisplayName())));
            event.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }

        if (!validateChannelAccess(player, currentChannel)) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }

        if (!service.checkCooldown(player)) {
            long remaining = service.getRemainingCooldown(player.getUniqueId());
            double seconds = remaining / 1000.0;
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("cooldown", "time", String.format("%.1f", seconds))));
            event.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }

        if (!service.checkSpam(player)) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("spam-warning")));
            event.setResult(PlayerChatEvent.ChatResult.denied());
            return;
        }

        event.setResult(PlayerChatEvent.ChatResult.denied());

        service.sendChannelMessage(player, currentChannel, message);
    }

    private boolean validateChannelAccess(Player player, ChannelType channel) {
        switch (channel) {
            case PARTY -> {
                if (!isInParty(player)) {
                    player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("party-not-in-party")));
                    return false;
                }
            }
            case GUILD -> {
                if (!isInGuild(player)) {
                    player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("guild-not-in-guild")));
                    return false;
                }
            }
            case OFFICER -> {
                if (!isInGuild(player)) {
                    player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("guild-not-in-guild")));
                    return false;
                }
                if (!isOfficer(player)) {
                    player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("guild-no-officer")));
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInParty(Player player) {
        return true;
    }

    private boolean isInGuild(Player player) {
        return true;
    }

    private boolean isOfficer(Player player) {
        return true;
    }
}
