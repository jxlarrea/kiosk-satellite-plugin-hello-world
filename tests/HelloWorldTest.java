// SPDX-License-Identifier: Apache-2.0
import java.util.HashMap;
import java.util.Collections;
import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;
import me.jxl.kiosk.plugins.hello.HelloWorldPlugin;

public final class HelloWorldTest {
    static final class Host implements PluginHost {
        String title;
        String message;
        boolean visible;
        public void showWindow(String title, String message, String button) {
            this.title = title;
            this.message = message;
            visible = true;
        }
        public void hideWindow() { visible = false; }
        public void log(String message) {}
    }
    public static void main(String[] args) {
        Host host = new Host();
        HelloWorldPlugin plugin = new HelloWorldPlugin();
        Map<String, Object> settings = new HashMap<>();
        settings.put("message", "Testing");
        settings.put("showOnStart", true);
        plugin.start(host, settings);
        assert host.visible && "Testing".equals(host.message);
        plugin.onEvent("window.action", Collections.emptyMap());
        assert host.message.contains("Greetings: 1");
        settings.put("message", "Updated");
        plugin.configure(settings);
        assert host.message.startsWith("Updated");
        plugin.execute("hide", Collections.emptyMap());
        assert !host.visible;
        plugin.execute("show", Collections.emptyMap());
        assert host.visible;
        plugin.stop();
        assert !host.visible;
        settings.put("showOnStart", false);
        plugin.start(host, settings);
        assert !host.visible;
        plugin.stop();
        System.out.println("Hello World lifecycle passed.");
    }
}
