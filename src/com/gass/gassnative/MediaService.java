package com.gass.gassnative;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MediaService extends Service {
    public static final String A_PLAY = "gass.PLAY";
    public static final String A_TOGGLE = "gass.TOGGLE";
    public static final String A_STOP = "gass.STOP";
    public static final String A_NEXT = "gass.NEXT";
    public static final String A_PREV = "gass.PREV";
    public static final String A_SEEK = "gass.SEEK";

    public static volatile String currentTitle;
    public static volatile boolean isPlaying;
    public static volatile int positionMs;
    public static volatile int durationMs;

    private static final int NID = 77;
    private MediaPlayer mp;
    private String title = "GassNative Music";
    private String target = "MusicActivity";
    private String[] paths;
    private String[] titles;
    private int index;
    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (mp != null) {
                try {
                    positionMs = mp.getCurrentPosition();
                    durationMs = mp.getDuration();
                    isPlaying = mp.isPlaying();
                    Island.music(title, isPlaying, target);
                } catch (Exception ignored) {}
            }
            if (mp != null) handler.postDelayed(this, 500);
        }
    };

    public static void play(Context c, String url, String title, String target) {
        Intent i = new Intent(c, MediaService.class).setAction(A_PLAY);
        i.putExtra("url", url);
        i.putExtra("title", title);
        i.putExtra("target", target);
        start(c, i);
    }

    public static void playPlaylist(Context c, String[] paths, String[] titles, int index) {
        Intent i = new Intent(c, MediaService.class).setAction(A_PLAY);
        i.putExtra("paths", paths);
        i.putExtra("titles", titles);
        i.putExtra("index", index);
        i.putExtra("target", "MusicActivity");
        start(c, i);
    }

    private static void start(Context c, Intent i) {
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    public static void toggle(Context c) {
        c.startService(new Intent(c, MediaService.class).setAction(A_TOGGLE));
    }

    public static void next(Context c) {
        c.startService(new Intent(c, MediaService.class).setAction(A_NEXT));
    }

    public static void previous(Context c) {
        c.startService(new Intent(c, MediaService.class).setAction(A_PREV));
    }

    public static void seek(Context c, int ms) {
        c.startService(new Intent(c, MediaService.class).setAction(A_SEEK).putExtra("position", ms));
    }

    public static void stop(Context c) {
        c.startService(new Intent(c, MediaService.class).setAction(A_STOP));
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent in, int flags, int id) {
        String a = in == null ? null : in.getAction();

        if (A_PLAY.equals(a)) {
            String[] newPaths = in.getStringArrayExtra("paths");
            String[] newTitles = in.getStringArrayExtra("titles");
            if (newPaths != null && newTitles != null && newPaths.length > 0
                    && newPaths.length == newTitles.length) {
                paths = newPaths;
                titles = newTitles;
                index = Math.max(0, Math.min(in.getIntExtra("index", 0), paths.length - 1));
                target = in.getStringExtra("target");
                if (target == null) target = "MusicActivity";
                playIndex(true);
            } else {
                paths = null;
                titles = null;
                title = in.getStringExtra("title");
                target = in.getStringExtra("target");
                if (title == null) title = "Playing";
                startForeground(NID, notif(false));
                begin(in.getStringExtra("url"));
            }
        } else if (A_TOGGLE.equals(a)) {
            if (mp != null) toggleNow();
        } else if (A_NEXT.equals(a)) {
            if (paths != null) { index = (index + 1) % paths.length; playIndex(true); }
        } else if (A_PREV.equals(a)) {
            if (paths != null) { index = (index + paths.length - 1) % paths.length; playIndex(true); }
        } else if (A_SEEK.equals(a)) {
            try {
                if (mp != null) mp.seekTo(Math.max(0, in.getIntExtra("position", 0)));
            } catch (Exception ignored) {}
        } else {
            shutdown();
        }
        return START_NOT_STICKY;
    }

    private String cachedAsset(String asset) throws Exception {
        File dir = new File(getCacheDir(), "music");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Cannot create music cache");
        File outFile = new File(dir, "cached_" + new File(asset).getName());
        if (outFile.isFile() && outFile.length() > 0) return outFile.getAbsolutePath();
        try (InputStream in = getAssets().open(asset);
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
        return outFile.getAbsolutePath();
    }

    private void playIndex(boolean autoplay) {
        title = titles[index];
        try {
            begin(cachedAsset(paths[index]));
        } catch (Exception e) {
            shutdown();
        }
    }

    private void begin(String url) {
        release();
        startForeground(NID, notif(false));
        currentTitle = title;
        isPlaying = false;
        positionMs = 0;
        durationMs = 0;
        Island.music(title, false, target);
        try {
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mp.setDataSource(url);
            mp.setOnPreparedListener(m -> {
                m.start();
                isPlaying = true;
                durationMs = m.getDuration();
                Island.music(title, true, target);
                refreshNotif(true);
                handler.removeCallbacks(ticker);
                handler.post(ticker);
            });
            mp.setOnCompletionListener(m -> {
                if (paths != null && paths.length > 0) {
                    index = (index + 1) % paths.length;
                    playIndex(true);
                } else {
                    shutdown();
                }
            });
            mp.setOnErrorListener((m, w, x) -> { shutdown(); return true; });
            mp.prepareAsync();
        } catch (Exception e) {
            shutdown();
        }
    }

    private void toggleNow() {
        try {
            if (mp.isPlaying()) {
                mp.pause();
                isPlaying = false;
                Island.music(title, false, target);
                refreshNotif(false);
            } else {
                mp.start();
                isPlaying = true;
                Island.music(title, true, target);
                refreshNotif(true);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    private void refreshNotif(boolean playing) {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
            .notify(NID, notif(playing));
    }

    private Notification notif(boolean playing) {
        String ch = "gass_media";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                ch, "Playback", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(c);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        PendingIntent open = PendingIntent.getActivity(this, 0,
            new Intent(this, HomeActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent tgl = PendingIntent.getService(this, 1,
            new Intent(this, MediaService.class).setAction(A_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stp = PendingIntent.getService(this, 2,
            new Intent(this, MediaService.class).setAction(A_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT);
        b.setSmallIcon(android.R.drawable.ic_media_play)
         .setContentTitle(title)
         .setContentText(playing ? "Playing" : "Paused")
         .setContentIntent(open)
         .setOngoing(true)
         .addAction(new Notification.Action.Builder(
             Icon.createWithResource(this, playing
                 ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play),
             playing ? "Pause" : "Play", tgl).build())
         .addAction(new Notification.Action.Builder(
             Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
             "Stop", stp).build());
        return b.build();
    }

    private void release() {
        handler.removeCallbacks(ticker);
        if (mp != null) {
            try { mp.setOnCompletionListener(null); mp.reset(); } catch (Exception ignored) {}
            try { mp.release(); } catch (Exception ignored) {}
            mp = null;
        }
    }

    private void shutdown() {
        release();
        currentTitle = null;
        isPlaying = false;
        positionMs = 0;
        durationMs = 0;
        Island.musicOff();
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        release();
        currentTitle = null;
        isPlaying = false;
        positionMs = 0;
        durationMs = 0;
        Island.musicOff();
        super.onDestroy();
    }
}
