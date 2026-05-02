package com.chatchannel.command;

import com.chatchannel.config.ChatChannelConfig;
import com.chatchannel.service.ChatService;
import com.chatchannel.util.MessageUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Optional;

public class ReplyCommand implements SimpleCommand {

    private final ChatService service;
    private final ChatChannelConfig config;
    private final ProxyServer server;

    public ReplyCommand(ChatService service, ChatChannelConfig config, ProxyServer server) {
        this.service = service;
        this.config = config;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(MessageUtil.colorize(config.getMessage("player-only")));
            return;
        }

        if (!player.hasPermission(config.getPermission("command-reply"))) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("no-permission")));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("usage", "usage", "/reply <消息>")));
            return;
        }

        if (!service.hasLastPartner(player.getUniqueId())) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("msg-no-reply")));
            return;
        }

        if (service.isPmExpired(player.getUniqueId())) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("msg-reply-expired")));
            return;
        }

        String partnerName = service.getLastPartnerName(player.getUniqueId());
        Optional<Player> targetOpt = server.getPlayer(partnerName);

        if (targetOpt.isEmpty()) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("msg-player-not-found", "player", partnerName)));
            return;
        }

        Player target = targetOpt.get();
        String message = String.join(" ", args);

        if (message.isEmpty()) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("message-empty")));
            return;
        }

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

        service.sendPrivateMessage(player, target, message);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(config.getPermission("command-reply"));
    }
}
