package com.chatchannel.service;

import com.chatchannel.api.ChatEvent;
import com.chatchannel.channel.ChannelType;
import com.chatchannel.config.ChatChannelConfig;
import com.chatchannel.manager.ChatLogManager;
import com.chatchannel.manager.CooldownManager;
import com.chatchannel.manager.PlayerChannelManager;
import com.chatchannel.manager.PrivateMessageManager;
import com.chatchannel.util.MessageUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.UUID;

public class ChatService {

    private final ProxyServer server;
    private final ChatChannelConfig config;
    private final PlayerChannelManager channelManager;
    private final CooldownManager cooldownManager;
    private final PrivateMessageManager pmManager;
    private final ChatLogManager logManager;
    private final Logger logger;

    public ChatService(ProxyServer server, ChatChannelConfig config,
                       PlayerChannelManager channelManager,
                       CooldownManager cooldownManager,
                       PrivateMessageManager pmManager,
                       ChatLogManager logManager,
                       Logger logger) {
        this.server = server;
        this.config = config;
        this.channelManager = channelManager;
        this.cooldownManager = cooldownManager;
        this.pmManager = pmManager;
        this.logManager = logManager;
        this.logger = logger;
    }

    public ChannelType getPlayerChannel(UUID uuid) {
        return channelManager.getChannel(uuid);
    }

    public void setPlayerChannel(UUID uuid, ChannelType channel) {
        channelManager.setChannel(uuid, channel);
    }

    public boolean hasChannelPermission(Player player, ChannelType channel) {
        return player.hasPermission(channel.getPermission());
    }

    public boolean checkCooldown(Player player) {
        if (player.hasPermission(config.getPermission("bypass-cooldown"))) {
            return true;
        }
        return !cooldownManager.isOnCooldown(player.getUniqueId());
    }

    public boolean checkSpam(Player player) {
        if (player.hasPermission(config.getPermission("bypass-spam"))) {
            return true;
        }
        return !cooldownManager.isSpamming(player.getUniqueId());
    }

    public void recordMessage(UUID uuid) {
        cooldownManager.recordMessage(uuid);
    }

    public long getRemainingCooldown(UUID uuid) {
        return cooldownManager.getRemainingCooldown(uuid);
    }

    public void sendChannelMessage(Player sender, ChannelType channel, String message) {
        String format = config.getFormat(channel.getName());
        String serverName = sender.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("未知");

        String formatted = format
                .replace("{server}", serverName)
                .replace("{player}", sender.getUsername())
                .replace("{message}", message)
                .replace("{prefix}", "")
                .replace("{guild-tag}", "");

        ChatEvent event = new ChatEvent(sender, channel, message, formatted);
        server.getEventManager().fire(event).thenAccept(firedEvent -> {
            if (firedEvent.getResult().isAllowed()) {
                cooldownManager.recordMessage(sender.getUniqueId());

                Component component = MessageUtil.colorize(firedEvent.getFormattedContent());
                Collection<Player> recipients = getChannelRecipients(sender, channel);
                for (Player recipient : recipients) {
                    recipient.sendMessage(component);
                }
                logger.info("[{}] {}: {}", channel.getName(), sender.getUsername(), message);

                if (config.isEnableChatLog()) {
                    logManager.log(channel, sender.getUsername(), message);
                }
            }
        });
    }

    public void sendPrivateMessage(Player sender, Player receiver, String message) {
        String sendFormat = config.getFormat("private-send");
        String receiveFormat = config.getFormat("private-receive");

        String sendFormatted = sendFormat
                .replace("{sender}", sender.getUsername())
                .replace("{receiver}", receiver.getUsername())
                .replace("{message}", message);

        String receiveFormatted = receiveFormat
                .replace("{sender}", sender.getUsername())
                .replace("{receiver}", receiver.getUsername())
                .replace("{message}", message);

        ChatEvent sendEvent = new ChatEvent(sender, ChannelType.ALL, message, sendFormatted);
        server.getEventManager().fire(sendEvent).thenAccept(firedEvent -> {
            if (firedEvent.getResult().isAllowed()) {
                sender.sendMessage(MessageUtil.colorize(sendFormatted));
                receiver.sendMessage(MessageUtil.colorize(receiveFormatted));

                pmManager.recordMessage(
                        sender.getUniqueId(), sender.getUsername(),
                        receiver.getUniqueId(), receiver.getUsername()
                );

                cooldownManager.recordMessage(sender.getUniqueId());

                logger.info("[PM] {} -> {}: {}", sender.getUsername(), receiver.getUsername(), message);

                if (config.isEnableChatLog()) {
                    logManager.logPrivateMessage(sender.getUsername(), receiver.getUsername(), message);
                }
            }
        });
    }

    public UUID getLastPartnerUuid(UUID uuid) {
        return pmManager.getLastPartnerUuid(uuid);
    }

    public String getLastPartnerName(UUID uuid) {
        return pmManager.getLastPartnerName(uuid);
    }

    public boolean hasLastPartner(UUID uuid) {
        return pmManager.hasLastPartner(uuid);
    }

    public boolean isPmExpired(UUID uuid) {
        return pmManager.isExpired(uuid);
    }

    private Collection<Player> getChannelRecipients(Player sender, ChannelType channel) {
        return switch (channel) {
            case ALL -> server.getAllPlayers();
            case PARTY -> getPartyMembers(sender);
            case GUILD -> getGuildMembers(sender);
            case OFFICER -> getOfficerMembers(sender);
        };
    }

    private Collection<Player> getPartyMembers(Player player) {
        return server.getAllPlayers();
    }

    private Collection<Player> getGuildMembers(Player player) {
        return server.getAllPlayers();
    }

    private Collection<Player> getOfficerMembers(Player player) {
        return server.getAllPlayers();
    }

    public void removePlayer(UUID uuid) {
        channelManager.removePlayer(uuid);
        cooldownManager.removePlayer(uuid);
        pmManager.removePlayer(uuid);
    }
}
