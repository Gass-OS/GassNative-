package com.gass.gassnative;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class UpdateChecker {
    private static final String UPDATE_URL =
        "https://gass-os.github.io/GassNative-/update.json";
    private static final String RELEASES_URL =
        "https://github.com/Gass-OS/GassNative-/releases/latest";

    private UpdateChecker() {}

    public static void check(Activity activity) {
        Thread worker = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection)
                    new URL(UPDATE_URL + "?t=" + System.currentTimeMillis())
                        .openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");

                if (connection.getResponseCode() != 200) return;

                StringBuilder body = new StringBuilder();
                try (InputStream in = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(
                         new InputStreamReader(in, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                        if (body.length() > 65536) return;
                    }
                }

                JSONObject data = new JSONObject(body.toString());
                int latestCode = data.optInt("versionCode", 0);
                String latestName = data.optString("versionName", "");
                String notes = data.optString(
                    "releaseNotes", "Bug fixes and improvements.");
                String downloadUrl = data.optString(
                    "downloadUrl", RELEASES_URL);

                if (latestCode <= 0 || latestName.trim().isEmpty()) return;

                if (!downloadUrl.startsWith(
                    "https://github.com/Gass-OS/GassNative-/releases")) {
                    downloadUrl = RELEASES_URL;
                }

                PackageInfo info = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);

                long installedCode = Build.VERSION.SDK_INT >= 28
                    ? info.getLongVersionCode() : info.versionCode;

                if (latestCode <= installedCode) return;

                final String version = latestName;
                final String releaseNotes = notes;
                final String url = downloadUrl;

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (activity.isFinishing()) return;
                    if (Build.VERSION.SDK_INT >= 17
                        && activity.isDestroyed()) return;

                    new AlertDialog.Builder(activity)
                        .setTitle("GassNative update available")
                        .setMessage("Version " + version
                            + " is available.\n\n" + releaseNotes)
                        .setNegativeButton("Later", null)
                        .setPositiveButton("Update now", (dialog, which) -> {
                            try {
                                activity.startActivity(new Intent(
                                    Intent.ACTION_VIEW, Uri.parse(url)));
                            } catch (Exception ignored) {
                            }
                        })
                        .show();
                });
            } catch (Exception ignored) {
                // Offline or unavailable: let the app open normally.
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "GassNative-UpdateCheck");

        worker.setDaemon(true);
        worker.start();
    }
}
