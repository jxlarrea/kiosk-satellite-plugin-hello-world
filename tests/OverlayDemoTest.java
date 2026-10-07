// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.*;
import me.jxl.kiosk.plugins.OverlayFactory;
import me.jxl.kiosk.plugins.OverlaySpec;
import me.jxl.kiosk.plugins.PluginHost;

/** Checks what the demo asks KS for. Drawing the views needs a device. */
public final class OverlayDemoTest {
    static final class Host implements PluginHost {
        final Map<String, OverlaySpec> shown = new LinkedHashMap<>();
        final List<String> calls = new ArrayList<>();
        boolean older;
        String status;
        public void showOverlay(String key, OverlaySpec spec, OverlayFactory factory) {
            if (older) throw new UnsupportedOperationException("Native overlays are unavailable");
            assert factory != null;
            shown.put(key, spec); calls.add("show " + key);
        }
        public void hideOverlay(String key) {
            if (older) throw new UnsupportedOperationException("Native overlays are unavailable");
            shown.remove(key); calls.add("hide " + key);
        }
        public void showWindow(String title, String message, String button) {}
        public void hideWindow() {}
        public void log(String message) {}
        public void status(String message, boolean error) { status = message; assert error; }
    }

    public static void main(String[] args) {
        Host host = new Host();
        OverlayDemo demo = new OverlayDemo();

        // Nothing shows before start or until an action asks for it.
        demo.showBar();
        assert host.calls.isEmpty();
        demo.start(host);
        assert host.calls.isEmpty();

        demo.showBar();
        OverlaySpec bar = host.shown.get(OverlayDemo.BAR);
        assert OverlaySpec.TOP.equals(bar.anchor()) && bar.inset() == 16;
        assert bar.width() == OverlaySpec.WRAP && bar.height() == OverlaySpec.WRAP;
        assert !bar.closeOnBack() && !bar.onTop() && bar.touchable();

        demo.showFull();
        OverlaySpec full = host.shown.get(OverlayDemo.FULL);
        assert full.width() == OverlaySpec.FILL && full.height() == OverlaySpec.FILL && full.closeOnBack();

        demo.showGlow();
        OverlaySpec glow = host.shown.get(OverlayDemo.GLOW);
        assert glow.width() == OverlaySpec.FILL && glow.onTop() && !glow.touchable() && glow.closeOnBack();
        demo.hideGlow();
        assert !host.shown.containsKey(OverlayDemo.GLOW);

        demo.hideBar();
        assert !host.shown.containsKey(OverlayDemo.BAR) && host.shown.containsKey(OverlayDemo.FULL);

        demo.stop();
        demo.showFull();
        assert host.calls.size() == 5;

        Host older = new Host();
        older.older = true;
        OverlayDemo fallback = new OverlayDemo();
        fallback.start(older);
        fallback.showBar();
        assert older.status.contains("newer Kiosk Satellite");
        fallback.hideBar();
        System.out.println("Overlay demo tests passed.");
    }
}
