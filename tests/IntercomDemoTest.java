// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.*;
import me.jxl.kiosk.plugins.PluginHost;

public final class IntercomDemoTest {
    static final class Host implements PluginHost {
        final Set<String> subscriptions = new HashSet<>();
        final Map<String, String> values = new HashMap<>();
        final List<CommandCallback> reads = new ArrayList<>();
        boolean older;
        public void executeCommand(String command, Map<String, Object> arguments, CommandCallback callback) {
            assert "getIntercomState".equals(command) && arguments.isEmpty();
            reads.add(callback);
        }
        public void subscribe(String event) { if (older) throw new IllegalArgumentException("Unknown KS event"); subscriptions.add(event); }
        public void unsubscribe(String event) { if (older) throw new IllegalArgumentException("Unknown KS event"); subscriptions.remove(event); }
        public void publishTextSensor(String key, String name, String value) { assert key.matches("[a-z][a-z0-9_]{0,39}"); assert value.length() <= 512; values.put(key, value); }
        public void removeTextSensor(String key) { values.remove(key); }
        public void showWindow(String title, String message, String button) {}
        public void hideWindow() {}
        public void log(String message) {}
        public void status(String message, boolean error) {}
    }

    static Map<String, Object> state(boolean enabled, String state, String kiosk, boolean dnd) {
        Map<String, Object> value = new HashMap<>();
        value.put("enabled", enabled); value.put("state", state); value.put("kiosk", kiosk); value.put("dnd", dnd);
        return value;
    }

    public static void main(String[] args) {
        Host host = new Host();
        IntercomDemo demo = new IntercomDemo();
        Map<String, Object> on = Collections.singletonMap("showIntercom", true);
        Map<String, Object> off = Collections.singletonMap("showIntercom", false);

        demo.configure(host, on);
        assert host.subscriptions.contains("intercom.state");
        assert "Reading".equals(host.values.get("intercom_state"));
        host.reads.get(0).onResult(true, state(true, "idle", "", true), null);
        assert "idle, do not disturb".equals(host.values.get("intercom_state"));
        demo.onEvent("ks.intercom.state", state(true, "ringing", "Kitchen", false));
        assert "ringing with Kitchen".equals(host.values.get("intercom_state"));

        // A read that lands after an event is older than it.
        demo.configure(host, off);
        assert host.subscriptions.isEmpty() && host.values.isEmpty();
        demo.configure(host, on);
        demo.onEvent("ks.intercom.state", state(true, "in_call", "Kitchen", false));
        host.reads.get(1).onResult(true, state(true, "idle", "", false), null);
        assert "in_call with Kitchen".equals(host.values.get("intercom_state"));

        // A read from before a settings change is stale.
        demo.configure(host, off);
        demo.configure(host, on);
        host.reads.get(1).onResult(true, state(true, "missed", "Kitchen", false), null);
        assert "Reading".equals(host.values.get("intercom_state"));
        host.reads.get(2).onResult(true, state(false, "idle", "", false), null);
        assert "Intercom is off".equals(host.values.get("intercom_state"));

        demo.stop();
        demo.onEvent("ks.intercom.state", state(true, "ringing", "Kitchen", false));
        assert "Intercom is off".equals(host.values.get("intercom_state"));

        Host older = new Host();
        older.older = true;
        IntercomDemo legacy = new IntercomDemo();
        legacy.configure(older, on);
        assert older.reads.isEmpty();
        assert "Needs a newer Kiosk Satellite".equals(older.values.get("intercom_state"));
    }
}
