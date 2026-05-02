package tech.ccat.chatchannel;

import tech.ccat.chatchannel.api.ChatChannelAPI;
import tech.ccat.chatchannel.api.ChatChannelAPIImpl;
import tech.ccat.chatchannel.channel.ChannelRegistry;
import tech.ccat.chatchannel.channel.ChannelType;
import tech.ccat.chatchannel.command.ChannelMessageCommand;
import tech.ccat.chatchannel.command.ChatChannelReloadCommand;
import tech.ccat.chatchannel.command.ChatCommand;
import tech.ccat.chatchannel.command.MsgCommand;
import tech.ccat.chatchannel.command.ReplyCommand;
import tech.ccat.chatchannel.config.ChatChannelConfig;
import tech.ccat.chatchannel.listener.ChatListener;
import tech.ccat.chatchannel.listener.PlayerListener;
import tech.ccat.chatchannel.manager.ChatLogManager;
import tech.ccat.chatchannel.manager.CooldownManager;
import tech.ccat.chatchannel.manager.PlayerChannelManager;
import tech.ccat.chatchannel.manager.PrivateMessageManager;
import tech.ccat.chatchannel.service.ChatService;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "chatchannel", name = "ChatChannel", version = "1.0.0",
        description = "Hypixel-style Chat Channel System for Velocity", authors = {"Cation"})
public class ChatChannelPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ChatChannelConfig config;
    private PlayerChannelManager channelManager;
    private CooldownManager cooldownManager;
    private PrivateMessageManager pmManager;
    private ChatLogManager logManager;
    private ChatService chatService;
    private ChatChannelAPI api;
    private ChannelRegistry registry;

    @Inject
    public ChatChannelPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        config = new ChatChannelConfig(dataDirectory, logger);
        config.load();

        registry = new ChannelRegistry();

        channelManager = new PlayerChannelManager(config.getDefaultChannel());
        cooldownManager = new CooldownManager(
                config.getChatCooldownMs(),
                config.getSpamThreshold(),
                config.getSpamTimeWindowMs()
        );
        pmManager = new PrivateMessageManager(config.getPrivateMessageExpiryMinutes());
        logManager = new ChatLogManager(dataDirectory, logger);

        chatService = new ChatService(server, config, channelManager, cooldownManager, pmManager, logManager, logger);

        api = new ChatChannelAPIImpl(chatService, config, server, registry);

        registerCommands();

        server.getEventManager().register(this, new PlayerListener(chatService));
        server.getEventManager().register(this, PlayerChatEvent.class, new ChatListener(chatService, config));

        if (config.isEnableChatLog()) {
            logManager.start();
        }

        logger.info("聊天频道系统已启动！");
        logger.info("API 已就绪 - 通过 ChatChannelPlugin.getAPI() 获取");
    }

    private void registerCommands() {
        ChatCommand chatCommand = new ChatCommand(chatService, config);
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("chat").aliases("ch").build(),
                chatCommand
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("achat").aliases("ac").build(),
                new ChannelMessageCommand(chatService, config, ChannelType.ALL)
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("pchat").aliases("pc").build(),
                new ChannelMessageCommand(chatService, config, ChannelType.PARTY)
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("gchat").aliases("gc").build(),
                new ChannelMessageCommand(chatService, config, ChannelType.GUILD)
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("ochat").aliases("oc").build(),
                new ChannelMessageCommand(chatService, config, ChannelType.OFFICER)
        );

        MsgCommand msgCommand = new MsgCommand(chatService, config, server);
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("msg").aliases("tell", "w").build(),
                msgCommand
        );

        ReplyCommand replyCommand = new ReplyCommand(chatService, config, server);
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("reply").aliases("r").build(),
                replyCommand
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("chatreload").build(),
                new ChatChannelReloadCommand(this, config)
        );
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (logManager != null) {
            logManager.stop();
        }
        if (channelManager != null) {
            channelManager.clear();
        }
        if (cooldownManager != null) {
            cooldownManager.clear();
        }
        if (pmManager != null) {
            pmManager.clear();
        }
        logger.info("聊天频道系统已关闭。");
    }

    public ChatChannelAPI getAPI() {
        return api;
    }

    public static ChatChannelAPI getAPI(ProxyServer server) {
        return server.getPluginManager().getPlugin("chatchannel")
                .flatMap(container -> container.getInstance().map(instance -> ((ChatChannelPlugin) instance).getAPI()))
                .orElse(null);
    }

    public ChatChannelConfig getConfig() {
        return config;
    }

    public ChatService getChatService() {
        return chatService;
    }

    public ChannelRegistry getRegistry() {
        return registry;
    }

    public void reloadConfig() {
        config.reload();
        logger.info("聊天频道配置已热重载！");
    }
}
