package tech.ccat.chatchannel.command;

import tech.ccat.chatchannel.config.ChatChannelConfig;
import tech.ccat.chatchannel.service.ChatService;
import tech.ccat.chatchannel.util.MessageUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MsgCommand implements SimpleCommand {

    private final ChatService service;
    private final ChatChannelConfig config;
    private final ProxyServer server;

    public MsgCommand(ChatService service, ChatChannelConfig config, ProxyServer server) {
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

        if (!player.hasPermission(config.getPermission("command-msg"))) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("no-permission")));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("usage", "usage", "/msg <玩家> <消息>")));
            return;
        }

        String targetName = args[0];
        Optional<Player> targetOpt = server.getPlayer(targetName);

        if (targetOpt.isEmpty()) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("msg-player-not-found", "player", targetName)));
            return;
        }

        Player target = targetOpt.get();

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(MessageUtil.format(config.getMessage("prefix") + config.getMessage("msg-self")));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

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
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase();
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
