package tech.ccat.chatchannel.channel;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelRegistry {

    private final Map<String, ChannelType> channels = new ConcurrentHashMap<>();

    public ChannelRegistry() {
        for (ChannelType type : ChannelType.values()) {
            register(type);
        }
    }

    public void register(ChannelType type) {
        channels.put(type.getName().toLowerCase(), type);
        channels.put(type.getShorthand().toLowerCase(), type);
    }

    public ChannelType getChannel(String name) {
        if (name == null) return null;
        return channels.get(name.toLowerCase());
    }

    public boolean isRegistered(String name) {
        return channels.containsKey(name.toLowerCase());
    }

    public Map<String, ChannelType> getAllChannels() {
        return Collections.unmodifiableMap(channels);
    }
}
