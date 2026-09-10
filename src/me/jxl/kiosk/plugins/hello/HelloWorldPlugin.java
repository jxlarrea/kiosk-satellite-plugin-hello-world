// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.Map;
import me.jxl.kiosk.plugins.KioskPlugin;
import me.jxl.kiosk.plugins.PluginHost;

/** A complete plugin with settings, commands and a window button event. */
public final class HelloWorldPlugin implements KioskPlugin {
    private PluginHost host;
    private String message;
    private boolean visible;
    private int greetings;

    @Override
    public void start(PluginHost host, Map<String, Object> settings) {
        this.host = host;
        configure(settings);
        host.log("Hello World started");
        if (Boolean.TRUE.equals(settings.get("showOnStart"))) show();
    }

    @Override
    public void configure(Map<String, Object> settings) {
        message = (String) settings.get("message");
        if (visible) show();
    }

    @Override
    public void execute(String command, Map<String, Object> arguments) {
        if ("show".equals(command)) show();
        else if ("hide".equals(command)) {
            visible = false;
            host.hideWindow();
        } else throw new IllegalArgumentException("Unknown command: " + command);
    }

    @Override
    public void onEvent(String event, Map<String, Object> payload) {
        if ("window.action".equals(event)) {
            greetings++;
            show();
        } else if ("window.closed".equals(event)) visible = false;
    }

    private void show() {
        visible = true;
        host.showWindow("Hello World", message + (greetings == 0 ? "" : "\nGreetings: " + greetings), "Say hello");
    }

    @Override
    public void stop() {
        visible = false;
        host.hideWindow();
        host.log("Hello World stopped");
        host = null;
    }
}
