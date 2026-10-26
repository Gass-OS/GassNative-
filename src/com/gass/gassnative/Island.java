package com.gass.gassnative;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;

public final class Island {
    public static volatile String musicTitle, musicTarget, taskLabel, taskTarget;
    public static volatile boolean musicPlaying;
    public static volatile int taskPct = -1;
    private static final ArrayList<Runnable> L = new ArrayList<>();
    private static final Handler H = new Handler(Looper.getMainLooper());

    private Island() {}

    public static synchronized void addListener(Runnable r) { if (!L.contains(r)) L.add(r); }

    public static synchronized void removeListener(Runnable r) { L.remove(r); }

    private static void fire() {
        final ArrayList<Runnable> copy;
        synchronized (Island.class) { copy = new ArrayList<>(L); }
        H.post(() -> { for (Runnable r : copy) r.run(); });
    }

    public static void music(String title, boolean playing, String target) {
        musicTitle = title; musicPlaying = playing; musicTarget = target; fire();
    }

    public static void musicOff() {
        musicTitle = null; musicPlaying = false; musicTarget = null; fire();
    }

    public static void task(String label, int pct, String target) {
        taskLabel = label; taskPct = pct; taskTarget = target; fire();
    }

    public static void taskOff() {
        taskLabel = null; taskPct = -1; taskTarget = null; fire();
    }
}
