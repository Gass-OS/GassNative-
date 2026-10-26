package com.gass.gassnative;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;

public class SettingsActivity extends Activity {
    private static final int BG = Color.rgb(9, 13, 21);
    private static final int PANEL = Color.rgb(20, 29, 43);
    private static final int TILE = Color.rgb(28, 42, 60);
    private static final int ORANGE = Color.rgb(255, 145, 75);
    private static final int BLUE = Color.rgb(106, 174, 255);
    private static final int GREEN = Color.rgb(114, 224, 177);
    private static final int FG = Color.rgb(239, 243, 250);
    private static final int MUTED = Color.rgb(166, 183, 205);

    private EditText dns, res, binds, env;
    private Spinner gpuMode;
    private TextView dashboard;

    private int dp(int n) {
        return (int) (n *
            getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius,
                                   int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (strokeColor != 0) {
            d.setStroke(dp(1), strokeColor);
        }
        return d;
    }

    private TextView text(String value, float size, int color,
                          boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void section(LinearLayout parent, String title,
                         String subtitle) {
        TextView h = text(title, 17, FG, true);
        h.setPadding(dp(2), dp(23), 0, dp(3));
        parent.addView(h);

        TextView sub = text(subtitle, 12, MUTED, false);
        sub.setPadding(dp(2), 0, 0, dp(11));
        parent.addView(sub);
    }

    // Large icon occupies its own full-size tile, not a tiny nested box.
    private void feature(LinearLayout parent, String glyph,
                         String title, String description, int accent) {
        LinearLayout item = row();
        item.setPadding(dp(13), dp(13), dp(12), dp(13));
        item.setBackground(shape(PANEL, 19,
            Color.rgb(39, 54, 74)));

        LinearLayout.LayoutParams itemLp =
            new LinearLayout.LayoutParams(-1, -2);
        itemLp.bottomMargin = dp(10);
        parent.addView(item, itemLp);

        TextView icon = text(glyph, 43, accent, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(TILE, 17, 0));
        item.addView(icon, new LinearLayout.LayoutParams(
            dp(76), dp(76)));

        LinearLayout words = column();
        words.setPadding(dp(14), 0, dp(4), 0);
        item.addView(words, new LinearLayout.LayoutParams(
            0, -2, 1f));

        words.addView(text(title, 16, accent, true));
        TextView desc = text(description, 12, MUTED, false);
        desc.setPadding(0, dp(5), 0, 0);
        words.addView(desc);

        TextView arrow = text("›", 29, BLUE, false);
        item.addView(arrow);
    }

    private EditText field(LinearLayout parent, String title,
                           String value, boolean multiline) {
        TextView caption = text(title, 13, FG, true);
        caption.setPadding(dp(2), dp(13), 0, dp(4));
        parent.addView(caption);

        EditText e = new EditText(this);
        e.setText(value);
        e.setTextColor(FG);
        e.setHintTextColor(MUTED);
        e.setTextSize(14);
        e.setPadding(dp(13), dp(11), dp(13), dp(11));
        e.setBackground(shape(Color.rgb(15, 22, 33), 13,
            Color.rgb(43, 59, 79)));

        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setSingleLine(true);
        }

        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(5);
        parent.addView(e, lp);
        return e;
    }

    private String systemSummary() {
        File root = Cfg.rootDir(this);
        StringBuilder out = new StringBuilder();

        out.append("   GASSNATIVE SYSTEM INFO\n");
        out.append("   ─────────────────────────\n");

        appendInfo(out, "📱", "DEVICE",
            Build.MANUFACTURER + " " + Build.MODEL);
        appendInfo(out, "🤖", "ANDROID",
            Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");

        String[] abis = Build.SUPPORTED_ABIS;
        appendInfo(out, "⚙️", "ARCH",
            abis != null && abis.length > 0 ? abis[0] : "Unknown");

        appendInfo(out, "👤", "PROFILE", Cfg.current(this));
        appendInfo(out, "🐧", "LINUX",
            root.isDirectory() ? "Debian ARM64 · directory found"
                               : "Debian not extracted");
        appendInfo(out, "📂", "ROOT PATH",
            root.getAbsolutePath());

        if (root.isDirectory()) {
            try {
                StatFs fs = new StatFs(root.getAbsolutePath());
                long free = fs.getAvailableBytes() /
                    (1024L * 1024L * 1024L);
                appendInfo(out, "💾", "FREE SPACE", free + " GiB");
            } catch (Exception ignored) {
                appendInfo(out, "💾", "FREE SPACE", "Unavailable");
            }
        }

        out.append("   ─────────────────────────\n");
        out.append(root.isDirectory()
            ? "   ✓ DEBIAN DIRECTORY FOUND"
            : "   ! DEBIAN SETUP REQUIRED");

        return out.toString();
    }

    private void appendInfo(
            StringBuilder out, String icon, String label, String value) {
        out.append(icon).append(" ")
            .append(String.format(java.util.Locale.US, "%-10s", label))
            .append("  ")
            .append(value)
            .append("\\n");
    }

    private void overviewRow(
            LinearLayout parent, String glyph, String label,
            String value, int accent) {

        LinearLayout item = row();
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(2), dp(10), dp(2), dp(10));

        TextView icon = text(glyph, 30, accent, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(
            Color.argb(145, 35, 51, 72), 16,
            Color.argb(90, 106, 174, 255)
        ));

        LinearLayout.LayoutParams iconLp =
            new LinearLayout.LayoutParams(dp(66), dp(66));
        iconLp.rightMargin = dp(14);
        item.addView(icon, iconLp);

        LinearLayout words = column();
        words.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(words,
            new LinearLayout.LayoutParams(0, -2, 1f));

        TextView heading = text(label.toUpperCase(java.util.Locale.US),
            11, MUTED, true);
        heading.setLetterSpacing(0.07f);
        words.addView(heading);

        TextView detail = text(value, 14, FG, false);
        detail.setPadding(0, dp(4), 0, 0);
        detail.setLineSpacing(dp(2), 1f);
        words.addView(detail);

        parent.addView(item, new LinearLayout.LayoutParams(-1, -2));

        View line = new View(this);
        line.setBackgroundColor(Color.argb(65, 150, 180, 215));
        LinearLayout.LayoutParams lineLp =
            new LinearLayout.LayoutParams(-1, dp(1));
        lineLp.leftMargin = dp(4);
        lineLp.rightMargin = dp(4);
        parent.addView(line, lineLp);
    }

    private void populateSystemOverview(LinearLayout panel) {
        panel.removeAllViews();

        File root = Cfg.rootDir(this);
        String[] abis = Build.SUPPORTED_ABIS;
        String arch = abis != null && abis.length > 0
            ? abis[0] : "Unknown";

        overviewRow(panel, "📱", "Device",
            Build.MANUFACTURER + " " + Build.MODEL, ORANGE);

        overviewRow(panel, "🤖", "Android",
            Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT,
            BLUE);

        overviewRow(panel, "⚙", "Architecture",
            arch, GREEN);

        overviewRow(panel, "👤", "Active profile",
            Cfg.current(this), ORANGE);

        overviewRow(panel, "🐧", "Debian ARM64",
            root.isDirectory() ? "Root filesystem directory found"
                               : "Not installed or not extracted",
            GREEN);

        overviewRow(panel, "📂", "Debian root path",
            root.getAbsolutePath(), BLUE);

        String freeSpace = "Unavailable";
        if (root.isDirectory()) {
            try {
                StatFs fs = new StatFs(root.getAbsolutePath());
                long free = fs.getAvailableBytes() /
                    (1024L * 1024L * 1024L);
                freeSpace = free + " GiB available";
            } catch (Exception ignored) {
                // Storage information may be restricted.
            }
        }

        overviewRow(panel, "💾", "Available storage",
            freeSpace, GREEN);
    }

    private boolean validateEnvironmentSettings() {
        String d = dns.getText().toString().trim();
        String[] octets = d.split("\\.", -1);
        boolean ipv4 = octets.length == 4;
        if (ipv4) {
            for (String part : octets) {
                if (!part.matches("[0-9]{1,3}")) {
                    ipv4 = false;
                    break;
                }
                int n = Integer.parseInt(part);
                if (n < 0 || n > 255) {
                    ipv4 = false;
                    break;
                }
            }
        }
        if (!ipv4) {
            dns.setError("Enter a valid IPv4 DNS address, e.g. 1.1.1.1");
            return false;
        }

        String resolution = res.getText().toString().trim();
        if (!resolution.matches("[0-9]{3,5}x[0-9]{3,5}")) {
            res.setError("Use a resolution like 1280x720");
            return false;
        }
        String[] dimensions = resolution.split("x");
        int width = Integer.parseInt(dimensions[0]);
        int height = Integer.parseInt(dimensions[1]);
        if (width < 320 || width > 16384 ||
            height < 200 || height > 16384) {
            res.setError("Width must be 320–16384; height 200–16384");
            return false;
        }

        String[] mountLines = binds.getText().toString().split("\\n");
        for (int i = 0; i < mountLines.length; i++) {
            String line = mountLines[i].trim();
            if (line.isEmpty()) continue;
            String[] pair = line.split(":", -1);
            if (pair.length != 2 ||
                !pair[0].startsWith("/") ||
                !pair[1].startsWith("/") ||
                pair[0].contains(" ") ||
                pair[1].contains(" ")) {
                binds.setError("Line " + (i + 1) +
                    ": use absolute paths host:/guest/path");
                return false;
            }
        }

        String[] envLines = env.getText().toString().split("\\n");
        for (int i = 0; i < envLines.length; i++) {
            String line = envLines[i].trim();
            if (line.isEmpty()) continue;
            int equals = line.indexOf('=');
            if (equals < 1 ||
                !line.substring(0, equals).matches("[A-Za-z_][A-Za-z0-9_]*") ||
                line.indexOf('\0') >= 0) {
                env.setError("Line " + (i + 1) +
                    ": use KEY=VALUE (valid environment variable name)");
                return false;
            }
        }
        return true;
    }

    private void saveSettings() {
        if (!validateEnvironmentSettings()) {
            Toast.makeText(this, "Please correct the highlighted settings",
                Toast.LENGTH_LONG).show();
            return;
        }
        String d = dns.getText().toString().trim();
        if (d.length() == 0) d = "1.1.1.1";

        String resolution = res.getText().toString().trim();
        String[] gpuValues = {
            "auto", "zink", "freedreno", "llvmpipe"
        };
        int index = gpuMode.getSelectedItemPosition();
        if (index < 0 || index >= gpuValues.length) index = 0;

        Cfg.sp(this).edit()
            .putString("dns", d)
            .putString("gpu_mode", gpuValues[index])
            .apply();

        Cfg.put(this, "res", resolution);
        Cfg.put(this, "binds", binds.getText().toString());
        Cfg.put(this, "env", env.getText().toString());

        File rc = new File(Cfg.rootDir(this), "etc/resolv.conf");
        if (rc.getParentFile() != null &&
            rc.getParentFile().isDirectory()) {
            try (FileWriter writer = new FileWriter(rc)) {
                writer.write("nameserver " + d + "\n");
            } catch (Exception e) {
                Toast.makeText(this,
                    "Settings saved, but resolv.conf could not be written",
                    Toast.LENGTH_LONG).show();
                return;
            }
        }

        Toast.makeText(this,
            "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout content = column();
        content.setPadding(dp(18), dp(8), dp(18), dp(30));
        scroll.addView(content);

        LinearLayout hero = row();
        hero.setPadding(dp(2), dp(13), dp(2), dp(12));
        content.addView(hero);

        TextView heroIcon = text("⚙", 49, ORANGE, true);
        heroIcon.setGravity(Gravity.CENTER);
        heroIcon.setBackground(shape(TILE, 21, 0));
        hero.addView(heroIcon, new LinearLayout.LayoutParams(
            dp(82), dp(82)));

        LinearLayout heroWords = column();
        heroWords.setPadding(dp(15), 0, 0, 0);
        hero.addView(heroWords, new LinearLayout.LayoutParams(
            0, -2, 1f));
        heroWords.addView(text("Control Center", 25, FG, true));
        heroWords.addView(text("GassNative · Debian ARM64",
            12, BLUE, true));
        TextView profile = text("Profile: " + Cfg.current(this),
            12, MUTED, false);
        profile.setPadding(0, dp(6), 0, 0);
        heroWords.addView(profile);

        section(content, "SYSTEM OVERVIEW",
            "Device status · Debian environment");

        LinearLayout infoPanel = column();
        infoPanel.setPadding(dp(12), dp(8), dp(12), dp(12));
        infoPanel.setBackground(shape(
            Color.argb(125, 16, 25, 38), 22,
            Color.argb(100, 100, 130, 165)
        ));
        content.addView(infoPanel);

        populateSystemOverview(infoPanel);

        Button refresh = new Button(this);
        refresh.setText("↻   Refresh system information");
        refresh.setOnClickListener(v ->
            populateSystemOverview(infoPanel));
        LinearLayout.LayoutParams refreshLp =
            new LinearLayout.LayoutParams(-1, -2);
        refreshLp.topMargin = dp(10);
        content.addView(refresh, refreshLp);

        section(content, "GRAPHICS",
            "Select the renderer for your Linux session");

        TextView gpuTitle = text("Graphics renderer", 14, FG, true);
        content.addView(gpuTitle);

        final String[] gpuValues = {
            "auto", "zink", "freedreno", "llvmpipe"
        };
        final String[] gpuLabels = {
            "Auto detect (recommended)",
            "Zink (Vulkan)",
            "Freedreno / Turnip",
            "llvmpipe (software)"
        };

        gpuMode = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, gpuLabels);
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item);
        gpuMode.setAdapter(adapter);
        gpuMode.setBackground(shape(PANEL, 13, 0));

        String savedGpu = Cfg.sp(this).getString("gpu_mode", "auto");
        int selected = 0;
        for (int i = 0; i < gpuValues.length; i++) {
            if (gpuValues[i].equals(savedGpu)) selected = i;
        }
        gpuMode.setSelection(selected);
        content.addView(gpuMode);

        TextView gpuNote = text(
            "The selected renderer is a preference, not proof that " +
            "hardware acceleration is active. Verify GL_RENDERER " +
            "inside the running desktop.",
            12, MUTED, false);
        gpuNote.setPadding(0, dp(7), 0, 0);
        content.addView(gpuNote);

        section(content, "NETWORK & DISPLAY",
            "Settings used by your Debian environment");

        dns = field(content, "DNS server",
            Cfg.dns(this), false);
        res = field(content,
            "Preferred desktop resolution (display bridge may override)",
            Cfg.get(this, "res", "1280x720"), false);
        binds = field(content,
            "Extra bind mounts — one per line (host:guest)",
            Cfg.get(this, "binds", ""), true);
        env = field(content,
            "Extra environment variables — one per line (KEY=VALUE)",
            Cfg.get(this, "env", ""), true);

        Button save = new Button(this);
        save.setText("✓   Save settings");
        save.setOnClickListener(v -> saveSettings());
        content.addView(save);

        section(content, "MAINTENANCE",
            "Destructive actions always require confirmation");

        Button delete = new Button(this);
        delete.setText("Delete this profile's Debian");
        delete.setTextColor(ORANGE);
        delete.setOnClickListener(v ->
            new AlertDialog.Builder(SettingsActivity.this)
                .setTitle("Delete Debian environment?")
                .setMessage(
                    "This permanently erases the Debian root filesystem " +
                    "for the current profile. Your other profiles are " +
                    "not selected by this action. Continue only if you " +
                    "intend to reinstall this environment.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    Cfg.rm(Cfg.rootDir(SettingsActivity.this));
                    Intent intent = new Intent(
                        SettingsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show());
        content.addView(delete);

        TextView tip = text(
            "✦  LINUX TIP\n\n" +
            "Keep a copy of important configuration files before " +
            "changing graphics drivers. If the desktop stops launching, " +
            "switch back to Auto detect and inspect the session logs.",
            13, MUTED, false);
        tip.setPadding(dp(16), dp(17), dp(16), dp(17));
        tip.setBackground(shape(PANEL, 17, 0));
        LinearLayout.LayoutParams tipLp =
            new LinearLayout.LayoutParams(-1, -2);
        tipLp.topMargin = dp(22);
        content.addView(tip, tipLp);

        Ui.addTopBar(this, content);
        Ui.styleTree(this, content);
        setContentView(scroll);
    }
}
