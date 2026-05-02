package tech.ccat.chatchannel.api;

import tech.ccat.chatchannel.channel.ChannelType;
import com.velocitypowered.api.proxy.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ChatChannelAPI {

    void sendChannelMessage(UUID senderUuid, ChannelType channel, String message);

    void sendPrivateMessage(UUID senderUuid, UUID receiverUuid, String message);

    ChannelType getPlayerChannel(UUID uuid);

    void setPlayerChannel(UUID uuid, ChannelType channel);

    void registerChannel(ChannelType channel);

    boolean hasChannelPermission(UUID uuid, ChannelType channel);

    String getFormat(ChannelType channel);

    void setFormat(ChannelType channel, String format);

    void setPartyMemberProvider(Function<Player, Collection<Player>> provider);

    void setGuildMemberProvider(Function<Player, Collection<Player>> provider);

    void setOfficerMemberProvider(Function<Player, Collection<Player>> provider);

    void setIsInPartyPredicate(Predicate<Player> predicate);

    void setIsInGuildPredicate(Predicate<Player> predicate);

    void setIsOfficerPredicate(Predicate<Player> predicate);
}
