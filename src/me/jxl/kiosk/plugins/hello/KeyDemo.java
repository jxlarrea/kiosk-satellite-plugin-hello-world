// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;

/** Shows the last hardware key KS received, such as a volume or remote button. */
final class KeyDemo {
    private PluginHost host;
    private boolean watching;
    private int presses;

    void configure(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        boolean next = Boolean.TRUE.equals(settings.get("showKeys"));
        if (next == watching) return;
        watching = next;
        if (watching) {
            host.publishTextSensor("last_key", "Last hardware key", "Press a hardware button");
            host.subscribe("device.key");
        } else {
            host.unsubscribe("device.key");
            host.removeTextSensor("last_key");
            presses = 0;
        }
    }

    void onEvent(String event, Map<String, Object> payload) {
        if (host == null || !watching || !"ks.device.key".equals(event)) return;
        // One count per press: skip the release and the repeats of a held key.
        if (!"down".equals(payload.get("action")) || !Integer.valueOf(0).equals(payload.get("repeat"))) return;
        presses++;
        host.publishTextSensor("last_key", "Last hardware key",
            payload.get("key") + " (scan code " + payload.get("scanCode") + "), " + presses + (presses == 1 ? " press" : " presses"));
    }

    void stop() { host = null; watching = false; presses = 0; }
}
