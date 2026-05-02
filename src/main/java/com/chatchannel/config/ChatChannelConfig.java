package com.chatchannel.config;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ChatChannelConfig {

    private final Path configPath;
    private final Logger logger;

    private String defaultChannel;
    private long chatCooldownMs;
    private int spamThreshold;
    private long spamTimeWindowMs;
    private int privateMessageExpiryMinutes;
    private boolean enableChannelSwitchTip;
    private boolean enableChatLog;

    private final Map<String, String> formats = new HashMap<>();
    private final Map<String, String> permissions = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();

    public ChatChannelConfig(Path dataDirectory, Logger logger) {
        this.configPath = dataDirectory.resolve("config.conf");
        this.logger = logger;
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath.getParent());
                try (InputStream is = getClass().getResourceAsStream("/config.conf")) {
                    if (is != null) {
                        Files.copy(is, configPath);
                    }
                }
            }

            String content = Files.readString(configPath);
            parseConfig(content);
            logger.info("聊天频道配置已加载！");
        } catch (IOException e) {
            logger.error("加载配置文件失败！", e);
            loadDefaults();
        }
    }

    public void reload() {
        load();
    }

    private void parseConfig(String content) {
        loadDefaults();

        String[] lines = content.split("\n");
        String section = "";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (trimmed.equals("formats {")) {
                section = "formats";
                continue;
            } else if (trimmed.equals("permissions {")) {
                section = "permissions";
                continue;
            } else if (trimmed.equals("messages {")) {
                section = "messages";
                continue;
            } else if (trimmed.equals("}")) {
                section = "";
                continue;
            }

            if (trimmed.contains("=")) {
                String[] parts = trimmed.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                switch (section) {
                    case "formats" -> formats.put(key, value);
                    case "permissions" -> permissions.put(key, value);
                    case "messages" -> messages.put(key, value);
                    default -> {
                        switch (key) {
                            case "default-channel" -> defaultChannel = value;
                            case "chat-cooldown-ms" -> chatCooldownMs = parseLong(value, 1000);
                            case "spam-threshold" -> spamThreshold = parseInt(value, 5);
                            case "spam-time-window-ms" -> spamTimeWindowMs = parseLong(value, 5000);
                            case "private-message-expiry-minutes" -> privateMessageExpiryMinutes = parseInt(value, 30);
                            case "enable-channel-switch-tip" -> enableChannelSwitchTip = parseBoolean(value, true);
                            case "enable-chat-log" -> enableChatLog = parseBoolean(value, true);
                        }
                    }
                }
            }
        }
    }

    private int parseInt(String value, int defaultVal) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private long parseLong(String value, long defaultVal) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private boolean parseBoolean(String value, boolean defaultVal) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return defaultVal;
    }

    private void loadDefaults() {
        defaultChannel = "all";
        chatCooldownMs = 1000;
        spamThreshold = 5;
        spamTimeWindowMs = 5000;
        privateMessageExpiryMinutes = 30;
        enableChannelSwitchTip = true;
        enableChatLog = true;

        formats.clear();
        formats.put("all", "&7[{server}] &r{prefix}{player}&7: &f{message}");
        formats.put("party", "&9[Party] &r{prefix}{player}&7: &b{message}");
        formats.put("guild", "&2[Guild] &r{guild-tag} {prefix}{player}&7: &a{message}");
        formats.put("officer", "&6[Officer] &r{guild-tag} {prefix}{player}&7: &e{message}");
        formats.put("private-send", "&6To &e{receiver}&6: &f{message}");
        formats.put("private-receive", "&6From &e{sender}&6: &f{message}");

        permissions.clear();
        permissions.put("channel-all", "chat.channel.all");
        permissions.put("channel-party", "chat.channel.party");
        permissions.put("channel-guild", "chat.channel.guild");
        permissions.put("channel-officer", "chat.channel.officer");
        permissions.put("command-chat", "chat.command.chat");
        permissions.put("command-msg", "chat.command.msg");
        permissions.put("command-reply", "chat.command.reply");
        permissions.put("bypass-cooldown", "chat.bypass.cooldown");
        permissions.put("bypass-spam", "chat.bypass.spam");

        messages.clear();
        messages.put("prefix", "&a频道 &7» &r");
        messages.put("help-header", "&a&m----------------&a 聊天频道帮助 &a&m----------------");
        messages.put("help-chat", "&a/chat <频道> &7- 切换聊天频道 (a/all, p/party, g/guild, o/officer)");
        messages.put("help-achat", "&a/achat <消息> &7- 强制发送到 All 频道");
        messages.put("help-pchat", "&a/pchat <消息> &7- 强制发送到 Party 频道");
        messages.put("help-gchat", "&a/gchat <消息> &7- 强制发送到 Guild 频道");
        messages.put("help-ochat", "&a/ochat <消息> &7- 强制发送到 Officer 频道");
        messages.put("help-msg", "&a/msg <玩家> <消息> &7- 发送私聊消息");
        messages.put("help-reply", "&a/reply <消息> &7- 回复最后私聊的人");
        messages.put("help-footer", "&a&m----------------------------------------");
        messages.put("channel-switched", "&a你已将聊天频道切换至 {channel}&a。");
        messages.put("channel-invalid", "&c无效的频道！可选: a/all, p/party, g/guild, o/officer");
        messages.put("channel-no-permission", "&c你没有权限使用 {channel} &c频道！");
        messages.put("channel-current", "&7你当前的聊天频道: {channel}");
        messages.put("message-empty", "&c消息不能为空！");
        messages.put("party-not-in-party", "&c你当前不在任何队伍中！");
        messages.put("guild-not-in-guild", "&c你当前不在任何公会中！");
        messages.put("guild-no-officer", "&c你不是公会官员，无法使用官员频道！");
        messages.put("msg-player-not-found", "&c找不到玩家 &e{player}&c！");
        messages.put("msg-self", "&c你不能给自己发送私聊！");
        messages.put("msg-sent", "&6To &e{receiver}&6: &f{message}");
        messages.put("msg-received", "&6From &e{sender}&6: &f{message}");
        messages.put("msg-no-reply", "&c你没有可以回复的私聊消息！");
        messages.put("msg-reply-expired", "&c私聊记录已过期！");
        messages.put("cooldown", "&c你发送消息太快了！请稍等 {time} &c秒。");
        messages.put("spam-warning", "&c请勿刷屏！你的消息已被拦截。");
        messages.put("no-permission", "&c你没有权限执行此命令！");
        messages.put("player-only", "&c此命令只能由玩家执行！");
        messages.put("usage", "&c用法: {usage}");
        messages.put("error", "&c发生错误，请稍后再试。");
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&c消息键缺失: " + key);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return msg;
    }

    public String getPermission(String key) {
        return permissions.getOrDefault(key, "chat.command");
    }

    public String getFormat(String key) {
        return formats.getOrDefault(key, "&f{message}");
    }

    public void setFormatOverride(String key, String format) {
        formats.put(key, format);
    }

    public String getDefaultChannel() { return defaultChannel; }
    public long getChatCooldownMs() { return chatCooldownMs; }
    public int getSpamThreshold() { return spamThreshold; }
    public long getSpamTimeWindowMs() { return spamTimeWindowMs; }
    public int getPrivateMessageExpiryMinutes() { return privateMessageExpiryMinutes; }
    public boolean isEnableChannelSwitchTip() { return enableChannelSwitchTip; }
    public boolean isEnableChatLog() { return enableChatLog; }
}
