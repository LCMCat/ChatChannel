package com.chatchannel.manager;

import com.chatchannel.channel.ChannelType;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatLogManager {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path logDirectory;
    private final Logger logger;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean running = false;

    public ChatLogManager(Path dataDirectory, Logger logger) {
        this.logDirectory = dataDirectory.resolve("chatlog");
        this.logger = logger;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ChatLog-Writer");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        running = true;
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException e) {
            logger.error("创建聊天日志目录失败！", e);
        }
        scheduler.scheduleWithFixedDelay(this::flushLogs, 5, 5, TimeUnit.SECONDS);
        logger.info("聊天日志系统已启动！");
    }

    public void stop() {
        running = false;
        flushLogs();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("聊天日志系统已关闭。");
    }

    public void log(ChannelType channel, String playerName, String message) {
        if (!running) return;

        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(TIME_FORMATTER);
        String logLine = String.format("[%s] [%s] %s: %s", timestamp, channel.getName().toUpperCase(), playerName, message);
        logQueue.add(logLine);
    }

    public void logPrivateMessage(String senderName, String receiverName, String message) {
        if (!running) return;

        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(TIME_FORMATTER);
        String logLine = String.format("[%s] [PM] %s -> %s: %s", timestamp, senderName, receiverName, message);
        logQueue.add(logLine);
    }

    private void flushLogs() {
        if (logQueue.isEmpty()) return;

        String date = LocalDate.now().format(DATE_FORMATTER);
        Path logFile = logDirectory.resolve(date + ".txt");

        try (BufferedWriter writer = Files.newBufferedWriter(logFile,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {

            String line;
            while ((line = logQueue.poll()) != null) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("写入聊天日志失败！", e);
        }
    }
}
