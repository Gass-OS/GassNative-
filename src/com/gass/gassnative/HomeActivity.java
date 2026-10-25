package com.gass.gassnative;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends Activity {
    private static final int WHITE = 0xFFF4F7FF, SOFT = 0xFF9FB0D6;
    private GassUi.GradText timeV;
    private TextView dayV, dateV;
    private GassUi.SecBar secBar;
    private View setupCard;
    private final Handler clock = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Locale loc = Locale.getDefault();
            Date d = new Date();
            timeV.setText(new SimpleDateFormat("HH:mm", loc).format(d));
            dayV.setText(new SimpleDateFormat("EEEE", loc).format(d).toUpperCase(loc));
            dateV.setText(new SimpleDateFormat("d MMMM yyyy", loc).format(d));
            Calendar cal = Calendar.getInstance();
            secBar.setFrac((cal.get(Calendar.SECOND) * 1000f + cal.get(Calendar.MILLISECOND)) / 60000f);
            clock.postDelayed(this, 500);
        }
    };

    private int dp(float v) { return GassUi.dp(this, v); }

    private GradientDrawable box(int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private LinearLayout card(int fill, int stroke) {
        LinearLayout c = new LinearLayout(this);
        c.setBackground(box(fill, stroke, 24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout glass() { return card(0x240C1930, 0x445E789E); }

    private TextView txt(String s, float sp, int color, String family, float spacing) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(family, Typeface.NORMAL));
        if (spacing != 0) t.setLetterSpacing(spacing);
        return t;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        sv.addView(root);

        LinearLayout head = glass();
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(12), dp(14), dp(16), dp(14));
        FrameLayout logo = new FrameLayout(this);
        logo.addView(new GassUi.GlowTile(this, 0, 0xFFA78BFA, false), new FrameLayout.LayoutParams(-1, -1));
        ImageView ic = new ImageView(this);
        try { ic.setImageDrawable(getPackageManager().getApplicationIcon(getApplicationInfo())); } catch (Throwable ignored) {}
        ic.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), v.getWidth() * 0.26f);
            }
        });
        ic.setClipToOutline(true);
        logo.addView(ic, new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER));
        head.addView(logo, new LinearLayout.LayoutParams(dp(88), dp(88)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), 0, 0, 0);
        GassUi.GradText title = new GassUi.GradText(this, new int[]{0xFF6EC1FF, 0xFFA78BFA, 0xFFF472B6});
        title.setText("GassNative");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        col.addView(title, new LinearLayout.LayoutParams(-2, -2));
        TextView pill = txt("PUMPKIN R2 \u2122", 10.5f, WHITE, "sans-serif-medium", 0.1f);
        pill.setBackground(box(0x33A78BFA, 0x66A78BFA, 12));
        pill.setPadding(dp(10), dp(3), dp(10), dp(3));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-2, -2);
        plp.topMargin = dp(4);
        plp.bottomMargin = dp(6);
        col.addView(pill, plp);
        col.addView(txt("Explore. Build. Innovate. Be Free.", 13, WHITE, "sans-serif", 0));
        col.addView(txt("Linux on Android without root. No ads. No telemetry. Powered by Gass-OS\u2122", 11.5f, SOFT, "sans-serif-light", 0));
        head.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(head);

        LinearLayout clk = glass();
        clk.setOrientation(LinearLayout.VERTICAL);
        clk.setPadding(dp(18), dp(14), dp(18), dp(16));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        timeV = new GassUi.GradText(this, new int[]{0xFF58A6FF, 0xFF9878F5, 0xFFE05270});
        timeV.setTextSize(70);
        timeV.setTextColor(Color.WHITE);
        timeV.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        timeV.setIncludeFontPadding(false);
        top.addView(timeV, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.END);
        side.setPadding(dp(8), 0, 0, 0);
        dayV = txt("", 12, Color.WHITE, "sans-serif-medium", 0.12f);
        GradientDrawable gp = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF2354A5, 0xFF84314F});
        gp.setCornerRadius(dp(14));
        dayV.setBackground(gp);
        dayV.setPadding(dp(14), dp(5), dp(14), dp(5));
        dayV.setSingleLine(true);
        side.addView(dayV, new LinearLayout.LayoutParams(-2, -2));
        dateV = txt("", 12.5f, WHITE, "sans-serif", 0.02f);
        dateV.setSingleLine(true);
        dateV.setPadding(0, dp(8), 0, 0);
        side.addView(dateV, new LinearLayout.LayoutParams(-2, -2));
        top.addView(side, new LinearLayout.LayoutParams(0, -2, 1));
        clk.addView(top);
        secBar = new GassUi.SecBar(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, dp(4));
        blp.topMargin = dp(12);
        clk.addView(secBar, blp);
        root.addView(clk);

        LinearLayout setup = card(0x26F97316, 0x66F97316);
        setup.setOrientation(LinearLayout.VERTICAL);
        setup.setPadding(dp(16), dp(12), dp(16), dp(12));
        setup.addView(txt("FIRST-TIME SETUP", 11, 0xFFFDBA74, "sans-serif-medium", 0.12f));
        TextView sb = txt("Open Terminal Debian to install Debian Desktop and Wine. It can take a while, so keep the app open. Then launch Linux from Debian Desktop.", 12.5f, WHITE, "sans-serif-light", 0);
        sb.setPadding(0, dp(4), 0, 0);
        setup.addView(sb);
        setup.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        setupCard = setup;
        root.addView(setup);

        LinearLayout upd = card(0x1A34D399, 0x5534D399);
        upd.setGravity(Gravity.CENTER_VERTICAL);
        upd.setPadding(dp(16), dp(10), dp(16), dp(10));
        SpannableString ss = new SpannableString("\u25CF  You are on the latest version \u2014 new release coming soon");
        ss.setSpan(new ForegroundColorSpan(0xFF34D399), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView ut = txt("", 12.5f, WHITE, "sans-serif", 0);
        ut.setText(ss);
        upd.addView(ut);
        root.addView(upd);

        LinearLayout grid = glass();
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(6), dp(10), dp(6), dp(6));
        Object[][] T = {
            {"TERMINAL DEBIAN", 0xFF174C48, 0xFF102C40, GassUi.TERMINAL, "term"},
            {"DEBIAN DESKTOP", 0xFF263D83, 0xFF49316B, GassUi.DESKTOP, "desktop"},
            {"SETTING & AUDIT", 0xFF39475E, 0xFF202B40, GassUi.SETTINGS, "settings"},
            {"WINDOW GAMES", 0xFF164A7C, 0xFF233A70, GassUi.WINDOWS, "games"},
            {"TELEVISION TV", 0xFF79294C, 0xFF48245D, GassUi.TV, "tv"},
            {"RADIO CHANNELS", 0xFF784331, 0xFF59283A, GassUi.RADIO, "radio"},
            {"OFFLINE MUSIC", 0xFF58347E, 0xFF702D58, GassUi.MUSIC, "music"},
            {"ABOUT ME", 0xFF174E70, 0xFF293B78, GassUi.INFO, "about"},
            {"USEFUL TIPS", 0xFF175A66, 0xFF233D70, GassUi.BULB, "tips"},
            {"COMING SOON", 0xFF803C32, 0xFF69233E, GassUi.PLANET, "ubuntu"},
            {"COMING SOON", 0xFF244E8B, 0xFF49316F, GassUi.SHIELD, "kali"},
            {"POWER OFF", 0xFF792B46, 0xFF481D45, GassUi.POWER, "power"} };
        for (int r = 0; r < 3; r++) {
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < 4; c++) {
                Object[] it = T[r * 4 + c];
                row.addView(tile((String) it[0], (Integer) it[1], (Integer) it[2], (Integer) it[3], (String) it[4]),
                    new LinearLayout.LayoutParams(0, -2, 1));
            }
            grid.addView(row);
        }
        root.addView(grid);

        LinearLayout foot = glass();
        foot.setOrientation(LinearLayout.VERTICAL);
        foot.setPadding(dp(16), dp(12), dp(16), dp(12));
        foot.addView(txt("GASSNATIVE PUMPKIN V0.5 \u2014 PRE-RELEASE APK\u2122", 11, WHITE, "sans-serif-medium", 0.04f));
        TextView fb = txt("No ads. No tracking. No telemetry. Your smartphone, your freedom. GassNative lets you run Linux operating systems on Android without root access. Your device belongs to you \u2014 as it should be. \uD83D\uDC99", 11, SOFT, "sans-serif-light", 0);
        fb.setPadding(0, dp(6), 0, 0);
        foot.addView(fb);
        root.addView(foot);

        GassUi.setContent(this, sv);
    }

    private View tile(String label, int c1, int c2, int glyph, final String code) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(2), dp(4), dp(2), dp(10));
        FrameLayout f = new FrameLayout(this);
        f.addView(new GassUi.GlowTile(this, c1, c2, true), new FrameLayout.LayoutParams(-1, -1));
        f.addView(new GassUi.PapirusIcon(this, glyph),
            new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER));
        t.addView(f, new LinearLayout.LayoutParams(dp(82), dp(82)));
        TextView l = txt(label, 10.5f, WHITE, "sans-serif-medium", 0.05f);
        l.setGravity(Gravity.CENTER);
        l.setMaxLines(2);
        t.addView(l, new LinearLayout.LayoutParams(-1, -2));
        t.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            return false;
        });
        t.setOnClickListener(v -> go(code));
        return t;
    }

    private boolean debianReady() {
        return new File(Cfg.rootDir(this), "usr/bin/bash").isFile();
    }

    private void needDebian(Class<?> c) {
        if (!debianReady()) {
            Toast.makeText(this, "Install Debian first: open Terminal Debian", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            return;
        }
        startActivity(new Intent(this, c));
    }

    private void go(String c) {
        switch (c) {
            case "term": startActivity(new Intent(this, MainActivity.class)); break;
            case "desktop": needDebian(DesktopActivity.class); break;
            case "settings": startActivity(new Intent(this, SettingsActivity.class)); break;
            case "games": needDebian(GamesActivity.class); break;
            case "tv": startActivity(new Intent(this, TvActivity.class)); break;
            case "radio": startActivity(new Intent(this, RadioActivity.class)); break;
            case "music": startActivity(new Intent(this, MusicActivity.class)); break;
            case "about": startActivity(new Intent(this, AboutActivity.class)); break;
            case "tips": startActivity(new Intent(this, TipsActivity.class)); break;
            case "ubuntu": Toast.makeText(this, "Ubuntu is coming soon", Toast.LENGTH_SHORT).show(); break;
            case "kali": Toast.makeText(this, "Kali Linux is coming soon", Toast.LENGTH_SHORT).show(); break;
            case "power": powerOff(); break;
        }
    }

    private void powerOff() {
        new AlertDialog.Builder(this)
            .setTitle("Power off")
            .setMessage("Stop the Linux desktop and close GassNative?")
            .setPositiveButton("Power off", (d, w) -> {
                if (DesktopActivity.instance != null) DesktopActivity.instance.powerOff();
                finishAffinity();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupCard.setVisibility(debianReady() ? View.GONE : View.VISIBLE);
        clock.removeCallbacks(tick);
        clock.post(tick);
    }

    @Override
    protected void onPause() {
        clock.removeCallbacks(tick);
        super.onPause();
    }
}
