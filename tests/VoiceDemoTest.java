// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.*;
import me.jxl.kiosk.plugins.PluginHost;

public final class VoiceDemoTest {
    static final class Host implements PluginHost {
        final Set<String> subscriptions = new HashSet<>();
        final Map<String, String> values = new HashMap<>();
        final List<CommandCallback> reads = new ArrayList<>();
        boolean older;
        public void executeCommand(String command, Map<String, Object> arguments, CommandCallback callback) {
            assert "getVoiceState".equals(command) && arguments.isEmpty();
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

    static Map<String, Object> state(boolean enabled, String state) {
        Map<String, Object> value = new HashMap<>();
        value.put("enabled", enabled); value.put("state", state);
        return value;
    }

    public static void main(String[] args) {
        Host host = new Host();
        VoiceDemo demo = new VoiceDemo();
        Map<String, Object> on = Collections.singletonMap("showVoice", true);
        Map<String, Object> off = Collections.singletonMap("showVoice", false);

        demo.configure(host, on);
        assert host.subscriptions.contains("voice.state");
        assert "Reading".equals(host.values.get("voice_state"));
        host.reads.get(0).onResult(true, state(true, "idle"), null);
        assert "idle".equals(host.values.get("voice_state"));
        demo.onEvent("ks.voice.state", Collections.singletonMap("state", "listening"));
        assert "listening".equals(host.values.get("voice_state"));
        demo.onEvent("ks.voice.interaction", Collections.singletonMap("state", "wrong"));
        assert "listening".equals(host.values.get("voice_state"));

        // A read that lands after an event is older than it.
        demo.configure(host, off);
        assert host.subscriptions.isEmpty() && host.values.isEmpty();
        demo.configure(host, on);
        demo.onEvent("ks.voice.state", Collections.singletonMap("state", "responding"));
        host.reads.get(1).onResult(true, state(true, "idle"), null);
        assert "responding".equals(host.values.get("voice_state"));

        // A read from before a settings change is stale.
        demo.configure(host, off);
        demo.configure(host, on);
        host.reads.get(1).onResult(true, state(true, "processing"), null);
        assert "Reading".equals(host.values.get("voice_state"));
        host.reads.get(2).onResult(true, state(false, "idle"), null);
        assert "Voice Satellite is off".equals(host.values.get("voice_state"));

        demo.stop();
        demo.onEvent("ks.voice.state", Collections.singletonMap("state", "listening"));
        assert "Voice Satellite is off".equals(host.values.get("voice_state"));

        Host older = new Host();
        older.older = true;
        VoiceDemo legacy = new VoiceDemo();
        legacy.configure(older, on);
        assert older.reads.isEmpty();
        assert "Needs a newer Kiosk Satellite".equals(older.values.get("voice_state"));
        legacy.configure(older, off);
        assert older.values.isEmpty();
    }
}
