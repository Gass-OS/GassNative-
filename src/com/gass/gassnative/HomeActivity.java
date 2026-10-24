package com.gass.gassnative;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.io.File;

public class HomeActivity extends Activity {
    private View banner;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        int p = GassUi.dp(this, 18);
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        sv.addView(root);

        TextView title = GassUi.text(this, "GassNative", 32, GassUi.TEXT, true);
        title.setPadding(GassUi.dp(this, 4), GassUi.dp(this, 18), 0, 0);
        root.addView(title);
        TextView by = GassUi.text(this, "Powered by Aymen Gharsalli", 13, GassUi.MUTED, false);
        by.setPadding(GassUi.dp(this, 4), GassUi.dp(this, 2), 0, GassUi.dp(this, 18));
        root.addView(by);

        LinearLayout bn = GassUi.row(this, "Linux setup needed", "Tap to download and install Debian");
        bn.setBackground(GassUi.glass(this, 18, 0x44FF9E64));
        bn.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        banner = bn;
        root.addView(bn);

        Object[][] items = {
            {GassUi.DESKTOP, "Desktop", "Linux desktop", DesktopActivity.class},
            {GassUi.GAME, "Windows games", "Wine, full screen", GamesActivity.class},
            {GassUi.TV, "TV", "Live channels", TvActivity.class},
            {GassUi.RADIO, "Radio", "Internet stations", RadioActivity.class},
            {GassUi.MUSIC, "Music", "Player", MusicActivity.class},
            {GassUi.TERMINAL, "Terminal", "Setup and commands", MainActivity.class},
            {GassUi.PROFILE, "Profiles", "Linux environments", ProfileActivity.class},
            {GassUi.SETTINGS, "Settings", "Options", SettingsActivity.class},
            {GassUi.INFO, "About", "Version and credits", AboutActivity.class} };
        for (int i = 0; i < items.length; i += 2) {
            LinearLayout r = new LinearLayout(this);
            for (int j = i; j < i + 2; j++) {
                View t;
                if (j < items.length) {
                    Object[] it = items[j];
                    t = tile((Integer) it[0], (String) it[1], (String) it[2], (Class<?>) it[3]);
                } else {
                    t = new View(this);
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, GassUi.dp(this, 128), 1);
                int m = GassUi.dp(this, 5);
                lp.setMargins(m, m, m, m);
                r.addView(t, lp);
            }
            root.addView(r);
        }
        setContentView(sv);
    }

    private View tile(int icon, String title, String sub, final Class<?> target) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setBackground(GassUi.glass(this, 22));
        int pd = GassUi.dp(this, 16);
        t.setPadding(pd, pd, pd, pd);
        t.setGravity(Gravity.BOTTOM);
        t.addView(new GassUi.Icon(this, icon, GassUi.BLUE),
            new LinearLayout.LayoutParams(GassUi.dp(this, 34), GassUi.dp(this, 34)));
        TextView name = GassUi.text(this, title, 17, GassUi.TEXT, true);
        name.setPadding(0, GassUi.dp(this, 14), 0, 0);
        t.addView(name);
        t.addView(GassUi.text(this, sub, 12, GassUi.MUTED, false));
        t.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) v.setAlpha(0.7f);
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) v.setAlpha(1f);
            return false;
        });
        t.setOnClickListener(v -> open(target));
        return t;
    }

    private boolean debianReady() {
        return new File(Cfg.rootDir(this), "usr/bin/bash").isFile();
    }

    private void open(Class<?> c) {
        if ((c == DesktopActivity.class || c == GamesActivity.class) && !debianReady()) {
            Toast.makeText(this, "Setting up Debian first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            return;
        }
        startActivity(new Intent(this, c));
    }

    @Override
    protected void onResume() {
        super.onResume();
        banner.setVisibility(debianReady() ? View.GONE : View.VISIBLE);
    }
}
