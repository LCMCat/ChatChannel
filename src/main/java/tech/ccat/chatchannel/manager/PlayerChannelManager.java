package tech.ccat.chatchannel.manager;

import tech.ccat.chatchannel.channel.ChannelType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerChannelManager {

    private final Map<UUID, ChannelType> playerChannels = new ConcurrentHashMap<>();
    private final String defaultChannelName;

    public PlayerChannelManager(String defaultChannelName) {
        this.defaultChannelName = defaultChannelName;
    }

    public ChannelType getChannel(UUID uuid) {
        return playerChannels.getOrDefault(uuid, ChannelType.fromString(defaultChannelName));
    }

    public void setChannel(UUID uuid, ChannelType channel) {
        playerChannels.put(uuid, channel);
    }

    public void removePlayer(UUID uuid) {
        playerChannels.remove(uuid);
    }

    public void clear() {
        playerChannels.clear();
    }

    public boolean hasChannel(UUID uuid) {
        return playerChannels.containsKey(uuid);
    }
}
