package com.chatchannel.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtil {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .build();

    public static Component colorize(String message) {
        return SERIALIZER.deserialize(message);
    }

    public static Component format(String message, String... replacements) {
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return colorize(message);
    }

    public static Component clickable(String text, String hoverText, String command) {
        Component component = colorize(text);
        if (hoverText != null && !hoverText.isEmpty()) {
            component = component.hoverEvent(HoverEvent.showText(colorize(hoverText)));
        }
        if (command != null && !command.isEmpty()) {
            component = component.clickEvent(ClickEvent.runCommand(command));
        }
        return component;
    }

    public static String stripColor(String message) {
        Component component = colorize(message);
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(component);
    }
}
