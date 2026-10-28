package com.gass.gassnative;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Xml;
import android.view.animation.LinearInterpolator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.*;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends Activity {
    private static final int WHITE = 0xFFF4F7FF, SOFT = 0xFF9FB0D6;
    private GassUi.GradText timeV;
    private TextView dayV, dateV;
    // GASS NEWS TICKER
    private FrameLayout tickerViewport;
    private TextView tickerText;
    private ValueAnimator tickerAnimator;
    private float tickerSegmentWidth = 0f;
    private String tickerSegmentText = "";
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private volatile boolean tickerActive = false;
    private volatile boolean tickerFetchRunning = false;
    private boolean tickerHasOnlineNews = false;

    private final Runnable tickerRefresh = new Runnable() {
        @Override public void run() {
            fetchTickerNews();
        }
    };
    private View setupCard;
    private TextView updDot, updTitle, updDetail;
    private LinearLayout updCard;
    private String updUrl;
    private LinearLayout newsCard;
    private TextView newsTitle, newsMsg, newsBtn;
    private String newsUrl, newsId;
    // GASS_SMART_BOX_V1
    private TextView newsModeBtn;
    private UpdateInfo.Result smartBoxResult;
    private int smartBoxMode = 0;
    private boolean smartBoxDismissed = false;
    private final Handler clock = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Locale loc = Locale.getDefault();
            Date d = new Date();
            timeV.setText(new SimpleDateFormat("HH:mm", loc).format(d));
            dayV.setText(new SimpleDateFormat("EEEE", loc).format(d).toUpperCase(loc));
            dateV.setText(new SimpleDateFormat("d MMMM yyyy", loc).format(d));
            Calendar cal = Calendar.getInstance();
            clock.postDelayed(this, 500);
        }
    };

    private int dp(float v) { return GassUi.dp(this, v); }

    private GradientDrawable box(int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private LinearLayout card(int fill, int stroke) {
        LinearLayout c = new LinearLayout(this);
        c.setBackground(box(fill, stroke, 24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private LinearLayout glass() {
        int panel = Appearance.panel(this);
        int stroke = Appearance.accent(this);
        return card(Color.argb(235, Color.red(panel),
            Color.green(panel), Color.blue(panel)),
            Color.argb(115, Color.red(stroke),
                Color.green(stroke), Color.blue(stroke)));
    }

    private TextView txt(String s, float sp, int color, String family, float spacing) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp * Appearance.scale(this));
        t.setTextColor(color);
        t.setTypeface(Appearance.typeface(this, false));
        if (spacing != 0) t.setLetterSpacing(spacing);
        return t;
    }


    private void addNewsTicker(LinearLayout root) {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(7), dp(6), dp(10), dp(6));
        strip.setBackground(box(0xFF111923, 0x665B718B, 13));
        strip.setClipChildren(true);
        strip.setClipToPadding(true);

        TextView badge = txt("GASS NEWS", 10, Color.WHITE,
            "sans-serif-medium", 0.04f);
        badge.setGravity(Gravity.CENTER);
        badge.setSingleLine(true);
        badge.setPadding(dp(10), dp(7), dp(10), dp(7));
        badge.setBackground(box(0xFFE44835, 0x00FFFFFF, 9));
        strip.addView(badge, new LinearLayout.LayoutParams(-2, -2));

        tickerViewport = new FrameLayout(this);
        tickerViewport.setClipChildren(true);
        tickerViewport.setClipToPadding(true);

        LinearLayout.LayoutParams vp =
            new LinearLayout.LayoutParams(0, -1, 1f);
        vp.leftMargin = dp(10);
        strip.addView(tickerViewport, vp);

        tickerText = txt(
            "GassNative • Debian ARM64 • Linux on Android • "
            + "Fresh technology headlines appear when connected",
            12.5f, 0xFFF4F7FF, "sans-serif-medium", 0
        );
        tickerText.setSingleLine(true);
        tickerText.setIncludeFontPadding(false);
        tickerText.setGravity(Gravity.CENTER_VERTICAL);

        tickerViewport.addView(tickerText,
            new FrameLayout.LayoutParams(-2, -1, Gravity.CENTER_VERTICAL));

        LinearLayout.LayoutParams slp =
            new LinearLayout.LayoutParams(-1, dp(44));
        slp.topMargin = dp(8);
        slp.bottomMargin = dp(12);
        root.addView(strip, slp);

        String cached = getSharedPreferences(
            "gass_news_ticker", MODE_PRIVATE
        ).getString("headlines", null);

        if (cached != null && !cached.trim().isEmpty()) {
            tickerHasOnlineNews = true;
            setTickerMessage(cached);
        }

        tickerViewport.addOnLayoutChangeListener(
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (right > left && (right - left) != (oldRight - oldLeft)
                        && tickerActive) {
                    restartTickerAnimation();
                }
            }
        );
    }

    // GASS_TICKER_LAYOUT_FIX_V2
    private void setTickerMessage(String message) {
        if (tickerText == null || message == null
                || message.trim().isEmpty()) return;

        String clean = message.trim().replaceAll("\\s+", " ");
        tickerSegmentText = "     \u2022     " + clean + "     \u2022     ";

        if (tickerAnimator != null) {
            tickerAnimator.cancel();
            tickerAnimator = null;
        }

        tickerText.setSingleLine(true);
        tickerText.setHorizontallyScrolling(true);
        tickerText.setEllipsize(null);
        tickerText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tickerText.setTranslationX(0f);
        tickerText.setText(tickerSegmentText + tickerSegmentText);
        tickerSegmentWidth = tickerText.getPaint().measureText(tickerSegmentText);

        if (tickerViewport != null) {
            tickerViewport.post(this::restartTickerAnimation);
        }
    }

    private void restartTickerAnimation() {
        if (!tickerActive || tickerText == null || tickerViewport == null) return;

        if (tickerAnimator != null) {
            tickerAnimator.cancel();
            tickerAnimator = null;
        }

        if (tickerViewport.getWidth() <= 0 || tickerSegmentText.isEmpty()) return;

        final String repeated = tickerSegmentText + tickerSegmentText;
        tickerText.setText(repeated);

        // Measure after TextView has its final text and paint configuration.
        tickerText.post(() -> {
            if (!tickerActive || tickerText == null || tickerViewport == null
                    || tickerViewport.getWidth() <= 0
                    || tickerSegmentText.isEmpty()) return;

            final float segmentWidth =
                tickerText.getPaint().measureText(tickerSegmentText);
            if (segmentWidth <= 1f) return;

            tickerSegmentWidth = segmentWidth;
            final int textWidth =
                (int) Math.ceil(tickerText.getPaint().measureText(repeated)) + dp(4);

            tickerText.setLayoutParams(new FrameLayout.LayoutParams(
                Math.max(1, textWidth), -1, Gravity.START | Gravity.CENTER_VERTICAL
            ));
            tickerText.setTranslationX(0f);
            tickerText.requestLayout();

            // Start only after the wide TextView has been laid out.
            tickerText.post(() -> {
                if (!tickerActive || tickerText == null || tickerViewport == null
                        || tickerViewport.getWidth() <= 0) return;

                if (tickerAnimator != null) {
                    tickerAnimator.cancel();
                    tickerAnimator = null;
                }

                ValueAnimator animator = ValueAnimator.ofFloat(0f, -segmentWidth);
                long duration = Math.max(
                    1000L,
                    (long) (segmentWidth * 1000f / Math.max(1, dp(36)))
                );
                animator.setDuration(duration);
                animator.setInterpolator(new LinearInterpolator());
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setRepeatMode(ValueAnimator.RESTART);
                animator.addUpdateListener(a -> {
                    if (tickerActive && tickerText != null) {
                        tickerText.setTranslationX((Float) a.getAnimatedValue());
                    }
                });

                tickerAnimator = animator;
                animator.start();
            });
        });
    }

    private void startNewsTicker() {
        tickerActive = true;
        if (tickerViewport != null) {
            tickerViewport.post(this::restartTickerAnimation);
        }
        tickerHandler.removeCallbacks(tickerRefresh);
        tickerHandler.post(tickerRefresh);
    }

    private void stopNewsTicker() {
        tickerActive = false;
        tickerHandler.removeCallbacks(tickerRefresh);
        if (tickerAnimator != null) {
            tickerAnimator.cancel();
            tickerAnimator = null;
        }
    }

    private void fetchTickerNews() {
        if (!tickerActive || tickerFetchRunning) return;
        tickerFetchRunning = true;

        new Thread(() -> {
            ArrayList<String> headlines = new ArrayList<>();

            String[] feeds = {
                "https://www.phoronix.com/rss.php",
                "https://9to5google.com/feed/",
                "https://www.omgubuntu.co.uk/feed/"
            };

            try {
                for (String feed : feeds) {
                    if (headlines.size() >= 10) break;
                    try {
                        headlines.addAll(readTickerFeed(feed, 10 - headlines.size()));
                    } catch (Exception ignored) {
                        // A feed can fail without preventing other feeds from loading.
                    }
                }
            } finally {
                tickerFetchRunning = false;

                if (tickerActive) {
                    if (!headlines.isEmpty()) {
                        ArrayList<String> unique = new ArrayList<>();
                        for (String h : headlines) {
                            if (!unique.contains(h)) unique.add(h);
                        }

                        String joined = android.text.TextUtils.join("     •     ", unique);
                        getSharedPreferences("gass_news_ticker", MODE_PRIVATE)
                            .edit().putString("headlines", joined).apply();

                        tickerHandler.post(() -> {
                            if (!tickerActive) return;
                            tickerHasOnlineNews = true;
                            setTickerMessage(joined);
                        });
                    } else if (!tickerHasOnlineNews) {
                        tickerHandler.post(() -> setTickerMessage(
                            "Offline mode • Debian ARM64 • Linux on Android "
                            + "• Open-source technology • GassNative"
                        ));
                    }

                    tickerHandler.postDelayed(tickerRefresh, 30 * 60 * 1000L);
                }
            }
        }, "GassNews-RSS").start();
    }

    private ArrayList<String> readTickerFeed(String address, int limit)
            throws Exception {
        ArrayList<String> result = new ArrayList<>();
        HttpURLConnection connection =
            (HttpURLConnection) new URL(address).openConnection();

        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("User-Agent", "GassNative/0.6 RSS reader");
        connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
        connection.setInstanceFollowRedirects(true);

        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) return result;

            try (InputStream input = connection.getInputStream()) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(input, null);

                boolean inItem = false;
                int event = parser.getEventType();

                while (event != XmlPullParser.END_DOCUMENT
                        && result.size() < limit) {
                    if (event == XmlPullParser.START_TAG
                            && "item".equalsIgnoreCase(parser.getName())) {
                        inItem = true;
                    } else if (event == XmlPullParser.END_TAG
                            && "item".equalsIgnoreCase(parser.getName())) {
                        inItem = false;
                    } else if (inItem
                            && event == XmlPullParser.START_TAG
                            && "title".equalsIgnoreCase(parser.getName())) {
                        String title = parser.nextText().trim();
                        title = title.replaceAll("\\s+", " ");
                        if (title.length() > 8 && !result.contains(title)) {
                            result.add(title);
                        }
                    }
                    event = parser.next();
                }
            }
        } finally {
            connection.disconnect();
        }

        return result;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        sv.addView(root);

        LinearLayout head = glass();
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(12), dp(14), dp(16), dp(14));
        FrameLayout logo = new FrameLayout(this);
        ImageView ic = new ImageView(this);
        try (java.io.InputStream imageStream =
                getAssets().open("GassNative-PUMPKIN-R8.png")) {
            ic.setImageBitmap(
                android.graphics.BitmapFactory.decodeStream(imageStream));
        } catch (Exception e) {
            try {
                ic.setImageDrawable(
                    getPackageManager().getApplicationIcon(getApplicationInfo()));
            } catch (Throwable ignored) {}
            android.util.Log.e("GassNative", "Main logo image failed", e);
        }
        ic.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ic.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), v.getWidth() * 0.26f);
            }
        });
        ic.setClipToOutline(false);
        logo.addView(ic, new FrameLayout.LayoutParams(dp(104), dp(104), Gravity.CENTER));
        head.addView(logo, new LinearLayout.LayoutParams(dp(124), dp(124)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), 0, 0, 0);
        GassUi.GradText title = new GassUi.GradText(this, new int[]{0xFF6EC1FF, 0xFFA78BFA, 0xFFF472B6});
        title.setText("GassNative");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        col.addView(title, new LinearLayout.LayoutParams(-2, -2));
        TextView pill = txt("PUMPKIN R8 \u2122", 11.5f, WHITE, "sans-serif-medium", 0.1f);
        pill.setBackground(box(0x33A78BFA, 0x66A78BFA, 12));
        pill.setPadding(dp(10), dp(3), dp(10), dp(3));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-2, -2);
        plp.topMargin = dp(4);
        plp.bottomMargin = dp(6);
        col.addView(pill, plp);
        col.addView(txt("Explore. Build. Innovate. Be Free.", 13, WHITE, "sans-serif", 0));
        col.addView(txt("Linux on Android without root. No ads. No telemetry. Powered by Gass-OS\u2122", 11.5f, SOFT, "sans-serif-light", 0));
        head.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(head);

        // Dynamic Island: music playback and background tasks
        IslandView islandView = new IslandView(this);
        LinearLayout.LayoutParams islandLp =
            new LinearLayout.LayoutParams(-1, -2);
        islandLp.topMargin = dp(8);
        islandLp.bottomMargin = dp(8);
        root.addView(islandView, islandLp);

        LinearLayout clk = glass();
        clk.setOrientation(LinearLayout.VERTICAL);
        clk.setPadding(dp(18), dp(14), dp(18), dp(16));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        timeV = new GassUi.GradText(this, new int[]{0xFF58A6FF, 0xFF9878F5, 0xFFE05270});
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        timeV.setTextSize(screenWidthDp < 360 ? 52 :
            screenWidthDp < 400 ? 60 :
            screenWidthDp < 440 ? 66 : 70);
        timeV.setTextColor(Color.WHITE);
        timeV.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        timeV.setIncludeFontPadding(false);
        top.addView(timeV, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.END);
        side.setPadding(dp(8), 0, 0, 0);
        dayV = txt("", 12, Color.WHITE, "sans-serif-medium", 0.12f);
        GradientDrawable gp = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF2354A5, 0xFF84314F});
        gp.setCornerRadius(dp(14));
        dayV.setBackground(gp);
        dayV.setPadding(dp(14), dp(5), dp(14), dp(5));
        dayV.setSingleLine(true);
        side.addView(dayV, new LinearLayout.LayoutParams(-2, -2));
        dateV = txt("", 12.5f, WHITE, "sans-serif", 0.02f);
        dateV.setSingleLine(true);
        dateV.setPadding(0, dp(8), 0, 0);
        side.addView(dateV, new LinearLayout.LayoutParams(-2, -2));
        top.addView(side, new LinearLayout.LayoutParams(0, -2, 1));
        clk.addView(top);
        root.addView(clk);
        addNewsTicker(root);
        clk.setClickable(true);
        clk.setFocusable(true);
        clk.setOnClickListener(v -> showClockGlass());


        LinearLayout setup = card(0x26F97316, 0x66F97316);
        setup.setOrientation(LinearLayout.VERTICAL);
        setup.setPadding(dp(16), dp(12), dp(16), dp(12));
        setup.addView(txt("FIRST-TIME SETUP", 11, 0xFFFDBA74, "sans-serif-medium", 0.12f));
        TextView sb = txt("Tap here to choose the Linux system to install. Debian is available now, more are coming soon.", 12.5f, WHITE, "sans-serif-light", 0);
        sb.setPadding(0, dp(4), 0, 0);
        setup.addView(sb);
        setup.setOnClickListener(v -> startActivity(new Intent(this, DistroActivity.class)));
        setupCard = setup;
        root.addView(setup);

        LinearLayout upd = card(0x1A94A3B8, 0x4494A3B8);
        upd.setGravity(Gravity.CENTER_VERTICAL);
        upd.setPadding(dp(16), dp(12), dp(16), dp(12));
        updDot = txt("\u25CF", 14, 0xFF94A3B8, "sans-serif", 0);
        updDot.setPadding(0, 0, dp(10), 0);
        upd.addView(updDot);
        LinearLayout ucol = new LinearLayout(this);
        ucol.setOrientation(LinearLayout.VERTICAL);
        updTitle = txt("Checking for updates...", 13, WHITE, "sans-serif-medium", 0);
        updDetail = txt("", 11.5f, SOFT, "sans-serif-light", 0);
        updDetail.setVisibility(View.GONE);
        ucol.addView(updTitle);
        ucol.addView(updDetail);
        upd.addView(ucol, new LinearLayout.LayoutParams(0, -2, 1));
        updCard = upd;
        upd.setOnClickListener(v -> {
            if (updUrl != null) openUrl(updUrl);
            else UpdateInfo.check(this, true, this::renderUpdate);
        });
        root.addView(upd);

        LinearLayout news = card(0x1A8B5CF6, 0x558B5CF6);
        news.setOrientation(LinearLayout.VERTICAL);
        news.setPadding(dp(16), dp(12), dp(16), dp(14));
        LinearLayout nh = new LinearLayout(this);
        nh.setGravity(Gravity.CENTER_VERTICAL);
        newsTitle = txt("", 14, WHITE, "sans-serif-medium", 0);
        nh.addView(newsTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView nx = txt("\u2715", 15, SOFT, "sans-serif", 0);
        nx.setPadding(dp(14), dp(2), dp(2), dp(2));
        nx.setOnClickListener(v -> {
            smartBoxDismissed = true;
            if (newsId != null) {
                getSharedPreferences("gass_news", MODE_PRIVATE)
                    .edit().putString("seen", newsId).apply();
            }
            newsCard.setVisibility(View.GONE);
        });
        newsModeBtn = txt("↻  NEWS", 11, WHITE, "sans-serif-medium", 0);
        newsModeBtn.setPadding(dp(10), dp(5), dp(10), dp(5));
        newsModeBtn.setOnClickListener(v -> {
            smartBoxMode = (smartBoxMode + 1) % 3;
            renderSmartBox();
        });
        nh.addView(newsModeBtn);
        nx.setContentDescription("Dismiss Smart Box");
        nh.addView(nx);
        news.addView(nh);
        newsMsg = txt("", 12, SOFT, "sans-serif-light", 0);
        newsMsg.setPadding(0, dp(4), 0, dp(10));
        news.addView(newsMsg);
        newsBtn = txt("", 12, WHITE, "sans-serif-medium", 0.04f);
        GradientDrawable nb = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xFF3B82F6, 0xFF8B5CF6});
        nb.setCornerRadius(dp(16));
        newsBtn.setBackground(nb);
        newsBtn.setPadding(dp(16), dp(7), dp(16), dp(7));
        newsBtn.setOnClickListener(v -> { if (newsUrl != null && newsUrl.length() > 0) openUrl(newsUrl); });
        news.addView(newsBtn, new LinearLayout.LayoutParams(-2, -2));
        news.setVisibility(View.GONE);
        newsCard = news;
        root.addView(news);

        LinearLayout grid = glass();
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(6), dp(10), dp(6), dp(6));
        Object[][] T = {
            {"TERMINAL", 0xFF174C48, 0xFF102C40, GassUi.TERMINAL, "term"},
            {"DESKTOP", 0xFF263D83, 0xFF49316B, GassUi.DESKTOP, "desktop"},
            {"SETTING", 0xFF39475E, 0xFF202B40, GassUi.SETTINGS, "settings"},
            {"WINE DESKTOP", 0xFF713F65, 0xFF3B2455, GassUi.WINDOWS, "games"},
            {"Windows VM", 0xFF185A8E, 0xFF263D83, GassUi.WINDOWS, "win7"},
            {"TV & STREAMING", 0xFF79294C, 0xFF48245D, GassUi.TV, "tv"},
            {"RADIO", 0xFF784331, 0xFF59283A, GassUi.RADIO, "radio"},
            {"MUSIC PLAYER", 0xFF58347E, 0xFF702D58, GassUi.MUSIC, "music"},
            {"ABOUT ME", 0xFF174E70, 0xFF293B78, GassUi.INFO, "about"},
            {"TALKIE WALKIE", 0xFF175A66, 0xFF233D70, GassUi.MIC, "talkie"},
            {"GPS & MAPS", 0xFF167C73, 0xFF244A83, GassUi.INFO, "gps"},
            {"COMING SOON", 0xFF244E8B, 0xFF49316F, GassUi.SHIELD, "kali"},
            {"POWER OFF", 0xFF792B46, 0xFF481D45, GassUi.POWER, "power"},
            {"GASS AI", 0xFF194C65, 0xFF493080, GassUi.BULB, "ai"},
            // GASS_LANGUAGE_TILE_V06
            {"LANGUAGE", 0xFF176B78, 0xFF343A80, GassUi.INFO, "language"},
    {"SUPPORT", 0xFF75522B, 0xFF49316F, GassUi.INFO, "support"} };
        for (int r = 0; r < (T.length + 3) / 4; r++) {
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < 4; c++) {
                if (r * 4 + c >= T.length) {
                    row.addView(new View(this),
                        new LinearLayout.LayoutParams(0, 1, 1));
                    continue;
                }
                Object[] it = T[r * 4 + c];
                row.addView(tile(localizedTileLabel((String) it[0]), (Integer) it[1], (Integer) it[2], (Integer) it[3], (String) it[4]),
                    new LinearLayout.LayoutParams(0, -2, 1));
            }
            grid.addView(row);
        }
        root.addView(grid);

        // GassNative-CrashReport
        LinearLayout report = card(0x2629B6F6, 0x6648A9FF);
        report.setOrientation(LinearLayout.VERTICAL);
        report.setPadding(dp(16), dp(12), dp(16), dp(12));
        report.addView(txt("BUG REPORT  •  TELEGRAM", 11,
            0xFF8CCBFF, "sans-serif-medium", 0.08f));
        TextView reportText = txt(
            "App crashed or something is not working? Contact Gass-OS support on Telegram.",
            12.5f, WHITE, "sans-serif", 0);
        reportText.setPadding(0, dp(5), 0, 0);
        report.addView(reportText);
        report.setOnClickListener(v -> {
            Intent telegram = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://t.me/Gassos94"));
            try {
                startActivity(telegram);
            } catch (Exception ignored) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://telegram.me/Gassos94")));
                } catch (Exception ignoredAgain) {}
            }
        });
        root.addView(report);


        LinearLayout foot = glass();
        foot.setOrientation(LinearLayout.VERTICAL);
        foot.setPadding(dp(16), dp(12), dp(16), dp(12));
        foot.addView(txt("GASSNATIVE \u2022 PUMPKIN R8\u2122 \u2022 VERSION " + UpdateInfo.installed(this), 11, WHITE, "sans-serif-medium", 0.04f));
        TextView fb = txt("Explore. Build. Innovate. Be Free.\n\nGassNative brings Debian GNU/Linux and an integrated XFCE4 desktop to Android without requiring root access or a separate display app. Manage Linux profiles, use terminal tools, explore desktop applications and customize your environment.\n\nNo ads. No tracking. No telemetry. Your device, your tools, your freedom. Built with passion by Aymen Gharsalli and powered by Gass-OS\u2122.", 11.5f, SOFT, "sans-serif-light", 0);
        fb.setPadding(0, dp(6), 0, 0);
        foot.addView(fb);
        root.addView(foot);

        GassUi.setContent(this, sv);
        root.post(() -> showFirstLaunchSetup());
        UpdateChecker.enforce(this);
    }



    // GASS_DARK_GLASS_CLOCK
    private void showClockGlass() {
        final int panel = 0xFF0B1426;
        final int border = 0xFF31517B;
        final int blue = 0xFF67B7FF;
        final int muted = 0xFF9FB0D6;
        final Locale loc = Locale.getDefault();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(18));
        content.setBackground(box(panel, border, 26));

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout headingText = new LinearLayout(this);
        headingText.setOrientation(LinearLayout.VERTICAL);
        TextView eyebrow = txt("GASSNATIVE  /  TIME", 10,
            blue, "sans-serif-medium", 0.14f);
        TextView headingTitle = txt("Dark Glass Clock", 20,
            WHITE, "sans-serif-medium", 0);
        headingText.addView(eyebrow);
        headingText.addView(headingTitle);
        heading.addView(headingText,
            new LinearLayout.LayoutParams(0, -2, 1));

        TextView close = txt("×", 28, muted, "sans-serif-light", 0);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Close clock");
        heading.addView(close, new LinearLayout.LayoutParams(dp(36), dp(40)));
        content.addView(heading);

        TextView liveTime = txt("--:--", 54, Color.WHITE,
            "sans-serif-light", 0.01f);
        liveTime.setGravity(Gravity.CENTER);
        liveTime.setPadding(0, dp(14), 0, 0);
        content.addView(liveTime);

        TextView fullDate = txt("", 14, blue,
            "sans-serif-medium", 0.01f);
        fullDate.setGravity(Gravity.CENTER);
        fullDate.setPadding(0, 0, 0, dp(16));
        content.addView(fullDate);

        LinearLayout calendarCard = new LinearLayout(this);
        calendarCard.setOrientation(LinearLayout.VERTICAL);
        calendarCard.setPadding(dp(10), dp(10), dp(10), dp(10));
        calendarCard.setBackground(box(0xFF111F35, 0x443E73B8, 18));

        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView previous = txt("‹", 28, blue, "sans-serif-light", 0);
        previous.setGravity(Gravity.CENTER);
        monthRow.addView(previous,
            new LinearLayout.LayoutParams(dp(42), dp(40)));

        TextView monthTitle = txt("", 15, WHITE,
            "sans-serif-medium", 0);
        monthTitle.setGravity(Gravity.CENTER);
        monthRow.addView(monthTitle,
            new LinearLayout.LayoutParams(0, -2, 1));

        TextView next = txt("›", 28, blue, "sans-serif-light", 0);
        next.setGravity(Gravity.CENTER);
        monthRow.addView(next,
            new LinearLayout.LayoutParams(dp(42), dp(40)));
        calendarCard.addView(monthRow);

        GridLayout calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setUseDefaultMargins(false);
        LinearLayout.LayoutParams gridLp =
            new LinearLayout.LayoutParams(-1, -2);
        gridLp.topMargin = dp(5);
        calendarCard.addView(calendarGrid, gridLp);
        content.addView(calendarCard);

        TextView hint = txt("Tap the clock card anytime to open this view",
            10, muted, "sans-serif", 0);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(12), 0, 0);
        content.addView(hint);

        final Calendar shownMonth = Calendar.getInstance();
        shownMonth.set(Calendar.DAY_OF_MONTH, 1);
        renderClockCalendar(calendarGrid, monthTitle, shownMonth);

        previous.setOnClickListener(v -> {
            shownMonth.add(Calendar.MONTH, -1);
            renderClockCalendar(calendarGrid, monthTitle, shownMonth);
        });
        next.setOnClickListener(v -> {
            shownMonth.add(Calendar.MONTH, 1);
            renderClockCalendar(calendarGrid, monthTitle, shownMonth);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(content)
            .create();

        final Handler popupClock = new Handler(Looper.getMainLooper());
        final Runnable[] ticker = new Runnable[1];
        ticker[0] = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                liveTime.setText(
                    new SimpleDateFormat("HH:mm:ss", loc).format(now));
                fullDate.setText(
                    new SimpleDateFormat("EEEE, d MMMM yyyy", loc)
                        .format(now));
                popupClock.postDelayed(this, 500);
            }
        };

        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d ->
            popupClock.removeCallbacks(ticker[0]));
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent);
                dialog.getWindow().setDimAmount(0.65f);
                dialog.getWindow().setLayout(
                    getResources().getDisplayMetrics().widthPixels - dp(32),
                    -2);
            }
        });
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(
                android.R.color.transparent);
            dialog.getWindow().setDimAmount(0.65f);
            dialog.getWindow().setLayout(
                getResources().getDisplayMetrics().widthPixels - dp(32),
                -2);
        }
        popupClock.post(ticker[0]);
    }

    private void renderClockCalendar(
        GridLayout grid, TextView monthTitle, Calendar shownMonth) {

        Locale loc = Locale.getDefault();
        monthTitle.setText(
            new SimpleDateFormat("MMMM yyyy", loc)
                .format(shownMonth.getTime()));

        grid.removeAllViews();
        String[] weekdays = {"S", "M", "T", "W", "T", "F", "S"};

        for (int i = 0; i < 7; i++) {
            TextView label = txt(weekdays[i], 11, 0xFF7FBFFF,
                "sans-serif-medium", 0);
            label.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(i, 1f));
            lp.width = 0;
            lp.height = dp(30);
            grid.addView(label, lp);
        }

        Calendar first = (Calendar) shownMonth.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = first.get(Calendar.DAY_OF_WEEK)
            - Calendar.SUNDAY;
        int days = shownMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();

        for (int i = 0; i < offset + days; i++) {
            final int day = i - offset + 1;
            TextView cell;

            if (day < 1) {
                cell = txt("", 12, WHITE, "sans-serif", 0);
            } else {
                cell = txt(String.valueOf(day), 12, WHITE,
                    "sans-serif", 0);
                if (shownMonth.get(Calendar.YEAR) ==
                        today.get(Calendar.YEAR)
                    && shownMonth.get(Calendar.MONTH) ==
                        today.get(Calendar.MONTH)
                    && day == today.get(Calendar.DAY_OF_MONTH)) {
                    cell.setBackground(box(0xFF245CA8, 0xFF67B7FF, 12));
                    cell.setTextColor(Color.WHITE);
                    cell.setTypeface(
                        Typeface.create("sans-serif-medium", Typeface.BOLD));
                } else if (i % 7 == 0 || i % 7 == 6) {
                    cell.setTextColor(0xFF9BBBE7);
                }
            }

            cell.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(i % 7, 1f));
            lp.width = 0;
            lp.height = dp(34);
            grid.addView(cell, lp);
        }
    }

    private void showFirstLaunchSetup() {
        android.content.SharedPreferences prefs =
            getSharedPreferences("gassnative_first_launch", MODE_PRIVATE);
        if (prefs.getBoolean("accepted", false)) return;

        int panel = 0xFF101B30;
        int border = 0xFF344765;

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(16));
        content.setBackground(box(panel, border, 22));

        TextView title = txt("Welcome to GassNative 💙", 22,
            WHITE, "sans-serif-medium", 0);
        content.addView(title);

        TextView subtitle = txt("PRIVACY & USER SETUP", 11,
            0xFFA78BFA, "sans-serif-medium", 0.10f);
        LinearLayout.LayoutParams subLp =
            new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(8);
        content.addView(subtitle, subLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);

        TextView privacy = txt(
            "GassNative runs Linux environments on Android without root. " +
            "The app is designed without ads, tracking, or telemetry. " +
            "Linux distributions, repositories, and packages may have " +
            "their own licenses, privacy practices, and network activity. " +
            "Please review the terms of any software you install.",
            13, WHITE, "sans-serif", 0);
        privacy.setPadding(0, dp(12), 0, dp(12));
        details.addView(privacy);

        LinearLayout tips = card(0x2634D399, 0x6634D399);
        tips.setOrientation(LinearLayout.VERTICAL);
        tips.setPadding(dp(14), dp(12), dp(14), dp(12));

        tips.addView(txt("💡 FIRST-TIME INSTALLATION TIPS",
            12, 0xFF6EE7B7, "sans-serif-medium", 0));

        TextView tipText = txt(
            "1. Open Terminal Debian to install Debian, the desktop " +
            "environment, required components, and Wine.\\n\\n" +
            "2. Progress in the terminal may not display accurately. " +
            "Be patient and do not close GassNative while installation runs.\\n\\n" +
            "3. When setup finishes, check the Debian distribution name, " +
            "Wine installation, and terminal readiness.\\n\\n" +
            "4. Then open Debian Desktop 🖥️ from the home menu.",
            12.5f, WHITE, "sans-serif", 0);
        tipText.setPadding(0, dp(8), 0, 0);
        tips.addView(tipText);
        details.addView(tips);

        scroll.addView(details);
        LinearLayout.LayoutParams scrollLp =
            new LinearLayout.LayoutParams(-1, 0, 1);
        scrollLp.topMargin = dp(4);
        content.addView(scroll, scrollLp);

        CheckBox accept = new CheckBox(this);
        accept.setText(
            "I have read and accept the privacy notice and user agreement.");
        accept.setTextColor(WHITE);
        accept.setTextSize(12);
        accept.setButtonTintList(
            android.content.res.ColorStateList.valueOf(0xFFA78BFA));
        accept.setPadding(0, dp(8), 0, dp(8));
        content.addView(accept);

        Button next = new Button(this);
        next.setText("Accept & Next  →");
        next.setTextColor(Color.WHITE);
        next.setEnabled(false);
        next.setBackground(box(0xFF6546C8, 0xFF9878F5, 16));
        accept.setOnCheckedChangeListener((button, checked) ->
            next.setEnabled(checked));

        LinearLayout.LayoutParams nextLp =
            new LinearLayout.LayoutParams(-1, dp(50));
        nextLp.topMargin = dp(4);
        content.addView(next, nextLp);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(content)
            .create();

        next.setOnClickListener(v -> {
            if (!accept.isChecked()) return;
            prefs.edit().putBoolean("accepted", true).apply();
            dialog.dismiss();
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent);
            }
        });
        dialog.show();
    }


    private String localizedTileLabel(String label) {
        String lang = getSharedPreferences(
            "gassnative_language", MODE_PRIVATE).getString("language", "en");

        if ("fr".equals(lang)) {
            if (label.equals("DEBIAN TERMINAL")) return "TERMINAL DEBIAN";
            if (label.equals("DEBIAN DESKTOP")) return "BUREAU DEBIAN";
            if (label.equals("SETTINGS & AUDIT")) return "PARAMÈTRES & AUDIT";
            if (label.equals("WINE DESKTOP")) return "BUREAU WINE";
            if (label.equals("TV & STREAMING")) return "TV & STREAMING";
            if (label.equals("RADIO")) return "RADIO";
            if (label.equals("MUSIC PLAYER")) return "LECTEUR MUSICAL";
            if (label.equals("ABOUT ME")) return "À PROPOS";
            if (label.equals("TALKIE WALKIE")) return "TALKIE-WALKIE";
            if (label.equals("COMING SOON")) return "BIENTÔT";
            if (label.equals("POWER OFF")) return "ÉTEINDRE";
            if (label.equals("GASS AI")) return "IA GASS";
            if (label.equals("LANGUAGE")) return "LANGUE";
        } else if ("ar".equals(lang)) {
            if (label.equals("DEBIAN TERMINAL")) return "طرفية ديبيان";
            if (label.equals("DEBIAN DESKTOP")) return "سطح مكتب ديبيان";
            if (label.equals("SETTINGS & AUDIT")) return "الإعدادات والتدقيق";
            if (label.equals("WINE DESKTOP")) return "سطح مكتب واين";
            if (label.equals("TV & STREAMING")) return "التلفاز والبث";
            if (label.equals("RADIO")) return "الراديو";
            if (label.equals("MUSIC PLAYER")) return "مشغل الموسيقى";
            if (label.equals("ABOUT ME")) return "من أنا";
            if (label.equals("TALKIE WALKIE")) return "اتصال لاسلكي";
            if (label.equals("COMING SOON")) return "قريباً";
            if (label.equals("POWER OFF")) return "إيقاف التشغيل";
            if (label.equals("GASS AI")) return "مساعد GASS";
            if (label.equals("LANGUAGE")) return "اللغة";
        }
        return label;
    }

    private void showLanguageDialog() {
        final String[] codes = {"en", "fr", "ar"};
        String current = getSharedPreferences(
            "gassnative_language", MODE_PRIVATE).getString("language", "en");
        int selected = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) selected = i;
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("Language / Langue / اللغة")
            .setSingleChoiceItems(
                new String[] {"🇬🇧 English", "🇫🇷 Français", "🇸🇦 العربية"},
                selected,
                (dialog, which) -> {
                    getSharedPreferences(
                        "gassnative_language", MODE_PRIVATE)
                        .edit().putString("language", codes[which]).apply();
                    dialog.dismiss();
                    recreate();
                })
            .setNegativeButton("Cancel / Annuler / إلغاء", null)
            .show();
    }

    private static final String[][] PNG_MAP = {
        {"m_terminal", "terminal", "term"},
        {"m_desktop", "desktop", "debiandesktop"},
        {"m_settings", "setting", "settings", "settingaudit"},
        {"m_wine", "winedesktop", "wine"},
        {"m_windowsvm", "windowsvm", "windowvm", "winvm", "vm", "windows"},
        {"m_tv", "tvstreaming", "tv", "television", "streaming"},
        {"m_radio", "radio"},
        {"m_music", "musicplayer", "music"},
        {"m_about", "aboutme", "about"},
        {"m_talkie", "talkiewalkie", "talkie", "walkie"},
        {"m_gps", "gpsmaps", "gps", "maps"},
        {"m_soon", "comingsoon", "soon"},
        {"m_power", "poweroff", "power"},
        {"m_language", "language", "lang"},
        {"m_support", "support"},
        {"gass_ai", "ai", "gassai", "gass"} };

    private final StringBuilder tileDbg = new StringBuilder();

    private void tileLog(String line) {
        tileDbg.append(line).append('\n');
        try (java.io.FileWriter w = new java.io.FileWriter("/sdcard/Download/gass-tiles.log", false)) {
            w.write(tileDbg.toString());
        } catch (Throwable ignored) {}
    }

    private static String nrm(String s) {
        return s == null ? "" : s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String pngName(String code, String label) {
        String c = nrm(code), l = nrm(label);
        for (String[] e : PNG_MAP) {
            for (int i = 1; i < e.length; i++) {
                if (e[i].equals(c) || e[i].equals(l)) return e[0] + ".png";
            }
        }
        return null;
    }

    private View[] holderOf(android.view.ViewGroup g) {
        int n = g.getChildCount();
        for (int i = 0; i < n; i++) {
            if (g.getChildAt(i) instanceof android.widget.TextView && i > 0) return new View[]{g, g.getChildAt(i - 1)};
        }
        for (int i = 0; i < n; i++) {
            View c = g.getChildAt(i);
            if (c instanceof android.view.ViewGroup) {
                View[] r = holderOf((android.view.ViewGroup) c);
                if (r != null) return r;
            }
        }
        return null;
    }

    private View decorateTile(View t, String code, String label) {
        try {
            String f = pngName(code, label);
            if (f == null) { tileLog("NO MATCH   code=" + code + "  label=" + label); return t; }
            android.graphics.Bitmap bm = loadIcon(f);
            if (bm == null) { tileLog("NO FILE    " + f + "  (code=" + code + ")"); return t; }
            if (!(t instanceof android.view.ViewGroup)) { tileLog("NOT A GROUP code=" + code); return t; }
            View[] hit = holderOf((android.view.ViewGroup) t);
            if (hit == null) { tileLog("NO HOLDER  code=" + code + "  label=" + label); return t; }
            android.view.ViewGroup parent = (android.view.ViewGroup) hit[0];
            View holder = hit[1];
            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageBitmap(bm);
            iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            iv.setAdjustViewBounds(false);
            if (holder instanceof android.view.ViewGroup) {
                android.view.ViewGroup h = (android.view.ViewGroup) holder;
                h.removeAllViews();
                h.setBackground(null);
                if ("ai".equals(code)) {
                    h.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    h.setElevation(0);
                }
                h.setPadding(0, 0, 0, 0);
                android.widget.FrameLayout frame =
                    new android.widget.FrameLayout(this);
                if ("ai".equals(code)) {
                    frame.setBackgroundColor(
                        android.graphics.Color.TRANSPARENT);
                    frame.setElevation(0);
                }

                frame.addView(iv,
                    new android.widget.FrameLayout.LayoutParams(
                        dp("ai".equals(code) ? 64 : 48), dp("ai".equals(code) ? 64 : 48),
                        android.view.Gravity.CENTER));

                h.addView(frame,
                    new android.view.ViewGroup.LayoutParams(-1, -1));
            } else {
                android.widget.FrameLayout nf = new android.widget.FrameLayout(this);
                nf.addView(iv,
                    new android.widget.FrameLayout.LayoutParams(
                        dp("ai".equals(code) ? 64 : 48), dp("ai".equals(code) ? 64 : 48),
                        android.view.Gravity.CENTER));
                int idx = parent.indexOfChild(holder);
                android.view.ViewGroup.LayoutParams lp = holder.getLayoutParams();
                parent.removeViewAt(idx);
                parent.addView(nf, idx, lp);
            }
            tileLog("OK         code=" + code + "  label=" + label + "  ->  " + f);
        } catch (Throwable e) {
            tileLog("ERROR      code=" + code + "  " + e);
        }
        return t;
    }

    private View tile(String label, int c1, int c2, int glyph, final String code) {
        return decorateTile(tileRaw(label, c1, c2, glyph, code), code, label);
    }

    private View tileRaw(String label, int c1, int c2, int glyph, final String code) {
        if (!"gass_orange".equals(Appearance.theme(this))) {
            c1 = Appearance.accent(this);
            c2 = Appearance.panel(this);
        }
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(2), dp(4), dp(2), dp(10));
        FrameLayout f = new FrameLayout(this);
        f.addView(new GassUi.GlowTile(this, c1, c2, true), new FrameLayout.LayoutParams(-1, -1));
        if ("games".equals(code)) {
            TextView wine = txt("🍷", 31, WHITE, "sans-serif", 0);
            wine.setGravity(Gravity.CENTER);
            f.addView(wine, new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.CENTER));
        } else if ("ai".equals(code)) {
            TextView robot = txt("🤖", 31, WHITE, "sans-serif", 0);
            robot.setGravity(Gravity.CENTER);
            f.addView(robot, new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.CENTER));
        } else if ("gps".equals(code)) {
            TextView gps = txt("📍", 31, WHITE, "sans-serif", 0);
            gps.setGravity(Gravity.CENTER);
            f.addView(gps, new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.CENTER));
        } else if ("language".equals(code)) {
            TextView language = txt("🌐", 29, WHITE, "sans-serif", 0);
            language.setGravity(Gravity.CENTER);
            f.addView(language, new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.CENTER));
        } else if ("support".equals(code)) {
            TextView coffee = txt("☕", 31, WHITE, "sans-serif", 0);
            coffee.setGravity(Gravity.CENTER);
            f.addView(coffee, new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.CENTER));
        } else {
            f.addView(new GassUi.PapirusIcon(this, glyph),
                new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER));
        }
        t.addView(f, new LinearLayout.LayoutParams(dp(82), dp(82)));
        TextView l = txt(label, 10.5f, WHITE, "sans-serif-medium", 0.05f);
        l.setGravity(Gravity.CENTER);
        l.setMaxLines(2);
        t.addView(l, new LinearLayout.LayoutParams(-1, -2));
        t.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            return false;
        });
        t.setOnClickListener(v -> {
            if ("language".equals(code)) showLanguageDialog();
            else go(code);
        });
        t.setOnLongClickListener(v -> {
            showTileDetails(label, code);
            return true;
        });
        t.setLongClickable(true);
        return t;
    }

    private void showTileDetails(String label, String code) {
        String description;
        String tip;
        String action;

        switch (code) {
            case "term":
                description = "Your Linux installation and maintenance terminal.";
                tip = "FIRST INSTALLATION\\nInstall Debian, the desktop components and Wine when prompted. " +
                    "Installation can take several minutes. Keep GassNative open and wait for completion.";
                action = "Open Terminal Debian to continue setup.";
                break;
            case "desktop":
                description = "Launch your XFCE Linux desktop directly on Android.";
                tip = "Make sure Debian installation has finished first. " +
                    "If the desktop does not start, check the terminal output and desktop settings.";
                action = "Install Debian first if this is your first run.";
                break;
            case "settings":
                description = "Manage the configuration of your GassNative environment.";
                tip = "Review DNS, display resolution, bind mounts and environment variables. " +
                    "Change settings carefully because they can affect applications inside Linux.";
                action = "Open Settings to review available options.";
                break;
            case "games":
                description = "Explore the Windows application launcher and supported runtime.";
                tip = "Windows compatibility varies by application. Wine or Hangover support " +
                    "does not guarantee that every program or game will work or use GPU acceleration.";
                action = "Check the runtime setup before launching an application.";
                break;
            case "tv":
                description = "Open the television section.";
                tip = "Available streams depend on their source, availability and network connection.";
                action = "Tap to browse the TV section.";
                break;
            case "radio":
                description = "Browse available radio stations.";
                tip = "Online stations need an internet connection. Stream availability may change.";
                action = "Tap to open radio.";
                break;
            case "music":
                description = "Open the music player for available audio files.";
                tip = "Choose audio files you can access on your device.";
                action = "Tap to open offline music.";
                break;
            case "about":
                description = "Discover GassNative and its developer.";
                tip = "GassNative aims to bring Linux environments to Android without root access.";
                action = "Open About Me for project information.";
                break;
            case "tips":
                description = "Read useful guidance for using GassNative.";
                tip = "Complete the initial setup, wait for installation to finish, " +
                    "then launch Debian Desktop. Keep enough free storage for packages and updates.";
                action = "Tap to open the full Tips section.";
                break;
            case "ubuntu":
                description = "Ubuntu support is planned for a future release.";
                tip = "This shortcut is not active yet.";
                action = "Check future GassNative releases.";
                break;
            case "kali":
                description = "Kali Linux support is planned for a future release.";
                tip = "This shortcut is not active yet.";
                action = "Check future GassNative releases.";
                break;
            case "power":
                description = "Close GassNative and stop the current desktop session.";
                tip = "Save your work first. Closing the session may stop running Linux applications.";
                action = "Use the normal tap action only when you are ready to exit.";
                break;
            default:
                description = "GassNative home-screen feature.";
                tip = "Use the tile's normal tap action to open it.";
                action = "Tap to continue.";
                break;
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(16));
        content.setBackground(box(0xFF101B30, 0xFF344765, 22));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(content);

        TextView heading = txt("ⓘ  " + label, 20, WHITE,
            "sans-serif-medium", 0);
        content.addView(heading);

        TextView intro = txt(description, 13.5f, SOFT,
            "sans-serif", 0);
        intro.setPadding(0, dp(8), 0, dp(14));
        content.addView(intro);

        LinearLayout status = card(0x222F80ED, 0x554F8FEF);
        status.setOrientation(LinearLayout.VERTICAL);
        status.setPadding(dp(13), dp(12), dp(13), dp(12));
        status.addView(txt("⚡ SMART SYSTEM STATUS", 11,
            0xFF80BFFF, "sans-serif-medium", 0.05f));

        boolean ready = debianReady();
        TextView state = txt(
            (ready ? "● Debian root filesystem detected" :
                     "● Debian installation required") +
            "\\n" + (ready
                ? "Bash is present. This does not confirm that XFCE, Wine or GPU acceleration is ready."
                : "Open Terminal Debian to install the Linux environment."),
            12, WHITE, "sans-serif", 0);
        state.setPadding(0, dp(7), 0, 0);
        status.addView(state);
        content.addView(status);

        LinearLayout tipCard = card(0x2634D399, 0x6634D399);
        tipCard.setOrientation(LinearLayout.VERTICAL);
        tipCard.setPadding(dp(13), dp(12), dp(13), dp(12));

        tipCard.addView(txt("💡 DETAILS & TIPS", 11,
            0xFF6EE7B7, "sans-serif-medium", 0.06f));
        TextView tipBody = txt(tip, 12.5f, WHITE, "sans-serif", 0);
        tipBody.setPadding(0, dp(8), 0, dp(8));
        tipCard.addView(tipBody);
        tipCard.addView(txt("NEXT STEP", 10, 0xFF6EE7B7,
            "sans-serif-medium", 0.08f));
        TextView nextStep = txt(action, 12, SOFT, "sans-serif", 0);
        nextStep.setPadding(0, dp(5), 0, 0);
        tipCard.addView(nextStep);
        content.addView(tipCard);

        TextView linksTitle = txt("🌐 PROJECT & RESOURCES", 11,
            0xFFA78BFA, "sans-serif-medium", 0.06f);
        LinearLayout.LayoutParams titleLp =
            new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(15);
        titleLp.bottomMargin = dp(8);
        content.addView(linksTitle, titleLp);

        addDetailLink(content, "⌘", "Gass-OS on GitHub",
            "Explore the open-source project", "https://github.com/Gass-OS");
        addDetailLink(content, "🖥", "GNOME-Termux",
            "Explore the related desktop project",
            "https://github.com/Gass-OS/GNOME-Termux");

        LinearLayout credits = new LinearLayout(this);
        credits.setOrientation(LinearLayout.VERTICAL);
        credits.setGravity(Gravity.CENTER);
        credits.setPadding(dp(8), dp(16), dp(8), dp(4));
        TextView powered = txt("POWERED BY", 10, SOFT,
            "sans-serif-medium", 0.16f);
        powered.setGravity(Gravity.CENTER);
        credits.addView(powered);

        TextView developer = txt("Aymen Gharsalli", 17, WHITE,
            "sans-serif-medium", 0);
        developer.setGravity(Gravity.CENTER);
        credits.addView(developer);

        TextView project = txt("Gass-OS Project  •  Linux on Android",
            11, 0xFFA78BFA, "sans-serif", 0);
        project.setGravity(Gravity.CENTER);
        credits.addView(project);
        content.addView(credits);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton("Got it", null)
            .create();

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(
                    android.R.color.transparent);
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(0xFFA78BFA);
        });
        dialog.show();
    }

    private void addDetailLink(LinearLayout parent, String symbol,
            String title, String subtitle, String url) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(12), dp(10), dp(12), dp(10));
        item.setBackground(box(0x22243B5A, 0x445E789E, 16));
        item.setClickable(true);
        item.setFocusable(true);

        TextView icon = txt(symbol, 21, 0xFF80BFFF,
            "sans-serif-medium", 0);
        icon.setGravity(Gravity.CENTER);
        item.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(42)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(9), 0, dp(4), 0);
        labels.addView(txt(title, 13, WHITE, "sans-serif-medium", 0));
        TextView sub = txt(subtitle, 11, SOFT, "sans-serif", 0);
        LinearLayout.LayoutParams subLp =
            new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(3);
        labels.addView(sub, subLp);
        item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        item.addView(txt("↗", 18, 0xFFA78BFA, "sans-serif-medium", 0));

        LinearLayout.LayoutParams itemLp =
            new LinearLayout.LayoutParams(-1, -2);
        itemLp.bottomMargin = dp(7);
        parent.addView(item, itemLp);

        item.setOnTouchListener((v, event) -> {
            int a = event.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.98f).scaleY(0.98f)
                    .setDuration(70).start();
            } else if (a == MotionEvent.ACTION_UP ||
                       a == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f)
                    .setDuration(100).start();
            }
            return false;
        });

        item.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open link",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openUrl(String u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(u)));
        } catch (Exception e) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderUpdate(UpdateInfo.Result r) {
        int dot = 0xFF94A3B8, fill = 0x1A94A3B8, line = 0x4494A3B8;
        String title;
        String detail = null;
        updUrl = null;
        String me = "v" + r.installed + " (build " + r.installedCode + ")";
        if (r.state == 1) {
            dot = 0xFF34D399; fill = 0x1A34D399; line = 0x5534D399;
            title = "Up to date \u2014 " + me;
            detail = r.notes != null && r.notes.length() > 0 ? r.notes : null;
        } else if (r.state == 2) {
            dot = 0xFFFBBF24; fill = 0x26FBBF24; line = 0x66FBBF24;
            title = "Update available: " + r.latest;
            detail = (r.notes != null && r.notes.length() > 0 ? r.notes + "\n" : "") + "You have " + me + ". Tap to download.";
            updUrl = r.url;
        } else {
            title = "Couldn't check for updates \u2014 " + me;
            detail = "Tap to try again";
        }
        updDot.setTextColor(dot);
        updCard.setBackground(box(fill, line, 24));
        updTitle.setText(title);
        if (detail == null) {
            updDetail.setVisibility(View.GONE);
        } else {
            updDetail.setText(detail);
            updDetail.setVisibility(View.VISIBLE);
        }
        smartBoxResult = r;
        renderSmartBox();
    }

    // GASS_SMART_BOX_V1: offline content plus the existing online news feed.
    private void renderSmartBox() {
        if (newsCard == null || newsTitle == null) return;

        newsCard.setVisibility(smartBoxDismissed ? View.GONE : View.VISIBLE);
        if (smartBoxDismissed) return;

        newsUrl = null;
        String title;
        String message;
        String action = "";
        boolean hasOnlineNews = smartBoxResult != null
            && smartBoxResult.newsId != null
            && smartBoxResult.newsId.length() > 0
            && smartBoxResult.newsTitle != null
            && smartBoxResult.newsTitle.length() > 0
            && !smartBoxResult.newsId.equals(
                getSharedPreferences("gass_news", MODE_PRIVATE)
                    .getString("seen", ""));

        if (smartBoxMode == 0 && hasOnlineNews) {
            title = "📰  " + smartBoxResult.newsTitle;
            message = smartBoxResult.newsMsg == null
                ? "Latest news from GassNative."
                : smartBoxResult.newsMsg;
            newsUrl = smartBoxResult.newsUrl;
            action = smartBoxResult.newsBtn == null
                || smartBoxResult.newsBtn.length() == 0
                ? "Read more"
                : smartBoxResult.newsBtn;
            newsId = smartBoxResult.newsId;
        } else if (smartBoxMode == 0) {
            title = "✨  GASS AI · SMART BOX";
            message = "Online headlines appear here when the news feed provides them. Tap ↻ to explore jokes and helpful tips.";
        } else if (smartBoxMode == 1) {
            String[] jokes = {
                "Why do programmers prefer dark mode? Because light attracts bugs. 🐛",
                "A Linux user walks into a bar, orders sudo juice. 🍹",
                "There are 10 kinds of people: those who understand binary and those who don't. 😄",
                "My computer needed a break. Too many open windows! 🪟",
                "Keep calm and let the terminal handle it. 😎"
            };
            int index = (int) ((System.currentTimeMillis() / 60000L) % jokes.length);
            title = "😂  GASS AI · JOKE OF THE MOMENT";
            message = jokes[index];
        } else {
            String[] tips = {
                "🆘 EMERGENCY: If someone is in immediate danger, contact your local emergency services.",
                "🛠️ GASS TIP: Back up important Linux files before changing system configuration.",
                "🔐 SECURITY: Never share passwords, API secret keys, or private credentials.",
                "📦 INSTALL TIP: If Debian installation fails, check free storage and the error log before retrying.",
                "🌡️ DEVICE TIP: Close demanding apps and let your phone cool down if it overheats."
            };
            int index = (int) ((System.currentTimeMillis() / 60000L) % tips.length);
            title = "🆘  GASS AI · SOS & SMART TIPS";
            message = tips[index];
        }

        newsTitle.setText(title);
        newsMsg.setText(message);
        newsModeBtn.setText(smartBoxMode == 0 ? "↻  NEWS"
            : smartBoxMode == 1 ? "↻  JOKE" : "↻  SOS");

        newsBtn.setText(action.length() > 0 ? action : "Explore GASS AI");
        newsBtn.setVisibility(newsUrl != null && newsUrl.length() > 0
            ? View.VISIBLE : View.GONE);
    }

    private String[] uiFiles;

    private static String norm(String n) {
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0, dot);
        return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private android.graphics.Bitmap loadIcon(String file) {
        try {
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (java.io.InputStream in = getAssets().open("ui/" + file)) {
                android.graphics.BitmapFactory.decodeStream(in, null, o);
            }
            int ss = 1;
            while (o.outWidth / (ss * 2) >= 64 && o.outHeight / (ss * 2) >= 64) ss *= 2;
            o = new android.graphics.BitmapFactory.Options();
            o.inSampleSize = ss;
            try (java.io.InputStream in = getAssets().open("ui/" + file)) {
                return android.graphics.BitmapFactory.decodeStream(in, null, o);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private android.graphics.Bitmap iconFor(String code) {
        String[] al;
        switch (code) {
            case "term": al = new String[]{"terminal", "console"}; break;
            case "desktop": al = new String[]{"desktop"}; break;
            case "settings": al = new String[]{"setting", "audit"}; break;
            case "games": al = new String[]{"game", "windows", "wine"}; break;
            case "tv": al = new String[]{"television", "tv"}; break;
            case "radio": al = new String[]{"radio"}; break;
            case "music": al = new String[]{"music", "headphone"}; break;
            case "about": al = new String[]{"about"}; break;
            case "tips": al = new String[]{"tip", "useful"}; break;
            case "ubuntu": al = new String[]{"ubuntu"}; break;
            case "kali": al = new String[]{"kali"}; break;
            case "power": al = new String[]{"power"}; break;
            default: return null;
        }
        try {
            if (uiFiles == null) uiFiles = getAssets().list("ui");
        } catch (Exception e) {
            uiFiles = new String[0];
        }
        if (uiFiles == null) return null;
        String hit = null;
        for (String a : al) {
            for (String f : uiFiles) {
                if (norm(f).equals(a)) { hit = f; break; }
            }
            if (hit != null) break;
        }
        if (hit == null) {
            for (String a : al) {
                if (a.length() < 3) continue;
                for (String f : uiFiles) {
                    if (norm(f).contains(a)) { hit = f; break; }
                }
                if (hit != null) break;
            }
        }
        return hit == null ? null : loadIcon(hit);
    }

    private boolean debianReady() {
        return new File(Cfg.rootDir(this), "usr/bin/bash").isFile();
    }

    private void needDebian(Class<?> c) {
        if (!debianReady()) {
            Toast.makeText(this, "Install a Linux system first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, DistroActivity.class));
            return;
        }
        if (c == DesktopActivity.class) DesktopActivity.launch(this, "xfce", null, null);
        else startActivity(new Intent(this, c));
    }

    private void go(String c) {
        if ("gps".equals(c)) {
            openUrl("https://www.google.com/maps");
            return;
        }
        if ("support".equals(c)) {
            openUrl("https://www.paypal.com/ncp/payment/TERMUCF6CWCRA");
            return;
        }
        switch (c) {
            case "term": startActivity(new Intent(this, MainActivity.class)); break;
            case "desktop": needDebian(DesktopActivity.class); break;
            case "settings": startActivity(new Intent(this, SettingsActivity.class)); break;
            case "games": needDebian(GamesActivity.class); break;
            case "win7": startActivity(new Intent(this, WindowsVmActivity.class)); break;
            case "tv": startActivity(new Intent(this, TvActivity.class)); break;
            case "radio": startActivity(new Intent(this, RadioActivity.class)); break;
            case "music": startActivity(new Intent(this, MusicActivity.class)); break;
            case "ai": startActivity(new Intent(this, AiActivity.class)); break;
            case "about": startActivity(new Intent(this, AboutActivity.class)); break;
            case "talkie": startActivity(new Intent(this, TalkieWalkieActivity.class)); break;
            case "ubuntu": startActivity(new Intent(this, DistroActivity.class)); break;
            case "kali": startActivity(new Intent(this, DistroActivity.class)); break;
            case "power": powerOff(); break;
        }
    }

    private void launchOpenPoc() {
        final String packageName = "org.openpoc.client";
        try {
            Intent launch = getPackageManager()
                .getLaunchIntentForPackage(packageName);
            if (launch != null) {
                startActivity(launch);
                return;
            }
        } catch (Exception ignored) {}

        new AlertDialog.Builder(this)
            .setTitle("GassNative Talkie Walkie")
            .setMessage("OpenPOC is not installed yet. Open its official installation page?")
            .setPositiveButton("Open page", (d, w) -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://openpoc.org/install")));
                } catch (Exception e) {
                    new AlertDialog.Builder(this)
                        .setMessage("Could not open the installation page.")
                        .setPositiveButton("OK", null).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void powerOff() {
        new AlertDialog.Builder(this)
            .setTitle("Power off")
            .setMessage("Stop the Linux desktop and close GassNative?")
            .setPositiveButton("Power off", (d, w) -> {
                if (DesktopActivity.instance != null) DesktopActivity.instance.powerOff();
                finishAffinity();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startNewsTicker();
        setupCard.setVisibility(debianReady() ? View.GONE : View.VISIBLE);
        UpdateInfo.check(this, false, this::renderUpdate);
        clock.removeCallbacks(tick);
        clock.post(tick);
    }

    @Override
    protected void onPause() {
        stopNewsTicker();
        clock.removeCallbacks(tick);
        super.onPause();
    }
}
