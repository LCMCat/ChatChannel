package com.chatchannel.command;

import com.chatchannel.ChatChannelPlugin;
import com.chatchannel.config.ChatChannelConfig;
import com.chatchannel.util.MessageUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

public class ChatChannelReloadCommand implements SimpleCommand {

    private final ChatChannelPlugin plugin;
    private final ChatChannelConfig config;

    public ChatChannelReloadCommand(ChatChannelPlugin plugin, ChatChannelConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        plugin.reloadConfig();
        source.sendMessage(MessageUtil.format(config.getMessage("prefix") + "&a聊天频道配置已热重载！"));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("chat.admin.reload");
    }
}
