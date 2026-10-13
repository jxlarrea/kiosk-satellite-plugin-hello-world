// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * Shows what configure receives for a list setting and the picker settings, as readings.
 * KS hands a list over as rows in the user's order, every field filled in.
 */
final class ListDemo {
    static final int MAX_TEXT = 512;

    void configure(PluginHost host, Map<String, Object> settings) {
        host.publishTextSensor("list_summary", "Shortcuts received", summary(settings.get("shortcuts")));
        host.publishTextSensor("picker_values", "Picker values", pickers(settings));
    }

    /** One line per shortcut: its label and where it goes. */
    static String summary(Object value) {
        List<?> rows = value instanceof List ? (List<?>) value : Collections.emptyList();
        if (rows.isEmpty()) return "No shortcuts";
        StringBuilder text = new StringBuilder(rows.size() == 1 ? "1 shortcut" : rows.size() + " shortcuts");
        for (Object row : rows) {
            if (!(row instanceof Map)) continue;
            Map<?, ?> shortcut = (Map<?, ?>) row;
            text.append('\n').append(shortcut.get("label")).append(": ").append(target(shortcut));
        }
        return clip(text.toString());
    }

    /**
     * Only the field the Opens choice shows applies. The others keep whatever the user set
     * before switching, so the demo never reads them.
     */
    static String target(Map<?, ?> shortcut) {
        switch (String.valueOf(shortcut.get("opens"))) {
            case "Camera view":
                return "camera view " + shortcut.get("camera");
            case "Menu": {
                Object entries = shortcut.get("entries");
                int count = entries instanceof List ? ((List<?>) entries).size() : 0;
                return "menu of " + count + (count == 1 ? " entry" : " entries");
            }
            default:
                return "page " + shortcut.get("page");
        }
    }

    static String pickers(Map<String, Object> settings) {
        String camera = Boolean.TRUE.equals(settings.get("showCamera")) ? orNone(settings.get("cameraView")) : "Off";
        return clip("Favorite page: " + orNone(settings.get("favoritePage"))
            + "\nFeatured icon: " + orNone(settings.get("featuredIcon"))
            + "\nCamera view: " + camera);
    }

    private static String orNone(Object value) {
        return value instanceof String && !((String) value).isEmpty() ? (String) value : "None";
    }

    private static String clip(String text) {
        return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT - 3) + "...";
    }
}
