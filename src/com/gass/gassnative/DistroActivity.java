package com.gass.gassnative;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.File;

public class DistroActivity extends Activity {
    private static final Object[][] D = {
        {"Debian", "Stable, lightweight and well supported. Recommended.", 0xFFE11D48, 0xFF9F1239, true, "debian.png"},
        {"Ubuntu", "Friendly and popular, with a huge software catalog.", 0xFFF97316, 0xFFDC2626, false, "ubuntu.png"},
        {"Kali Linux", "Security testing and forensics toolkit.", 0xFF3B82F6, 0xFF1E3A8A, false, "kali.png"},
        {"Arch Linux", "Rolling release. Build your system your way.", 0xFF38BDF8, 0xFF2563EB, false, "arch.png"},
        {"Parrot OS", "Security, privacy and development tools.", 0xFF14B8A6, 0xFF0F766E, false, "parrot.png"} };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        int p = GassUi.dp(this, 16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        root.addView(GassUi.header(this, "Choose your Linux", GassUi.DESKTOP));
        TextView sub = GassUi.text(this, "Pick the system to install the first time. Debian is ready now, the others are on the way.", 13, GassUi.MUTED, false);
        sub.setPadding(0, 0, 0, GassUi.dp(this, 12));
        root.addView(sub);
        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        boolean installed = new File(Cfg.rootDir(this), "usr/bin/bash").isFile();
        for (Object[] d : D) list.addView(row(d, installed));
        GassUi.setContent(this, root);
    }

    private View row(Object[] d, boolean installed) {
        final String name = (String) d[0];
        final boolean ok = (Boolean) d[4];
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(GassUi.glass(this, 20));
        int pd = GassUi.dp(this, 14);
        r.setPadding(pd, pd, pd, pd);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = GassUi.dp(this, 10);
        r.setLayoutParams(lp);
        FrameLayout f = new FrameLayout(this);
        f.addView(new GassUi.GlowTile(this, (Integer) d[2], (Integer) d[3], true),
                new FrameLayout.LayoutParams(-1, -1));

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try (java.io.InputStream in = getAssets().open("ui/" + (String) d[5])) {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(in);
            if (bitmap != null) icon.setImageBitmap(bitmap);
        } catch (Exception ignored) {}

        if (icon.getDrawable() != null) {
            icon.setPadding(GassUi.dp(this, 10), GassUi.dp(this, 10),
                    GassUi.dp(this, 10), GassUi.dp(this, 10));
            f.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        } else {
            f.addView(GassUi.text(this, name.substring(0, 1), 24, Color.WHITE, true),
                    new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        }
        r.addView(f, new LinearLayout.LayoutParams(GassUi.dp(this, 64), GassUi.dp(this, 64)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(GassUi.dp(this, 12), 0, GassUi.dp(this, 8), 0);
        col.addView(GassUi.text(this, name, 18, GassUi.TEXT, true));
        col.addView(GassUi.text(this, (String) d[1], 12, GassUi.MUTED, false));
        r.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = GassUi.text(this, ok ? (installed ? "INSTALLED" : "INSTALL") : "COMING SOON", 10, Color.WHITE, true);
        badge.setLetterSpacing(0.08f);
        GradientDrawable g = new GradientDrawable();
        g.setColor(ok ? 0xFF16A34A : 0x66F59E0B);
        g.setCornerRadius(GassUi.dp(this, 12));
        badge.setBackground(g);
        badge.setPadding(GassUi.dp(this, 10), GassUi.dp(this, 5), GassUi.dp(this, 10), GassUi.dp(this, 5));
        r.addView(badge);
        if (!ok) r.setAlpha(0.7f);
        r.setOnClickListener(v -> {
            if (ok) startActivity(new Intent(this, MainActivity.class));
            else Toast.makeText(this, name + " is coming soon", Toast.LENGTH_SHORT).show();
        });
        return r;
    }
}
