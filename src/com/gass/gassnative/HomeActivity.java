package com.gass.gassnative;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

public class HomeActivity extends Activity {
    private static final int BG = Color.rgb(9, 13, 21);
    private static final int CARD = Color.rgb(20, 28, 41);
    private static final int BORDER = Color.rgb(42, 56, 75);
    private static final int ORANGE = Color.rgb(255, 138, 66);
    private static final int BLUE = Color.rgb(100, 181, 255);
    private static final int MUTED = Color.rgb(180, 194, 213);

    private LinearLayout activeGridRow;
    private int activeGridCount = 0;

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void finishGridRow(LinearLayout parent) {
        activeGridRow = null;
        activeGridCount = 0;
    }

    private void section(LinearLayout parent, String value) {
        finishGridRow(parent);

        TextView t = text(value, 12, BLUE);
        t.setTypeface(null, Typeface.BOLD);
        t.setLetterSpacing(0.12f);
        t.setPadding(dp(3), dp(22), 0, dp(12));
        parent.addView(t);
    }

    private void item(LinearLayout parent, String glyph, String title,
                      String description, int accent, Runnable action) {
        if (activeGridRow == null || activeGridCount >= 2) {
            activeGridRow = new LinearLayout(this);
            activeGridRow.setOrientation(LinearLayout.HORIZONTAL);
            activeGridRow.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rowLp =
                new LinearLayout.LayoutParams(-1, -2);
            rowLp.bottomMargin = dp(16);
            parent.addView(activeGridRow, rowLp);
            activeGridCount = 0;
        }

        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(8), dp(6), dp(8), dp(10));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setBackgroundResource(android.R.drawable.list_selector_background);

        LinearLayout.LayoutParams tileLp =
            new LinearLayout.LayoutParams(0, -1, 1f);
        activeGridRow.addView(tile, tileLp);
        activeGridCount++;

        TextView icon = text(glyph, 43, accent);
        icon.setGravity(Gravity.CENTER);
        icon.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        tile.addView(icon, new LinearLayout.LayoutParams(-1, dp(64)));

        TextView heading = text(title, 15, Color.WHITE);
        heading.setGravity(Gravity.CENTER);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(0, dp(7), 0, dp(4));
        tile.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = text(description, 11, MUTED);
        sub.setGravity(Gravity.CENTER);
        sub.setLineSpacing(dp(2), 1.0f);
        tile.addView(sub, new LinearLayout.LayoutParams(-1, -2));

        tile.setOnClickListener(v -> action.run());
    }

    private void open(Class<?> c) {
        startActivity(new Intent(this, c));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        UpdateChecker.check(this);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(BG);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(col);

        // GassNative branding: free-standing white emblem, no logo box.
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(4), dp(10), dp(4), dp(12));
        col.addView(hero);

        ImageView logo = new ImageView(this);
        int logoId = getResources().getIdentifier(
            "gassnative_mark", "drawable", getPackageName());
        if (logoId != 0) {
            logo.setImageResource(logoId);
        }
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription("GassNative emblem");
        LinearLayout.LayoutParams logoLp =
            new LinearLayout.LayoutParams(dp(116), dp(116));
        logoLp.gravity = Gravity.CENTER;
        hero.addView(logo, logoLp);

        TextView brand = text("GassNative", 32, Color.WHITE);
        brand.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        brand.setGravity(Gravity.CENTER);
        brand.setPadding(0, dp(7), 0, 0);
        hero.addView(brand);

        TextView tagline = text("Linux on Android", 16, Color.WHITE);
        tagline.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tagline.setGravity(Gravity.CENTER);
        tagline.setPadding(0, dp(3), 0, 0);
        hero.addView(tagline);

        TextView version = text("Gass-OS v3  ·  TEST RP", 12, MUTED);
        version.setGravity(Gravity.CENTER);
        version.setLetterSpacing(0.04f);
        version.setPadding(0, dp(7), 0, dp(12));
        hero.addView(version);

        TextView hardware = text(
            "SNAPDRAGON 8 ELITE  ·  ADRENO 830", 11, BLUE);
        hardware.setGravity(Gravity.CENTER);
        hardware.setTypeface(null, Typeface.BOLD);
        hardware.setLetterSpacing(0.025f);
        hero.addView(hardware);

        TextView graphics = text(
            "MESA  ·  TURNIP  ·  VULKAN", 11, MUTED);
        graphics.setGravity(Gravity.CENTER);
        graphics.setPadding(0, dp(6), 0, dp(2));
        hero.addView(graphics);

        section(col, "YOUR WORKSPACE");

        item(col, "\u25A3", "Desktop",
            "Launch the Linux desktop, open your workspace and use desktop applications.",
            ORANGE, () -> open(DesktopActivity.class));

        item(col, "◎", "System Information",
            "Inspect your device, processor, Snapdragon platform, graphics hardware and rendering driver.",
            Color.rgb(114, 224, 177),
            () -> open(SystemInfoActivity.class));

        item(col, "\u2699", "Graphics & Settings",
            "Review graphics acceleration, display settings, network configuration and runtime environment.",
            BLUE, () -> open(SettingsActivity.class));

        item(col, "\u25C7", "Profiles",
            "View available Linux profiles and select the environment you want to use.",
            Color.rgb(183, 160, 255),
            () -> open(ProfileActivity.class));

        item(col, "\u25B8", "Terminal / Setup",
            "Open setup tools, inspect installation status and troubleshoot the Linux environment.",
            Color.rgb(114, 224, 177),
            () -> open(MainActivity.class));

        item(col, "\u24D8", "About GassNative",
            "Learn about GassNative, its build, features and project information.",
            BLUE, () -> open(AboutActivity.class));


        item(col, "\u25ce", "Crash Reports",
            "View local crash logs and diagnostic details.",
            BLUE, () -> open(CrashReportsActivity.class));

        section(col, "MUSIC");
        item(col, "♫", "GassNative Music",
            "Play your local music collection and GassNative audio tracks offline.",
            ORANGE, () -> open(MusicActivity.class));


section(col, "SMART HELPER");

        addSmartTip(col);

        section(col, "MORE LINUX SYSTEMS");

        TextView comingInfo = text(
            "Explore the roadmap. These systems are not installed by this screen.",
            12, MUTED);
        comingInfo.setPadding(dp(2), 0, dp(2), dp(12));
        col.addView(comingInfo);

        LinearLayout distroRow1 = new LinearLayout(this);
        distroRow1.setOrientation(LinearLayout.HORIZONTAL);
        col.addView(distroRow1);

        addDistroTile(distroRow1, "◉", "Ubuntu",
            "Friendly general-purpose Linux.",
            "https://ubuntu.com/", ORANGE);

        addDistroTile(distroRow1, "◈", "Kali Linux",
            "Security and penetration-testing distribution.",
            "https://www.kali.org/", BLUE);

        LinearLayout distroRow2 = new LinearLayout(this);
        distroRow2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams distroRowLp =
            new LinearLayout.LayoutParams(-1, -2);
        distroRowLp.topMargin = dp(10);
        col.addView(distroRow2, distroRowLp);

        addDistroTile(distroRow2, "△", "Alpine OS",
            "Small, security-focused Linux distribution.",
            "https://alpinelinux.org/", Color.rgb(114, 224, 177));

        addDistroTile(distroRow2, "λ", "Arch Linux",
            "Flexible rolling-release Linux distribution.",
            "https://archlinux.org/", Color.rgb(183, 160, 255));

        section(col, "SESSION");

        item(col, "\u23FB", "Power",
            "Close the GassNative home screen. Shut Linux down separately from the desktop.",
            ORANGE, () ->
                new AlertDialog.Builder(this)
                    .setTitle("Exit GassNative?")
                    .setMessage(
                        "This closes the Home screen. " +
                        "Use Power Off inside the desktop to stop Linux.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Exit", (d, w) -> finish())
                    .show());

        TextView footer = text(
            "GASSNATIVE  \u2022  LINUX ON ANDROID",
            10, Color.rgb(111, 139, 169));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(15), 0, dp(4));
        col.addView(footer);

        setContentView(scroll);
    }


    private void copyText(String label, String value) {
        ClipboardManager clipboard =
            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "Clipboard unavailable",
                Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, label + " copied",
            Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                .setTitle("Unable to open link")
                .setMessage("Open this address in your browser:\\n\\n" + url)
                .setPositiveButton("Copy link",
                    (d, w) -> copyText("Website", url))
                .setNegativeButton("Close", null)
                .show();
        }
    }

    private void addSmartTip(LinearLayout parent) {
        String[] tips = {
            "DESKTOP TIP\\nIf the desktop fails to start, return to Settings and try the Auto GPU option before changing other settings.",
            "GRAPHICS TIP\\nA selected GPU mode is only a preference. Inside Linux, check glxinfo -B and look at the renderer to verify real acceleration.",
            "STORAGE TIP\\nKeep enough free Android storage for the Debian root filesystem, package downloads and temporary extraction files.",
            "NETWORK TIP\\nIf Debian cannot resolve hostnames, inspect /etc/resolv.conf and verify that the Android connection is working.",
            "SAFE UPDATE TIP\\nBack up important files before large system changes. Run package-management commands inside the Debian environment, not blindly in Termux.",
            "TROUBLESHOOTING TIP\\nWhen reporting a problem, copy the exact error and include the command that produced it. This makes diagnosis much faster."
        };

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(15), dp(16), dp(15));
        panel.setBackground(shape(Color.rgb(18, 34, 43), 18,
            Color.rgb(45, 91, 100)));
        parent.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(top);

        TextView icon = text("✦", 30, Color.rgb(114, 224, 177));
        icon.setGravity(Gravity.CENTER);
        top.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout headingBox = new LinearLayout(this);
        headingBox.setOrientation(LinearLayout.VERTICAL);
        headingBox.setPadding(dp(12), 0, 0, 0);
        top.addView(headingBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView heading = text("GASS SMART TIP", 13, Color.rgb(114, 224, 177));
        heading.setTypeface(null, Typeface.BOLD);
        headingBox.addView(heading);

        TextView subtitle = text("Useful advice for your Linux workspace",
            11, MUTED);
        subtitle.setPadding(0, dp(3), 0, 0);
        headingBox.addView(subtitle);

        TextView body = text(tips[0], 14, Color.rgb(230, 239, 244));
        body.setLineSpacing(dp(3), 1f);
        body.setPadding(0, dp(14), 0, dp(10));
        panel.addView(body);

        final int[] index = {0};
        Button next = new Button(this);
        next.setText("Next tip  ›");
        next.setOnClickListener(v -> {
            index[0] = (index[0] + 1) % tips.length;
            body.setText(tips[index[0]]);
        });
        panel.addView(next, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addDistroTile(LinearLayout parent, String glyph,
            String name, String description, String url, int accent) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(8), dp(12), dp(8), dp(13));
        tile.setClickable(true);
        tile.setFocusable(true);

        LinearLayout.LayoutParams tileLp =
            new LinearLayout.LayoutParams(0, -2, 1f);
        tileLp.leftMargin = dp(4);
        tileLp.rightMargin = dp(4);
        parent.addView(tile, tileLp);

        TextView icon = text(glyph, 46, accent);
        icon.setGravity(Gravity.CENTER);
        icon.setTypeface(null, Typeface.BOLD);
        tile.addView(icon, new LinearLayout.LayoutParams(-1, dp(76)));

        TextView title = text(name, 15, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(5), 0, dp(5));
        tile.addView(title);

        TextView status = text("COMING SOON", 9, ORANGE);
        status.setGravity(Gravity.CENTER);
        status.setTypeface(null, Typeface.BOLD);
        status.setLetterSpacing(0.06f);
        status.setPadding(dp(5), dp(5), dp(5), dp(5));
        tile.addView(status);

        TextView detail = text(description, 11, MUTED);
        detail.setGravity(Gravity.CENTER);
        detail.setPadding(dp(3), dp(9), dp(3), dp(5));
        tile.addView(detail);

        tile.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle(name + " · Coming Soon")
                .setMessage(description + "\\n\\n" +
                    "This distribution is not installed by GassNative yet. " +
                    "You can read about it on its official website.")
                .setNegativeButton("Close", null)
                .setPositiveButton("Official website",
                    (dialog, which) -> openUrl(url))
                .show());
    }

    private void addBadge(LinearLayout parent, String label, int color) {
        TextView badge = text(label, 11, color);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(8), dp(9), dp(8));
        badge.setBackground(shape(Color.rgb(23, 34, 49), 10, BORDER));

        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = dp(7);
        parent.addView(badge, lp);
    }
}
