// SPDX-License-Identifier: Apache-2.0
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import me.jxl.kiosk.plugins.PluginHost;
import me.jxl.kiosk.plugins.hello.HelloWorldPlugin;

public final class HelloWorldTest {
    static final class Host implements PluginHost {
        String title;
        String message;
        String status;
        boolean visible;
        volatile int publications;
        volatile Map<String, Object> chart;
        public void showWindow(String title, String message, String button) {
            this.title = title; this.message = message; visible = true;
        }
        public void hideWindow() { visible = false; }
        public void log(String message) {}
        public void status(String message, boolean error) { status = message; }
        public void publishSeries(String key, Map<String, Object> chart) {
            assert "demo".equals(key); this.chart = chart; publications++;
        }
        public void removeSeries(String key) { chart = null; }
        void awaitPublication(int previous) throws Exception {
            long end = System.nanoTime() + 4_000_000_000L;
            while (publications <= previous && System.nanoTime() < end) Thread.sleep(10);
            assert publications > previous : "Demo sampler did not publish";
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Host host = new Host();
        HelloWorldPlugin plugin = new HelloWorldPlugin();
        Map<String, Object> settings = new HashMap<>();
        settings.put("message", "Testing"); settings.put("showOnStart", true);
        settings.put("showChart", true); settings.put("amplitude", 60); settings.put("chartSize", "Regular");
        settings.put("pattern", "Sine"); settings.put("seriesColor", "#1976D2");
        try {
            plugin.start(host, settings);
            assert host.visible && "Testing".equals(host.message);
            assert host.status.contains("simulated");
            assert ((List<?>) host.chart.get("timestamps")).size() == 40;
            assert ((List<?>) host.chart.get("series")).size() == 2;
            plugin.onEvent("window.action", Collections.emptyMap());
            assert host.message.contains("Greetings: 1");
            settings.put("message", "Updated"); plugin.configure(settings);
            assert host.message.startsWith("Updated");
            plugin.execute("hide", Collections.emptyMap()); assert !host.visible;
            plugin.execute("show", Collections.emptyMap()); assert host.visible;
            settings.put("chartSize", "Mini");
            settings.put("amplitude", 0); settings.put("pattern", "Triangle"); settings.put("seriesColor", "#FF0000");
            plugin.configure(settings);
            int before = host.publications; host.awaitPublication(before);
            assert Boolean.TRUE.equals(host.chart.get("compact"));
            List<Map<String, Object>> series = (List<Map<String, Object>>) host.chart.get("series");
            assert "#FF0000".equals(series.get(0).get("color"));
            for (Map<String, Object> item : series) {
                List<Double> values = (List<Double>) item.get("values");
                assert values.get(values.size() - 1) == 0;
            }
            settings.put("showChart", false); plugin.configure(settings); assert host.chart == null;
            before = host.publications;
            Thread.sleep(2100); assert host.publications == before;
            settings.put("showChart", true); plugin.configure(settings);
            host.awaitPublication(before); assert host.chart != null;
        } finally { plugin.stop(); }
        int stopped = host.publications;
        Thread.sleep(2100); assert host.publications == stopped : "Sampler survived stop";
        settings.put("showOnStart", false); host.visible = false;
        try {
            plugin.start(host, settings);
            assert !host.visible;
            assert ((List<?>) host.chart.get("timestamps")).size() == 40;
        } finally { plugin.stop(); }
        System.out.println("Hello World controls, chart updates and lifecycle passed.");
    }
}
