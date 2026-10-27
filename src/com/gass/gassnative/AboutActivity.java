package com.gass.gassnative;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class AboutActivity extends Activity {

    final int BG = Color.rgb(13, 17, 25);
    final int CARD = Color.rgb(25, 31, 43);
    final int WHITE = Color.WHITE;
    final int MUTED = Color.rgb(155, 166, 184);
    final int BLUE = Color.rgb(75, 155, 255);

    LinearLayout page;

    int dp(int n) {
        return (int)(n * getResources()
            .getDisplayMetrics().density + 0.5f);
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    void section(String title) {
        TextView t = text(title.toUpperCase(), 12, BLUE);
        t.setTypeface(null, Typeface.BOLD);
        t.setLetterSpacing(0.12f);
        t.setPadding(0, dp(24), 0, dp(12));
        page.addView(t);
    }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(1);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(shape(CARD, 16));
        page.addView(c, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }

    void info(LinearLayout c, String label, String value) {
        TextView a = text(label, 12, MUTED);
        c.addView(a);

        TextView b = text(value, 15, WHITE);
        b.setTextIsSelectable(true);
        b.setPadding(0, dp(4), 0, dp(14));
        c.addView(b);
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        String version = "0.1";
        try {
            version = getPackageManager()
                .getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {}

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        page = new LinearLayout(this);
        page.setOrientation(1);
        page.setPadding(dp(24), dp(28), dp(24), dp(35));

        scroll.addView(page);

        TextView back = text("‹   About", 20, WHITE);
        back.setTypeface(null, Typeface.BOLD);
        back.setOnClickListener(v -> finish());
        page.addView(back);

        ImageView logo = new ImageView(this);
        logo.setImageResource(
            getResources().getIdentifier(
                "ic_launcher", "mipmap", getPackageName()
            )
        );

        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(dp(100), dp(100));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp(32);
        page.addView(logo, lp);

        TextView name = text("GassNative", 29, WHITE);
        name.setGravity(Gravity.CENTER);
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, dp(15), 0, dp(5));
        page.addView(name);

        TextView ver = text("Version " + version, 14, BLUE);
        ver.setGravity(Gravity.CENTER);
        page.addView(ver);

        TextView slogan = text(
            "PUMPKIN R8\u2122  |  Explore. Build. Innovate. Be Free.",
            14, MUTED
        );
        slogan.setGravity(Gravity.CENTER);
        slogan.setPadding(0, dp(12), 0, dp(15));
        page.addView(slogan);

        section("About the project");

        LinearLayout description = card();

        TextView desc = text(
            "GassNative PUMPKIN R8\u2122 brings Linux closer to your pocket. "
          + "Run a Debian 13 (Trixie) ARM64 environment on Android "
          + "without root access.\n\n"
          + "Use the integrated XFCE4 desktop, terminal tools, "
          + "Linux profiles, system information and configurable "
          + "settings from one Android app. The integrated display "
          + "is designed to avoid requiring a separate display app.\n\n"
          + "Explore Linux applications, customize your workspace "
          + "and build your mobile development environment. "
          + "GassNative is an evolving Gass-OS project made for "
          + "developers, makers and Linux enthusiasts.\n\n"
          + "No ads. No telemetry. Your device, your freedom.",
            14, WHITE
        );
        desc.setLineSpacing(dp(4), 1f);
        description.addView(desc);

        section("System information");

        LinearLayout sys = card();

        info(sys, "Linux distribution", "Debian 13 (Trixie)");
        info(sys, "Desktop environment", "XFCE4");
        info(sys, "Architecture", Build.SUPPORTED_ABIS[0]);
        info(sys, "Android version", Build.VERSION.RELEASE);
        info(sys, "Device", Build.MANUFACTURER + " " + Build.MODEL);
        info(sys, "Runtime", "PRoot");
        info(sys, "Display", "Integrated desktop display");

        section("Developer");

        LinearLayout dev = card();

        info(dev, "Created by", "Aymen Gharsalli");
        info(dev, "Project", "Gass-OS");
        info(dev, "Application", "GassNative");

        TextView github = text(
            "GitHub  ↗  github.com/Gass-OS",
            15, BLUE
        );
        github.setPadding(0, dp(5), 0, dp(8));

        github.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/Gass-OS"));
            startActivity(i);
        });

        dev.addView(github);

        TextView youtube = text(
            "\u25B6  YouTube  \u2022  LuxTrust OS  \u2197",
            15, BLUE
        );
        youtube.setPadding(0, dp(8), 0, dp(8));
        youtube.setOnClickListener(v -> openExternal(
            "https://www.youtube.com/results?search_query=LuxTrust+OS"));
        dev.addView(youtube);

        TextView telegram = text(
            "\u2708  Telegram  \u2022  Gass-OS  \u2197",
            15, BLUE
        );
        telegram.setPadding(0, dp(8), 0, dp(8));
        telegram.setOnClickListener(v ->
            openExternal("https://t.me/Gassos94"));
        dev.addView(telegram);

        TextView projectSite = text(
            "\uD83C\uDF10  Gass-OS on GitHub  \u2197",
            15, BLUE
        );
        projectSite.setPadding(0, dp(8), 0, dp(4));
        projectSite.setOnClickListener(v ->
            openExternal("https://github.com/Gass-OS"));
        dev.addView(projectSite);

        section("Open-source credits");

        LinearLayout credits = card();

        TextView licenses = text(
            "Debian GNU/Linux\n"
          + "XFCE Desktop Environment\n"
          + "PRoot\n"
          + "talloc\n"
          + "XZ / liblzma\n\n"
          + "Third-party components remain subject to "
          + "their respective open-source licenses.",
            14, MUTED
        );
        licenses.setLineSpacing(dp(5), 1f);
        credits.addView(licenses);

        TextView footer = text(
            "© 2026 Gass-OS\nMade with passion.",
            12, MUTED
        );
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(35), 0, dp(10));
        page.addView(footer);

        Ui.addTopBar(this, page);
        Ui.styleTree(this, page);
        setContentView(scroll);
    }
}
