package com.chatchannel.manager;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CooldownManager {

    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, Queue<Long>> messageTimestamps = new ConcurrentHashMap<>();
    private final long cooldownMs;
    private final int spamThreshold;
    private final long spamTimeWindowMs;

    public CooldownManager(long cooldownMs, int spamThreshold, long spamTimeWindowMs) {
        this.cooldownMs = cooldownMs;
        this.spamThreshold = spamThreshold;
        this.spamTimeWindowMs = spamTimeWindowMs;
    }

    public boolean isOnCooldown(UUID uuid) {
        Long lastTime = lastMessageTime.get(uuid);
        if (lastTime == null) return false;
        return System.currentTimeMillis() - lastTime < cooldownMs;
    }

    public long getRemainingCooldown(UUID uuid) {
        Long lastTime = lastMessageTime.get(uuid);
        if (lastTime == null) return 0;
        long remaining = cooldownMs - (System.currentTimeMillis() - lastTime);
        return Math.max(0, remaining);
    }

    public boolean isSpamming(UUID uuid) {
        Queue<Long> timestamps = messageTimestamps.get(uuid);
        if (timestamps == null) return false;

        long now = System.currentTimeMillis();
        long windowStart = now - spamTimeWindowMs;

        long count = timestamps.stream()
                .filter(t -> t > windowStart)
                .count();

        return count >= spamThreshold;
    }

    public void recordMessage(UUID uuid) {
        long now = System.currentTimeMillis();
        lastMessageTime.put(uuid, now);

        Queue<Long> timestamps = messageTimestamps.computeIfAbsent(uuid, k -> new ConcurrentLinkedQueue<>());
        timestamps.add(now);

        cleanOldTimestamps(uuid, now);
    }

    private void cleanOldTimestamps(UUID uuid, long now) {
        Queue<Long> timestamps = messageTimestamps.get(uuid);
        if (timestamps == null) return;

        long cutoff = now - spamTimeWindowMs * 2;
        while (!timestamps.isEmpty() && timestamps.peek() < cutoff) {
            timestamps.poll();
        }
    }

    public void removePlayer(UUID uuid) {
        lastMessageTime.remove(uuid);
        messageTimestamps.remove(uuid);
    }

    public void clear() {
        lastMessageTime.clear();
        messageTimestamps.clear();
    }
}
