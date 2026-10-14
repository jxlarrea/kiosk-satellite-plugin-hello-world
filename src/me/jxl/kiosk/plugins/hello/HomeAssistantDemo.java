// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * A selected HA entity observed through KS without handling credentials. With the ha capability it also follows the
 * entity with a state trigger and toggles it through a service call.
 */
final class HomeAssistantDemo {
    static final String TRIGGER = "entityChanges";

    private PluginHost host;
    private String entity = "";
    private int changes;

    void configure(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        Object configured = settings.get("haEntity");
        String next = configured instanceof String ? (String) configured : "";
        if (next.equals(entity)) return;
        if (!entity.isEmpty()) {
            host.unsubscribe("ha.entity." + entity);
            host.haUnsubscribe(TRIGGER);
        }
        entity = next;
        changes = 0;
        if (entity.isEmpty()) {
            host.removeTextSensor("ha_state");
            host.removeTextSensor("ha_details");
            host.removeTextSensor("ha_trigger");
            return;
        }
        host.publishTextSensor("ha_state", "Home Assistant state", "Connecting");
        host.publishTextSensor("ha_details", "Home Assistant details", "Entity: " + entity);
        host.publishTextSensor("ha_trigger", "Home Assistant trigger", "Waiting for a change");
        host.subscribe("ha.entity." + entity);

        // The same message the HA frontend sends, without an id. KS numbers it.
        Map<String, Object> trigger = new LinkedHashMap<>();
        trigger.put("platform", "state");
        trigger.put("entity_id", entity);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "subscribe_trigger");
        message.put("trigger", trigger);
        String subscribed = entity;
        host.haSubscribe(TRIGGER, message, (ok, data, error) -> {
            if (!ok) trigger(subscribed, "Not subscribed: " + error);
        });
    }

    /** The Toggle Home Assistant entity action. */
    void toggle() {
        if (host == null || entity.isEmpty()) throw new IllegalStateException("Choose a Home Assistant entity first");
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "call_service");
        message.put("domain", "homeassistant");
        message.put("service", "toggle");
        message.put("target", Collections.singletonMap("entity_id", entity));
        PluginHost current = host;
        host.haCall(message, (ok, data, error) -> {
            // The new state arrives through the entity subscription and the trigger.
            if (!ok) current.status("Toggle failed: " + error, true);
        });
    }

    void onEvent(String event, Map<String, Object> payload) {
        if (host == null || entity.isEmpty()) return;
        if (event.equals("ks.ha.subscription." + TRIGGER)) {
            triggerEvent(payload);
            return;
        }
        if (!event.equals("ks.ha.entity." + entity)) return;
        String status = String.valueOf(payload.get("status"));
        Object state = payload.get("state");
        String value = "available".equals(status) && state != null ? String.valueOf(state) : status;
        host.publishTextSensor("ha_state", "Home Assistant state", value.substring(0, Math.min(512, value.length())));
        Map<?, ?> attributes = payload.get("attributes") instanceof Map ? (Map<?, ?>) payload.get("attributes") : Collections.emptyMap();
        String details = "Entity: " + entity;
        if (attributes.get("friendly_name") != null) details += "\nName: " + attributes.get("friendly_name");
        if (attributes.get("unit_of_measurement") != null) details += "\nUnit: " + attributes.get("unit_of_measurement");
        if (payload.get("lastUpdated") != null) details += "\nUpdated: " + payload.get("lastUpdated");
        host.publishTextSensor("ha_details", "Home Assistant details", details.substring(0, Math.min(512, details.length())));
    }

    private void triggerEvent(Map<String, Object> payload) {
        Object type = payload.get("type");
        if ("disconnected".equals(type)) trigger(entity, "Reconnecting");
        else if ("ended".equals(type)) trigger(entity, "Ended: " + payload.get("error"));
        else if ("overflow".equals(type)) trigger(entity, "Missed " + payload.get("dropped") + " changes");
        else if ("event".equals(type)) {
            // subscribe_trigger events carry the trigger variables, with the old and new state objects.
            Map<?, ?> event = map(payload.get("event"));
            Map<?, ?> trigger = map(map(event.get("variables")).get("trigger"));
            Object from = map(trigger.get("from_state")).get("state");
            Object to = map(trigger.get("to_state")).get("state");
            changes++;
            trigger(entity, from + " to " + to + " (" + changes + (changes == 1 ? " change)" : " changes)"));
        }
    }

    private void trigger(String forEntity, String text) {
        if (host == null || !forEntity.equals(entity)) return;
        host.publishTextSensor("ha_trigger", "Home Assistant trigger", text.substring(0, Math.min(512, text.length())));
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map ? (Map<?, ?>) value : Collections.emptyMap();
    }

    void stop() { host = null; entity = ""; changes = 0; }
}
