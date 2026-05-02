package tech.ccat.chatchannel.command;

import tech.ccat.chatchannel.channel.ChannelType;
import tech.ccat.chatchannel.config.ChatChannelConfig;
import tech.ccat.chatchannel.service.ChatService;
import tech.ccat.chatchannel.util.MessageUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

public class ChannelMessageCommand implements SimpleCommand {

    private final ChatService service;
    private final ChatChannelConfig config;
    private final ChannelType channel;

    public ChannelMessageCommand(ChatService service, ChatChannelConfig config, ChannelType channel) {
        this.service = service;
        this.config = config;
        this.channel = channel;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(MessageUtil.colorize(config.getMessage("player-only")));
            return;
        }

        if (!service.hasChannelPermission(player, channel)) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("channel-no-permission", "channel", channel.getDisplayName())));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("message-empty")));
            return;
        }

        if (!validateChannelAccess(player)) {
            return;
        }

        String message = String.join(" ", args);

        if (!service.checkCooldown(player)) {
            long remaining = service.getRemainingCooldown(player.getUniqueId());
            double seconds = remaining / 1000.0;
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("cooldown", "time", String.format("%.1f", seconds))));
            return;
        }

        if (!service.checkSpam(player)) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("spam-warning")));
            return;
        }

        service.sendChannelMessage(player, channel, message);
    }

    private boolean validateChannelAccess(Player player) {
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

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
