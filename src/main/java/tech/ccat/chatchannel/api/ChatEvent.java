package tech.ccat.chatchannel.api;

import tech.ccat.chatchannel.channel.ChannelType;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

public class ChatEvent implements ResultedEvent<ChatEvent.ChatResult> {

    private final Player sender;
    private final ChannelType channel;
    private final String rawMessage;
    private String formattedContent;
    private ChatResult result = ChatResult.allowed();

    public ChatEvent(Player sender, ChannelType channel, String rawMessage, String formattedContent) {
        this.sender = sender;
        this.channel = channel;
        this.rawMessage = rawMessage;
        this.formattedContent = formattedContent;
    }

    public Player getSender() {
        return sender;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public String getFormattedContent() {
        return formattedContent;
    }

    public void setFormattedContent(String formattedContent) {
        this.formattedContent = formattedContent;
    }

    @Override
    public @NotNull ChatResult getResult() {
        return result;
    }

    @Override
    public void setResult(@NotNull ChatResult result) {
        this.result = result;
    }

    public static class ChatResult implements ResultedEvent.Result {

        private static final ChatResult ALLOWED = new ChatResult(true, null);
        private static final ChatResult DENIED = new ChatResult(false, null);

        private final boolean allowed;
        private final String reason;

        private ChatResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public static ChatResult allowed() {
            return ALLOWED;
        }

        public static ChatResult denied(String reason) {
            return new ChatResult(false, reason);
        }

        @Override
        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return allowed ? "Allowed" : "Denied: " + reason;
        }
    }
}
