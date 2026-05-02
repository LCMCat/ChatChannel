package tech.ccat.chatchannel.api;

import tech.ccat.chatchannel.channel.ChannelType;

import java.util.UUID;

public interface ChatChannelAPI {

    void sendChannelMessage(UUID senderUuid, ChannelType channel, String message);

    void sendPrivateMessage(UUID senderUuid, UUID receiverUuid, String message);

    ChannelType getPlayerChannel(UUID uuid);

    void setPlayerChannel(UUID uuid, ChannelType channel);

    void registerChannel(ChannelType channel);

    boolean hasChannelPermission(UUID uuid, ChannelType channel);

    String getFormat(ChannelType channel);

    void setFormat(ChannelType channel, String format);
}
