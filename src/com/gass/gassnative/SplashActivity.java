package com.gass.gassnative;

import android.app.Activity;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.BitmapFactory;
import java.io.IOException;

public class SplashActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private boolean leaving = false;
    private ProgressBar progress;
    private TextView percent, status;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.create(
            "sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return v;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(7, 11, 23));
        getWindow().setNavigationBarColor(Color.rgb(7, 11, 23));
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        FrameLayout screen = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] {0xFF101B35, 0xFF080C18, 0xFF05070D});
        screen.setBackground(bg);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(28), dp(24), dp(28), dp(24));

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
            -1, -2, Gravity.CENTER);
        screen.addView(content, cp);

        ImageView logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            logo.setImageBitmap(BitmapFactory.decodeStream(
                getAssets().open("gassnative-brandmark.png"),
                null, options));
        } catch (Exception ignored) {}

        LinearLayout.LayoutParams logoLp =
            new LinearLayout.LayoutParams(dp(148), dp(148));
        logoLp.bottomMargin = dp(22);
        content.addView(logo, logoLp);

        logo.setAlpha(0f);
        logo.setScaleX(0.78f);
        logo.setScaleY(0.78f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(1100).setInterpolator(
                new DecelerateInterpolator()).start();

        TextView title = label("GassNative", 30, Color.WHITE, true);
        content.addView(title);

        TextView tagline = label(
            "LINUX, UNLEASHED.", 11, 0xFF8DBBFF, true);
        tagline.setLetterSpacing(0.22f);
        LinearLayout.LayoutParams tagLp =
            new LinearLayout.LayoutParams(-1, -2);
        tagLp.topMargin = dp(8);
        content.addView(tagline, tagLp);

        TextView powered = label(
            "Powered by Aymen Gharsalli", 13, 0xFF9AA9C4, false);
        LinearLayout.LayoutParams powerLp =
            new LinearLayout.LayoutParams(-1, -2);
        powerLp.topMargin = dp(10);
        powerLp.bottomMargin = dp(38);
        content.addView(powered, powerLp);

        progress = new ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setProgressTintList(
            android.content.res.ColorStateList.valueOf(0xFF65B5FF));
        progress.setProgressBackgroundTintList(
            android.content.res.ColorStateList.valueOf(0xFF26334B));

        LinearLayout.LayoutParams barLp =
            new LinearLayout.LayoutParams(-1, dp(5));
        content.addView(progress, barLp);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, 0);

        status = label("Starting GassNative…", 12, 0xFF9AA9C4, false);
        status.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        row.addView(status, new LinearLayout.LayoutParams(0, -2, 1));

        percent = label("0%", 12, 0xFF8DBBFF, true);
        row.addView(percent);
        content.addView(row);

        setContentView(screen);
        startIntro();
        animateProgress();

        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            leaving = true;
            stopIntro();
            startActivity(new android.content.Intent(
                SplashActivity.this, HomeActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in,
                                      android.R.anim.fade_out);
        }, 6000);
    }

    private void animateProgress() {
        ValueAnimator animation = ValueAnimator.ofInt(0, 100);
        animation.setDuration(5700);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.addUpdateListener(a -> {
            int value = (Integer) a.getAnimatedValue();
            progress.setProgress(value);
            percent.setText(value + "%");
            if (value < 30) status.setText("Preparing interface…");
            else if (value < 65) status.setText("Loading GassNative…");
            else if (value < 90) status.setText("Initializing services…");
            else status.setText("Ready to explore");
        });
        animation.start();
    }

    private void startIntro() {
        try {
            android.content.res.AssetFileDescriptor afd =
                getAssets().openFd("audio/gassnative_intro.mp3");
            player = new MediaPlayer();
            player.setVolume(0.55f, 0.55f);
            player.setDataSource(afd.getFileDescriptor(),
                afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setOnPreparedListener(mp -> {
                if (!isFinishing() && !isDestroyed() && !leaving) {
                    try { mp.start(); } catch (Exception ignored) {}
                }
            });
            player.prepareAsync();
        } catch (Exception ignored) {
            stopIntro();
        }
    }

    private void stopIntro() {
        MediaPlayer mp = player;
        player = null;
        if (mp != null) {
            try { if (mp.isPlaying()) mp.stop(); } catch (Exception ignored) {}
            try { mp.release(); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopIntro();
        super.onDestroy();
    }
}
