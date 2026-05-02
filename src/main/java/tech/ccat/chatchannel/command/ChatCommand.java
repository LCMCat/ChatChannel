package tech.ccat.chatchannel.command;

import tech.ccat.chatchannel.channel.ChannelType;
import tech.ccat.chatchannel.config.ChatChannelConfig;
import tech.ccat.chatchannel.service.ChatService;
import tech.ccat.chatchannel.util.MessageUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ChatCommand implements SimpleCommand {

    private final ChatService service;
    private final ChatChannelConfig config;

    public ChatCommand(ChatService service, ChatChannelConfig config) {
        this.service = service;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(MessageUtil.colorize(config.getMessage("player-only")));
            return;
        }

        if (!player.hasPermission(config.getPermission("command-chat"))) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("no-permission")));
            return;
        }

        if (args.length == 0) {
            ChannelType current = service.getPlayerChannel(player.getUniqueId());
            String channelDisplay = current.getDisplayName();
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("channel-current", "channel", channelDisplay)));
            return;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("help")) {
            sendHelp(player);
            return;
        }

        ChannelType targetChannel = ChannelType.fromString(subCommand);
        if (targetChannel == null) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("channel-invalid")));
            return;
        }

        if (!service.hasChannelPermission(player, targetChannel)) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("channel-no-permission", "channel", targetChannel.getDisplayName())));
            return;
        }

        if (!validateChannelAccess(player, targetChannel)) {
            return;
        }

        service.setPlayerChannel(player.getUniqueId(), targetChannel);
        player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("channel-switched", "channel", targetChannel.getDisplayName())));
    }

    private boolean validateChannelAccess(Player player, ChannelType channel) {
        switch (channel) {
            case PARTY -> {
                if (!service.isInParty(player)) {
                    player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("party-not-in-party")));
                    return false;
                }
            }
            case GUILD -> {
                if (!service.isInGuild(player)) {
                    player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("guild-not-in-guild")));
                    return false;
                }
            }
            case OFFICER -> {
                if (!service.isOfficer(player)) {
                    player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("guild-no-officer")));
                    return false;
                }
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtil.colorize(config.getMessage("help-header")));
        player.sendMessage(MessageUtil.clickable(config.getMessage("help-chat"), null, "/chat "));
        player.sendMessage(MessageUtil.clickable(config.getMessage("help-achat"), null, "/achat "));
        player.sendMessage(MessageUtil.clickable(config.getMessage("help-pchat"), null, "/pchat "));
        player.sendMessage(MessageUtil.clickable(config.getMessage("help-gchat"), null, "/gchat "));
        player.sendMessage(MessageUtil.clickable(config.getMessage("help-ochat"), null, "/ochat "));
        player.sendMessage(MessageUtil.clickable(config.getMessage("help-msg"), null, "/msg "));
        player.sendMessage(MessageUtil.clickable(config.getMessage("help-reply"), null, "/reply "));
        player.sendMessage(MessageUtil.colorize(config.getMessage("help-footer")));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return ChannelType.getAllNames().stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
