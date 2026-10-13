// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.*;
import me.jxl.kiosk.plugins.PluginHost;

public final class ListDemoTest {
    static final class Host implements PluginHost {
        final Map<String, String> values = new HashMap<>();
        public void publishTextSensor(String key, String name, String value) { assert key.matches("[a-z][a-z0-9_]{0,39}"); assert value.length() <= 512; values.put(key, value); }
        public void showWindow(String title, String message, String button) {}
        public void hideWindow() {}
        public void log(String message) {}
        public void status(String message, boolean error) {}
    }

    static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]);
        return Collections.unmodifiableMap(row);
    }

    public static void main(String[] args) {
        Host host = new Host();
        ListDemo demo = new ListDemo();
        List<Object> shortcuts = Arrays.asList(
            row("label", "Home", "icon", "mdi:home", "opens", "Page", "page", "lovelace/0", "camera", "old", "entries", Collections.emptyList()),
            row("label", "Front door", "icon", "", "opens", "Camera view", "page", "lovelace/0", "camera", "a1b2c3d4e5f60718293a4b5c6d7e8f90", "entries", Collections.emptyList()),
            row("label", "Rooms", "icon", "", "opens", "Menu", "page", "", "camera", "", "entries", Arrays.asList(row("label", "Den", "page", "lovelace/den"))));
        Map<String, Object> settings = new HashMap<>();
        settings.put("shortcuts", Collections.unmodifiableList(shortcuts));
        settings.put("favoritePage", "lovelace/0");
        settings.put("featuredIcon", "mdi:star");
        settings.put("showCamera", false);
        settings.put("cameraView", "a1b2c3d4e5f60718293a4b5c6d7e8f90");
        demo.configure(host, settings);
        // Each row reads only the field its Opens choice shows.
        assert "3 shortcuts\nHome: page lovelace/0\nFront door: camera view a1b2c3d4e5f60718293a4b5c6d7e8f90\nRooms: menu of 1 entry"
            .equals(host.values.get("list_summary")) : host.values.get("list_summary");
        // A hidden camera view keeps its value, and the demo ignores it.
        assert "Favorite page: lovelace/0\nFeatured icon: mdi:star\nCamera view: Off".equals(host.values.get("picker_values"));
        settings.put("showCamera", true);
        demo.configure(host, settings);
        assert host.values.get("picker_values").endsWith("Camera view: a1b2c3d4e5f60718293a4b5c6d7e8f90");

        // Empty and missing values stay readable.
        demo.configure(host, new HashMap<String, Object>());
        assert "No shortcuts".equals(host.values.get("list_summary"));
        assert "Favorite page: None\nFeatured icon: None\nCamera view: Off".equals(host.values.get("picker_values"));

        // A long list stays within the text sensor limit.
        List<Object> many = new ArrayList<>();
        for (int i = 0; i < 32; i++) many.add(row("label", "Shortcut number " + i, "opens", "Page", "page", "dashboard-with-a-long-name/view-" + i));
        settings.put("shortcuts", many);
        demo.configure(host, settings);
        assert host.values.get("list_summary").length() == 512;
        assert host.values.get("list_summary").endsWith("...");
    }
}
