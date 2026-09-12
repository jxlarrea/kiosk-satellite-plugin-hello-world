# Plugin screensavers

A plugin screensaver replaces the rendered content inside the stock KS screensaver. KS still owns activation, idle timeout, schedules, brightness, motion and other wake triggers, touch dismissal, screen-off timing, notification brightness, widgets, At a Glance, pixel shift and Now Playing layouts. The plugin does not open its own overlay or implement its own idle timer.

## Try the DVD demo

Enable **Hello World** in Plugin Manager. Open **Screensaver > Screensaver mode** and choose **DVD Logo (Hello World)**. You can also choose it in a screensaver schedule entry. Start the screensaver from the kiosk menu or wait for the configured idle timeout.

In **Plugin Manager > Hello World > Screensaver demo**, change **Logo color** and **Background color**. Both save automatically and refresh an active renderer. The logo bounces around its available viewport, including the smaller viewport beside Now Playing. It keeps the selected color on each bounce. The greeting, charts and optional Shizuku demo work independently.

## Register a renderer

Declare `"screensaver"` in the manifest's `capabilities` and keep `apiVersion: 1`. Use the current SDK interfaces from this repository.

```java
host.publishScreensaver("clock", "My Clock", html);
host.removeScreensaver("clock");
```

`publishScreensaver(String key, String title, String html)` registers or replaces a self-contained HTML document. It does not select or start a screensaver. Publish during `start` and again when rendering settings or content change. KS lists the renderer as **Title (Plugin Name)** in both the mode picker and schedule editor. Multiple plugins can use the same key.

`removeScreensaver(String key)` withdraws that renderer. Every renderer is automatically removed when its plugin session ends, including disable, uninstall, failed startup and replacement during an update. Calls through a revoked host fail.

The stored mode is `plugin:PLUGIN_ID:KEY`. Keep IDs and keys stable across releases so selections and schedules reconnect when the plugin is enabled again. KS preserves the selected mode when its renderer is unavailable and shows a black background beneath any configured stock overlays. It keeps all screensaver policies active and resumes rendering when that key is published again. It does not wake the kiosk or rewrite its settings.

| Field | Limit |
| --- | --- |
| Key | Lowercase letter followed by up to 39 lowercase letters, digits or underscores |
| Title | Nonblank, up to 80 characters |
| HTML | Nonblank, up to 256 KiB in UTF-8 |
| Renderers | Up to four per plugin session |
| Changes | Up to four publish or remove calls per second per session |

## Rendering contract

Documents support HTML, inline CSS, inline JavaScript, SVG, canvas and embedded data URLs. Bundle everything needed for rendering into the document. External network requests, local file access, navigation, popups, forms and browser permissions are blocked. The document runs in a frame with an opaque origin and has no KS JavaScript bridge, dashboard credentials or shared storage. The Java plugin itself remains trusted code running inside KS with app permissions.

Use `requestAnimationFrame` for animation. Do not send each animation frame through `publishScreensaver`. Render against the current viewport and respond to resizing. Use viewport units or a `resize` listener for rotation and Now Playing layouts. Avoid fixed screen resolutions. The [DVD sample](../src/me/jxl/kiosk/plugins/hello/DvdScreensaver.java) demonstrates a responsive SVG and elapsed-time animation with bounds checks.

KS creates the document only while that screensaver surface is visible. Replacing its HTML recreates the document. Dismissal, switching modes and full-screen Now Playing remove it. Screen-off and app-background transitions pause the individual WebView without pausing the dashboard's timers. A renderer crash leaves a black background for the remainder of that document's lifetime. Starting a new session or replacing the document creates a fresh renderer.

Input belongs to KS. Buttons and links inside the document do not receive taps. Stock dismissal settings apply to the whole surface. Stock widgets and At a Glance render above it and retain their schedule overrides. Stock pixel shift moves the rendered surface. Mode-specific settings for other screensavers, such as a stock photo folder or clock font, continue to configure those screensavers. Define your own content settings in the plugin manifest.

If native code needs to fetch content, perform that work asynchronously in the plugin and publish a new document when it changes. Do not block SDK lifecycle callbacks. Optional `host.read` subscriptions can observe stock screensaver state through the existing [KS interaction API](ks-api.md). Rendering alone needs only the `screensaver` capability.

## Fleet management

Plugin packages, renderer documents and plugin settings remain local to each kiosk. A selected plugin mode and any schedule containing plugin modes are excluded from fleet pushes. A follower's local plugin mode or plugin schedule is preserved when a leader sends stock screensaver settings. Other stock screensaver settings continue following their normal fleet rules.

## Build and install

Run `python3 tools/test.py` and `python3 tools/build.py` with the Android SDK and Java configured as described in the [build guide](creating-plugins.md). Developer ZIP installation is for local testing. Publish plugins through GitHub Releases using the supplied workflow and attestation, as described in the [installation guide](installing-plugins.md).
