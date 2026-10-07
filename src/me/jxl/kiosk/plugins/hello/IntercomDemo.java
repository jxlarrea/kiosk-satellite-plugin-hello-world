// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.Collections;
import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * Follows this kiosk's intercom the way a call light would: ringing, in a
 * call, announcing and missed calls, with the other kiosk's name.
 */
final class IntercomDemo {
    private PluginHost host;
    private boolean watching;
    private int generation;
    private boolean live;

    synchronized void configure(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        boolean next = Boolean.TRUE.equals(settings.get("showIntercom"));
        if (next == watching) return;
        watching = next;
        generation++;
        live = false;
        if (!watching) {
            try { host.unsubscribe("intercom.state"); } catch (IllegalArgumentException olderHost) {}
            host.removeTextSensor("intercom_state");
            return;
        }
        try {
            host.subscribe("intercom.state");
        } catch (IllegalArgumentException olderHost) {
            host.publishTextSensor("intercom_state", "Intercom state", "Needs a newer Kiosk Satellite");
            return;
        }
        host.publishTextSensor("intercom_state", "Intercom state", "Reading");
        int current = generation;
        host.executeCommand("getIntercomState", Collections.<String, Object>emptyMap(), (ok, data, error) -> {
            synchronized (IntercomDemo.this) {
                // A later event is newer than this read, and a settings change makes it stale.
                if (this.host == null || current != generation || live) return;
                Map<?, ?> state = ok && data instanceof Map ? (Map<?, ?>) data : null;
                if (state == null) this.host.publishTextSensor("intercom_state", "Intercom state", "Not available");
                else if (!Boolean.TRUE.equals(state.get("enabled"))) this.host.publishTextSensor("intercom_state", "Intercom state", "Intercom is off");
                else show(state);
            }
        });
    }

    synchronized void onEvent(String event, Map<String, Object> payload) {
        if (host == null || !watching || !"ks.intercom.state".equals(event)) return;
        live = true;
        show(payload);
    }

    private void show(Map<?, ?> state) {
        String value = String.valueOf(state.get("state"));
        Object kiosk = state.get("kiosk");
        if (kiosk instanceof String && !((String) kiosk).isEmpty()) value += " with " + kiosk;
        if (Boolean.TRUE.equals(state.get("dnd"))) value += ", do not disturb";
        host.publishTextSensor("intercom_state", "Intercom state", value.substring(0, Math.min(512, value.length())));
    }

    synchronized void stop() { host = null; watching = false; live = false; generation++; }
}
