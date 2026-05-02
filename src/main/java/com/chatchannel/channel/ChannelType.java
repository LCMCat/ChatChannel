package com.chatchannel.channel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ChannelType {

    ALL("all", "a", "chat.channel.all", "&a"),
    PARTY("party", "p", "chat.channel.party", "&9"),
    GUILD("guild", "g", "chat.channel.guild", "&2"),
    OFFICER("officer", "o", "chat.channel.officer", "&6");

    private final String name;
    private final String shorthand;
    private final String permission;
    private final String color;

    ChannelType(String name, String shorthand, String permission, String color) {
        this.name = name;
        this.shorthand = shorthand;
        this.permission = permission;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getShorthand() {
        return shorthand;
    }

    public String getPermission() {
        return permission;
    }

    public String getColor() {
        return color;
    }

    public String getDisplayName() {
        return color + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static ChannelType fromString(String input) {
        if (input == null) return null;
        String lower = input.toLowerCase();
        for (ChannelType type : values()) {
            if (type.name.equals(lower) || type.shorthand.equals(lower)) {
                return type;
            }
        }
        return null;
    }

    public static List<String> getAllNames() {
        return Arrays.stream(values())
                .flatMap(t -> List.of(t.name, t.shorthand).stream())
                .collect(Collectors.toList());
    }
}
