// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import me.jxl.kiosk.plugins.KsTheme;
import me.jxl.kiosk.plugins.OverlayFactory;
import me.jxl.kiosk.plugins.OverlaySpec;
import me.jxl.kiosk.plugins.PluginHost;

/**
 * Native overlays drawn with the KS theme: a small bar at the top of the
 * kiosk, a full screen card and an animated edge glow that draws over
 * everything like the voice overlay while every touch passes through.
 * Views are built on the main thread, so their click listeners run there too.
 */
final class OverlayDemo {
    static final String BAR = "bar";
    static final String FULL = "full";
    static final String GLOW = "glow";
    private PluginHost host;
    private volatile int greetings;

    synchronized void start(PluginHost host) { this.host = host; }

    /** The Show top bar action. Back leaves the bar alone, the way it would a status bar. */
    synchronized void showBar() {
        if (host == null) return;
        try {
            host.showOverlay(BAR, OverlaySpec.at(OverlaySpec.TOP).inset(16).closeOnBack(false), new Bar());
        } catch (UnsupportedOperationException olderHost) {
            host.status("Native overlays need a newer Kiosk Satellite.", true);
        }
    }

    /** The Hide top bar action. */
    synchronized void hideBar() {
        if (host == null) return;
        try { host.hideOverlay(BAR); } catch (UnsupportedOperationException olderHost) {}
    }

    /** The Show full screen overlay action and the bar's Full screen button. Back closes it. */
    synchronized void showFull() {
        if (host == null) return;
        try {
            host.showOverlay(FULL, OverlaySpec.fullScreen(), new Full());
        } catch (UnsupportedOperationException olderHost) {
            host.status("Native overlays need a newer Kiosk Satellite.", true);
        }
    }

    /**
     * The Show edge glow action. Visual only: it covers the whole kiosk,
     * even the screensaver and camera views, and every touch goes through
     * to whatever is under it. Back still clears it.
     */
    synchronized void showGlow() {
        if (host == null) return;
        try {
            host.showOverlay(GLOW, OverlaySpec.fullScreen().onTop(true).touchable(false), new Glow());
        } catch (UnsupportedOperationException olderHost) {
            host.status("Native overlays need a newer Kiosk Satellite.", true);
        }
    }

    /** The Hide edge glow action. */
    synchronized void hideGlow() {
        if (host == null) return;
        try { host.hideOverlay(GLOW); } catch (UnsupportedOperationException olderHost) {}
    }

    private synchronized void hideFull() {
        if (host != null) host.hideOverlay(FULL);
    }

    /**
     * Click listeners run on the main thread, outside the callbacks KS
     * guards. The session can end between a tap and the host call, and an
     * exception thrown here would end the app, so it is dropped.
     */
    private static void tap(Runnable action) {
        try { action.run(); } catch (RuntimeException sessionEnded) {}
    }

    /** KS revokes the host and removes the overlays before stop, so nothing is hidden here. */
    synchronized void stop() { host = null; }

    private String greeting() {
        return greetings == 0 ? "Hello World" : "Hello World · " + greetings;
    }

    /** A pill with a label and two buttons. Its size follows its content. */
    private final class Bar implements OverlayFactory {
        @Override public View create(Context context, KsTheme theme) {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = new TextView(context);
            Button hello = new Button(context);
            Button full = new Button(context);
            label.setText(greeting());
            hello.setText("Say hello");
            full.setText("Full screen");
            hello.setOnClickListener(view -> {
                greetings++;
                label.setText(greeting());
            });
            full.setOnClickListener(view -> tap(OverlayDemo.this::showFull));
            root.addView(label);
            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            gap.setMarginStart(theme.px(8));
            root.addView(hello, gap);
            root.addView(full);
            onThemeChanged(root, theme);
            return root;
        }

        @Override public void onThemeChanged(View view, KsTheme theme) {
            LinearLayout root = (LinearLayout) view;
            GradientDrawable pill = theme.card();
            pill.setCornerRadius(theme.px(100));
            root.setBackground(pill);
            root.setPadding(theme.px(20), theme.px(8), theme.px(8), theme.px(8));
            theme.styleText((TextView) root.getChildAt(0), 15, 500, "onSurface");
            theme.stylePill((TextView) root.getChildAt(1), true);
            theme.stylePill((TextView) root.getChildAt(2), false);
        }
    }

    /** A card centered over a dimmed kiosk. Tapping outside the card closes it. */
    private final class Full implements OverlayFactory {
        @Override public View create(Context context, KsTheme theme) {
            FrameLayout root = new FrameLayout(context);
            root.setOnClickListener(view -> tap(OverlayDemo.this::hideFull));
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            // Taps on the card stay on the card.
            card.setClickable(true);
            TextView title = new TextView(context);
            TextView body = new TextView(context);
            Button close = new Button(context);
            title.setText("Hello World");
            body.setText("This full screen overlay is a native Android view. It takes every tap until you close it with the button, a tap outside the card or the back button.");
            close.setText("Close");
            close.setOnClickListener(view -> tap(OverlayDemo.this::hideFull));
            card.addView(title);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = theme.px(12);
            card.addView(body, bodyParams);
            LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            closeParams.topMargin = theme.px(24);
            closeParams.gravity = Gravity.END;
            card.addView(close, closeParams);
            int width = Math.min(theme.px(420), context.getResources().getDisplayMetrics().widthPixels - theme.px(48));
            FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            root.addView(card, cardParams);
            onThemeChanged(root, theme);
            return root;
        }

        @Override public void onThemeChanged(View view, KsTheme theme) {
            FrameLayout root = (FrameLayout) view;
            root.setBackgroundColor(Color.argb(160, 0, 0, 0));
            LinearLayout card = (LinearLayout) root.getChildAt(0);
            card.setBackground(theme.card());
            card.setPadding(theme.px(theme.inset()), theme.px(theme.inset()), theme.px(theme.inset()), theme.px(theme.inset()));
            theme.styleText((TextView) card.getChildAt(0), 22, 600, "onSurface");
            theme.styleText((TextView) card.getChildAt(1), 15, 400, "onSurfaceVariant");
            theme.stylePill((TextView) card.getChildAt(2), true);
        }
    }

    /** A breathing glow along the screen edges in the theme's accent colors. */
    private static final class Glow implements OverlayFactory {
        @Override public View create(Context context, KsTheme theme) {
            return new GlowView(context, theme);
        }

        @Override public void onThemeChanged(View view, KsTheme theme) {
            ((GlowView) view).theme = theme;
            view.invalidate();
        }
    }

    private static final class GlowView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        KsTheme theme;

        GlowView(Context context, KsTheme theme) {
            super(context);
            this.theme = theme;
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override protected void onDraw(Canvas canvas) {
            // One breath every three seconds, cycling primary into tertiary.
            double phase = (SystemClock.uptimeMillis() % 3000) / 3000.0;
            float breath = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2));
            int color = blend(theme.color("primary"), theme.color("tertiary"), breath);
            float radius = theme.px(theme.radiusCard());
            // Wide faint strokes under narrow strong ones read as a soft glow
            // without a blur, which hardware canvases skip on older Android.
            for (int ring = 6; ring >= 1; ring--) {
                float width = theme.px(ring * 6);
                paint.setStrokeWidth(width);
                paint.setColor(Color.argb((int) ((40 + 140 * breath) / ring), Color.red(color), Color.green(color), Color.blue(color)));
                bounds.set(0, 0, getWidth(), getHeight());
                canvas.drawRoundRect(bounds, radius, radius, paint);
            }
            postInvalidateOnAnimation();
        }

        private static int blend(int from, int to, float amount) {
            return Color.rgb(
                (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * amount),
                (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * amount),
                (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount));
        }
    }
}
