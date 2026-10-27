package com.gass.gassnative;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.droidnative.display.Compositor;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class DesktopActivity extends Activity
        implements SurfaceHolder.Callback {

    private Compositor compositor;
    private SurfaceView view;
    private EditText command;
    private TextView status;
    private TextView output;
    private File runDir;
    private File root;
    private String nativeDir;
    private volatile Process guest;
    private volatile Process appGuest;
    private volatile Process audioServer;
    private File audioRuntimeDir;
    private Surface surface;
    private TextView tpDbg;
    private boolean keyToast;
    private boolean imeDebugToast;
    private int windowId = -1;
    private int surfaceWidth = 1;
    private int surfaceHeight = 1;
    private boolean starting;
private volatile MediaPlayer desktopIntroPlayer;
private volatile boolean desktopIntroCancelled;

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void say(String s) {
        runOnUiThread(() -> {
            if (status != null) status.setText(s);
            if (output != null) output.append(s + "\n");
            android.util.Log.i("GassNativeDesktop", s);
        });
    }

    private String pendingX11Command;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        root = Cfg.rootDir(this); instance = this; prepareWine(getIntent());
        pendingX11Command = getIntent() == null ? null : getIntent().getStringExtra("x11_command");
        writeAutorun(getIntent());
        askStorage(); barH.postDelayed(barTick, 1500); android.widget.Toast.makeText(this, "GassNative build 27", android.widget.Toast.LENGTH_SHORT).show();
        nativeDir = getApplicationInfo().nativeLibraryDir;
        runDir = new File(getFilesDir(), "run");
        runDir.mkdirs(); new File(getCacheDir(),"dnc").mkdirs(); new File(getCacheDir(),"proot").mkdirs();

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Color.rgb(20, 22, 27));

        // Kept for internal status/log reporting; not displayed over the desktop.
        status = new TextView(this);
        status.setTextColor(0xFF9FE7B2);
        status.setPadding(dp(8), dp(4), dp(8), dp(4));
        status.setText("Preparing Linux desktop...");
        col.addView(status);

        view = new SurfaceView(this) {
            @Override public boolean onCheckIsTextEditor() { return true; }

            @Override
            public android.view.inputmethod.InputConnection onCreateInputConnection(
                    android.view.inputmethod.EditorInfo ei) {
                ei.inputType = android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
                ei.imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN
                    | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI;

                return new android.view.inputmethod.BaseInputConnection(this, false) {
                    private String composingText = "";

                    @Override
                    public boolean setComposingText(CharSequence text, int newCursorPosition) {
                        composingText = text == null ? "" : text.toString();
                        return true;
                    }

                    @Override
                    public boolean commitText(CharSequence text, int newCursorPosition) {
                        if (text != null && text.length() > 0) {
                            if (!imeDebugToast) {
                                imeDebugToast = true;
                                runOnUiThread(() -> android.widget.Toast.makeText(
                                    DesktopActivity.this,
                                    "DEBUG: Android keyboard text received",
                                    android.widget.Toast.LENGTH_LONG
                                ).show());
                            }
                            typeText(text);
                        }
                        composingText = "";
                        return true;
                    }

                    @Override
                    public boolean finishComposingText() {
                        // Some Android IMEs finish composition without commitText().
                        // Forward the final composed string once, then clear it.
                        if (!composingText.isEmpty()) {
                            String pending = composingText;
                            composingText = "";
                            typeText(pending);
                        }
                        return true;
                    }

                    @Override
                    public boolean endBatchEdit() {
                        boolean result = super.endBatchEdit();
                        return result;
                    }

                    @Override
                    public boolean commitCompletion(
                            android.view.inputmethod.CompletionInfo text) {
                        if (text != null && text.getText() != null) {
                            typeText(text.getText());
                            composingText = "";
                        }
                        return true;
                    }

                    @Override
                    public boolean deleteSurroundingText(int before, int after) {
                        composingText = "";
                        for (int i = 0; i < Math.max(1, before); i++) {
                            tapKey(14, 0);
                        }
                        return true;
                    }

                    @Override
                    public boolean sendKeyEvent(android.view.KeyEvent ev) {
                        return handleKey(ev) || super.sendKeyEvent(ev);
                    }
                };
            }
        };

        view.getHolder().addCallback(this);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnTouchListener((v, e) -> handleTouch(e));
        col.addView(view, new LinearLayout.LayoutParams(-1, 0, 1));

        output = new TextView(this);
        output.setTextColor(Color.LTGRAY);
        output.setTextSize(12);
        output.setPadding(dp(8), dp(4), dp(8), dp(4));
        ScrollView log = new ScrollView(this);
        log.addView(output);
        col.addView(log, new LinearLayout.LayoutParams(-1, dp(240)));

        col.removeView(view);
        android.widget.FrameLayout rootView = new android.widget.FrameLayout(this);
        rootView.addView(view, new android.widget.FrameLayout.LayoutParams(-1, -1));


        view.setOnTouchListener((v, event) -> handleTouch(event));

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = 1;
            getWindow().setAttributes(lp);
        }
        // Minimal floating controls inside the Linux desktop: Keyboard and Power Off.
        final Button kb = new Button(this); kbBtn = kb;
        kb.setText("\u2328");
        kb.setTextSize(22);
        kb.setAllCaps(false);
        kb.setAlpha(0.78f);
        kb.setContentDescription("Show keyboard");
        kb.setOnClickListener(v -> showKeyboard());

        android.widget.FrameLayout.LayoutParams klp =
            new android.widget.FrameLayout.LayoutParams(
                dp(56), dp(56), Gravity.BOTTOM | Gravity.END);
        klp.setMargins(0, 0, dp(12), dp(12));
        rootView.addView(kb, klp);

        Button powerOff = new Button(this);
        powerOff.setText("Off");
        powerOff.setAllCaps(false);
        powerOff.setAlpha(0.78f);
        powerOff.setContentDescription("Power off desktop");
        powerOff.setOnClickListener(v ->
            new android.app.AlertDialog.Builder(DesktopActivity.this)
                .setTitle("Power off")
                .setMessage("Stop the Linux desktop and close GassNative?")
                .setPositiveButton("Power off", (dialog, which) -> {
                    stopDesktop();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show());

        android.widget.FrameLayout.LayoutParams powerLp =
            new android.widget.FrameLayout.LayoutParams(
                dp(56), dp(56), Gravity.BOTTOM | Gravity.END);
        powerLp.setMargins(0, 0, dp(12), dp(76));
        rootView.addView(powerOff, powerLp);

        final android.widget.FrameLayout sp = new android.widget.FrameLayout(this);
        sp.setBackgroundColor(Color.rgb(14, 16, 22));
        sp.setClickable(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        // GASS_BOOT_LOGO_V1: branded Debian desktop startup
        android.widget.ImageView bootLogo = new android.widget.ImageView(this);
        bootLogo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        bootLogo.setAdjustViewBounds(true);
        try (java.io.InputStream logoStream =
                 getAssets().open("gassnative-square.png")) {
            android.graphics.Bitmap logoBitmap =
                android.graphics.BitmapFactory.decodeStream(logoStream);
            if (logoBitmap != null) {
                bootLogo.setImageBitmap(logoBitmap);
            }
        } catch (Exception e) {
            android.util.Log.w("GassNative", "Boot logo could not be loaded", e);
        }
        LinearLayout.LayoutParams logoLp =
            new LinearLayout.LayoutParams(dp(112), dp(112));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        logoLp.bottomMargin = dp(14);
        box.addView(bootLogo, 0, logoLp);

        TextView ttl = new TextView(this);
        ttl.setText("GassNative");
        ttl.setTextColor(Color.WHITE);
        ttl.setTextSize(34);
        ttl.setTypeface(null, android.graphics.Typeface.BOLD);
        ttl.setGravity(Gravity.CENTER);
        box.addView(ttl, new LinearLayout.LayoutParams(-1, -2));
        TextView by = new TextView(this);
        by.setText("Powered by Aymen Gharsalli");
        by.setTextColor(Color.rgb(150, 160, 175));
        by.setTextSize(14);
        by.setGravity(Gravity.CENTER);
        by.setPadding(0, dp(4), 0, dp(36));
        box.addView(by, new LinearLayout.LayoutParams(-1, -2));
        splashBar = new SplashBar(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(240), dp(6));
        blp.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(splashBar, blp);
        splashMsg = new TextView(this);
        splashMsg.setText("Booting Debian Desktop...");
        splashMsg.setTextColor(Color.rgb(150, 160, 175));
        splashMsg.setTextSize(13);
        splashMsg.setGravity(Gravity.CENTER);
        splashMsg.setPadding(0, dp(14), 0, 0);
        box.addView(splashMsg, new LinearLayout.LayoutParams(-1, -2));
        sp.addView(box, new android.widget.FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        rootView.addView(sp, new android.widget.FrameLayout.LayoutParams(-1, -1));
        splash = sp;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> hideSplash(), 40000);

rootRef = rootView; setContentView(rootView);
        startDesktopIntroAudio();

        // Visible Android pointer following the existing touch-to-mouse input.
        tpCursor = new View(this) {
            private final android.graphics.Paint fill =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            private final android.graphics.Paint outline =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            private final android.graphics.Path arrow = new android.graphics.Path();

            {
                fill.setColor(Color.WHITE);
                outline.setColor(Color.BLACK);
                outline.setStyle(android.graphics.Paint.Style.STROKE);
                outline.setStrokeWidth(1.8f);
                arrow.moveTo(2, 2);
                arrow.lineTo(2, 27);
                arrow.lineTo(8, 21);
                arrow.lineTo(13, 32);
                arrow.lineTo(18, 29);
                arrow.lineTo(13, 18);
                arrow.lineTo(23, 18);
                arrow.close();
            }

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                canvas.drawPath(arrow, fill);
                canvas.drawPath(arrow, outline);
            }
        };
        tpCursor.setClickable(false);
        tpCursor.setFocusable(false);
        tpCursor.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        tpCursor.setElevation(dp(30));
        rootView.addView(tpCursor,
            new android.widget.FrameLayout.LayoutParams(dp(28), dp(38)));
        tpCursor.setX(tpCx);
        tpCursor.setY(tpCy);
        tpCursor.setVisibility(View.VISIBLE);


        tpDbg = new TextView(this);
        tpDbg.setTextColor(Color.YELLOW);
        tpDbg.setBackgroundColor(0x88000000);
        tpDbg.setTextSize(12);
        tpDbg.setText("touchMode 2");
        rootView.addView(tpDbg, new android.widget.FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START));
        hideSystemUi();
    }

    private static final String GASS_SESSION = "#!/bin/bash\nexport DISPLAY=:1 XDG_SESSION_TYPE=x11 GDK_BACKEND=x11 QT_QPA_PLATFORM=xcb\nexport XDG_RUNTIME_DIR=/tmp/runtime-user\nmkdir -p $XDG_RUNTIME_DIR; chmod 700 $XDG_RUNTIME_DIR\nexport MOZ_DISABLE_CONTENT_SANDBOX=1 MOZ_DISABLE_GMP_SANDBOX=1 MOZ_DISABLE_RDD_SANDBOX=1 MOZ_DISABLE_SOCKET_PROCESS_SANDBOX=1 MOZ_ENABLE_WAYLAND=0\n(exec 3<>/dev/tcp/127.0.0.1/4713) 2>/dev/null && export PULSE_SERVER=127.0.0.1\nexport TU_DEBUG=noconform MESA_NO_ERROR=1 vblank_mode=0 mesa_glthread=true MESA_VK_WSI_PRESENT_MODE=mailbox\nGASS_EXTRA_ENV_BLOCK\nICD=/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json\nXM=$(cat /tmp/gass-xmode 2>/dev/null || echo shm)\nCACHE=$HOME/.gass-gpu-$XM\nPREF=GASS_GPU_PREFERENCE\nmode_env() {\n  case $1 in\n    freedreno) echo \"MESA_LOADER_DRIVER_OVERRIDE=kgsl TURNIP_KMD=kgsl FD_FORCE_KGSL=1 GALLIUM_DRIVER=freedreno\" ;;\n    zink) echo \"VK_ICD_FILENAMES=$ICD MESA_LOADER_DRIVER_OVERRIDE=zink GALLIUM_DRIVER=zink\" ;;\n    *) echo \"LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe\" ;;\n  esac\n}\nworks() {\n  env $(mode_env $1) timeout 10 glxinfo -B 2>/dev/null | grep -i \"renderer string\" | grep -qiE \"adreno|zink|freedreno|turnip\"\n}\necho \"GASS-STATUS Selecting GPU mode\"\nMODE=llvmpipe\ncase \"$PREF\" in\n  zink|freedreno|llvmpipe) MODE=$PREF ;;\n  *)\n    if [ \"$XM\" = glamor ]; then\n      if [ -r \"$CACHE\" ]; then\n        cached=$(cat \"$CACHE\")\n        case \"$cached\" in zink|freedreno|llvmpipe) MODE=$cached ;; esac\n      elif command -v glxinfo >/dev/null 2>&1; then\n        for m in zink freedreno; do\n          if works $m; then MODE=$m; break; fi\n        done\n        echo \"$MODE\" > \"$CACHE\"\n      fi\n    fi\n    ;;\nesac\necho \"X server: $XM, GPU mode: $MODE\" > $HOME/.gass-gpu.log\necho \"GASS-STATUS GPU: $MODE - starting XFCE\"\nexport $(mode_env $MODE)\nif command -v xclip >/dev/null 2>&1; then\n  ( lp=\"\"; lc=\"\"\n    while true; do\n      a=$(timeout 2 xclip -o -selection primary 2>/dev/null | base64 -w0)\n      if [ -n \"$a\" ] && [ \"$a\" != \"$lp\" ]; then lp=$a; echo \"GASS-CLIP $a\"; fi\n      b=$(timeout 2 xclip -o -selection clipboard 2>/dev/null | base64 -w0)\n      if [ -n \"$b\" ] && [ \"$b\" != \"$lc\" ]; then lc=$b; echo \"GASS-CLIP $b\"; fi\n      sleep 1\n    done ) &\nfi\n( for i in $(seq 1 60); do\n    if pgrep -x xfce4-panel >/dev/null 2>&1; then sleep 1; echo GASS-READY; break; fi\n    sleep 1\n  done ) &\n( while true; do\n    if [ -s $HOME/.gass-autorun ] && pgrep -x xfce4-panel >/dev/null 2>&1; then\n      c=$(cat $HOME/.gass-autorun); rm -f $HOME/.gass-autorun\n      ( export DISPLAY=:1; unset WAYLAND_DISPLAY; sh -c \"$c\" >/tmp/gass-autorun.log 2>&1 ) &\n    fi\n    sleep 1\n  done ) &\nexec dbus-launch --exit-with-session startxfce4\n";
    static class SplashBar extends View {
        final android.graphics.Paint pt = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float prog = 0f;
        boolean done = false;
        SplashBar(android.content.Context c) { super(c); }
        @Override protected void onDraw(android.graphics.Canvas cv) {
            float w = getWidth(), h = getHeight();
            pt.setColor(Color.rgb(40, 46, 58));
            cv.drawRoundRect(0, 0, w, h, h / 2, h / 2, pt);
            prog += ((done ? 1f : 0.93f) - prog) * (done ? 0.12f : 0.006f);
            pt.setColor(Color.rgb(90, 170, 255));
            cv.drawRoundRect(0, 0, Math.max(h, w * prog), h, h / 2, h / 2, pt);
            invalidate();
        }
    }
    private SplashBar splashBar;
    private boolean splashClosing;
    private View splash;
    private TextView splashMsg;

    private void installSession() {
        try {
            File f = new File(root, "usr/local/bin/gass-session");
            f.getParentFile().mkdirs();
            String gpuMode = Cfg.sp(this).getString("gpu_mode", "auto");
            if (!"auto".equals(gpuMode) &&
                !"zink".equals(gpuMode) &&
                !"freedreno".equals(gpuMode) &&
                !"llvmpipe".equals(gpuMode)) {
                gpuMode = "auto";
            }
            StringBuilder customEnv = new StringBuilder();
            for (String line : Cfg.get(this, "env", "").split("\\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq);
                String value = line.substring(eq + 1);
                if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) continue;
                customEnv.append("export ").append(key).append("=")
                    .append(Cfg.q(value)).append("\\n");
            }
            java.io.FileWriter w = new java.io.FileWriter(f);
            w.write(GASS_SESSION
                .replace("GASS_GPU_PREFERENCE", gpuMode)
                .replace("GASS_EXTRA_ENV_BLOCK", customEnv.toString()));
            w.close();
            f.setExecutable(true, false);
        } catch (Throwable t) {
            android.util.Log.e("GassNativeDesktop", "session install failed", t);
        }
    }


    private void startDesktopIntroAudio() {
        if (!Cfg.sp(this).getBoolean("intro_sound", true)) return;

        desktopIntroCancelled = false;

        // Copy from APK assets off the UI thread; compressed APK assets
        // cannot reliably be opened with AssetManager.openFd().
        new Thread(() -> {
            File audioFile = new File(getCacheDir(), "gassnative_intro.mp3");
            try (
                java.io.InputStream in = getAssets().open("audio/gassnative_intro.mp3");
                java.io.FileOutputStream out = new java.io.FileOutputStream(audioFile)
            ) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    if (desktopIntroCancelled) return;
                    out.write(buffer, 0, count);
                }
                out.flush();
            } catch (Throwable e) {
                android.util.Log.w("GassNativeDesktop",
                    "Intro audio unavailable: assets/audio/gassnative_intro.mp3", e);
                return;
            }

            runOnUiThread(() -> {
                View sp = splash;
                if (desktopIntroCancelled || isFinishing() || isDestroyed()
                        || splashClosing || sp == null
                        || sp.getVisibility() != View.VISIBLE
                        || !Cfg.sp(this).getBoolean("intro_sound", true)) {
                    return;
                }

                MediaPlayer player = new MediaPlayer();
                try {
                    player.setDataSource(audioFile.getAbsolutePath());
                    desktopIntroPlayer = player;

                    player.setOnPreparedListener(mp -> {
                        if (desktopIntroPlayer != mp || desktopIntroCancelled
                                || splashClosing || isFinishing()) {
                            stopDesktopIntroAudio();
                            return;
                        }
                        try {
                            mp.start();
                        } catch (Throwable e) {
                            android.util.Log.w("GassNativeDesktop",
                                "Could not start intro audio", e);
                            stopDesktopIntroAudio();
                        }
                    });

                    player.setOnCompletionListener(mp -> {
                        if (desktopIntroPlayer == mp) stopDesktopIntroAudio();
                    });

                    player.setOnErrorListener((mp, what, extra) -> {
                        android.util.Log.w("GassNativeDesktop",
                            "Intro playback error: " + what + "/" + extra);
                        if (desktopIntroPlayer == mp) stopDesktopIntroAudio();
                        return true;
                    });

                    player.prepareAsync();
                } catch (Throwable e) {
                    android.util.Log.w("GassNativeDesktop",
                        "Could not prepare intro audio", e);
                    if (desktopIntroPlayer == player) desktopIntroPlayer = null;
                    try { player.release(); } catch (Throwable ignored) {}
                }
            });
        }, "GassIntroAudio").start();
    }

    private void stopDesktopIntroAudio() {
        desktopIntroCancelled = true;
        MediaPlayer player = desktopIntroPlayer;
        desktopIntroPlayer = null;
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
            } catch (Throwable ignored) {}
            try { player.release(); } catch (Throwable ignored) {}
        }
    }

    private void hideSplash() {
        final View sp = splash;
        if (sp == null || sp.getVisibility() != View.VISIBLE || splashClosing) return;
        splashClosing = true;
        stopDesktopIntroAudio();
        if (splashBar != null) splashBar.done = true;
        sp.postDelayed(() -> sp.animate().alpha(0f).setDuration(500).withEndAction(() -> sp.setVisibility(View.GONE)).start(), 700);
    }

    private boolean autoStarted;
    private static final String XFCE_CMD = "/usr/local/bin/gass-start";

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    private static final String GPU_LAUNCHER = "#!/bin/sh\nfor v in DISPLAY LIBGL_ALWAYS_SOFTWARE LIBGL_KOPPER_DISABLE GALLIUM_DRIVER MESA_LOADER_DRIVER_OVERRIDE EGL_PLATFORM TURNIP_KMD FD_FORCE_KGSL XWAYLAND_FORCE_KGSL_SURFACELESS; do unset $v; done\nexport WAYLAND_DISPLAY=/run/droidnative/wayland-0 XDG_RUNTIME_DIR=/tmp/runtime-user\nmkdir -p $XDG_RUNTIME_DIR; chmod 700 $XDG_RUNTIME_DIR\nexport VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json\nexport MESA_LOADER_DRIVER_OVERRIDE=zink GALLIUM_DRIVER=zink TURNIP_KMD=kgsl FD_FORCE_KGSL=1 TU_DEBUG=noconform MESA_VK_WSI_PRESENT_MODE=mailbox\nexport GDK_BACKEND=wayland QT_QPA_PLATFORM=wayland SDL_VIDEODRIVER=wayland\nexec \"$@\"\n";
    private final java.util.ArrayList<Integer> wins = new java.util.ArrayList<>();

    private void installGpuLauncher() {
        try {
            File f = new File(root, "usr/local/bin/gpu");
            f.getParentFile().mkdirs();
            java.io.FileWriter w = new java.io.FileWriter(f);
            w.write(GPU_LAUNCHER);
            w.close();
            f.setExecutable(true, false);
        } catch (Throwable t) {
            android.util.Log.e("GassNativeDesktop", "gpu launcher failed", t);
        }
    }

    private String shmBind() {
        File d = new File(getFilesDir(), "shm");
        d.mkdirs();
        d.setReadable(true, false); d.setWritable(true, false); d.setExecutable(true, false);
        new File(root, "dev/shm").mkdirs();
        return " -b " + d.getAbsolutePath() + ":/dev/shm";
    }

    private String sdBinds() {
        String[] cands = {
            android.os.Environment.getExternalStorageDirectory().getAbsolutePath(),
            "/storage/emulated/0", "/storage/self/primary", "/sdcard" };
        String path = null;
        int count = 0;
        for (String c : cands) {
            String[] n = new File(c).list();
            if (n != null && n.length > 0) { path = c; count = n.length; break; }
        }
        boolean mgr = false;
        try { mgr = (Boolean) android.os.Environment.class.getMethod("isExternalStorageManager").invoke(null); } catch (Throwable ignored) {}
        final String msg = path != null
            ? "Storage OK: " + count + " items at " + path + " (all-files=" + mgr + ")"
            : "Storage EMPTY or denied (all-files=" + mgr + "). Allow All files access, force-stop the app, reopen.";
        runOnUiThread(() -> android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show());
        if (path == null) return "";
        new File(root, "sdcard").mkdirs();
        new File(root, "home/user/Download").mkdirs();
        String out = " -b " + path + ":/sdcard";
        if (new File(path, "Download").exists()) out += " -b " + path + "/Download:/home/user/Download";
        return out;
    }

    private void askStorage() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                boolean ok = (Boolean) android.os.Environment.class.getMethod("isExternalStorageManager").invoke(null);
                if (!ok) {
                    startActivity(new android.content.Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
                        android.net.Uri.parse("package:" + getPackageName())));
                }
            } else if (android.os.Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE"}, 1);
            }
        } catch (Throwable t) {
            android.util.Log.e("GassNativeDesktop", "storage permission", t);
        }
    }

    private void switchWindow() {
        if (!floats.isEmpty()) { say("Use the floating windows"); return; }
        if (compositor == null || wins.size() < 2) { say("Only one Linux window is open."); return; }
        int next = wins.get((Math.max(0, wins.indexOf(windowId)) + 1) % wins.size());
        if (windowId >= 0) compositor.detach(windowId);
        windowId = next;
        attachSurface();
        say("Showing window " + next + " of " + wins.size());
    }


    private void copyAudioAssetTree(String assetPath, File dest)
            throws java.io.IOException {
        String[] children = getAssets().list(assetPath);
        if (children != null && children.length > 0) {
            if (!dest.exists() && !dest.mkdirs())
                throw new java.io.IOException("Cannot create " + dest);
            for (String child : children) {
                copyAudioAssetTree(assetPath + "/" + child,
                    new File(dest, child));
            }
            return;
        }

        File parent = dest.getParentFile();
        if (parent != null) parent.mkdirs();
        try (InputStream in = getAssets().open(assetPath);
             java.io.FileOutputStream out =
                 new java.io.FileOutputStream(dest)) {
            byte[] buf = new byte[32768];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    private synchronized boolean startAudioServer() {
        try {
            if (audioServer != null && audioServer.isAlive()) return true;

            audioRuntimeDir = new File(getFilesDir(), "audio-runtime");
            File marker = new File(audioRuntimeDir, ".installed-v1");
            if (!marker.isFile()) {
                if (audioRuntimeDir.exists()) {
                    File[] old = audioRuntimeDir.listFiles();
                    if (old != null) {
                        for (File f : old) deleteAudioTree(f);
                    }
                }
                copyAudioAssetTree("audio/bin",
                    new File(audioRuntimeDir, "bin"));
                copyAudioAssetTree("audio/lib",
                    new File(audioRuntimeDir, "lib"));
                copyAudioAssetTree("audio/default.pa",
                    new File(audioRuntimeDir, "default.pa"));
                new File(audioRuntimeDir, "bin/pulseaudio")
                    .setExecutable(true, true);
                marker.createNewFile();
            }

            File bin = new File(audioRuntimeDir, "bin/pulseaudio");
            File libs = new File(audioRuntimeDir, "lib");
            File modules = new File(libs, "pulseaudio/modules");
            File config = new File(audioRuntimeDir, "default.pa");
            File runtime = new File(getFilesDir(), "audio-run");
            runtime.mkdirs();

            File log = new File(getFilesDir(), "pulseaudio.log");
            ProcessBuilder pb = new ProcessBuilder(
                bin.getAbsolutePath(),
                "-n", "--daemonize=no", "--use-pid-file=no",
                "--exit-idle-time=-1",
                "--dl-search-path=" + modules.getAbsolutePath(),
                "--file=" + config.getAbsolutePath()
            );
            java.util.Map<String, String> env = pb.environment();
            env.put("HOME", getFilesDir().getAbsolutePath());
            env.put("LD_LIBRARY_PATH",
                libs.getAbsolutePath() + ":" + modules.getAbsolutePath());
            env.put("PULSE_RUNTIME_PATH", runtime.getAbsolutePath());
            pb.redirectErrorStream(true);
            pb.redirectOutput(log);
            audioServer = pb.start();

            Thread.sleep(500);
            if (!audioServer.isAlive()) {
                android.util.Log.e("GassNativeAudio",
                    "PulseAudio exited; inspect " + log);
                audioServer = null;
                return false;
            }
            android.util.Log.i("GassNativeAudio",
                "Bundled PulseAudio started on port 4714");
            return true;
        } catch (Throwable t) {
            android.util.Log.e("GassNativeAudio",
                "Audio startup failed", t);
            return false;
        }
    }

    private void deleteAudioTree(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null)
                for (File child : children) deleteAudioTree(child);
        }
        f.delete();
    }

    private synchronized void stopAudioServer() {
        Process p = audioServer;
        audioServer = null;
        if (p != null) {
            p.destroy();
            try {
                if (!p.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS))
                    p.destroyForcibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                p.destroyForcibly();
            }
        }
    }

    private static final String GASS_START = "#!/bin/bash\necho \"GASS-STATUS Preparing desktop\"\npkill -9 -x Xwayland 2>/dev/null; pkill -9 -f wineserver 2>/dev/null\nmkdir -p /etc/sudoers.d; echo 'user ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/user; chmod 440 /etc/sudoers.d/user\nif [ ! -s /root/gass.xkb ]; then\n  echo \"GASS-STATUS Creating keyboard layout\"\n  command -v xkbcomp >/dev/null 2>&1 || { apt-get update >/dev/null 2>&1; apt-get install -y x11-xkb-utils >/dev/null 2>&1; }\n  echo 'xkb_keymap { xkb_keycodes { include \"evdev+aliases(qwerty)\" }; xkb_types { include \"complete\" }; xkb_compat { include \"complete\" }; xkb_symbols { include \"pc+us+inet(evdev)\" }; };' | xkbcomp -w 0 -xkb - /root/gass.xkb 2>/dev/null\nfi\n[ -s /root/gass.xkb ] && echo \"GASS-KEYMAP /root/gass.xkb\"\necho 'Xft.dpi: 192' > /root/.Xresources\ncp -f /root/.Xresources /home/user/.Xresources 2>/dev/null\nmkdir -p /usr/lib/firefox-esr/defaults/pref /home/user/.config/vlc\ncat > /usr/lib/firefox-esr/defaults/pref/gass.js <<'E'\npref(\"security.sandbox.content.level\", 0);\npref(\"media.hardware-video-decoding.enabled\", false);\npref(\"media.ffmpeg.vaapi.enabled\", false);\npref(\"layers.acceleration.disabled\", true);\npref(\"gfx.webrender.software\", true);\nE\n[ -e /home/user/.config/vlc/vlcrc ] || printf '[avcodec]\\navcodec-hw=none\\n[core]\\nvout=x11\\n' > /home/user/.config/vlc/vlcrc\ncat > /usr/local/bin/gpu-run <<'E'\n#!/bin/sh\n${GPU_CMD:-/usr/local/bin/gpu} \"$@\" 2>&1 | tee /home/user/gpu-last.log\necho\necho \"=== finished - log saved in ~/gpu-last.log ===\"\nE\ncat > /usr/local/bin/gpu-gl <<'E'\n#!/bin/sh\nfor v in DISPLAY LIBGL_ALWAYS_SOFTWARE LIBGL_KOPPER_DISABLE GALLIUM_DRIVER MESA_LOADER_DRIVER_OVERRIDE EGL_PLATFORM TURNIP_KMD FD_FORCE_KGSL XWAYLAND_FORCE_KGSL_SURFACELESS; do unset $v; done\nexport WAYLAND_DISPLAY=/run/droidnative/wayland-0 XDG_RUNTIME_DIR=/tmp/runtime-user\nmkdir -p $XDG_RUNTIME_DIR; chmod 700 $XDG_RUNTIME_DIR\nexport MESA_LOADER_DRIVER_OVERRIDE=kgsl TURNIP_KMD=kgsl GALLIUM_DRIVER=freedreno FD_FORCE_KGSL=1\nexport GDK_BACKEND=wayland QT_QPA_PLATFORM=wayland SDL_VIDEODRIVER=wayland\nexec \"$@\"\nE\nchmod +x /usr/local/bin/gpu-run /usr/local/bin/gpu-gl\nD=/home/user/.local/share/applications; mkdir -p $D\nprintf '[Desktop Entry]\\nType=Application\\nName=GPU glmark2\\nExec=xfce4-terminal --hold -x /usr/local/bin/gpu-run glmark2-wayland\\nTerminal=false\\nCategories=System;\\n' > $D/gpu-glmark2.desktop\nprintf '[Desktop Entry]\\nType=Application\\nName=GPU glmark2 (freedreno GL)\\nExec=xfce4-terminal --hold -x env GPU_CMD=/usr/local/bin/gpu-gl /usr/local/bin/gpu-run glmark2-wayland\\nTerminal=false\\nCategories=System;\\n' > $D/gpu-glmark2-gl.desktop\nprintf '[Desktop Entry]\\nType=Application\\nName=GPU Terminal (foot)\\nExec=/usr/local/bin/gpu foot\\nTerminal=false\\nCategories=System;\\n' > $D/gpu-terminal.desktop\nchown -R user:user /home/user/.local /home/user/.config 2>/dev/null\ncat > /usr/local/bin/firefox-safe <<'E'\n#!/bin/sh\nunset WAYLAND_DISPLAY\nexport DISPLAY=:1 GDK_BACKEND=x11 MOZ_ENABLE_WAYLAND=0 MOZ_DISABLE_CONTENT_SANDBOX=1 MOZ_DISABLE_GMP_SANDBOX=1 MOZ_DISABLE_RDD_SANDBOX=1 MOZ_DISABLE_SOCKET_PROCESS_SANDBOX=1 MOZ_ACCELERATED=0 LIBGL_ALWAYS_SOFTWARE=1 MOZ_CRASHREPORTER_DISABLE=1\nexec firefox-esr --no-remote --new-instance \"$@\"\nE\ncat > /usr/local/bin/vlc-safe <<'E'\n#!/bin/sh\nunset WAYLAND_DISPLAY\nexport DISPLAY=:1\nA=dummy; [ -n \"$PULSE_SERVER\" ] && A=pulse\nexec vlc --vout=x11 --aout=$A --avcodec-hw=none \"$@\"\nE\nchmod +x /usr/local/bin/firefox-safe /usr/local/bin/vlc-safe\nD=/home/user/.local/share/applications; mkdir -p $D\nprintf '[Desktop Entry]\\nType=Application\\nName=Firefox (safe)\\nExec=/usr/local/bin/firefox-safe\\nTerminal=false\\nCategories=Network;\\n' > $D/firefox-safe.desktop\nprintf '[Desktop Entry]\\nType=Application\\nName=VLC (safe)\\nExec=/usr/local/bin/vlc-safe\\nTerminal=false\\nCategories=AudioVideo;\\n' > $D/vlc-safe.desktop\nchown -R user:user /home/user/.local 2>/dev/null\nif [ \"$1\" = wine ]; then\n  if [ \"$(cat /home/user/.gass-wine-mode 2>/dev/null)\" != wayland ]; then\n    /usr/local/bin/gass-xserver\n    export DISPLAY=:1\n    xhost +local: >/dev/null 2>&1\n  fi\n  unset WAYLAND_DISPLAY\n  exec su user -c /usr/local/bin/gass-wine\nfi\n/usr/local/bin/gass-xserver\nexport DISPLAY=:1\nxhost +local: >/dev/null 2>&1\nunset WAYLAND_DISPLAY\nexec su user -c /usr/local/bin/gass-session\n";
    private static final String GASS_XSERVER = "#!/bin/bash\nexport XDG_RUNTIME_DIR=/run/droidnative WAYLAND_DISPLAY=wayland-0\nrm -f /tmp/.X1-lock /tmp/.X11-unix/X1\nLOG=/tmp/gass-xserver.log\n: > $LOG\nwait_x() { for i in $(seq 1 12); do [ -S /tmp/.X11-unix/X1 ] && return 0; sleep 0.5; done; return 1; }\nXM=shm\nif [ ! -e /home/user/.gass-safe ] && [ -c /dev/kgsl-3d0 ]; then\n  echo \"GASS-STATUS Starting X server (GPU)\"\n  env MESA_LOADER_DRIVER_OVERRIDE=kgsl TURNIP_KMD=kgsl FD_FORCE_KGSL=1 GALLIUM_DRIVER=freedreno XWAYLAND_FORCE_KGSL_SURFACELESS=1 EGL_PLATFORM=surfaceless TU_DEBUG=noconform Xwayland :1 -fullscreen -noreset -glamor gl >> $LOG 2>&1 &\n  XP=$!\n  if wait_x; then\n    sleep 1\n    if kill -0 $XP 2>/dev/null && command -v xdpyinfo >/dev/null 2>&1 && DISPLAY=:1 timeout 5 xdpyinfo -queryExtensions 2>/dev/null | grep -qi dri3; then XM=glamor; fi\n  fi\n  if [ \"$XM\" != glamor ]; then\n    kill $XP 2>/dev/null; sleep 1; kill -9 $XP 2>/dev/null\n    rm -f /tmp/.X1-lock /tmp/.X11-unix/X1\n  fi\nfi\nif [ \"$XM\" != glamor ]; then\n  echo \"GASS-STATUS Starting X server (software)\"\n  Xwayland :1 -shm -fullscreen -noreset >> $LOG 2>&1 &\n  wait_x\nfi\necho $XM > /tmp/gass-xmode\n";

    private static final String GASS_LABWC = "#!/bin/bash\nLOG=$HOME/labwc-test.log\nfor c in labwc foot glmark2-wayland; do\n  command -v $c >/dev/null 2>&1 || { echo \"Missing $c. Run: sudo apt install -y labwc foot glmark2-wayland\"; exit 1; }\ndone\nPARENT=/run/droidnative/wayland-0\n[ -S $PARENT ] || { echo \"Compositor socket not found: $PARENT\"; exit 1; }\nexport XDG_RUNTIME_DIR=/tmp/runtime-user\nmkdir -p $XDG_RUNTIME_DIR; chmod 700 $XDG_RUNTIME_DIR\ncat > /tmp/labwc-test.sh <<'E'\n#!/bin/bash\nunset LIBGL_ALWAYS_SOFTWARE GALLIUM_DRIVER MESA_LOADER_DRIVER_OVERRIDE LIBGL_KOPPER_DISABLE DISPLAY\necho \"== nested labwc test ==\"\necho \"WAYLAND_DISPLAY=$WAYLAND_DISPLAY\"\necho\necho \"-- software baseline\"\nLIBGL_ALWAYS_SOFTWARE=1 glmark2-wayland --benchmark build:duration=1 2>&1 | grep -i -E \"renderer|score|error|fail\" | head -5\necho\necho \"-- zink on Turnip (GPU)\"\nVK_ICD_FILENAMES=/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json MESA_LOADER_DRIVER_OVERRIDE=zink GALLIUM_DRIVER=zink TU_DEBUG=noconform glmark2-wayland --benchmark build:duration=1 2>&1 | grep -i -E \"renderer|score|error|fail\" | head -6\necho\necho \"Finished. Close this window when you are done.\"\nsleep 3600\nE\nchmod +x /tmp/labwc-test.sh\nunset DISPLAY\nexport WAYLAND_DISPLAY=$PARENT\nexport WLR_BACKENDS=wayland WLR_RENDERER=pixman WLR_NO_HARDWARE_CURSORS=1 XDG_SESSION_TYPE=wayland\necho \"Starting nested labwc, log in $LOG\"\nexec labwc -s \"foot -e /tmp/labwc-test.sh\" > $LOG 2>&1\n";

    private static final String GASS_WINE = "#!/bin/bash\nexport XDG_RUNTIME_DIR=/tmp/runtime-user\nmkdir -p $XDG_RUNTIME_DIR; chmod 700 $XDG_RUNTIME_DIR\nexport WINEPREFIX=/home/user/.gass-wine WINEDEBUG=fixme-all,err+all\ncommand -v wine >/dev/null 2>&1 || { echo \"GASS-STATUS Wine is not installed\"; sleep 4; echo GASS-EXIT; exit 1; }\nif command -v pulseaudio >/dev/null 2>&1; then\n  unset PULSE_SERVER\n  cat > $HOME/.gass.pa <<'E'\nload-module module-native-protocol-unix\nload-module module-pipe-sink file=/run/droidnative/audio.pcm sink_name=android format=s16le rate=44100 channels=2\nset-default-sink android\nE\n  pulseaudio --kill 2>/dev/null\n  pulseaudio -n -F $HOME/.gass.pa --daemonize=yes --exit-idle-time=-1 --log-target=file:/tmp/pa.log\nfi\nR=$(cat $HOME/.gass-screen 2>/dev/null); [ -z \"$R\" ] && R=1280x720\nMODE=$(cat $HOME/.gass-wine-mode 2>/dev/null || echo x11)\nif [ \"$MODE\" = wayland ]; then\n  unset DISPLAY\n  export WAYLAND_DISPLAY=/run/droidnative/wayland-0\n  export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json MESA_LOADER_DRIVER_OVERRIDE=zink GALLIUM_DRIVER=zink TU_DEBUG=noconform\n  DRV=wayland,x11\nelse\n  export DISPLAY=:1\n  unset WAYLAND_DISPLAY\n  DRV=x11\nfi\nmkdir -p $WINEPREFIX\n/usr/local/bin/gass-dxvk\nif [ \"$(cat $WINEPREFIX/.gass-driver 2>/dev/null)\" != \"$DRV\" ]; then\n  echo \"GASS-STATUS Preparing Windows environment (first run can take a few minutes)\"\n  wine reg add 'HKCU\\Software\\Wine\\Drivers' /v Graphics /d $DRV /f >/dev/null 2>&1\n  echo \"$DRV\" > $WINEPREFIX/.gass-driver\nfi\nD=$WINEPREFIX/drive_c/users/$(id -un)/Desktop\nmkdir -p \"$D\" 2>/dev/null\n[ -e \"$D/Download\" ] || ln -s /sdcard/Download \"$D/Download\" 2>/dev/null\n[ -d /sdcard/Download/Games ] && { [ -e \"$D/Games\" ] || ln -s /sdcard/Download/Games \"$D/Games\" 2>/dev/null; }\n[ -e \"$D/Phone storage\" ] || ln -s /sdcard \"$D/Phone storage\" 2>/dev/null\nif [ ! -e $WINEPREFIX/.gass-dpi ]; then\n  wine reg add 'HKCU\\Control Panel\\Desktop' /v LogPixels /t REG_DWORD /d 192 /f >/dev/null 2>&1\n  touch $WINEPREFIX/.gass-dpi\nfi\n( for i in $(seq 1 150); do\n    if pgrep -f explorer.exe >/dev/null 2>&1; then sleep 2; echo GASS-READY; break; fi\n    sleep 1\n  done ) &\nRUN=$(cat $HOME/.gass-wine-run 2>/dev/null)\nrm -f $HOME/.gass-wine-run\necho \"GASS-STATUS Starting Windows desktop ($MODE)\"\nif [ -n \"$RUN\" ]; then\n  W=$(winepath -w \"$RUN\" 2>/dev/null); [ -z \"$W\" ] && W=\"$RUN\"\n  cd \"$(dirname \"$RUN\")\"\n  wine explorer /desktop=GassGame,$R \"$W\" 2>&1 | tee $HOME/wine-session.log\nelse\n  wine explorer C:\\\\ 2>&1 | tee $HOME/wine-session.log\nfi\nwineserver -w 2>/dev/null\necho GASS-EXIT\n";

    private static final String GASS_DXVK =
        "#!/bin/sh\n" +
        "set -eu\n" +
        "WINEPREFIX=/home/user/.gass-wine\n" +
        "DXVK=/opt/gassnative/dxvk-v2.7.1/arm64ec\n" +
        "if [ -s $DXVK/dxgi.dll ]; then\n" +
        "  mkdir -p $WINEPREFIX/drive_c/windows/system32\n" +
        "  for dll in d3d9 d3d10core d3d11 dxgi; do\n" +
        "    [ -s $DXVK/$dll.dll ] || continue\n" +
        "    cp -f $DXVK/$dll.dll $WINEPREFIX/drive_c/windows/system32/$dll.dll\n" +
        "    wine reg add 'HKCU\\Software\\Wine\\DllOverrides' /v $dll /d native,builtin /f >/dev/null 2>&1 || true\n" +
        "  done\n" +
        "  echo GASS-DXVK-ARM64EC-installed\n" +
        "else\n" +
        "  echo GASS-DXVK-files-missing\n" +
        "fi\n";

    private void writeScript(String rel, String body) {
        try {
            File f = new File(root, rel);
            f.getParentFile().mkdirs();
            java.io.FileWriter w = new java.io.FileWriter(f);
            w.write(body);
            w.close();
            f.setExecutable(true, false);
        } catch (Throwable t) {
            android.util.Log.e("GassNativeDesktop", "write " + rel, t);
        }
    }

    private void installStartScripts() {
        writeScript("usr/local/bin/gass-start", GASS_START);
        writeScript("usr/local/bin/gass-xserver", GASS_XSERVER);
        writeScript("usr/local/bin/gass-wine", GASS_WINE);
        writeScript("usr/local/bin/gass-dxvk", GASS_DXVK);
        writeScript("usr/local/bin/gass-labwc", GASS_LABWC);
    }

    private int deskId = -1;
    private Button swBtn, deskBtn;

    private final android.os.Handler barH = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable barTick = new Runnable() {
        @Override public void run() { updateBar(); barH.postDelayed(this, 500); }
    };

    private void ensureBar() {
        try {
            android.view.ViewGroup content = (android.view.ViewGroup) findViewById(android.R.id.content);
            if (content == null || content.getChildCount() == 0) return;
            android.view.ViewGroup fl = (android.view.ViewGroup) content.getChildAt(0);
            for (int i = 0; i < fl.getChildCount(); i++) {
                View v = fl.getChildAt(i);
                if (!(v instanceof Button)) continue;
                CharSequence t = ((Button) v).getText();
                if (swBtn == null && ("Off".contentEquals(t) || "Close".contentEquals(t))) {
                    swBtn = (Button) v;
                    swBtn.setOnClickListener(x -> onBarMain());
                }
                if (deskBtn == null && "Desk".contentEquals(t)) deskBtn = (Button) v;
            }
            if (deskBtn == null && fl instanceof android.widget.FrameLayout) {
                Button db = new Button(this);
                db.setText("Desk");
                db.setAllCaps(false);
                db.setAlpha(0.5f);
                db.setVisibility(View.GONE);
                db.setOnClickListener(x -> showDesktop());
                android.widget.FrameLayout.LayoutParams dlp = new android.widget.FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM | Gravity.END);
                dlp.setMargins(0, 0, dp(12), dp(148));
                fl.addView(db, dlp);
                deskBtn = db;
            }
        } catch (Throwable t) {
            android.util.Log.e("GassNativeDesktop", "ensureBar", t);
        }
    }

    private void updateBar() {
        if (swBtn == null || deskBtn == null) ensureBar();
        boolean app = windowId >= 0 && deskId >= 0 && windowId != deskId;
        if (swBtn != null) swBtn.setText(app ? "Close" : "Off");
        if (deskBtn != null) deskBtn.setVisibility(app ? View.VISIBLE : View.GONE);
    }

    private void onBarMain() {
        if (windowId >= 0 && deskId >= 0 && windowId != deskId) { closeCurrentWindow(); return; }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Power off")
            .setMessage("Stop the desktop and close GassNative?")
            .setPositiveButton("Power off", (d, which) -> { stopDesktop(); finish(); })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void closeCurrentWindow() {
        final int id = windowId;
        final Compositor c = compositor;
        if (c == null || id < 0) return;
        c.close(id);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (wins.contains(id) && compositor != null) compositor.kill(id);
        }, 2500);
    }

    private void showDesktop() {
        if (compositor == null || deskId < 0 || windowId == deskId) return;
        if (windowId >= 0) compositor.detach(windowId);
        windowId = deskId;
        attachSurface();
    }

    private android.widget.FrameLayout rootRef;
    private int kbTarget = -1;
    private Button kbBtn;
    private int floatCount = 0;
    private final java.util.HashMap<Integer, FloatWin> floats = new java.util.HashMap<>();

    private class FloatWin {
        int id;
        android.widget.FrameLayout frame;
        SurfaceView sv;
        TextView titleView;
        boolean maxed = false;
        int normW, normH;
        float normX, normY;

        FloatWin(final int wid, String title) {
            this.id = wid;
            final android.content.Context cx = DesktopActivity.this;
            final int sw = rootRef.getWidth() > 0 ? rootRef.getWidth() : getResources().getDisplayMetrics().widthPixels;
            final int sh = rootRef.getHeight() > 0 ? rootRef.getHeight() : getResources().getDisplayMetrics().heightPixels;
            frame = new android.widget.FrameLayout(cx);
            frame.setBackgroundColor(Color.rgb(32, 36, 44));
            LinearLayout col = new LinearLayout(cx);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout bar = new LinearLayout(cx);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setBackgroundColor(Color.rgb(52, 58, 70));
            titleView = new TextView(cx);
            titleView.setText(title == null || title.isEmpty() ? "Linux app" : title);
            titleView.setTextColor(Color.WHITE);
            titleView.setTextSize(13);
            titleView.setSingleLine(true);
            titleView.setPadding(dp(10), 0, dp(6), 0);
            bar.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
            Button maxBtn = new Button(cx);
            maxBtn.setText("[ ]");
            maxBtn.setAllCaps(false);
            maxBtn.setPadding(0, 0, 0, 0);
            Button xBtn = new Button(cx);
            xBtn.setText("X");
            xBtn.setAllCaps(false);
            xBtn.setPadding(0, 0, 0, 0);
            bar.addView(maxBtn, new LinearLayout.LayoutParams(dp(48), -1));
            bar.addView(xBtn, new LinearLayout.LayoutParams(dp(48), -1));
            col.addView(bar, new LinearLayout.LayoutParams(-1, dp(40)));
            sv = new SurfaceView(cx);
            sv.setZOrderMediaOverlay(true);
            col.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
            frame.addView(col, new android.widget.FrameLayout.LayoutParams(-1, -1));
            View handle = new View(cx);
            handle.setBackgroundColor(0x99FFFFFF);
            frame.addView(handle, new android.widget.FrameLayout.LayoutParams(dp(30), dp(30), Gravity.BOTTOM | Gravity.END));

            sv.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override public void surfaceCreated(SurfaceHolder holder) { }
                @Override public void surfaceChanged(SurfaceHolder holder, int fmt, int width, int height) {
                    if (compositor != null && width > 0 && height > 0) {
                        compositor.attach(wid, holder.getSurface(), width, height);
                        compositor.resize(wid, width, height);
                        compositor.focus(wid, true);
                    }
                }
                @Override public void surfaceDestroyed(SurfaceHolder holder) {
                    if (compositor != null) compositor.detach(wid);
                }
            });

            sv.setOnTouchListener((v, e) -> {
                Compositor c = compositor;
                if (c == null) return true;
                int t = (int) SystemClock.uptimeMillis();
                kbTarget = wid;
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        c.focus(wid, true);
                        c.pointerMotion(wid, e.getX(), e.getY(), t);
                        c.pointerButton(wid, 272, true, t);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        c.pointerMotion(wid, e.getX(), e.getY(), t);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        c.pointerMotion(wid, e.getX(), e.getY(), t);
                        c.pointerButton(wid, 272, false, t);
                        break;
                }
                return true;
            });

            final float[] last = new float[2];
            bar.setOnTouchListener((v, e) -> {
                if (maxed) return true;
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        last[0] = e.getRawX(); last[1] = e.getRawY();
                        kbTarget = wid;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        frame.setX(frame.getX() + e.getRawX() - last[0]);
                        frame.setY(frame.getY() + e.getRawY() - last[1]);
                        last[0] = e.getRawX(); last[1] = e.getRawY();
                        return true;
                }
                return true;
            });

            final float[] hs = new float[4];
            handle.setOnTouchListener((v, e) -> {
                if (maxed) return true;
                android.view.ViewGroup.LayoutParams p = frame.getLayoutParams();
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        hs[0] = e.getRawX(); hs[1] = e.getRawY(); hs[2] = frame.getWidth(); hs[3] = frame.getHeight();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        p.width = (int) Math.max(dp(180), hs[2] + e.getRawX() - hs[0]);
                        p.height = (int) Math.max(dp(160), hs[3] + e.getRawY() - hs[1]);
                        frame.setLayoutParams(p);
                        return true;
                }
                return true;
            });

            maxBtn.setOnClickListener(v -> {
                android.widget.FrameLayout.LayoutParams p = (android.widget.FrameLayout.LayoutParams) frame.getLayoutParams();
                if (!maxed) {
                    normW = frame.getWidth(); normH = frame.getHeight(); normX = frame.getX(); normY = frame.getY();
                    p.width = -1; p.height = -1; p.leftMargin = 0; p.topMargin = 0;
                    frame.setLayoutParams(p);
                    frame.setX(0); frame.setY(0);
                    maxed = true;
                } else {
                    p.width = normW; p.height = normH;
                    frame.setLayoutParams(p);
                    frame.setX(normX); frame.setY(normY);
                    maxed = false;
                }
            });

            xBtn.setOnClickListener(v -> {
                if (compositor == null) return;
                compositor.close(wid);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (wins.contains(wid) && compositor != null) compositor.kill(wid);
                }, 2500);
            });

            int w = (int) (sw * 0.86f), h = (int) (sh * 0.45f);
            android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(w, h);
            lp.leftMargin = (sw - w) / 2;
            lp.topMargin = dp(50) + (floatCount++ % 4) * dp(36);
            rootRef.addView(frame, lp);
        }
    }

    private void openFloat(int id, String title) {
        if (rootRef == null || floats.containsKey(id)) return;
        floats.put(id, new FloatWin(id, title));
        kbTarget = id;
        if (tpCursor != null) tpCursor.bringToFront();
        if (swBtn != null) swBtn.bringToFront();
        if (deskBtn != null) deskBtn.bringToFront();
        if (kbBtn != null) kbBtn.bringToFront();
    }

    private void closeFloat(int id) {
        FloatWin fw = floats.remove(id);
        if (fw == null) return;
        rootRef.removeView(fw.frame);
        if (kbTarget == id) kbTarget = -1;
        if (compositor != null && windowId >= 0) compositor.focus(windowId, true);
    }

    private java.io.FileWriter guestLog;

    private synchronized void logGuest(String line) {
        try {
            if (line.startsWith("GASS-CLIP ")) return;
            if (guestLog == null) guestLog = new java.io.FileWriter(new File("/sdcard/Download/gass-guest.log"), false);
            guestLog.write(line + "\n");
            guestLog.flush();
        } catch (Throwable ignored) {}
    }

    private void writeAutorun(android.content.Intent in) {
        try {
            String c = in == null ? null : in.getStringExtra("autorun");
            if (c == null || c.isEmpty()) return;
            File f = new File(root, "home/user/.gass-autorun");
            f.getParentFile().mkdirs();
            java.io.FileWriter w = new java.io.FileWriter(f);
            w.write(c);
            w.close();
        } catch (Throwable t) {
            android.util.Log.e("GassNativeDesktop", "autorun", t);
        }
    }

    static DesktopActivity instance;

    void powerOff() {
        stopDesktop();
        finish();
    }

    private String devBinds() {
        File rd = new File(root, "dev");
        rd.mkdirs();
        File shm = new File(rd, "shm");
        shm.mkdirs();
        shm.setReadable(true, false);
        shm.setWritable(true, false);
        shm.setExecutable(true, false);
        String[] nodes = {"null", "zero", "full", "random", "urandom", "tty", "ptmx", "ashmem", "ion", "kgsl-3d0", "pts", "dma_heap"};
        StringBuilder sb = new StringBuilder();
        for (String n : nodes) {
            File host = new File("/dev", n);
            if (!host.exists()) continue;
            File guest = new File(rd, n);
            try {
                if (host.isDirectory()) guest.mkdirs();
                else if (!guest.exists()) guest.createNewFile();
            } catch (Exception ignored) {}
            sb.append(" -b /dev/").append(n);
        }
        String[][] links = {{"fd", "/proc/self/fd"}, {"stdin", "/proc/self/fd/0"}, {"stdout", "/proc/self/fd/1"}, {"stderr", "/proc/self/fd/2"}};
        for (String[] l : links) {
            try { android.system.Os.symlink(l[1], new File(rd, l[0]).getAbsolutePath()); } catch (Throwable ignored) {}
        }
        return sb.toString();
    }

    private boolean wineMode;

    private static void writeText(File f, String t) throws java.io.IOException {
        java.io.FileWriter w = new java.io.FileWriter(f);
        w.write(t);
        w.close();
    }

    private void prepareWine(android.content.Intent in) {
        wineMode = in != null && "wine".equals(in.getStringExtra("mode"));
        try {
            File home = new File(root, "home/user");
            home.mkdirs();
            new File(home, ".gass-safe").createNewFile();
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            writeText(new File(home, ".gass-screen"), dm.widthPixels + "x" + dm.heightPixels);
            if (wineMode) {
                writeText(new File(home, ".gass-wine-mode"), "wayland".equals(in.getStringExtra("wmode")) ? "wayland" : "x11");
                String exe = in.getStringExtra("exe");
                File run = new File(home, ".gass-wine-run");
                if (exe != null && exe.length() > 0) writeText(run, exe); else run.delete();
            }
        } catch (Throwable t) {
            android.util.Log.e("GassNativeDesktop", "prepareWine", t);
        }
    }

    private String bootCmd() {
        return wineMode ? "/usr/local/bin/gass-start wine" : XFCE_CMD;
    }

    static void launch(final android.content.Context c, String mode, String exe, String wmode) {
        final android.content.Intent i = new android.content.Intent(c, DesktopActivity.class);
        i.putExtra("mode", mode);
        if (exe != null) i.putExtra("exe", exe);
        if (wmode != null) i.putExtra("wmode", wmode);
        DesktopActivity cur = instance;
        if (cur != null && ("wine".equals(mode) || cur.wineMode)) {
            cur.powerOff();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> c.startActivity(i), 1500);
        } else {
            c.startActivity(i);
        }
    }

    private void startCompositor() {
        if (starting || (compositor != null && compositor.isRunning())) {
            say("Compositor is already running or starting.");
            return;
        }
        if (!new File(root, "usr/bin/bash").isFile()) {
            say("Debian root not found for this profile: " + root);
            return;
        }

        File setupMarker = new File(root,
            "var/lib/gassnative-desktop-setup-v1.done");
        if (!setupMarker.isFile()) {
            say("Desktop installation is incomplete. Open Terminal and wait for automatic setup.");
            return;
        }

        starting = true;
        say("Starting native Wayland compositor...");
        new Thread(() -> {
            try {
                runDir.mkdirs(); new File(getCacheDir(),"dnc").mkdirs(); new File(getCacheDir(),"proot").mkdirs();
                new File(runDir, "wayland-0").delete(); installStartScripts(); installGpuLauncher(); installSession();
                if (!startAudioServer()) {
                    android.util.Log.e("GassNativeAudio",
                        "Desktop will start, but audio server failed");
                }

                File guestRun = new File(root, "run/droidnative");
                guestRun.mkdirs();

                android.util.DisplayMetrics dm =
                    getResources().getDisplayMetrics();
                int w = Math.max(1, dm.widthPixels);
                int h = Math.max(1, dm.heightPixels);

                Compositor c = new Compositor();
                c.setListener(new Compositor.Listener() {
                    @Override
                    public void onTextInput(int id, boolean active) {
                        android.util.Log.d(
                            "GassNativeDesktop",
                            "Linux text input active=" + active + " window=" + id
                        );
                    }

                    @Override
                    public void onPointerLock(int id, boolean locked) {
                        android.util.Log.d("GassNativeDesktop",
                            "Pointer lock: " + locked + " for window " + id);
                    }

                    @Override
                    public void onToplevelNew(int id, String appId,
                                               String title, int pid) {
                        runOnUiThread(() -> {
                            say("Linux window: " + title + " (" + appId + ")"); wins.add(id); if (appId != null && appId.contains("Xwayland")) deskId = id;
                            if (appId != null && appId.contains("Xwayland")) { if (windowId >= 0 && windowId != id && compositor != null) compositor.detach(windowId); windowId = id; attachSurface(); } else if (windowId == -1) { windowId = id; attachSurface(); } else { openFloat(id, title); }
                        });
                    }

                    @Override
                    public void onToplevelChanged(int id, String appId,
                                                  String title) {
                        runOnUiThread(() -> {
                            if (id == windowId) status.setText(title); FloatWin fw = floats.get(id); if (fw != null) fw.titleView.setText(title);
                        });
                    }

                    @Override
                    public void onToplevelClosed(int id) {
                        runOnUiThread(() -> {
                            closeFloat(id); wins.remove(Integer.valueOf(id)); if (id == windowId) { windowId = wins.isEmpty() ? -1 : wins.get(wins.size() - 1); say("Linux window closed."); attachSurface(); }
                        });
                    }
                });

                boolean ok = c.start(
                    new File(runDir, "wayland-0").getAbsolutePath(),
                    new File(root, "root/gass.xkb").isFile() ? new File(root, "root/gass.xkb").getAbsolutePath() : null,
                    new File(getCacheDir(), "dnc").getAbsolutePath(),
                    1, w, h, 68, 145, 60000, 0
                );

                runOnUiThread(() -> {
                    starting = false;
                    if (ok) {
                        compositor = c;
                        say("Compositor running."); launchGuest(bootCmd());
                        launchPendingX11();
                        attachSurface();
                    } else {
                        say("Compositor startup failed. Check Android logcat.");
                        c.stop();
                    }
                });
            } catch (Throwable t) {
                android.util.Log.e("GassNativeDesktop", "Startup failed", t);
                runOnUiThread(() -> {
                    starting = false;
                    say("Startup error: " + t);
                });
            }
        }, "desktop-start").start();
    }

    private void attachSurface() {
        updateBar();
        if (compositor == null || !compositor.isRunning()
                || surface == null || !surface.isValid() || windowId < 0) {
            return;
        }
        say("attach win=" + windowId + " size=" + surfaceWidth + "x" + surfaceHeight); say("attach win=" + windowId + " size=" + surfaceWidth + "x" + surfaceHeight); compositor.attach(windowId, surface, surfaceWidth, surfaceHeight);
        compositor.resize(windowId, surfaceWidth, surfaceHeight);
        compositor.focus(windowId, true);
    }


    private float cursorX = 200f;
    private float cursorY = 200f;
    private View tpCursor;
    private float lastX, lastY, downX, downY;
    private long downTime;
    private boolean moved;
    private boolean dragging;
    private boolean rightTap;

    private float scrollY = -1;
    private float scrollAcc = 0;
    private boolean scrolling = false;

    private float tpCx = -1, tpCy = -1, tpLx, tpLy, tpDx, tpDy, tpSy, tpAcc;
    private boolean tpMoved, tpDrag, tpScroll, tpTwoMoved;
    private final android.os.Handler tpH = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable tpRun;

    private boolean handleTouch(MotionEvent e) {
        if (tpCursor != null) {
            tpCursor.setX(tpCx);
            tpCursor.setY(tpCy);
            tpCursor.setVisibility(View.VISIBLE);
        }

        final Compositor c = compositor;
        final int id = windowId;
        if (c == null || !c.isRunning() || id < 0) return true;
        int t = (int) SystemClock.uptimeMillis();
        if (tpCx < 0) { tpCx = surfaceWidth / 2f; tpCy = surfaceHeight / 2f; }
        int act = e.getActionMasked();
        switch (act) {
            case MotionEvent.ACTION_DOWN:
                kbTarget = -1; c.focus(id, true);
                tpLx = tpDx = e.getX(); tpLy = tpDy = e.getY();
                tpMoved = false; tpDrag = false; tpScroll = false; tpTwoMoved = false;
                tpRun = () -> {
                    if (!tpMoved && !tpScroll) {
                        tpDrag = true;
                        c.pointerMotion(id, tpCx, tpCy, (int) SystemClock.uptimeMillis());
                        c.pointerButton(id, 272, true, (int) SystemClock.uptimeMillis());
                    }
                };
                tpH.postDelayed(tpRun, 450);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (e.getPointerCount() == 2 && !tpDrag) {
                    tpH.removeCallbacks(tpRun);
                    tpScroll = true; tpTwoMoved = false;
                    tpSy = (e.getY(0) + e.getY(1)) / 2f; tpAcc = 0;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tpScroll) {
                    if (e.getPointerCount() >= 2) {
                        float y = (e.getY(0) + e.getY(1)) / 2f;
                        float d = y - tpSy; tpSy = y;
                        if (Math.abs(d) > 1) tpTwoMoved = true;
                        tpAcc += d;
                        float step = dp(24);
                        while (tpAcc >= step) { c.pointerWheel(id, 0, -1, t); tpAcc -= step; }
                        while (tpAcc <= -step) { c.pointerWheel(id, 0, 1, t); tpAcc += step; }
                    }
                    return true;
                }
                float mx = e.getX() - tpLx, my = e.getY() - tpLy;
                tpLx = e.getX(); tpLy = e.getY();
                if (!tpMoved && Math.hypot(tpLx - tpDx, tpLy - tpDy) > dp(8)) {
                    tpMoved = true;
                    tpH.removeCallbacks(tpRun);
                }
                if (tpMoved || tpDrag) {
                    tpCx = Math.max(0, Math.min(surfaceWidth - 1, tpCx + mx * 1.5f));
                    tpCy = Math.max(0, Math.min(surfaceHeight - 1, tpCy + my * 1.5f));
                    if (tpCursor != null) {
                        tpCursor.setX(tpCx);
                        tpCursor.setY(tpCy);
                        tpCursor.invalidate();
                    }
                    if (tpDbg != null) tpDbg.setText("cursor " + (int) tpCx + "," + (int) tpCy + "  win " + id + " desk " + deskId);
                    c.pointerMotion(id, tpCx, tpCy, t);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tpH.removeCallbacks(tpRun);
                if (tpScroll) {
                    if (!tpTwoMoved && act == MotionEvent.ACTION_UP) {
                        c.pointerMotion(id, tpCx, tpCy, t);
                        c.pointerButton(id, 273, true, t);
                        c.pointerButton(id, 273, false, t);
                    }
                    tpScroll = false;
                } else if (tpDrag) {
                    c.pointerButton(id, 272, false, t);
                    tpDrag = false;
                } else if (!tpMoved && act == MotionEvent.ACTION_UP) {
                    c.pointerMotion(id, tpCx, tpCy, t);
                    c.pointerButton(id, 272, true, t);
                    c.pointerButton(id, 272, false, t);
                }
                return true;
        }
        return true;
    }



    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        writeAutorun(intent);
        if (intent != null) {
            String x11 = intent.getStringExtra("x11_command");
            if (x11 != null && !x11.trim().isEmpty()) {
                pendingX11Command = x11;
                launchPendingX11();
            }
            String cmd = intent.getStringExtra("linux_command");
            if (cmd != null && !cmd.trim().isEmpty()) {
                launchAppGuest(cmd);
            }
        }
    }

    private void launchPendingX11() {
        final String cmd = pendingX11Command;
        if (cmd == null || cmd.trim().isEmpty()) return;
        if (compositor == null || !compositor.isRunning()) return;
        pendingX11Command = null;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            () -> launchAppGuest("GASS_X11:" + cmd), 1200);
    }

    // Separate process: XFCE occupies "guest", so Linux apps must not reuse it.
    private void launchAppGuest(String appCommand) {
        final boolean useX11 = appCommand.startsWith("GASS_X11:");
        final String cmd = (useX11 ? appCommand.substring(9) : appCommand).trim();
        if (cmd.isEmpty()) return;
        if (compositor == null || !compositor.isRunning()) {
            android.widget.Toast.makeText(this,
                "Start the desktop before launching Linux apps.",
                android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (appGuest != null && appGuest.isAlive()) {
            android.widget.Toast.makeText(this,
                "A Linux app is already being launched.",
                android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            Process p = null;
            try {
                File fakeStat = new File(getFilesDir(), "fake_stat");
                if (!fakeStat.isFile()) {
                    try (java.io.FileWriter fw = new java.io.FileWriter(fakeStat)) {
                        fw.write("cpu  4000 0 3000 90000 200 0 100 0 0 0\\n"
                            + "intr 100000 0 0 0\\nctxt 500000\\nbtime 1700000000\\n"
                            + "processes 20000\\nprocs_running 1\\nprocs_blocked 0\\n");
                    }
                }

                String proot = "LD_LIBRARY_PATH=" + Cfg.q(nativeDir)
                    + " PROOT_LOADER=" + Cfg.q(nativeDir + "/libproot-loader.so")
                    + " PROOT_TMP_DIR=" + Cfg.q(new File(getCacheDir(), "proot").getAbsolutePath())
                    + " exec " + Cfg.q(nativeDir + "/libproot.so")
                    + " --link2symlink -0 -r " + Cfg.q(root.getAbsolutePath())
                    + devBinds() + " -b /proc -b /sys" + sdBinds()
                    + " -b " + Cfg.q(fakeStat.getAbsolutePath() + ":/proc/stat")
                    + " -b " + Cfg.q(runDir.getAbsolutePath() + ":/run/droidnative")
                    + Cfg.binds(this)
                    + " -w /root /usr/bin/env -i"
                    + " HOME=/root USER=root LOGNAME=root TERM=xterm LANG=C.UTF-8"
                    + " PULSE_SERVER=tcp:127.0.0.1:4714"
                    + " PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                    + (useX11
                        ? " DISPLAY=:1 XDG_RUNTIME_DIR=/tmp/runtime-user"
                          + " XDG_SESSION_TYPE=x11 GDK_BACKEND=x11"
                          + " QT_QPA_PLATFORM=xcb SDL_VIDEODRIVER=x11"
                        : " XDG_RUNTIME_DIR=/run/droidnative WAYLAND_DISPLAY=wayland-0"
                          + " XDG_SESSION_TYPE=wayland GDK_BACKEND=wayland"
                          + " QT_QPA_PLATFORM=wayland SDL_VIDEODRIVER=wayland")
                    + Cfg.env(this)
                    + " /usr/bin/bash -lc " + Cfg.q(cmd);

                p = new ProcessBuilder("sh", "-c", proot)
                    .redirectErrorStream(true).start();
                appGuest = p;
                say("Launching Linux app: " + cmd);

                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = r.readLine()) != null) say(line);
                }
                int rc = p.waitFor();
                say("Linux app exited: " + rc);
            } catch (Throwable t) {
                say("App launch error: " + t);
            } finally {
                appGuest = null;
            }
        }, "gass-app-launch").start();
    }

    private void launchGuest(String appCommand) {
        if (appCommand == null || appCommand.trim().isEmpty()) return;
        if (compositor == null || !compositor.isRunning()) {
            say("Start the compositor first.");
            return;
        }
        if (guest != null && guest.isAlive()) {
            say("A Linux command is already running. Stop it first.");
            return;
        }

        final String cmd = appCommand.trim();
        new Thread(() -> {
            try {
                new File(getCacheDir(),"proot").mkdirs(); File fakeStat = new File(getFilesDir(), "fake_stat");
                if (!fakeStat.exists()) {
                    StringBuilder fs = new StringBuilder();
                    fs.append("cpu  4000 0 3000 90000 200 0 100 0 0 0\n");
                    for (int i = 0; i < 8; i++) fs.append("cpu" + i + " 500 0 375 11250 25 0 12 0 0 0\n");
                    fs.append("intr 100000 0 0 0\nctxt 500000\nbtime 1700000000\nprocesses 20000\nprocs_running 1\nprocs_blocked 0\nsoftirq 50000 0 0 0 0 0 0 0 0 0 0\n");
                    java.io.FileWriter fw = new java.io.FileWriter(fakeStat);
                    fw.write(fs.toString()); fw.close();
                }
                String runBind = runDir.getAbsolutePath()
                    + ":/run/droidnative";

                String proot = "LD_LIBRARY_PATH=" + Cfg.q(nativeDir)
                    + " PROOT_LOADER=" + Cfg.q(nativeDir + "/libproot-loader.so")
                    + " PROOT_TMP_DIR=" + Cfg.q(new File(getCacheDir(), "proot").getAbsolutePath())
                    + " exec " + Cfg.q(nativeDir + "/libproot.so")
                    + " --link2symlink -0 -r " + Cfg.q(root.getAbsolutePath())
                    + " -b /dev -b /proc -b /sys" + sdBinds()
                    + " -b " + Cfg.q(fakeStat.getAbsolutePath() + ":/proc/stat")
                    + " -b " + Cfg.q(runBind)
                    + Cfg.binds(this)
                    + " -w /root /usr/bin/env -i"
                    + " HOME=/root USER=root LOGNAME=root TERM=xterm LANG=C.UTF-8"
                    + " PULSE_SERVER=tcp:127.0.0.1:4714"
                    + " PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                    + " XDG_RUNTIME_DIR=/run/droidnative WAYLAND_DISPLAY=wayland-0"
                    + " XDG_SESSION_TYPE=wayland GDK_BACKEND=wayland"
                    + " QT_QPA_PLATFORM=wayland SDL_VIDEODRIVER=wayland"
                    + Cfg.env(this)
                    + " /usr/bin/bash -lc " + Cfg.q(cmd);

                logGuest("CMD: " + proot);
                Process p = new ProcessBuilder("sh", "-c", proot)
                    .redirectErrorStream(true).start();
                guest = p;
                say("Started: " + cmd);

                BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), "UTF-8"));
                String line;
                while ((line = r.readLine()) != null) {
                    logGuest(line); say(line);
                    if (line.startsWith("GASS-STATUS ")) {
                        final String msg = line.substring(12);
                        runOnUiThread(() -> { if (splashMsg != null) splashMsg.setText(msg); });
                    } else if (line.startsWith("GASS-KEYMAP ")) {
                        runOnUiThread(() -> { if (compositor != null) compositor.setKeymap(new File(root, "root/gass.xkb").getAbsolutePath()); });
                    } else if (line.startsWith("GASS-CLIP ")) {
                        try {
                            final String txt = new String(android.util.Base64.decode(line.substring(10), android.util.Base64.DEFAULT), "UTF-8");
                            runOnUiThread(() -> {
                                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("gass", txt));
                                android.widget.Toast.makeText(this, "Copied to Android clipboard", android.widget.Toast.LENGTH_SHORT).show();
                            });
                        } catch (Throwable ignored) {}
                    } else if (line.startsWith("GASS-EXIT")) {
                        runOnUiThread(() -> powerOff());
                    } else if (line.contains("GASS-READY")) {
                        runOnUiThread(() -> hideSplash());
                    }
                }
                int rc = p.waitFor();
                say("Linux command exited: " + rc);
            } catch (Throwable t) {
                say("Launch error: " + t);
            } finally {
                guest = null;
            }
        }, "desktop-guest").start();
    }

    private int[] charKey(char c) {
        if (c == ' ') return new int[]{57, 0};
        if (c == '\n') return new int[]{28, 0};
        if (c == '\t') return new int[]{15, 0};
        boolean up = Character.isUpperCase(c);
        char l = Character.toLowerCase(c);
        int i;
        if ((i = "qwertyuiop".indexOf(l)) >= 0) return new int[]{16 + i, up ? 1 : 0};
        if ((i = "asdfghjkl".indexOf(l)) >= 0) return new int[]{30 + i, up ? 1 : 0};
        if ((i = "zxcvbnm".indexOf(l)) >= 0) return new int[]{44 + i, up ? 1 : 0};
        if ((i = "1234567890".indexOf(c)) >= 0) return new int[]{2 + i, 0};
        if ((i = ")!@#$%^&*(".indexOf(c)) >= 0) return new int[]{i == 0 ? 11 : i + 1, 1};
        int[] sc = {12, 13, 26, 27, 39, 40, 41, 43, 51, 52, 53};
        if ((i = "-=[];'`\\,./".indexOf(c)) >= 0) return new int[]{sc[i], 0};
        if ((i = "_+{}:\"~|<>?".indexOf(c)) >= 0) return new int[]{sc[i], 1};
        return null;
    }

    private int specialKey(int kc) {
        switch (kc) {
            case android.view.KeyEvent.KEYCODE_DEL: return 14;
            case android.view.KeyEvent.KEYCODE_ENTER:
            case android.view.KeyEvent.KEYCODE_NUMPAD_ENTER: return 28;
            case android.view.KeyEvent.KEYCODE_TAB: return 15;
            case android.view.KeyEvent.KEYCODE_ESCAPE: return 1;
            case android.view.KeyEvent.KEYCODE_FORWARD_DEL: return 111;
            case android.view.KeyEvent.KEYCODE_DPAD_UP: return 103;
            case android.view.KeyEvent.KEYCODE_DPAD_DOWN: return 108;
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT: return 105;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: return 106;
            case android.view.KeyEvent.KEYCODE_MOVE_HOME: return 102;
            case android.view.KeyEvent.KEYCODE_MOVE_END: return 107;
            case android.view.KeyEvent.KEYCODE_PAGE_UP: return 104;
            case android.view.KeyEvent.KEYCODE_PAGE_DOWN: return 109;
            case android.view.KeyEvent.KEYCODE_INSERT: return 110;
        }
        return -1;
    }

    private void sendKey(int code, int mods, boolean down) {
        Compositor c = compositor;
        int id = kbTarget >= 0 ? kbTarget : windowId;
        if (c == null || id < 0) return;
        if (down && !keyToast) { keyToast = true; final int w = id; runOnUiThread(() -> android.widget.Toast.makeText(this, "first key sent to window " + w, android.widget.Toast.LENGTH_LONG).show()); }
        if (down && tpDbg != null) tpDbg.setText("key " + code + " win " + id);
        c.key(id, code, down, mods, 0, (int) SystemClock.uptimeMillis());
    }

    private void tapKey(int code, int mods) {
        sendKey(code, mods, true);
        sendKey(code, mods, false);
    }

    private void typeText(CharSequence t) {
        if (t == null || t.length() == 0) return;

        Compositor c = compositor;
        int id = windowId;

        if (c == null || id < 0) {
            android.util.Log.e("GassNativeDesktop",
                    "Cannot send text: compositor=" + c + ", windowId=" + id);
            return;
        }

        try {
            c.focus(id, true);

            for (int i = 0; i < t.length(); i++) {
                char ch = t.charAt(i);

                if (ch == '\r') continue;

                int[] key = charKey(ch);
                if (key != null) {
                    tapKey(key[0], key[1]);
                } else if (ch == '\n') {
                    tapKey(28, 0);
                } else if (ch == '\t') {
                    tapKey(15, 0);
                } else {
                    android.util.Log.w("GassNativeDesktop",
                            "Unsupported IME character U+" +
                            String.format("%04X", (int) ch));
                }
            }

            android.util.Log.d("GassNativeDesktop",
                    "IME text converted to key events: " + t.length());
        } catch (RuntimeException e) {
            android.util.Log.e("GassNativeDesktop",
                    "IME key injection failed", e);
        }
    }

    private boolean handleKey(android.view.KeyEvent e) {
        if (compositor == null || windowId < 0) return false;
        int kc = e.getKeyCode();
        if (kc == android.view.KeyEvent.KEYCODE_BACK) return false;
        boolean down = e.getAction() == android.view.KeyEvent.ACTION_DOWN;
        if (down && e.isCtrlPressed() && kc == android.view.KeyEvent.KEYCODE_V) {
            try {
                android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip()) {
                    android.content.ClipData clip = cm.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence paste = clip.getItemAt(0).coerceToText(this);
                        if (paste != null && paste.length() > 0) typeText(paste);
                    }
                }
            } catch (Throwable t) {
                runOnUiThread(() -> android.widget.Toast.makeText(
                    this, "Clipboard paste failed", android.widget.Toast.LENGTH_SHORT).show());
            }
            return true;
        }
        int mods = (e.isShiftPressed() ? 1 : 0) | (e.isCtrlPressed() ? 4 : 0) | (e.isAltPressed() ? 8 : 0);
        int sp = specialKey(kc);
        if (sp >= 0) { sendKey(sp, mods, down); return true; }
        int u = e.getUnicodeChar(0);
        if (u <= 0) return false;
        if (down) {
            int[] k = charKey((char) u);
            if (k != null) tapKey(k[0], mods | k[1]);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int kc, android.view.KeyEvent e) {
        return handleKey(e) || super.onKeyDown(kc, e);
    }

    @Override
    public boolean onKeyUp(int kc, android.view.KeyEvent e) {
        return handleKey(e) || super.onKeyUp(kc, e);
    }

    private void stopDesktop() {
        Process runningApp = appGuest;
        appGuest = null;
        if (runningApp != null) {
            try { runningApp.destroy(); } catch (Throwable ignored) {}
        }

        stopAudioServer();
        Process p = guest;
        guest = null;
        if (p != null) {
            p.destroy();
            try { p.destroyForcibly(); } catch (Throwable ignored) {}
        }
        Compositor c = compositor;
        compositor = null;
        windowId = -1;
        if (c != null) {
            new Thread(() -> c.stop(), "desktop-stop").start();
        }
        say("Desktop stopping.");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!autoStarted) { autoStarted = true; startCompositor(); }
        surface = holder.getSurface();
        attachSurface();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surface = holder.getSurface();
        surfaceWidth = Math.max(1, width);
        surfaceHeight = Math.max(1, height);
        attachSurface();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (compositor != null && windowId >= 0) compositor.detach(windowId);
        surface = null;
    }

    private void showKeyboard() {
        if (view == null) {
            say("Desktop surface is not ready yet.");
            return;
        }

        view.setFocusableInTouchMode(true);
        view.requestFocus();

        final InputMethodManager imm =
            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (imm != null) {
            view.post(() -> {
                if (isFinishing() || view == null || !view.isAttachedToWindow()) {
                    return;
                }
                view.setFocusable(true);
                view.setFocusableInTouchMode(true);
                view.requestFocus();
                imm.restartInput(view);
                boolean shown = imm.showSoftInput(
                    view, InputMethodManager.SHOW_IMPLICIT);
                android.util.Log.i("GassNativeDesktop",
                    "Soft keyboard requested; showSoftInput=" + shown);
            });
        }
    }

    @Override
    public void onBackPressed() {
        InputMethodManager imm =
            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (imm != null && imm.isAcceptingText()) {
            imm.hideSoftInputFromWindow(
                getWindow().getDecorView().getWindowToken(), 0);
            return;
        }

        // Keep the Linux desktop session alive when Back is pressed.
        say("Desktop session is still running. Use Stop to end it.");
    }

    @Override
    protected void onDestroy() {
        if (instance == this) instance = null;
        stopDesktopIntroAudio();
        stopDesktop();
        super.onDestroy();
    }
}
