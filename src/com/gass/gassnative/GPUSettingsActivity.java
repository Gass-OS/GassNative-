package com.gass.gassnative;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class GPUSettingsActivity extends Activity {
    private final int BG = Color.rgb(9, 13, 21);
    private final int CARD = Color.rgb(20, 28, 41);
    private final int BORDER = Color.rgb(42, 56, 75);
    private final int BLUE = Color.rgb(100, 181, 255);
    private final int ORANGE = Color.rgb(255, 153, 84);
    private final int TEXT = Color.rgb(235, 241, 250);
    private final int MUTED = Color.rgb(170, 187, 207);
    private final int GREEN = Color.rgb(114, 224, 177);
    private final int RED = Color.rgb(255, 135, 135);

    private final String[] gpuValues = {
        "auto", "zink", "freedreno", "llvmpipe"
    };

    private final String[] gpuLabels = {
        "Auto detect (recommended)",
        "Zink — OpenGL over Vulkan",
        "Freedreno / Turnip — experimental",
        "llvmpipe — software rendering"
    };

    private Spinner renderer;
    private LinearLayout page;
    private File root;

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), BORDER);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private void section(String title) {
        TextView t = text(title, 12, BLUE, true);
        t.setLetterSpacing(0.08f);
        t.setPadding(dp(2), dp(22), 0, dp(10));
        page.addView(t);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(15), dp(16), dp(5));
        c.setBackground(shape(CARD, 16));

        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(2);
        page.addView(c, lp);
        return c;
    }

    private void info(LinearLayout c, String label, String value, int color) {
        TextView l = text(label, 12, MUTED, false);
        c.addView(l);

        TextView v = text(value, 14, color, true);
        v.setTextIsSelectable(true);
        v.setPadding(0, dp(3), 0, dp(14));
        c.addView(v);
    }

    private boolean exists(String path) {
        return new File(root, path).isFile();
    }

    private String androidGlRenderer() {
        EGL10 egl = null;
        EGLDisplay display = EGL10.EGL_NO_DISPLAY;
        EGLSurface surface = EGL10.EGL_NO_SURFACE;
        EGLContext context = EGL10.EGL_NO_CONTEXT;

        try {
            egl = (EGL10) EGLContext.getEGL();
            display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            if (display == EGL10.EGL_NO_DISPLAY ||
                !egl.eglInitialize(display, new int[2])) {
                return "EGL initialization unavailable";
            }

            int[] attrs = {
                0x3024, 8, 0x3023, 8, 0x3022, 8,
                0x3021, 8, 0x3040, 4, 0x3038
            };

            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];

            if (!egl.eglChooseConfig(display, attrs, configs, 1, count)
                || count[0] < 1) {
                return "No compatible EGL configuration";
            }

            int[] pbufferAttrs = {
                0x3057, 1, 0x3056, 1, 0x3038
            };

            surface = egl.eglCreatePbufferSurface(
                display, configs[0], pbufferAttrs);

            int[] contextAttrs = {0x3098, 2, 0x3038};
            context = egl.eglCreateContext(
                display, configs[0], EGL10.EGL_NO_CONTEXT, contextAttrs);

            if (surface == EGL10.EGL_NO_SURFACE ||
                context == EGL10.EGL_NO_CONTEXT) {
                return "Unable to create OpenGL context";
            }

            if (!egl.eglMakeCurrent(display, surface, surface, context)) {
                return "Unable to activate OpenGL context";
            }

            GL10 gl = (GL10) context.getGL();
            String vendor = gl.glGetString(GL10.GL_VENDOR);
            String rendererName = gl.glGetString(GL10.GL_RENDERER);
            String version = gl.glGetString(GL10.GL_VERSION);

            return (rendererName == null ? "Unknown renderer" : rendererName)
                + "\nVendor: " + (vendor == null ? "Unknown" : vendor)
                + "\nOpenGL: " + (version == null ? "Unknown" : version);

        } catch (Throwable e) {
            return "Android OpenGL query failed: " + e.getClass().getSimpleName();
        } finally {
            if (egl != null && display != EGL10.EGL_NO_DISPLAY) {
                try {
                    egl.eglMakeCurrent(display,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);

                    if (surface != EGL10.EGL_NO_SURFACE) {
                        egl.eglDestroySurface(display, surface);
                    }
                    if (context != EGL10.EGL_NO_CONTEXT) {
                        egl.eglDestroyContext(display, context);
                    }
                    egl.eglTerminate(display);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private String readSmallFile(File file) {
        if (!file.isFile()) return null;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            StringBuilder out = new StringBuilder();
            String line;
            int lines = 0;

            while ((line = reader.readLine()) != null && lines++ < 12) {
                if (out.length() > 0) out.append('\n');
                out.append(line);
            }
            return out.toString();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String lastGpuLog() {
        String[] paths = {
            "home/user/.gass-gpu.log",
            "root/.gass-gpu.log"
        };

        for (String path : paths) {
            String content = readSmallFile(new File(root, path));
            if (content != null && !content.trim().isEmpty()) return content;
        }

        return "No desktop GPU log found yet. Start XFCE once to create it.";
    }

    private String recommendation(String androidRenderer) {
        String r = androidRenderer.toLowerCase(Locale.ROOT);

        if (!new File(root, "usr/bin/glxinfo").isFile()) {
            return "Install or verify mesa-utils in Debian first. " +
                "Without glxinfo, the desktop renderer cannot be checked.";
        }

        if (r.contains("llvmpipe") || r.contains("softpipe")) {
            return "Android's current OpenGL context reports software rendering. " +
                "This does not prove Android Vulkan is software-rendered. " +
                "Keep Auto selected and check Vulkan and Linux drivers separately.";
        }

        if (exists("usr/share/vulkan/icd.d/freedreno_icd.aarch64.json") &&
            exists("usr/lib/aarch64-linux-gnu/dri/zink_dri.so")) {
            return "Zink's Vulkan ICD and DRI driver appear to be installed. " +
                "Auto is the safest starting point. Test Zink in the actual " +
                "Linux desktop and inspect GL_RENDERER before keeping it.";
        }

        if (exists("usr/lib/aarch64-linux-gnu/dri/freedreno_dri.so")) {
            return "A Freedreno DRI driver file appears to be installed. " +
                "Keep Auto initially; the current X11 startup script must " +
                "successfully test the renderer before hardware acceleration " +
                "can be confirmed.";
        }

        return "Keep Auto selected initially. Android GPU detection alone " +
            "cannot confirm that Debian can access the GPU. Check the Linux " +
            "driver files and the renderer reported by glxinfo.";
    }

    private void addBackend(LinearLayout c, String name,
                            String description, boolean available) {
        info(c, name, (available ? "File detected" : "Not detected")
            + " — " + description, available ? GREEN : MUTED);
    }

    private void buildPage() {
        page.removeAllViews();

        TextView back = text("\u2039   GPU Settings", 24, ORANGE, true);
        back.setPadding(0, 0, 0, dp(8));
        back.setOnClickListener(v -> finish());
        page.addView(back);

        TextView subtitle = text(
            "Hardware detection, Linux renderer selection and diagnostics",
            13, MUTED, false);
        subtitle.setPadding(0, 0, 0, dp(8));
        page.addView(subtitle);

        String gl = androidGlRenderer();

        section("ANDROID HARDWARE");
        LinearLayout device = card();
        info(device, "Device", Build.MANUFACTURER + " " + Build.MODEL, ORANGE);
        info(device, "Android", Build.VERSION.RELEASE +
            " (API " + Build.VERSION.SDK_INT + ")", BLUE);
        info(device, "CPU ABI", android.text.TextUtils.join(
            ", ", Build.SUPPORTED_ABIS), BLUE);
        info(device, "CPU cores",
            String.valueOf(Runtime.getRuntime().availableProcessors()), BLUE);
        info(device, "Android OpenGL renderer", gl, GREEN);

        section("DEBIAN GRAPHICS COMPONENTS");
        LinearLayout drivers = card();

        boolean debian = new File(root, "usr/bin/bash").isFile();
        info(drivers, "Debian root filesystem",
            debian ? "Installed" : "Not installed", debian ? GREEN : RED);

        addBackend(drivers, "glxinfo",
            "Linux renderer diagnostic",
            exists("usr/bin/glxinfo"));

        addBackend(drivers, "Zink DRI",
            "OpenGL over Vulkan",
            exists("usr/lib/aarch64-linux-gnu/dri/zink_dri.so"));

        addBackend(drivers, "Freedreno DRI",
            "Mesa Qualcomm driver",
            exists("usr/lib/aarch64-linux-gnu/dri/freedreno_dri.so"));

        addBackend(drivers, "Freedreno Vulkan ICD",
            "Vulkan device driver manifest",
            exists("usr/share/vulkan/icd.d/freedreno_icd.aarch64.json"));

        addBackend(drivers, "VirGL",
            "Virtual GPU backend; needs a compatible host/server",
            exists("usr/bin/virgl_test_server") ||
            exists("usr/lib/aarch64-linux-gnu/dri/virgl_dri.so"));

        boolean angle =
            exists("usr/lib/aarch64-linux-gnu/libEGL_angle.so") ||
            exists("usr/lib/aarch64-linux-gnu/libGLESv2_angle.so");

        addBackend(drivers, "ANGLE",
            "Translation layer; requires compatible ANGLE libraries",
            angle);

        section("DESKTOP RENDERER");
        LinearLayout selection = card();

        TextView label = text("Renderer preference", 14, TEXT, true);
        selection.addView(label);

        renderer = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, gpuLabels);
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        renderer.setAdapter(adapter);

        String saved = Cfg.sp(this).getString("gpu_mode", "auto");
        int index = 0;
        for (int i = 0; i < gpuValues.length; i++) {
            if (gpuValues[i].equals(saved)) index = i;
        }
        renderer.setSelection(index);
        selection.addView(renderer);

        TextView note = text(
            "This saves a preference for the next desktop start. " +
            "It does not install drivers or guarantee hardware acceleration.",
            12, MUTED, false);
        note.setPadding(0, dp(8), 0, dp(12));
        selection.addView(note);

        Button save = new Button(this);
        save.setText("Save renderer preference");
        save.setOnClickListener(v -> {
            int pos = renderer.getSelectedItemPosition();
            if (pos < 0 || pos >= gpuValues.length) pos = 0;

            Cfg.sp(this).edit()
                .putString("gpu_mode", gpuValues[pos])
                .apply();

            Toast.makeText(this,
                "Saved. Restart the Linux desktop to apply it.",
                Toast.LENGTH_LONG).show();
        });
        selection.addView(save);

        section("RECOMMENDATION");
        LinearLayout advice = card();
        info(advice, "Suggested next step", recommendation(gl), BLUE);

        section("LAST RECORDED GPU MODE");
        LinearLayout logCard = card();
        info(logCard, "Desktop startup log", lastGpuLog(), MUTED);

        TextView disclaimer = text(
            "Important: the Android OpenGL renderer is not the same as " +
            "the Linux desktop renderer. The last recorded GPU mode is " +
            "only the startup selection. To verify the active Linux " +
            "renderer, run glxinfo -B inside the Debian desktop and " +
            "inspect the OpenGL renderer string.",
            13, MUTED, false);
        disclaimer.setLineSpacing(dp(3), 1.0f);
        disclaimer.setPadding(dp(2), dp(18), dp(2), dp(12));
        page.addView(disclaimer);

        Button systemInfo = new Button(this);
        systemInfo.setText("Open full System Information");
        systemInfo.setOnClickListener(v ->
            startActivity(new Intent(this, SystemInfoActivity.class)));
        page.addView(systemInfo);

        Button refresh = new Button(this);
        refresh.setText("Refresh GPU diagnostics");
        refresh.setOnClickListener(v -> buildPage());
        LinearLayout.LayoutParams refreshLp =
            new LinearLayout.LayoutParams(-1, -2);
        refreshLp.topMargin = dp(8);
        page.addView(refresh, refreshLp);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        root = Cfg.rootDir(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(20), dp(20), dp(28));

        scroll.addView(page);
        setContentView(scroll);
        buildPage();
    }
}
