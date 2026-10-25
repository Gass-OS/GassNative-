package com.gass.gassnative;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import java.io.File;
import java.util.ArrayList;

public class GamesActivity extends Activity {
    private static final String RES = "export DISPLAY=:1; R=$(xdpyinfo 2>/dev/null | awk '/dimensions:/{print $2; exit}'); [ -z \"$R\" ] && R=1280x720; export WINEPREFIX=/home/user/.gass-wine; ";
    private LinearLayout list;
    private TextView hint;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        int p = GassUi.dp(this, 16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        root.addView(GassUi.header(this, "Windows games", GassUi.GAME));

        LinearLayout d = GassUi.row(this, "Wine desktop", "Full-screen Windows desktop");
        d.setOnClickListener(v -> launch(RES + "wine explorer /desktop=shell,$R"));
        root.addView(d);

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
        if (ext.list() == null) {
            hint.setText("Storage not readable. Allow 'All files access' for GassNative in Android settings.");
        } else if (found.isEmpty()) {
            hint.setText("No .exe files found. Put games in Download/Games, then come back here.");
        } else {
            hint.setText(found.size() + " programs found");
        }
        String base = ext.getAbsolutePath();
        for (final File f : found) {
            String rel = f.getParent() == null ? "" : f.getParent().replace(base, "");
            LinearLayout r = GassUi.row(this, f.getName(), rel);
            r.setOnClickListener(v -> launch(exeCmd(f)));
            list.addView(r);
        }
    }

    private String exeCmd(File f) {
        String ext = Environment.getExternalStorageDirectory().getAbsolutePath();
        String g = "/sdcard" + f.getAbsolutePath().substring(ext.length());
        String q = "'" + g.replace("'", "'\\''") + "'";
        return RES + "W=$(winepath -w " + q + " 2>/dev/null); [ -z \"$W\" ] && W=" + q
            + "; cd \"$(dirname " + q + ")\"; wine explorer /desktop=GassGame,$R \"$W\"";
    }

    private void launch(String cmd) {
        Intent i = new Intent(this, DesktopActivity.class);
        i.putExtra("autorun", cmd);
        startActivity(i);
    }
}
