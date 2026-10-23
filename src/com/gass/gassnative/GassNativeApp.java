package com.gass.gassnative;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GassNativeApp extends Application {
    private Thread.UncaughtExceptionHandler previousHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        previousHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                saveCrashReport(thread, error);
            } catch (Throwable ignored) {
                // Never let reporting prevent normal crash handling.
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, error);
            } else {
                android.os.Process.killProcess(
                    android.os.Process.myPid()
                );
                System.exit(10);
            }
        });
    }

    private void saveCrashReport(Thread thread, Throwable error)
            throws Exception {
        File dir = new File(getFilesDir(), "crash-reports");
        if (!dir.exists() && !dir.mkdirs()) return;

        String stamp = new SimpleDateFormat(
            "yyyyMMdd-HHmmss", Locale.US
        ).format(new Date());

        File report = new File(dir, "crash-" + stamp + ".txt");

        StringWriter stack = new StringWriter();
        error.printStackTrace(new PrintWriter(stack));

        String version = "unknown";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                getPackageName(), 0
            );
            version = info.versionName + " (" + info.versionCode + ")";
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(report), "UTF-8"))) {
            writer.write("GassNative local crash report\n");
            writer.write("Time: " + new Date() + "\n");
            writer.write("App version: " + version + "\n");
            writer.write("Android: " + Build.VERSION.RELEASE + "\n");
            writer.write("SDK: " + Build.VERSION.SDK_INT + "\n");
            writer.write("Device: " + Build.MANUFACTURER + " "
                + Build.MODEL + "\n");
            writer.write("Thread: " + thread.getName() + "\n\n");
            writer.write(stack.toString());
        }
    }
}
