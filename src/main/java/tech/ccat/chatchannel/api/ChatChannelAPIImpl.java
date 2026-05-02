package tech.ccat.chatchannel.api;

import tech.ccat.chatchannel.channel.ChannelRegistry;
import tech.ccat.chatchannel.channel.ChannelType;
import tech.ccat.chatchannel.config.ChatChannelConfig;
import tech.ccat.chatchannel.service.ChatService;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Optional;
import java.util.UUID;

public class ChatChannelAPIImpl implements ChatChannelAPI {

    private final ChatService service;
    private final ChatChannelConfig config;
    private final ProxyServer server;
    private final ChannelRegistry registry;

    public ChatChannelAPIImpl(ChatService service, ChatChannelConfig config,
                              ProxyServer server, ChannelRegistry registry) {
        this.service = service;
        this.config = config;
        this.server = server;
        this.registry = registry;
    }

    @Override
    public void sendChannelMessage(UUID senderUuid, ChannelType channel, String message) {
        Optional<Player> senderOpt = server.getPlayer(senderUuid);
        if (senderOpt.isEmpty()) return;
        service.sendChannelMessage(senderOpt.get(), channel, message);
    }

    @Override
    public void sendPrivateMessage(UUID senderUuid, UUID receiverUuid, String message) {
        Optional<Player> senderOpt = server.getPlayer(senderUuid);
        Optional<Player> receiverOpt = server.getPlayer(receiverUuid);
        if (senderOpt.isEmpty() || receiverOpt.isEmpty()) return;
        service.sendPrivateMessage(senderOpt.get(), receiverOpt.get(), message);
    }

    @Override
    public ChannelType getPlayerChannel(UUID uuid) {
        return service.getPlayerChannel(uuid);
    }

    @Override
    public void setPlayerChannel(UUID uuid, ChannelType channel) {
        service.setPlayerChannel(uuid, channel);
    }

    @Override
    public void registerChannel(ChannelType channel) {
        registry.register(channel);
    }

    @Override
    public boolean hasChannelPermission(UUID uuid, ChannelType channel) {
        Optional<Player> playerOpt = server.getPlayer(uuid);
        if (playerOpt.isEmpty()) return false;
        return service.hasChannelPermission(playerOpt.get(), channel);
    }

    @Override
    public String getFormat(ChannelType channel) {
        return config.getFormat(channel.getName());
    }

    @Override
    public void setFormat(ChannelType channel, String format) {
        config.setFormatOverride(channel.getName(), format);
    }
}
