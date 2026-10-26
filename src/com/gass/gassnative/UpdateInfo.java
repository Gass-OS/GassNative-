package com.gass.gassnative;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;

public final class UpdateInfo {
    private static final String[] URLS = {
        "https://raw.githubusercontent.com/Gass-OS/GassNative-/main/update.json",
        "https://raw.githubusercontent.com/Gass-OS/GassNative-/master/update.json",
        "https://gass-os.github.io/GassNative-/update.json" };

    public interface Callback { void done(Result r); }

    public static class Result {
        public int state;
        public int installedCode, latestCode;
        public String installed, latest, notes, url;
        public String newsId, newsTitle, newsMsg, newsBtn, newsUrl;
    }

    private UpdateInfo() {}

    public static String installed(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    public static int installedCode(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    static int[] nums(String s) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)*").matcher(s == null ? "" : s);
        if (!m.find()) return new int[0];
        String[] p = m.group().split("\\.");
        int[] r = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            try { r[i] = Integer.parseInt(p[i]); } catch (Exception e) { r[i] = 0; }
        }
        return r;
    }

    static int cmp(int[] a, int[] b) {
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (x != y) return x < y ? -1 : 1;
        }
        return 0;
    }

    public static void check(final Context ctx, final boolean force, final Callback cb) {
        final Handler ui = new Handler(Looper.getMainLooper());
        final SharedPreferences sp = ctx.getSharedPreferences("gass_update2", Context.MODE_PRIVATE);
        final String instName = installed(ctx);
        final int instCode = installedCode(ctx);
        new Thread(() -> {
            final Result r = new Result();
            r.installed = instName;
            r.installedCode = instCode;
            String body = null;
            long now = System.currentTimeMillis();
            if (!force && now - sp.getLong("t", 0) < 3600L * 1000) body = sp.getString("json", null);
            if (body == null) {
                for (String u : URLS) {
                    try {
                        String b = GassUi.httpGet(u);
                        if (b.trim().startsWith("{") && b.contains("version")) { body = b; break; }
                    } catch (Exception ignored) {}
                }
                if (body != null) sp.edit().putString("json", body).putLong("t", now).apply();
                else body = sp.getString("json", null);
            }
            r.state = 3;
            if (body != null) {
                try {
                    JSONObject o = new JSONObject(body);
                    String vn = o.optString("versionName", "");
                    r.latestCode = o.optInt("versionCode", 0);
                    r.latest = "v" + vn + (r.latestCode > 0 ? " (build " + r.latestCode + ")" : "");
                    r.notes = o.optString("releaseNotes", "");
                    r.url = o.optString("downloadUrl", "");
                    boolean newer = r.latestCode > 0
                        ? r.latestCode > instCode
                        : cmp(nums(vn), nums(instName)) > 0;
                    r.state = newer ? 2 : 1;
                    JSONArray n = o.optJSONArray("news");
                    if (n != null && n.length() > 0) {
                        JSONObject x = n.getJSONObject(0);
                        r.newsId = x.optString("id", "");
                        r.newsTitle = x.optString("title", "");
                        r.newsMsg = x.optString("message", "");
                        r.newsBtn = x.optString("buttonText", "");
                        r.newsUrl = x.optString("url", "");
                    }
                } catch (Exception e) {
                    r.state = 3;
                }
            }
            ui.post(() -> cb.done(r));
        }, "gass-update").start();
    }
}
