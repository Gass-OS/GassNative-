package com.gass.gassnative;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import java.io.File;
import java.util.ArrayList;

public class GamesActivity extends Activity {
    private LinearLayout list;
    private TextView hint, modeTitle, modeSub;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        sp = getSharedPreferences("gass", MODE_PRIVATE);
        int p = GassUi.dp(this, 16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        root.addView(GassUi.header(this, "Windows", GassUi.GAME));

        LinearLayout d = GassUi.row(this, "Windows desktop", "Full-screen Wine desktop with taskbar and Start menu. No Linux desktop.");
        d.setOnClickListener(v -> launch(null));
        root.addView(d);

        LinearLayout m = GassUi.row(this, "x", "x");
        modeTitle = (TextView) m.getChildAt(0);
        modeSub = (TextView) m.getChildAt(1);
        m.setOnClickListener(v -> {
            sp.edit().putBoolean("wine_wayland", !wayland()).apply();
            showMode();
        });
        root.addView(m);
        showMode();

        hint = GassUi.text(this, "", 12, GassUi.MUTED, false);
        hint.setPadding(0, 0, 0, GassUi.dp(this, 8));
        root.addView(hint);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        GassUi.setContent(this, root);
    }

    private boolean wayland() { return sp.getBoolean("wine_wayland", false); }

    private void showMode() {
        modeTitle.setText("Graphics: " + (wayland() ? "Wayland + GPU (experimental)" : "X11 (safe)"));
        modeSub.setText("Tap to switch. Wayland needs the viewporter-enabled compositor.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void scan(File dir, int depth, ArrayList<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (out.size() >= 150) return;
            if (f.isDirectory()) {
                if (depth < 3) scan(f, depth + 1, out);
            } else if (f.getName().toLowerCase().endsWith(".exe") && !out.contains(f)) {
                out.add(f);
            }
        }
    }

    private void refresh() {
        list.removeAllViews();
        File ext = Environment.getExternalStorageDirectory();
        ArrayList<File> found = new ArrayList<>();
        scan(new File(ext, "Games"), 1, found);
        scan(new File(ext, "Download/Games"), 1, found);
        scan(new File(ext, "Download"), 2, found);
        java.util.Collections.sort(found, (a, c) -> a.getName().compareToIgnoreCase(c.getName()));
        if (ext.list() == null) hint.setText("Storage not readable. Allow 'All files access' for GassNative in Android settings.");
        else if (found.isEmpty()) hint.setText("No .exe files found. Put games in Download/Games, then come back here.");
        else hint.setText(found.size() + " programs found");
        String base = ext.getAbsolutePath();
        for (final File f : found) {
            String rel = f.getParent() == null ? "" : f.getParent().replace(base, "");
            LinearLayout r = GassUi.row(this, f.getName(), rel);
            r.setOnClickListener(v -> launch(guestPath(f)));
            list.addView(r);
        }
    }

    private String guestPath(File f) {
        String ext = Environment.getExternalStorageDirectory().getAbsolutePath();
        return "/sdcard" + f.getAbsolutePath().substring(ext.length());
    }

    private void launch(String exe) {
        DesktopActivity.launch(this, "wine", exe, wayland() ? "wayland" : "x11");
    }
}
