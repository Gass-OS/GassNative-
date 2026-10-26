package com.gass.gassnative;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;
import java.io.*;

public class MusicActivity extends Activity {

    private final int BG = Color.rgb(9,13,21);
    private final int PANEL = Color.rgb(20,29,43);
    private final int ORANGE = Color.rgb(255,145,75);
    private final int BLUE = Color.rgb(106,174,255);
    private final int WHITE = Color.rgb(239,243,250);
    private final int MUTED = Color.rgb(166,183,205);

    private final String[] titles = {
        "Mataafi",
        "Midnight",
        "Ready",
        "Block",
        "Monday"
    };

    private final String[] tracks = {
        "audio/music/mataafi.mp3",
        "audio/music/Midnight.mp3",
        "audio/music/Ready.mp3",
        "audio/music/Block.mp3",
        "audio/music/Monday.mp3"
    };

    private MediaPlayer player;
    private int index = 0;
    private TextView title, status, elapsed, total;
    private Button play;
    private SeekBar seek;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView label(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private String clock(int ms) {
        int seconds = Math.max(0, ms / 1000);
        return String.format(java.util.Locale.US,
            "%d:%02d", seconds / 60, seconds % 60);
    }

    private File cachedTrack(int n) throws IOException {
        File directory = new File(getCacheDir(), "music");
        if (!directory.exists() && !directory.mkdirs())
            throw new IOException("Cannot create music cache");

        File destination = new File(directory, "cached_" + new File(tracks[n]).getName());

        if (destination.isFile() && destination.length() > 0)
            return destination;

        try (InputStream in = getAssets().open(tracks[n]);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = in.read(buffer)) != -1)
                out.write(buffer, 0, count);
        }

        return destination;
    }

    private void releasePlayer() {
        // MediaService owns playback. Closing this Activity must not stop music.
        player = null;
    }

    private void load(int n, boolean autoplay) {
        index = (n + tracks.length) % tracks.length;

        if (autoplay) {
            MediaService.playPlaylist(this, tracks, titles, index);
        } else if (MediaService.currentTitle != null) {
            for (int i = 0; i < titles.length; i++) {
                if (titles[i].equals(MediaService.currentTitle)) {
                    index = i;
                    break;
                }
            }
        }

        title.setText(MediaService.currentTitle != null
            ? MediaService.currentTitle : titles[index]);
        status.setText(MediaService.isPlaying ? "Now playing"
            : (MediaService.currentTitle != null ? "Paused"
            : "AYMEN PRODUCTION  •  OFFLINE MUSIC"));
        play.setText(MediaService.isPlaying ? "Ⅱ" : "▶");
        seek.setMax(Math.max(1, MediaService.durationMs));
        seek.setProgress(Math.max(0, MediaService.positionMs));
        elapsed.setText(clock(Math.max(0, MediaService.positionMs)));
        total.setText(clock(Math.max(0, MediaService.durationMs)));
    }

    private void toggle() {
        if (MediaService.currentTitle == null) {
            MediaService.playPlaylist(this, tracks, titles, index);
        } else {
            MediaService.toggle(this);
        }
        status.setText(MediaService.isPlaying ? "Now playing" : "Paused");
        play.setText(MediaService.isPlaying ? "Ⅱ" : "▶");
    }

    private Button control(String symbol, int color) {
        Button b = new Button(this);
        b.setText(symbol);
        b.setTextSize(25);
        b.setTextColor(WHITE);
        b.setAllCaps(false);
        b.setBackground(bg(color, 18));
        return b;
    }

    private final Runnable progress = new Runnable() {
        @Override public void run() {
            if (!isFinishing()) {
                String current = MediaService.currentTitle;
                if (current != null) {
                    title.setText(current);
                    status.setText(MediaService.isPlaying
                        ? "Now playing" : "Paused");
                    play.setText(MediaService.isPlaying ? "Ⅱ" : "▶");
                    seek.setMax(Math.max(1, MediaService.durationMs));
                    seek.setProgress(Math.max(0, MediaService.positionMs));
                    elapsed.setText(clock(Math.max(0, MediaService.positionMs)));
                    total.setText(clock(Math.max(0, MediaService.durationMs)));
                }
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(25), dp(22), dp(28));
        scroll.addView(page);

        TextView heading = label("GASSNATIVE MUSIC", 15, BLUE);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setLetterSpacing(.12f);
        page.addView(heading);

        LinearLayout window = new LinearLayout(this);
        window.setOrientation(LinearLayout.VERTICAL);
        window.setGravity(Gravity.CENTER);
        window.setPadding(dp(18), dp(26), dp(18), dp(25));
        window.setBackground(bg(PANEL, 24));

        LayoutParams windowLp = new LayoutParams(-1, -2);
        windowLp.topMargin = dp(22);
        page.addView(window, windowLp);

        TextView artwork = label("♫", 95, ORANGE);
        artwork.setTypeface(null, Typeface.BOLD);
        artwork.setBackground(bg(Color.rgb(37,48,65), 25));

        LayoutParams artworkLp =
            new LayoutParams(dp(180), dp(180));
        artworkLp.bottomMargin = dp(25);
        window.addView(artwork, artworkLp);

        title = label("", 24, WHITE);
        title.setTypeface(null, Typeface.BOLD);
        window.addView(title);

        status = label("", 12, MUTED);
        status.setPadding(0, dp(10), 0, dp(22));
        window.addView(status);

        seek = new SeekBar(this);
        seek.setMax(1);
        seek.setProgressTintList(
            android.content.res.ColorStateList.valueOf(ORANGE));
        seek.setThumbTintList(
            android.content.res.ColorStateList.valueOf(ORANGE));
        window.addView(seek, new LayoutParams(-1, dp(45)));

        LinearLayout times = new LinearLayout(this);
        times.setOrientation(LinearLayout.HORIZONTAL);
        window.addView(times, new LayoutParams(-1, -2));

        elapsed = label("0:00", 12, MUTED);
        total = label("0:00", 12, MUTED);

        times.addView(elapsed, new LayoutParams(0, -2, 1));
        times.addView(total, new LayoutParams(0, -2, 1));

        elapsed.setGravity(Gravity.LEFT);
        total.setGravity(Gravity.RIGHT);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setPadding(0, dp(24), 0, dp(10));
        window.addView(controls);

        Button previous = control("⏮", Color.rgb(37,48,65));
        play = control("▶", ORANGE);
        Button next = control("⏭", Color.rgb(37,48,65));

        LayoutParams small = new LayoutParams(0, dp(65), 1);
        small.rightMargin = dp(9);
        controls.addView(previous, small);

        LayoutParams center = new LayoutParams(0, dp(72), 1.25f);
        center.rightMargin = dp(9);
        controls.addView(play, center);

        controls.addView(next, new LayoutParams(0, dp(65), 1));

        previous.setOnClickListener(v -> load(index - 1, true));
        play.setOnClickListener(v -> toggle());
        next.setOnClickListener(v -> load(index + 1, true));

        seek.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                public void onStartTrackingTouch(SeekBar bar) {}
                public void onProgressChanged(
                    SeekBar bar, int value, boolean user) {}
                public void onStopTrackingTouch(SeekBar bar) {
                    MediaService.seek(MusicActivity.this, bar.getProgress());
                }
            });

        TextView playlist = label("YOUR MUSIC COLLECTION", 13, BLUE);
        playlist.setPadding(0, dp(25), 0, dp(13));
        playlist.setTypeface(null, Typeface.BOLD);
        page.addView(playlist);

        for (int i = 0; i < titles.length; i++) {
            final int track = i;
            Button song = new Button(this);
            song.setAllCaps(false);
            song.setText("♫    " + titles[i] + "     ›");
            song.setTextSize(15);
            song.setTextColor(WHITE);
            song.setBackground(bg(PANEL, 15));
            song.setOnClickListener(v -> load(track, true));

            LayoutParams lp = new LayoutParams(-1, dp(65));
            lp.bottomMargin = dp(10);
            page.addView(song, lp);
        }

        TextView footer = label(
            tracks.length + " TRACKS  •  OFFLINE  •  AYMEN PRODUCTION",
            11, MUTED);
        footer.setPadding(0, dp(20), 0, 0);
        page.addView(footer);

        setContentView(scroll);
        load(0, false);
        handler.post(progress);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(progress);
        super.onDestroy();
    }
}
