package com.gass.gassnative;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class GassUi {
    public static final int BLUE = 0xFF78AEED;
    public static final int TEXT = 0xFFF2F4F8;
    public static final int MUTED = 0xFF9AA4B5;
    public static final int TV = 0, RADIO = 1, GAME = 2, PLAY = 3, STOP = 4, BACK = 5, SEARCH = 6, DESKTOP = 7, MUSIC = 8, TERMINAL = 9, SETTINGS = 10, INFO = 11, PROFILE = 12;

    private GassUi() {}

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void setup(Activity a) {
        a.getWindow().setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF0E1320, 0xFF1A2342, 0xFF2B1D40}));
        a.getWindow().setStatusBarColor(Color.TRANSPARENT);
        a.getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    public static GradientDrawable glass(Context c, float radiusDp, int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), 0x33FFFFFF);
        return g;
    }

    public static GradientDrawable glass(Context c, float radiusDp) {
        return glass(c, radiusDp, 0x22FFFFFF);
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    public static TextView chip(Context c, String s) {
        TextView t = text(c, s, 13, TEXT, false);
        t.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        t.setBackground(glass(c, 18));
        return t;
    }

    public static void chipSelect(TextView t, boolean on) {
        t.setBackground(glass(t.getContext(), 18, on ? 0x6678AEED : 0x22FFFFFF));
    }

    public static LinearLayout row(Context c, String title, String sub) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setBackground(glass(c, 14));
        r.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        r.addView(text(c, title, 16, TEXT, true));
        if (sub != null && sub.length() > 0) r.addView(text(c, sub, 12, MUTED, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(c, 8);
        r.setLayoutParams(lp);
        return r;
    }

    public static FrameLayout iconButton(Context c, int type, int sizeDp, View.OnClickListener l) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(glass(c, sizeDp / 2f));
        Icon i = new Icon(c, type, TEXT);
        f.addView(i, new FrameLayout.LayoutParams(dp(c, sizeDp * 0.55f), dp(c, sizeDp * 0.55f), Gravity.CENTER));
        f.setOnClickListener(l);
        f.setTag(i);
        return f;
    }

    public static LinearLayout header(final Activity a, String title, int iconType) {
        LinearLayout h = new LinearLayout(a);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.addView(iconButton(a, BACK, 44, v -> a.finish()), new LinearLayout.LayoutParams(dp(a, 44), dp(a, 44)));
        TextView t = text(a, title, 22, TEXT, true);
        t.setPadding(dp(a, 14), 0, 0, 0);
        h.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        h.addView(new Icon(a, iconType, BLUE), new LinearLayout.LayoutParams(dp(a, 30), dp(a, 30)));
        h.setPadding(0, 0, 0, dp(a, 12));
        return h;
    }

    public static String httpGet(String u) throws Exception {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "GassNative/1.0");
        java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = r.readLine()) != null) sb.append(l).append('\n');
        r.close();
        return sb.toString();
    }

    public static class Icon extends View {
        private int type;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        public Icon(Context c, int type, int color) {
            super(c);
            this.type = type;
            p.setColor(color);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }

        public void setType(int t) { type = t; invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h), u = s / 24f;
            c.save();
            c.translate((w - s) / 2f, (h - s) / 2f);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f * u);
            switch (type) {
                case TV:
                    c.drawRoundRect(new RectF(2.5f * u, 7 * u, 21.5f * u, 20 * u), 3 * u, 3 * u, p);
                    c.drawLine(8 * u, 3 * u, 12 * u, 7 * u, p);
                    c.drawLine(16 * u, 3 * u, 12 * u, 7 * u, p);
                    break;
                case RADIO:
                    c.drawRoundRect(new RectF(3 * u, 9 * u, 21 * u, 21 * u), 3 * u, 3 * u, p);
                    c.drawLine(7 * u, 9 * u, 17 * u, 3 * u, p);
                    c.drawCircle(9 * u, 15 * u, 2.5f * u, p);
                    c.drawLine(14 * u, 13 * u, 18 * u, 13 * u, p);
                    c.drawLine(14 * u, 17 * u, 18 * u, 17 * u, p);
                    break;
                case GAME:
                    c.drawRoundRect(new RectF(2 * u, 7 * u, 22 * u, 18 * u), 5 * u, 5 * u, p);
                    c.drawLine(6 * u, 12.5f * u, 10 * u, 12.5f * u, p);
                    c.drawLine(8 * u, 10.5f * u, 8 * u, 14.5f * u, p);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(15.5f * u, 11.5f * u, 1.4f * u, p);
                    c.drawCircle(18 * u, 14 * u, 1.4f * u, p);
                    break;
                case PLAY:
                    p.setStyle(Paint.Style.FILL_AND_STROKE);
                    path.reset();
                    path.moveTo(8 * u, 5 * u);
                    path.lineTo(19 * u, 12 * u);
                    path.lineTo(8 * u, 19 * u);
                    path.close();
                    c.drawPath(path, p);
                    break;
                case STOP:
                    p.setStyle(Paint.Style.FILL_AND_STROKE);
                    c.drawRoundRect(new RectF(6 * u, 6 * u, 18 * u, 18 * u), 2 * u, 2 * u, p);
                    break;
                case BACK:
                    path.reset();
                    path.moveTo(15 * u, 5 * u);
                    path.lineTo(8 * u, 12 * u);
                    path.lineTo(15 * u, 19 * u);
                    c.drawPath(path, p);
                    break;
                case DESKTOP:
                    c.drawRoundRect(new RectF(2.5f * u, 4 * u, 21.5f * u, 16 * u), 2 * u, 2 * u, p);
                    c.drawLine(12 * u, 16 * u, 12 * u, 20 * u, p);
                    c.drawLine(7 * u, 20 * u, 17 * u, 20 * u, p);
                    break;
                case MUSIC:
                    c.drawCircle(8 * u, 17.5f * u, 3 * u, p);
                    c.drawLine(11 * u, 17.5f * u, 11 * u, 5 * u, p);
                    c.drawLine(11 * u, 5 * u, 19 * u, 8 * u, p);
                    break;
                case TERMINAL:
                    c.drawRoundRect(new RectF(2.5f * u, 4 * u, 21.5f * u, 20 * u), 3 * u, 3 * u, p);
                    path.reset();
                    path.moveTo(7 * u, 9 * u);
                    path.lineTo(11 * u, 12 * u);
                    path.lineTo(7 * u, 15 * u);
                    c.drawPath(path, p);
                    c.drawLine(13 * u, 16 * u, 17 * u, 16 * u, p);
                    break;
                case SETTINGS:
                    c.drawLine(3 * u, 6 * u, 21 * u, 6 * u, p);
                    c.drawLine(3 * u, 12 * u, 21 * u, 12 * u, p);
                    c.drawLine(3 * u, 18 * u, 21 * u, 18 * u, p);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(8 * u, 6 * u, 2.4f * u, p);
                    c.drawCircle(16 * u, 12 * u, 2.4f * u, p);
                    c.drawCircle(10 * u, 18 * u, 2.4f * u, p);
                    break;
                case INFO:
                    c.drawCircle(12 * u, 12 * u, 9 * u, p);
                    c.drawLine(12 * u, 11 * u, 12 * u, 17 * u, p);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(12 * u, 7.5f * u, 1.2f * u, p);
                    break;
                case PROFILE:
                    c.drawCircle(12 * u, 8 * u, 4 * u, p);
                    c.drawArc(new RectF(4 * u, 14 * u, 20 * u, 30 * u), 180, 180, false, p);
                    break;
                case SEARCH:
                    c.drawCircle(10.5f * u, 10.5f * u, 6 * u, p);
                    c.drawLine(15 * u, 15 * u, 20 * u, 20 * u, p);
                    break;
            }
            c.restore();
        }
    }
}
