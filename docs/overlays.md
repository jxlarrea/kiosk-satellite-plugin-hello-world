# Native overlays

SDK 1 plugins with the `overlay` capability can draw their own Android views over the kiosk. Use them for a control bar, a volume slider, an alert banner, a full screen panel or an animated full screen visual like the Voice Satellite overlay. KS hosts each view inside its own screen, so the menu, the screensaver and Lockdown Mode cover it the way they cover the dashboard, and it disappears when KS goes to the background. A `KsTheme` passes the KS colors, shapes and typeface, so the overlay can look like the rest of the app.

## Show an overlay

Call `showOverlay(String key, OverlaySpec spec, OverlayFactory factory)` on the host. Declare `"overlay"` in the manifest's `capabilities` and keep `apiVersion: 1`.

```java
host.showOverlay("bar", OverlaySpec.at(OverlaySpec.TOP).inset(16), new OverlayFactory() {
    @Override public View create(Context context, KsTheme theme) {
        TextView label = new TextView(context);
        label.setText("Hello");
        label.setBackground(theme.card());
        label.setPadding(theme.px(20), theme.px(12), theme.px(20), theme.px(12));
        theme.styleText(label, 15, 500, "onSurface");
        return label;
    }
});
```

Each call replaces the overlay at that key with a fresh view from the new factory. Call `hideOverlay(String key)` to remove it. Hiding an unknown key does nothing.

| Field | Contract |
| --- | --- |
| `key` | Stable overlay ID matching `[a-z][a-z0-9_]{0,39}`. Scoped to the plugin |
| `spec` | Placement, size and behavior. See below |
| `factory` | Builds the view. KS calls it on the Android main thread |

## Placement and size

`OverlaySpec` describes where the overlay sits. Sizes and insets are in dp.

| Method | Contract |
| --- | --- |
| `OverlaySpec.at(anchor)` | A wrapped overlay at `TOP_LEFT`, `TOP`, `TOP_RIGHT`, `LEFT`, `CENTER`, `RIGHT`, `BOTTOM_LEFT`, `BOTTOM` or `BOTTOM_RIGHT` |
| `OverlaySpec.fullScreen()` | Covers the whole kiosk area |
| `size(width, height)` | `OverlaySpec.WRAP`, `OverlaySpec.FILL` or 1 to 4096 dp for each side. Both default to `WRAP` |
| `inset(dp)` | Distance from the kiosk edges, 0 to 200 dp. Defaults to 0 |
| `closeOnBack(boolean)` | Whether back closes the overlay. Defaults to on |
| `onTop(boolean)` | Draw where the KS voice overlay does, over the screensaver, the menu and camera views. Defaults to off |
| `touchable(boolean)` | Off makes the overlay visual only, and every touch passes through to whatever is under it. Defaults to on |

A `WRAP` side follows the view's measured size and updates when the view asks for a new layout. KS measures it against the kiosk area less the inset on both sides. A `FILL` side takes the whole area less the inset. A fixed size is capped at the available area.

Transparent and translucent pixels show what is under the overlay, so a full screen view can dim the kiosk or draw effects over it.

A touchable overlay takes every touch inside its rectangle, including transparent parts of the view. Touches outside it reach the dashboard. A touchable full screen overlay therefore blocks the dashboard until it closes. To float a few controls over the dashboard, show several small overlays instead of one transparent full screen view. A full screen visual that should not block anything, such as a voice animation or an ambient effect, uses `touchable(false)`.

## The view

`OverlayFactory` has one required method and two optional callbacks. All of them run on the Android main thread.

| Method | Contract |
| --- | --- |
| `create(context, theme)` | Build and return the view. KS may call it again for the same overlay, for example after its Activity restarts |
| `onThemeChanged(view, theme)` | The KS theme changed, for example from light to dark. Restyle the view |
| `onDestroy(view)` | KS removed the view. Stop its animations and drop references to it |

Plugin callbacks such as `configure` and `onEvent` run on the plugin worker, not the main thread. Post view updates with `view.post(...)` or a main `Handler`. Keep a reference to the current view if you update it later, and clear it in `onDestroy`.

The view is drawn into a texture that KS composites with the rest of its screen. Regular views, layouts, animations and canvas drawing work. `SurfaceView`, `VideoView` and other views that render outside the view hierarchy fall back to a slower path and are not supported. Shadows from `setElevation` are clipped to the overlay's rectangle.

## Theme

`KsTheme` mirrors the KS theme in native units. KS builds a new one for each view and for every theme change.

| Method | Contract |
| --- | --- |
| `color(role)` | ARGB color by Material 3 role name, such as `surface`, `onSurface`, `onSurfaceVariant`, `surfaceContainerHigh`, `primary`, `onPrimary`, `primaryContainer`, `secondary`, `tertiary`, `error`, `outline` and `outlineVariant`, plus `success`. Unknown roles throw `IllegalArgumentException` |
| `colors()` | Every role this KS version provides |
| `isDark()` | Whether KS is in its dark theme |
| `px(dp)` | Pixels for a length in dp |
| `radiusCard()`, `radiusRow()`, `radiusControl()` | Corner radii in dp: 24 for cards and dialogs, 14 for rows, 12 for inputs |
| `inset()`, `cardGap()` | Card padding and the gap between cards, in dp |
| `typeface(weight)` | Rubik, the KS typeface, at a weight from 100 to 900 |
| `card()` | A background like the one KS toasts use over the dashboard: `surfaceContainerHigh`, the card radius and a hairline outline |
| `styleText(view, sp, weight, role)` | Applies the typeface, size and a color role to a `TextView` |
| `stylePill(view, filled)` | Turns a `TextView` or `Button` into a KS pill button. Filled uses `primary`, otherwise it is a quiet text button |
| `styleSlider(seekBar)` | Styles a `SeekBar` like the KS sliders: a 4 dp track filled in `primary` over `surfaceContainerHighest`, a 20 dp `primary` thumb and a 44 dp touch target |

The context passed to `create` carries the matching Android `DeviceDefault` light or dark theme, so stock widgets start close to KS. The theme does not change that context later, so restyle in `onThemeChanged`.

## Back and events

Back closes the topmost overlay with `closeOnBack` on, after any floating window. KS removes it and sends `onEvent("overlay.closed", {"key": key})`. An overlay with `closeOnBack(false)` is left alone and back does what it normally does. Use that for bars that should stay up. `hideOverlay` does not send `overlay.closed`.

While Lockdown Mode is on, back never closes an overlay.

## Layering

Overlays sit above the dashboard and its web overlays and below the Sendspin player, the menu, the screensaver, notifications and Lockdown Mode. The menu slides them aside with the dashboard. Floating windows sit above them. Within those layers, a new or replaced overlay goes on top.

An overlay with `onTop(true)` moves to the slot of the KS voice overlay, over the screensaver, the menu and camera views. Notifications, intercom call cards, announcements, the voice overlay itself, the screensaver's black cover and Lockdown Mode still cover it. Use it for visuals that must show whatever the kiosk is doing. It does not stop or dismiss the screensaver.

## Limits and lifetime

- Each plugin session can own at most four overlays. Replacing an existing key does not add another overlay.
- At most eight `showOverlay` calls are accepted per plugin per one-second rate window. Invalid input throws `IllegalArgumentException`. A stopped session or exhausted rate limit throws `IllegalStateException`.
- Disabling, uninstalling, updating or stopping the plugin, stopping the app or turning off **Enable Plugins** removes the session's overlays. Overlays are never persisted or included in fleet sync.
- KS revokes the host before calling `stop`. A `hideOverlay` call there is ignored.
- Kiosk Satellite versions without native overlays throw `UnsupportedOperationException`. Catch it if the plugin should keep working there.

## Errors in view code

An exception thrown from `create`, `onThemeChanged` or `onDestroy` disables the plugin, like an error in any other callback. Click listeners, `onDraw`, `onTouchEvent` and runnables you post run outside those guards, so an exception there ends the app. KS switches the plugin off before the crash, and the app comes back without it. Catch exceptions in listeners, and expect host calls from the main thread to throw `IllegalStateException` once the session has ended.

## Hello World demo

Hello World's actions show each kind. **Show top bar** puts a wrapped pill at the top of the kiosk with a greeting counter, a slider with its value, a filled **Say hello** button and a quiet **Full screen** button. Back leaves the bar alone, and **Hide top bar** removes it. **Full screen** and the **Show full screen overlay** action open a card centered over a dimmed kiosk. Back, **Close** or a tap outside the card closes it. **Show edge glow** draws a breathing glow along the screen edges with `onTop(true)` and `touchable(false)`: it stays over the screensaver and camera views while the dashboard keeps answering every tap. Back or **Hide edge glow** removes it. Switch KS between its light and dark themes to see them restyle. See [OverlayDemo.java](../src/me/jxl/kiosk/plugins/hello/OverlayDemo.java).

## Updating an existing plugin repository

1. Update Kiosk Satellite to a build that includes native overlays.
2. Copy the current files from [`sdk/src/me/jxl/kiosk/plugins/`](../sdk/src/me/jxl/kiosk/plugins) into the same directory in your plugin repository, including `OverlaySpec.java`, `OverlayFactory.java` and `KsTheme.java`. Do not package the SDK classes in your plugin ZIP.
3. The overlay classes use Android views, so the SDK now compiles against the Android platform. Copy the current `tools/build.py` and `tools/test.py`, or add the platform's `android.jar` to the classpath of your own SDK and test compile steps.
4. Add `"overlay"` to `capabilities` and keep `apiVersion: 1`.
5. Test the local ZIP through **Plugin Manager > Developer Tools > Install from ZIP**, then publish a release as described in [Creating plugins](creating-plugins.md).
