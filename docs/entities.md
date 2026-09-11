# Plugin sensors and selects

SDK 1 plugins can expose numeric sensors, text sensors, binary sensors and selects through the kiosk's existing ESPHome connection. Add `entities` to the manifest's capabilities. The kiosk must have ESPHome and native entities enabled and be connected to Home Assistant. Users can exclude individual plugin entities in the existing ESPHome entity picker.

Numeric, text and binary sensors are read-only. A **select** is a writable control with a fixed set of advertised options. Use a text sensor when a value such as the connection type is observed rather than chosen. Publishing an entity does not add a setting to the plugin subpage. Declare a setting separately if you want a local control too.

## Publish readings

```java
Map<String, Object> metadata = new LinkedHashMap<>();
metadata.put("unit", "dBm");
metadata.put("deviceClass", "signal_strength");
metadata.put("stateClass", "measurement");
metadata.put("accuracyDecimals", 0);
host.publishSensor("rssi", "WiFi signal", metadata, -65.0);
host.publishTextSensor("connection", "Connection type", "WiFi");
host.publishBinarySensor("connected", "Connected", "connectivity", true);
```

Use `null` when a reading is unknown. The bridge sends ESPHome's missing-state flag, preserving the distinction between missing data, zero, an empty string and `false`. Do not use NaN or infinity to signal a missing reading.

| Method | Arguments |
| --- | --- |
| `publishSensor(key, name, metadata, state)` | `Map<String, Object>` metadata and a nullable `Double` state |
| `publishTextSensor(key, name, state)` | Nullable `String` state, at most 512 characters |
| `publishBinarySensor(key, name, deviceClass, state)` | Device class string, or `""` for none, and a nullable `Boolean` state |
| `publishSelect(key, name, options, state)` | `String[]` options and a nullable `String` current selection |
| `removeSensor(key)` | Remove this plugin session's numeric sensor |
| `removeTextSensor(key)` | Remove this plugin session's text sensor |
| `removeBinarySensor(key)` | Remove this plugin session's binary sensor |
| `removeSelect(key)` | Remove this plugin session's select |

Numeric sensor metadata accepts only these fields:

| Field | Default | Contract |
| --- | --- | --- |
| `unit` | `""` | String of up to 32 characters, such as `ms`, `%` or `dBm` |
| `deviceClass` | `""` | Home Assistant device class, such as `temperature` or `signal_strength`. Empty means none |
| `stateClass` | `"none"` | `none`, `measurement`, `total_increasing` or `total` |
| `accuracyDecimals` | `0` | Integer from 0 to 6, used as a display precision hint |

Device class strings must be empty or match `[a-z][a-z0-9_]{0,39}`. Choose a class and unit appropriate to the measurement. KS validates the format rather than maintaining a copy of Home Assistant's complete class list. Numeric values must be finite and remain finite when converted to an ESPHome 32-bit float. The transport can round values to float precision. Display precision does not change the measured value.

## Writable selects

```java
host.publishSelect("mode", "Performance mode",
    new String[] {"Auto", "Performance", "Efficiency"}, "Auto");
```

KS delivers a requested change through the existing serialized callback:

```java
@Override
public void onEvent(String event, Map<String, Object> payload) {
    if (event.equals("select.mode")) {
        String option = (String) payload.get("option");
        // Apply the choice in your plugin, then publish the actual result.
        applyMode(option);
        host.publishSelect("mode", "Performance mode",
            new String[] {"Auto", "Performance", "Efficiency"}, option);
    }
}
```

`applyMode` above represents your plugin's own implementation. Options must contain 1 to 32 unique, nonempty strings of at most 80 characters each. A non-null published selection must match an option exactly. Requests for an unadvertised option or a removed select are rejected before reaching the plugin. Sensor entities reject commands entirely.

There is no optimistic state change. After applying a choice, publish the resulting selection. If the operation fails, report the failure with `status` and keep or republish the actual state. The callback still has a three-second deadline, so start longer work on a plugin-owned worker and publish the confirmed result when it completes.

A select does not automatically write settings. To retain a plugin-owned choice, call `saveSettings` with the complete validated settings map. Hello World's `select.pattern` handler updates its own Pattern setting, saves it and republishes the selection. Home Assistant and the plugin subpage then show the same choice. This API does not allow a plugin to edit saved KS settings or control another plugin's entities.

## Identity, updates and lifetime

- Keys match `[a-z][a-z0-9_]{0,39}` and names contain 1 to 80 characters. A key belongs to one entity type within one plugin. Reuse the key on every update.
- New sensor and select object IDs use `plugin_<normalized-plugin-id>____<type>_<key>`. Plugin ID hyphens become underscores. The four underscores separate these from existing plugin light and action button IDs. Final Home Assistant entity IDs can differ if the user renames them.
- A session can own up to 32 numeric, text, binary and select entities combined. This is separate from the existing four-light limit and declared action buttons.
- At most 64 new-entity publications or removals are accepted per plugin per one-second window. KS copies and validates inputs synchronously. Invalid input, exhausted limits or a stopped session throws `IllegalArgumentException` or `IllegalStateException` and leaves the previous declaration intact.
- Changes are coalesced over 250 milliseconds. Intermediate readings can be skipped. Publish the latest state rather than relying on every update as an event counter.
- Changing only a value sends a state update. It does not reconnect ESPHome or refresh the plugin's settings form. Adding or removing an entity or changing metadata, names or select options updates the catalog and uses the existing ESPHome reconnect behavior.
- Current states, including unknown values, are replayed when ESPHome attaches or reconnects.
- Disabling, uninstalling, updating or turning off **Enable Plugins** removes the session's entities from the active catalog. KS sends a missing state for removed sensors and selects. Home Assistant may retain their entity registry entries. Plugin removal does not delete Home Assistant history or registry entries.
- KS does not persist these readings or declarations. Republish them in `start` and stop all samplers in `stop`. The host is revoked before `stop`, so do not call removal methods from that callback. Old sessions cannot deliver updates or receive new select commands.
- Entities, state and plugin-specific exclusions stay local to the kiosk and do not sync through Fleet Management. Home Assistant receives only entities allowed by the kiosk's ESPHome configuration.

## Hello World example

Hello World publishes four entities alongside its chart demo:

| Entity | Type | Behavior |
| --- | --- | --- |
| Simulated wave | Numeric sensor | The latest Wave value in percent, with measurement state class and two display decimals. Unknown when the demo chart is hidden |
| Demo status | Text sensor | `Chart running` or `Chart hidden` |
| Demo chart active | Binary sensor | Whether the demo chart is enabled |
| Demo pattern | Select | `Sine` or `Triangle`. Changing it updates the same Pattern setting used in the plugin subpage |

All values are simulated. See [HelloWorldPlugin.java](../src/me/jxl/kiosk/plugins/hello/HelloWorldPlugin.java) for initialization, state publication, select handling and shutdown.

## Update an existing repository

1. Update KS to a build that contains these entity methods.
2. Copy the current SDK interfaces from [`sdk/src/me/jxl/kiosk/plugins/`](../sdk/src/me/jxl/kiosk/plugins) into your repository. Keep `apiVersion: 1` and add `entities` to the manifest's capabilities if it is missing.
3. Publish readings from your sampler with the methods above. Add `onEvent("select.KEY", payload)` handling only for writable selects. Update test hosts to implement the methods exercised by your tests.
4. Build and test locally. Verify null readings, repeated updates and shutdown. For a select, also verify invalid-option rejection and the confirmed value after a change.
5. Test the ZIP through **Developer Tools > Install from ZIP**. A GitHub-installed plugin must be uninstalled before switching to a local ZIP, which deletes its settings. A separate development kiosk avoids disturbing the installed copy.
6. Increase the plugin version and publish a stable GitHub release. Let GitHub Actions build and attach the release assets for repository installation.
