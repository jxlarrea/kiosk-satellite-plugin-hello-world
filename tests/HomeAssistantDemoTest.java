// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.*;
import me.jxl.kiosk.plugins.PluginHost;

public final class HomeAssistantDemoTest {
    static final class Host implements PluginHost {
        final Set<String> subscriptions = new HashSet<>();
        final Map<String, String> values = new HashMap<>();
        public void subscribe(String event) { subscriptions.add(event); }
        public void unsubscribe(String event) { subscriptions.remove(event); }
        public void publishTextSensor(String key, String name, String value) { assert key.matches("[a-z][a-z0-9_]{0,39}"); assert value.length() <= 512; values.put(key, value); }
        public void removeTextSensor(String key) { values.remove(key); }
        public void showWindow(String title, String message, String button) {}
        public void hideWindow() {}
        public void log(String message) {}
        public void status(String message, boolean error) { if (error) statusError = message; }
        final Map<String, Map<String, Object>> haSubscriptions = new HashMap<>();
        final List<Map<String, Object>> haCalls = new ArrayList<>();
        PluginHost.CommandCallback lastCall;
        String statusError;
        public void haSubscribe(String key, Map<String, Object> message, PluginHost.CommandCallback callback) {
            assert !haSubscriptions.containsKey(key);
            assert !message.containsKey("id");
            haSubscriptions.put(key, message);
            callback.onResult(true, null, null);
        }
        public void haUnsubscribe(String key) { haSubscriptions.remove(key); }
        public void haCall(Map<String, Object> message, PluginHost.CommandCallback callback) { haCalls.add(message); lastCall = callback; }
    }

    static Map<String, Object> state(String value) { return Collections.singletonMap("state", value); }
    public static void main(String[] args) {
        Host host = new Host();
        HomeAssistantDemo demo = new HomeAssistantDemo();
        demo.configure(host, Collections.singletonMap("haEntity", "sensor.room"));
        assert host.subscriptions.contains("ha.entity.sensor.room");
        Map<String, Object> state = new HashMap<>();
        state.put("status", "available"); state.put("state", "21");
        state.put("attributes", Collections.singletonMap("unit_of_measurement", "C"));
        demo.onEvent("ks.ha.entity.sensor.room", state);
        assert "21".equals(host.values.get("ha_state"));
        // The trigger subscription follows the selected entity.
        Map<?, ?> trigger = (Map<?, ?>) host.haSubscriptions.get(HomeAssistantDemo.TRIGGER).get("trigger");
        assert "subscribe_trigger".equals(host.haSubscriptions.get(HomeAssistantDemo.TRIGGER).get("type"));
        assert "sensor.room".equals(trigger.get("entity_id"));
        assert "Waiting for a change".equals(host.values.get("ha_trigger"));
        Map<String, Object> variables = new HashMap<>();
        Map<String, Object> fired = new HashMap<>();
        fired.put("from_state", state("20")); fired.put("to_state", state("21"));
        variables.put("trigger", fired);
        Map<String, Object> event = new HashMap<>();
        event.put("type", "event");
        event.put("event", Collections.singletonMap("variables", variables));
        demo.onEvent("ks.ha.subscription." + HomeAssistantDemo.TRIGGER, event);
        assert "20 to 21 (1 change)".equals(host.values.get("ha_trigger"));
        demo.onEvent("ks.ha.subscription." + HomeAssistantDemo.TRIGGER, Collections.singletonMap("type", "disconnected"));
        assert "Reconnecting".equals(host.values.get("ha_trigger"));

        // Toggling is one service call on the selected entity, an error lands in the status.
        demo.toggle();
        Map<String, Object> call = host.haCalls.get(0);
        assert "call_service".equals(call.get("type")) && "homeassistant".equals(call.get("domain")) && "toggle".equals(call.get("service"));
        assert "sensor.room".equals(((Map<?, ?>) call.get("target")).get("entity_id"));
        host.lastCall.onResult(false, null, "service_not_found: Not found");
        assert "Toggle failed: service_not_found: Not found".equals(host.statusError);

        demo.configure(host, Collections.singletonMap("haEntity", "sensor.other"));
        assert "sensor.other".equals(((Map<?, ?>) host.haSubscriptions.get(HomeAssistantDemo.TRIGGER).get("trigger")).get("entity_id"));
        assert !host.subscriptions.contains("ha.entity.sensor.room");
        demo.onEvent("ks.ha.entity.sensor.room", state);
        assert "Connecting".equals(host.values.get("ha_state"));
        state.put("status", "disconnected"); state.put("state", null);
        demo.onEvent("ks.ha.entity.sensor.other", state);
        assert "disconnected".equals(host.values.get("ha_state"));
        demo.configure(host, Collections.singletonMap("haEntity", ""));
        assert host.values.isEmpty() && host.subscriptions.isEmpty() && host.haSubscriptions.isEmpty();
        boolean refused = false;
        try { demo.toggle(); } catch (IllegalStateException expected) { refused = true; }
        assert refused;
        demo.stop();
        demo.onEvent("ks.ha.entity.sensor.other", state);
        assert host.values.isEmpty();
    }
}
