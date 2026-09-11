# Plugin interactions with Kiosk Satellite

SDK 1 is the first public plugin API. Plugins can inspect KS state, observe passive events and use transient controls such as dismissing the screensaver or opening a configured camera view. The host bridge does not edit saved KS settings, manage files or change permissions. Plugin-owned windows, settings, charts, actions, sensors, selects, switches and RGB lights are also supported.

## Capabilities

Every plugin declares `"apiVersion": 1`. All features below belong to that single public SDK.

| Manifest capability | Host interaction |
| --- | --- |
| `overlay` | Show or update the plugin's floating window |
| None | Hide its window, log, publish runtime status and charts, save its own settings and receive declared actions |
| `native` | Locate its verified packaged JNI library and DEX container |
| `entities` | Publish and remove its own RGB lights, sensors, selects and switches |
| `host.read` | Execute read commands and subscribe to passive events |
| `host.control` | Execute the transient controls listed below |

Request only the capabilities the plugin uses. `getHostApi` is available with either host capability and lists the commands and events available to that session. Plugins execute inside the app and are not sandboxed from arbitrary Java or native code. These capabilities describe the public API contract. Users must trust installed code.

## Read commands

```java
host.executeCommand("isScreensaverActive", Collections.emptyMap(), (ok, data, error) -> {
    if (ok) {
        boolean active = Boolean.TRUE.equals(data);
        host.status(active ? "Screensaver active" : "Screensaver idle", false);
    } else {
        host.status(error, true);
    }
});
```

`executeCommand(String command, Map<String, Object> arguments, PluginHost.CommandCallback callback)` returns immediately. `onResult(boolean ok, Object data, String error)` runs later on the plugin's serialized callback worker. Data is a detached JSON-compatible value: a map, list, string, number, boolean or null. Check `ok` before reading it. On success `error` is null. Missing platform support or an unavailable feature can produce a null reading or a failed result.

Except for `getBrightness`, every read command requires an empty arguments map. Names are case-sensitive. The read and control tables form the complete command allowlist. A new core KS command does not automatically become available to plugins.

| Command | Result |
| --- | --- |
| `getHostApi` | `{apiVersion: 1, capabilities: [...], commands: [...], events: [...]}` listing this host's supported names |
| `isScreensaverActive` | Boolean indicating whether the screensaver is active |
| `getScreensaverDue` | Next idle deadline as an ISO 8601 UTC string, or null while no countdown is armed |
| `getScreensaverSuppressed` | Boolean. Currently true because KS owns the screen and suppresses the dashboard's separate screensaver |
| `isScreenOn` | Boolean representing KS's logical screen state |
| `getBrightness` | Brightness from 0 to 1, or null if unavailable. `{}` reads the level controlled by the Screen light. `{"panel": true}` reads current panel brightness. `{"ceiling": true}` reads the adaptive brightness ceiling. Both flags accept booleans and cannot both be true |
| `getAmbientDisplay` | Boolean indicating whether KS has detected that the device leaves an ambient display lit after screen-off |
| `getVolume` | Media volume percentage from 0 to 100 |
| `getLightLevel` | `{present, lux, live}`. Sensor availability, latest lux reading and whether readings are live. Lux can be null |
| `getStats` | `{battery, charging, cpu, temp}`. Battery percentage, external power connected, CPU usage percentage and CPU temperature in Celsius. Unavailable numeric readings can be null |
| `getUptime` | `{app, network}` in seconds. Network is null while offline |
| `getDashboardState` | `{homeAssistantUrl, startUrl, currentUrl, currentPath}`. Sanitized HTTP/HTTPS URLs and the main WebView path. Fields can be null. See the [dashboard URL guide](dashboard.md) |
| `getDeviceInfo` | `{name, model, os, osVersion, sdkInt, appVersion, buildNumber, buildMode, package}`. Device and app metadata. IP addresses and unrelated fields are omitted |
| `getMotionEnabled` | Boolean indicating whether camera motion detection is enabled |
| `getFaceEnabled` | Boolean indicating whether camera face detection is enabled |
| `getProximityEnabled` | Boolean indicating whether proximity detection is enabled |
| `getCameraViewState` | `{active, viewId, viewName, focusedCameraId}` for the existing camera overlay. IDs and name can be null. No images or stream URLs |
| `getWakeWordState` | `{available, stopWordAvailable, enabled, active, listening, engine, engineLabel, status, statusLabel}`. Engine names can be null. No audio, model files or internal configuration |
| `haStatus` | `{configured, connected}` booleans reflecting KS's existing Home Assistant connection check. This does not initiate a new connection check |

Object responses expose only the fields listed above. A field absent from the underlying feature is returned as null. KS failure details are reduced to a generic SDK error rather than exposing internal responses. Reads use the existing feature state and queries. They do not request Android permissions or enable disabled features.

## Transient controls

Declare `host.control` and call the same asynchronous `executeCommand` method:

```java
host.executeCommand("stopScreensaver", Collections.emptyMap(), (ok, data, error) -> {
    if (!ok) host.status(error, true);
});
host.executeCommand("showCameraView", Collections.singletonMap("viewId", "front-door"),
    (ok, data, error) -> { if (!ok) host.status(error, true); });
```

| Command | Arguments and behavior |
| --- | --- |
| `startScreensaver` | `{}`. Start the configured screensaver |
| `stopScreensaver` | `{}`. Dismiss it, wake the display and reset the idle timer |
| `postponeScreensaver` | `{}`. Report activity to reset the idle timer and dismiss an active screensaver |
| `nextScreensaverSlide` | `{}`. Step forward in the active slideshow or camera rotation. Returns whether anything stepped |
| `previousScreensaverSlide` | `{}`. Step backward. Returns whether anything stepped |
| `screenOn` | `{}`. Wake the display |
| `screenOff` | `{}`. Turn off the display using an existing permission. Fails quietly when permission is missing and never opens a grant prompt |
| `showCameraView` | `{viewId: string, toggle?: boolean}`. Open an existing configured view. `toggle: true` closes that view if already open |
| `hideCameraView` | `{}`. Close the current camera overlay |
| `focusCamera` | `{cameraId: string}`. Focus an existing camera in the active view. Empty string or `{}` returns to its grid |
| `showNowPlaying` | `{}`. Show full-screen Now Playing when enabled and a track is loaded |
| `hideNowPlaying` | `{}`. Dismiss full-screen Now Playing. Playback and saved preferences stay unchanged. Normal idle behavior can show it again later |
| `sendspinControl` | `{command: "play" | "pause" | "next" | "previous"}`. Control the current Sendspin group or followed player. Does not edit queues or playlists |
| `showAppLauncher` | `{}`. Open the configured launcher when enabled and apps exist |
| `hideAppLauncher` | `{}`. Close the launcher overlay |
| `showOverlayPage` | `{url: string}`. Open an HTTP or HTTPS page over the dashboard without a close button |
| `showLinkPage` | `{url: string}`. Open an HTTP or HTTPS page with a close button |
| `hideOverlayPage` | `{}`. Close the web overlay, including Music Assistant |
| `showMusicAssistant` | `{}`. Open the configured Music Assistant interface |
| `loadUrl` | `{url: string}`. Navigate the main WebView to HTTP or HTTPS. Does not change the saved Start URL |
| `loadDashboard` | `{dashboard: string}`. Navigate to a dashboard path on the configured HA server |
| `loadStartUrl` | `{}`. Navigate to the saved Start URL |
| `haNavigate` | `{path: string}`. Navigate to a dashboard view and dismiss obstructing overlays. Returns whether navigation succeeded |
| `reload` | `{}`. Reload the current dashboard page without clearing browser data |

Controls return null on success unless the table documents a boolean result. Missing configuration, unsupported media commands and normal KS feature gates can cause failure. URLs must not contain credentials or use executable schemes such as `javascript:`. Dashboard and view paths accept slash-separated letters, digits, underscores and hyphens. Unknown arguments are rejected.

These commands change live presentation or playback without editing saved configuration or content. Navigation can leave an unfinished form and screen-off can interrupt someone using the device. Authors should make these behaviors intentional. The usual screensaver session bookkeeping and idle behavior still apply. Plugins cannot take ownership of shared internal voice suppression or screen wake-lock flags.

## Events

```java
host.subscribe("screensaver.state");

// In KioskPlugin.onEvent:
if (event.equals("ks.screensaver.state")) {
    boolean active = Boolean.TRUE.equals(payload.get("active"));
    host.status(active ? "Screensaver active" : "Screensaver idle", false);
}

// While still running, if the plugin no longer needs this event:
host.unsubscribe("screensaver.state");
```

Pass the subscription name from this table to `subscribe` or `unsubscribe`. Delivery uses `onEvent("ks." + name, payload)`. Every payload also contains `time`, an ISO 8601 UTC string recording when the SDK observed the event. Subscriptions are idempotent, apply only to the current plugin session and do not replay earlier events. Subscribe first and use read commands for initial state. Responses and subsequent events can race, so prefer the latest event if a read was requested before it.

| Subscription name | Payload fields besides `time` |
| --- | --- |
| `screensaver.state` | `active`: boolean |
| `screensaver.countdown` | `due`: ISO 8601 UTC string or null |
| `screensaver.view` | `view`: active overlay mode string or null. Dim mode has no overlay |
| `screen.state` | `on`: boolean, `source`: `app`, `system` or `probe` |
| `screen.brightness` | `level`: configured control level, `panel`: actual panel level. Both normalized to 0 through 1 |
| `screen.ambient` | `on`: boolean indicating detected ambient display behavior |
| `device.power` | `charging`: boolean indicating external power connected |
| `device.network` | `up`: boolean for the default network |
| `device.volume` | No additional fields. Read `getVolume` for the current value |
| `device.light` | `lux`: ambient light reading |
| `detection.motion` | No additional fields |
| `detection.face` | No additional fields |
| `detection.proximity` | `held`: false on approach, true while still near |
| `detection.person` | `held`: false on arrival, true while presence continues |
| `detection.presence` | `present`: boolean from the device person sensor |
| `voice.interaction` | `active`: boolean, `source`: `page`, `sendspin` or `command`. No speech or conversation content |
| `wakeword.state` | `active`, `listening` and `muted`: booleans |
| `wakeword.detected` | `model` and `phrase`: wake-word identifiers. No captured audio |
| `stopword.detected` | No additional fields |
| `browser.state` | No additional fields. The main WebView URL or saved HA/Start URL changed. Read `getDashboardState` for the current snapshot |
| `camera.view` | `active`, `viewId`, `viewName` and `focusedCameraId` |

Events are passive observations. Motion, face, person, proximity and wake-word events only exist when the corresponding KS feature is already producing them. A subscription never starts a camera, opens a microphone or changes the screensaver policy. No general event-bus subscription is provided.

Delivery is best effort. KS coalesces repeated events of the same type over a 100 ms window and keeps only the latest pending payload of each type while the plugin is busy. Do not use these events as an audit log or exact occurrence counter.

## Lifetime, errors and limits

- Commands require an active SDK 1 session with the matching host capability. Subscriptions require `host.read`. Missing access, invalid native arguments, unknown subscription names or exceeded request limits throw synchronously. Handle these errors in plugin code.
- Unknown commands, invalid command-specific arguments, unavailable features and command failures return `ok=false` through the callback.
- At most eight command callbacks may be pending per plugin. At most 20 commands may start per one-second rate window. Arguments are limited to two fields with boolean or string values. Keys are at most 32 characters and strings at most 2048 characters and responses to 32 KiB of JSON.
- KS commands time out after eight seconds. The Android bridge has a ten-second response deadline. Callback delivery can be later while the plugin worker is occupied. A timed-out command may still finish internally. Its late result is ignored, but a control may already have taken effect. Do not blindly retry controls such as toggle or next.
- Command completions, KS events and existing lifecycle callbacks share the plugin's worker. Each callback must finish within three seconds. Never block a lifecycle callback waiting for a command callback. A callback exception or timeout disables the plugin through the existing failure handling.
- Disabling, uninstalling, updating or turning off Enable Plugins revokes the session. Pending command completions and queued events for it are discarded. A replacement session cannot receive old results, even when it has the same plugin ID.
- Subscriptions are not saved. Subscribe again in `start`. KS revokes access before calling `stop`, so `stop` should release plugin resources and should not call the host API to unsubscribe.
- Command requests are logged with the plugin ID through KS's normal command logging. Existing quiet queries remain quiet. Subscribing does not add polling.

## Other supported host interactions

These methods operate on resources owned by the plugin and are part of SDK 1.

| Method or callback | Contract |
| --- | --- |
| `showWindow(title, message, buttonLabel)` | One draggable plain-text window per plugin above the dashboard. Title is 1 to 80 characters, message at most 4096 and button label at most 80. An empty label hides its button |
| `hideWindow()` | Removes that plugin's window |
| `log(message)` | Plugin-prefixed diagnostic log, truncated to 1000 characters |
| `status(message, error)` | Runtime status in the plugin subpage, at most 1000 characters. Not persisted |
| `publishSeries(key, chart)` | Publish a read-only chart in the plugin subpage. Up to four charts per session with four series and 240 samples each. See the [chart API](charts.md) |
| `removeSeries(key)` | Remove a chart from the active plugin session |
| `saveSettings(values)` | Saves values validated against the plugin's declared settings. Unknown keys are rejected and omitted values use defaults. Does not invoke `configure` again |
| `nativeLibraryPath(name)` | Verified per-session path to a packaged library matching the process ABI. Pass `rockchip_led` for `librockchip_led.so` |
| `packagePath()` | Verified plugin DEX JAR path, for a plugin-owned helper |
| `publishLight(key, name, effects, state)` | Registers or updates a plugin-owned RGB light. At most four lights, 24 effects per light and bounded names. See [RGB states](creating-plugins.md#rich-settings-and-hardware) |
| `publishSensor`, `publishTextSensor`, `publishBinarySensor` | Publish numeric, text or boolean readings shown on the plugin subpage and exposed through ESPHome. See the [entity API](entities.md) for signatures, metadata and limits |
| `publishSwitch(key, name, state)` | Publish a writable switch with a confirmed boolean state. Commands arrive as `onEvent("switch.KEY", {"on": boolean})`. See the [entity API](entities.md#writable-switches) |
| `publishSelect(key, name, options, state)` | Publish a writable select. Confirm applied changes by publishing its resulting state |
| `removeSensor`, `removeTextSensor`, `removeBinarySensor`, `removeSelect`, `removeSwitch` | Remove the corresponding entity owned by the active session |
| `onEvent("select.KEY", payload)` | A request for an advertised select option, passed as `option`. No automatic setting write or optimistic state change |
| `removeLight(key)` | Removes the plugin-owned light |
| `start(host, settings)` | Receives the host and validated settings when the plugin starts |
| `configure(settings)` | Receives validated saved settings changes |
| `execute(command, arguments)` | Receives a declared action invoked by a gesture, drawer shortcut, ESPHome button or authenticated API. Arguments are currently empty |
| `onEvent("window.action", payload)` | The plugin window's button was pressed. Empty payload |
| `onEvent("window.closed", payload)` | The user dismissed its window. Empty payload |
| `onEvent("light.KEY", payload)` | A command for its RGB light, containing the supplied fields from `on`, `brightness`, `red`, `green`, `blue` and `effect` |
| `onEvent("ks.NAME", payload)` | A subscribed SDK 1 event from the table above |
| `stop()` | Ends the session. Release timers, threads and hardware handles |

Settings controls support strings, booleans, numbers, colors, selections, groups and descriptions. Actions and RGB lights are namespaced by plugin ID. Users opt actions into the drawer and Home Assistant through Plugin Manager. Neither mechanism lets a plugin replace a core KS command.

## Not exposed

The host bridge does not expose edits to saved KS settings, file creation or deletion, kiosk unlock, app launching outside the configured launcher, restarts, installs, permission changes or commands targeting another plugin. It also excludes raw settings, credentials, browser storage, arbitrary JavaScript, camera images, microphone audio, screenshots, notification management, logs and arbitrary Home Assistant calls. A command name beginning with `get` does not grant access. Brightness and volume setters are excluded because the existing handlers persist settings. Internal voice suppression and wake-lock controls are excluded because they modify shared ownership state.

## Buildable example

The [read-only example](../examples/read-only/src/example/kiosk/ReadOnlyPlugin.java) reads initial screen and screensaver state and observes changes. It reports them in its own runtime status without changing KS state.

From this repository:

```sh
python3 tools/build.py examples/read-only
```

Install `examples/read-only/dist/ks-read-only-example-1.0.0.zip` through **Plugin Manager > Developer Tools > Install from ZIP** on an SDK 1 build. Enable the example and open its subpage to see its status. The Hello World template and Rockchip LED Control use the same SDK 1 contract.

## Optional Shizuku commands

The `shizuku` capability adds `shizukuState()` and asynchronous `executeShizuku(command, timeoutMs, callback)`. It requires separate user permission for KS and does not change the `host.read` or `host.control` allowlists. See the [Shizuku guide](shizuku.md) for the complete contract and separate example.
