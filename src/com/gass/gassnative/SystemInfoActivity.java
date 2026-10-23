package com.gass.gassnative;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class SystemInfoActivity extends Activity {
    private final int BG = Color.rgb(9, 13, 21);
    private final int CARD = Color.rgb(20, 28, 41);
    private final int BORDER = Color.rgb(42, 56, 75);
    private final int ORANGE = Color.rgb(255, 138, 66);
    private final int BLUE = Color.rgb(100, 181, 255);
    private final int MUTED = Color.rgb(180, 194, 213);
    private final int GREEN = Color.rgb(114, 224, 177);

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), BORDER);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void section(LinearLayout page, String title) {
        TextView t = text(title, 12, BLUE);
        t.setTypeface(null, Typeface.BOLD);
        t.setLetterSpacing(0.1f);
        t.setPadding(dp(2), dp(22), 0, dp(10));
        page.addView(t);
    }

    private LinearLayout card(LinearLayout page) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(15), dp(16), dp(4));
        c.setBackground(shape(CARD, 16));
        page.addView(c, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }

    private void info(LinearLayout card, String label, String value,
                      int valueColor) {
        TextView l = text(label, 12, MUTED);
        card.addView(l);

        TextView v = text(value, 14, valueColor);
        v.setTextIsSelectable(true);
        v.setPadding(0, dp(3), 0, dp(15));
        card.addView(v);
    }

    private String property(String key) {
        BufferedReader r = null;
        try {
            Process p = new ProcessBuilder("getprop", key)
                .redirectErrorStream(true).start();
            r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String value = r.readLine();
            p.waitFor();
            if (value != null) {
                value = value.trim();
                if (!value.isEmpty() && !value.equals("[]")) return value;
            }
        } catch (Exception ignored) {
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) {}
        }
        return "Unavailable";
    }

    private String cpuInfo() {
        String fallback = "Unavailable";
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            while ((line = r.readLine()) != null) {
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("hardware") ||
                    lower.startsWith("model name") ||
                    lower.startsWith("processor")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                        String v = parts[1].trim();
                        if (lower.startsWith("hardware") ||
                            lower.startsWith("model name")) return v;
                        if (fallback.equals("Unavailable")) fallback = v;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) {}
        }
        return fallback;
    }

    private Map<String, String> graphicsInfo() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("GL vendor", "Unavailable");
        result.put("GL renderer", "Unavailable");
        result.put("GL version", "Unavailable");

        EGL10 egl = null;
        EGLDisplay display = EGL10.EGL_NO_DISPLAY;
        EGLSurface surface = EGL10.EGL_NO_SURFACE;
        EGLContext context = EGL10.EGL_NO_CONTEXT;

        try {
            egl = (EGL10) EGLContext.getEGL();
            display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            if (display == EGL10.EGL_NO_DISPLAY ||
                !egl.eglInitialize(display, new int[2])) return result;

            int[] attrs = {
                0x3024, 8, 0x3023, 8, 0x3022, 8,
                0x3021, 8, 0x3040, 4, 0x3038
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];

            if (!egl.eglChooseConfig(display, attrs, configs, 1, count)
                || count[0] < 1) return result;

            int[] pbufferAttrs = {0x3057, 1, 0x3056, 1, 0x3038};
            surface = egl.eglCreatePbufferSurface(display, configs[0],
                pbufferAttrs);

            int[] contextAttrs = {0x3098, 2, 0x3038};
            context = egl.eglCreateContext(display, configs[0],
                EGL10.EGL_NO_CONTEXT, contextAttrs);

            if (surface == EGL10.EGL_NO_SURFACE ||
                context == EGL10.EGL_NO_CONTEXT) return result;

            if (!egl.eglMakeCurrent(display, surface, surface, context))
                return result;

            GL10 gl = (GL10) context.getGL();
            String vendor = gl.glGetString(GL10.GL_VENDOR);
            String renderer = gl.glGetString(GL10.GL_RENDERER);
            String version = gl.glGetString(GL10.GL_VERSION);

            if (vendor != null) result.put("GL vendor", vendor);
            if (renderer != null) result.put("GL renderer", renderer);
            if (version != null) result.put("GL version", version);
        } catch (Exception ignored) {
        } finally {
            if (egl != null && display != EGL10.EGL_NO_DISPLAY) {
                try {
                    egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                    if (surface != EGL10.EGL_NO_SURFACE)
                        egl.eglDestroySurface(display, surface);
                    if (context != EGL10.EGL_NO_CONTEXT)
                        egl.eglDestroyContext(display, context);
                    egl.eglTerminate(display);
                } catch (Exception ignored) {}
            }
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(20), dp(20), dp(30));
        scroll.addView(page);

        TextView back = text("\u2039   System Information", 22, ORANGE);
        back.setTypeface(null, Typeface.BOLD);
        back.setPadding(0, 0, 0, dp(8));
        back.setOnClickListener(v -> finish());
        page.addView(back);

        TextView subtitle = text(
            "Live device details reported by Android", 13, MUTED);
        subtitle.setPadding(0, 0, 0, dp(5));
        page.addView(subtitle);

        section(page, "\u25C8  DEVICE");
        LinearLayout device = card(page);
        info(device, "Manufacturer", Build.MANUFACTURER, ORANGE);
        info(device, "Model", Build.MODEL, BLUE);
        info(device, "Device codename", Build.DEVICE, BLUE);
        info(device, "Android version", Build.VERSION.RELEASE, BLUE);
        info(device, "Android API level",
            String.valueOf(Build.VERSION.SDK_INT), BLUE);
        info(device, "Build fingerprint", Build.FINGERPRINT, MUTED);

        section(page, "\u25A3  CPU & SOC");
        LinearLayout cpu = card(page);
        info(cpu, "Supported ABI",
            android.text.TextUtils.join(", ", Build.SUPPORTED_ABIS), ORANGE);
        info(cpu, "Available CPU cores",
            String.valueOf(Runtime.getRuntime().availableProcessors()), BLUE);
        info(cpu, "CPU information from /proc/cpuinfo",
            cpuInfo(), GREEN);
        info(cpu, "SoC model property",
            property("ro.soc.model"), GREEN);
        info(cpu, "Board platform",
            property("ro.board.platform"), BLUE);
        info(cpu, "Hardware property",
            property("ro.hardware"), BLUE);

        section(page, "\u25C9  ANDROID GRAPHICS");
        LinearLayout gpu = card(page);
        Map<String, String> gl = graphicsInfo();
        info(gpu, "OpenGL vendor", gl.get("GL vendor"), ORANGE);
        info(gpu, "OpenGL renderer", gl.get("GL renderer"), GREEN);
        info(gpu, "OpenGL version", gl.get("GL version"), BLUE);
        info(gpu, "Vulkan availability",
            property("ro.vulkan.version"), MUTED);

        section(page, "\u25B8  NOTES");
        LinearLayout notes = card(page);
        TextView note = text(
            "GPU details above come from an EGL/OpenGL context created by " +
            "this Android app. They are not hardcoded. The Linux desktop " +
            "can use a different renderer, such as Zink, Freedreno or " +
            "llvmpipe. Vulkan device enumeration and the active Linux " +
            "desktop renderer can be added separately.",
            13, MUTED);
        note.setLineSpacing(dp(3), 1.0f);
        notes.addView(note);

        setContentView(scroll);
    }
}
