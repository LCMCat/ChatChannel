package tech.ccat.chatchannel.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateMessageManager {

    private static class PrivateMessageRecord {
        final UUID partnerUuid;
        final String partnerName;
        final long timestamp;

        PrivateMessageRecord(UUID partnerUuid, String partnerName, long timestamp) {
            this.partnerUuid = partnerUuid;
            this.partnerName = partnerName;
            this.timestamp = timestamp;
        }
    }

    private final Map<UUID, PrivateMessageRecord> lastSender = new ConcurrentHashMap<>();
    private final int expiryMinutes;

    public PrivateMessageManager(int expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }

    public void recordMessage(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName) {
        long now = System.currentTimeMillis();
        lastSender.put(senderUuid, new PrivateMessageRecord(receiverUuid, receiverName, now));
        lastSender.put(receiverUuid, new PrivateMessageRecord(senderUuid, senderName, now));
    }

    public UUID getLastPartnerUuid(UUID uuid) {
        PrivateMessageRecord record = lastSender.get(uuid);
        if (record == null) return null;
        if (isExpired(record.timestamp)) {
            lastSender.remove(uuid);
            return null;
        }
        return record.partnerUuid;
    }

    public String getLastPartnerName(UUID uuid) {
        PrivateMessageRecord record = lastSender.get(uuid);
        if (record == null) return null;
        if (isExpired(record.timestamp)) {
            lastSender.remove(uuid);
            return null;
        }
        return record.partnerName;
    }

    public boolean hasLastPartner(UUID uuid) {
        PrivateMessageRecord record = lastSender.get(uuid);
        if (record == null) return false;
        if (isExpired(record.timestamp)) {
            lastSender.remove(uuid);
            return false;
        }
        return true;
    }

    public boolean isExpired(UUID uuid) {
        PrivateMessageRecord record = lastSender.get(uuid);
        if (record == null) return true;
        return isExpired(record.timestamp);
    }

    private boolean isExpired(long timestamp) {
        return System.currentTimeMillis() - timestamp > expiryMinutes * 60L * 1000L;
    }

    public void removePlayer(UUID uuid) {
        lastSender.remove(uuid);
    }

    public void clear() {
        lastSender.clear();
    }
}
