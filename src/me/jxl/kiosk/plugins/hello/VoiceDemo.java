// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.Collections;
import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * Follows this kiosk's Voice Satellite turns the way an LED ring would:
 * idle, listening, processing or responding, realtime conversations included.
 */
final class VoiceDemo {
    private PluginHost host;
    private boolean watching;
    private int generation;
    private boolean live;

    synchronized void configure(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        boolean next = Boolean.TRUE.equals(settings.get("showVoice"));
        if (next == watching) return;
        watching = next;
        generation++;
        live = false;
        if (!watching) {
            try { host.unsubscribe("voice.state"); } catch (IllegalArgumentException olderHost) {}
            host.removeTextSensor("voice_state");
            return;
        }
        try {
            host.subscribe("voice.state");
        } catch (IllegalArgumentException olderHost) {
            host.publishTextSensor("voice_state", "Voice Satellite state", "Needs a newer Kiosk Satellite");
            return;
        }
        host.publishTextSensor("voice_state", "Voice Satellite state", "Reading");
        int current = generation;
        host.executeCommand("getVoiceState", Collections.<String, Object>emptyMap(), (ok, data, error) -> {
            synchronized (VoiceDemo.this) {
                // A later event is newer than this read, and a settings change makes it stale.
                if (this.host == null || current != generation || live) return;
                Map<?, ?> state = ok && data instanceof Map ? (Map<?, ?>) data : null;
                String value = state == null ? "Not available"
                    : Boolean.TRUE.equals(state.get("enabled")) ? String.valueOf(state.get("state"))
                    : "Voice Satellite is off";
                this.host.publishTextSensor("voice_state", "Voice Satellite state", value);
            }
        });
    }

    synchronized void onEvent(String event, Map<String, Object> payload) {
        if (host == null || !watching || !"ks.voice.state".equals(event)) return;
        live = true;
        host.publishTextSensor("voice_state", "Voice Satellite state", String.valueOf(payload.get("state")));
    }

    synchronized void stop() { host = null; watching = false; live = false; generation++; }
}
