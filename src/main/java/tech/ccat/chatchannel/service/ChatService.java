package tech.ccat.chatchannel.service;

import tech.ccat.chatchannel.api.ChatEvent;
import tech.ccat.chatchannel.channel.ChannelType;
import tech.ccat.chatchannel.config.ChatChannelConfig;
import tech.ccat.chatchannel.manager.ChatLogManager;
import tech.ccat.chatchannel.manager.CooldownManager;
import tech.ccat.chatchannel.manager.PlayerChannelManager;
import tech.ccat.chatchannel.manager.PrivateMessageManager;
import tech.ccat.chatchannel.util.MessageUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class ChatService {

    private final ProxyServer server;
    private final ChatChannelConfig config;
    private final PlayerChannelManager channelManager;
    private final CooldownManager cooldownManager;
    private final PrivateMessageManager pmManager;
    private final ChatLogManager logManager;
    private final Logger logger;

    private Function<Player, Collection<Player>> partyMemberProvider = p -> Collections.emptyList();
    private Function<Player, Collection<Player>> guildMemberProvider = p -> Collections.emptyList();
    private Function<Player, Collection<Player>> officerMemberProvider = p -> Collections.emptyList();

    private Predicate<Player> isInPartyPredicate = p -> false;
    private Predicate<Player> isInGuildPredicate = p -> false;
    private Predicate<Player> isOfficerPredicate = p -> false;

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
            case PARTY -> partyMemberProvider.apply(sender);
            case GUILD -> guildMemberProvider.apply(sender);
            case OFFICER -> officerMemberProvider.apply(sender);
        };
    }

    public void setPartyMemberProvider(Function<Player, Collection<Player>> provider) {
        this.partyMemberProvider = provider != null ? provider : p -> Collections.emptyList();
    }

    public void setGuildMemberProvider(Function<Player, Collection<Player>> provider) {
        this.guildMemberProvider = provider != null ? provider : p -> Collections.emptyList();
    }

    public void setOfficerMemberProvider(Function<Player, Collection<Player>> provider) {
        this.officerMemberProvider = provider != null ? provider : p -> Collections.emptyList();
    }

    public boolean isInParty(Player player) {
        return isInPartyPredicate.test(player);
    }

    public boolean isInGuild(Player player) {
        return isInGuildPredicate.test(player);
    }

    public boolean isOfficer(Player player) {
        return player.hasPermission(config.getPermission("channel-officer"));
    }

    public void setIsInPartyPredicate(Predicate<Player> predicate) {
        this.isInPartyPredicate = predicate != null ? predicate : p -> false;
    }

    public void setIsInGuildPredicate(Predicate<Player> predicate) {
        this.isInGuildPredicate = predicate != null ? predicate : p -> false;
    }

    public void setIsOfficerPredicate(Predicate<Player> predicate) {
        this.isOfficerPredicate = predicate != null ? predicate : p -> false;
    }

    public void removePlayer(UUID uuid) {
        channelManager.removePlayer(uuid);
        cooldownManager.removePlayer(uuid);
        pmManager.removePlayer(uuid);
    }
}
