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
    public static final int BLUE = 0xFF6EC1FF;
    public static final int TEXT = 0xFFF4F7FF;
    public static final int MUTED = 0xFF9FB0D6;
    public static final int TV = 0, RADIO = 1, GAME = 2, PLAY = 3, STOP = 4, BACK = 5, SEARCH = 6, DESKTOP = 7, MUSIC = 8, TERMINAL = 9, SETTINGS = 10, INFO = 11, PROFILE = 12, POWER = 13, BULB = 14, WINDOWS = 15, PLANET = 16, SHIELD = 17;

    private GassUi() {}

    public static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void setup(Activity a) {
        a.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF05070E));
        a.getWindow().setStatusBarColor(Color.TRANSPARENT);
        a.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        a.getWindow().getDecorView().setSystemUiVisibility(0);
    }

    public static GradientDrawable glass(Context c, float radiusDp, int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), 0x33FFFFFF);
        return g;
    }

    public static GradientDrawable glass(Context c, float radiusDp) {
        return glass(c, radiusDp, 0x1AFFFFFF);
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
        t.setBackground(glass(t.getContext(), 18, on ? 0x556EC1FF : 0x1AFFFFFF));
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
        f.addView(i, new FrameLayout.LayoutParams(dp(c, sizeDp * 0.72f), dp(c, sizeDp * 0.72f), Gravity.CENTER));
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
        h.addView(new Icon(a, iconType, BLUE), new LinearLayout.LayoutParams(dp(a, 36), dp(a, 36)));
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

    public static void setContent(Activity a, View content) {
        FrameLayout f = new FrameLayout(a);
        f.addView(new SpaceView(a), new FrameLayout.LayoutParams(-1, -1));
        f.addView(content, new FrameLayout.LayoutParams(-1, -1));
        a.setContentView(f);
    }

    public static class SpaceView extends View {
        private static final int[] BC = {0x664176FF, 0x553066CC, 0x665C1735, 0x66451B40, 0x554A1630};
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long t0 = System.nanoTime();
        private final java.util.Random rnd = new java.util.Random(11);
        private final float dens;
        private float[] sx, sy, sr, sph, sv;
        private int[] sc;
        private android.graphics.Shader bg;
        private final android.graphics.RadialGradient[] glow = new android.graphics.RadialGradient[5];
        private final float[][] bl = new float[5][];
        private float shootT = -1, shootX, shootY, nextShoot = 5f;

        public SpaceView(Context c) {
            super(c);
            dens = c.getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            bg = new android.graphics.LinearGradient(0, 0, 0, h,
                new int[]{0xFF05070E, 0xFF0B1934, 0xFF321020}, new float[]{0f, 0.55f, 1f},
                android.graphics.Shader.TileMode.CLAMP);
            int n = 48;
            sx = new float[n]; sy = new float[n]; sr = new float[n]; sph = new float[n]; sv = new float[n]; sc = new int[n];
            int[] tint = {0xFFFFFFFF, 0xFFBFD7FF, 0xFFFFE3C2, 0xFFE0C8FF};
            for (int i = 0; i < n; i++) {
                sx[i] = rnd.nextFloat() * w;
                sy[i] = rnd.nextFloat() * h;
                sr[i] = (0.35f + rnd.nextFloat() * 0.85f) * dens;
                sph[i] = rnd.nextFloat() * 6.28f;
                sv[i] = 0.4f + rnd.nextFloat() * 2.2f;
                sc[i] = tint[rnd.nextInt(tint.length)];
            }
            float m = Math.max(w, h);
            float[][] cfg = {
                {0.15f, 0.20f, 0.10f, 0.06f, 0.021f, 0f, 0.55f},
                {0.85f, 0.35f, 0.12f, 0.08f, 0.017f, 1.7f, 0.50f},
                {0.30f, 0.75f, 0.10f, 0.07f, 0.025f, 3.1f, 0.55f},
                {0.80f, 0.85f, 0.08f, 0.06f, 0.019f, 4.4f, 0.48f},
                {0.55f, 0.50f, 0.07f, 0.05f, 0.015f, 5.5f, 0.35f} };
            for (int i = 0; i < 5; i++) {
                float[] k = cfg[i];
                bl[i] = new float[]{k[0] * w, k[1] * h, k[2] * w, k[3] * h, k[4], k[5], k[6] * m};
                glow[i] = new android.graphics.RadialGradient(0, 0, bl[i][6], BC[i], BC[i] & 0x00FFFFFF,
                    android.graphics.Shader.TileMode.CLAMP);
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            if (bg == null) return;
            int w = getWidth(), h = getHeight();
            float t = (System.nanoTime() - t0) / 1e9f;
            p.setStyle(Paint.Style.FILL);
            p.setShader(bg);
            c.drawRect(0, 0, w, h, p);
            for (int i = 0; i < 5; i++) {
                float[] b = bl[i];
                float x = b[0] + (float) Math.sin(t * b[4] * 6.28f + b[5]) * b[2];
                float y = b[1] + (float) Math.cos(t * b[4] * 5.1f + b[5]) * b[3];
                c.save();
                c.translate(x, y);
                p.setShader(glow[i]);
                c.drawCircle(0, 0, b[6], p);
                c.restore();
            }
            p.setShader(null);
            for (int i = 0; i < sx.length; i++) {
                float y = (sy[i] - t * sv[i] * 3f * dens) % h;
                if (y < 0) y += h;
                float a = 0.35f + 0.65f * (float) Math.abs(Math.sin(t * sv[i] * 0.9f + sph[i]));
                p.setColor(sc[i]);
                p.setAlpha((int) (a * 255));
                c.drawCircle(sx[i], y, sr[i], p);
            }
            if (shootT < 0 && t > nextShoot) {
                shootT = t;
                shootX = (0.3f + rnd.nextFloat() * 0.7f) * w;
                shootY = rnd.nextFloat() * h * 0.4f;
            }
            if (shootT >= 0) {
                float k = (t - shootT) / 0.9f;
                if (k > 1f) {
                    shootT = -1;
                    nextShoot = t + 6f + rnd.nextFloat() * 8f;
                } else {
                    float hx = shootX - w * 0.35f * k, hy = shootY + w * 0.2f * k;
                    p.setStrokeCap(Paint.Cap.ROUND);
                    p.setStrokeWidth(1.6f * dens);
                    for (int s = 0; s < 8; s++) {
                        float f0 = s / 8f, f1 = (s + 1) / 8f;
                        p.setColor(0xFFFFFFFF);
                        p.setAlpha((int) ((1f - f0) * (1f - k) * 230));
                        c.drawLine(hx + w * 0.035f * f0, hy - w * 0.02f * f0, hx + w * 0.035f * f1, hy - w * 0.02f * f1, p);
                    }
                }
            }
            postInvalidateDelayed(33);
        }
    }

    public static class GlowTile extends View {
        private final int c1, c2;
        private final boolean tile;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public GlowTile(Context c, int c1, int c2, boolean tile) {
            super(c);
            this.c1 = c1;
            this.c2 = c2;
            this.tile = tile;
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h), cx = w / 2f, cy = h / 2f;
            p.setStyle(Paint.Style.FILL);
            p.setShader(new android.graphics.RadialGradient(cx, cy, s * 0.5f,
                (c2 & 0x00FFFFFF) | 0x77000000, c2 & 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, s * 0.5f, p);
            if (!tile) { p.setShader(null); return; }
            float t = s * 0.86f, l = cx - t / 2f, tp = cy - t / 2f;
            RectF r = new RectF(l, tp, l + t, tp + t);
            p.setShader(new android.graphics.LinearGradient(l, tp, l + t, tp + t, c1, c2, android.graphics.Shader.TileMode.CLAMP));
            cv.drawRoundRect(r, t * 0.32f, t * 0.32f, p);
            p.setShader(new android.graphics.LinearGradient(0, tp, 0, tp + t * 0.55f, 0x55FFFFFF, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
            cv.drawRoundRect(r, t * 0.32f, t * 0.32f, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, s * 0.012f));
            p.setColor(0x44FFFFFF);
            cv.drawRoundRect(r, t * 0.32f, t * 0.32f, p);
        }
    }

    public static class GradText extends TextView {
        private final int[] cols;

        public GradText(Context c, int[] cols) {
            super(c);
            this.cols = cols;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            if (w > 0) getPaint().setShader(new android.graphics.LinearGradient(0, 0, w, 0, cols, null, android.graphics.Shader.TileMode.CLAMP));
        }
    }

    public static class SecBar extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float frac;

        public SecBar(Context c) { super(c); }

        public void setFrac(float f) { frac = f; invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setStyle(Paint.Style.FILL);
            p.setShader(null);
            p.setColor(0x22FFFFFF);
            c.drawRoundRect(new RectF(0, 0, w, h), h / 2, h / 2, p);
            p.setShader(new android.graphics.LinearGradient(0, 0, w, 0, new int[]{0xFF58A6FF, 0xFF8769E8, 0xFFE05270}, null, android.graphics.Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(0, 0, Math.max(h, w * frac), h), h / 2, h / 2, p);
            p.setShader(null);
        }
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
            float w = getWidth(), h = getHeight();
            float s = Math.min(w, h);
            float u = s / 24f;

            c.save();

            // Modern oversized glyphs
            float scale = 1.22f;
            c.translate(w / 2f, h / 2f);
            c.scale(scale, scale);
            c.translate(-s / 2f, -s / 2f);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.25f * u);
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
                case POWER:
                    c.drawArc(new RectF(5 * u, 5 * u, 19 * u, 19 * u), -60, 300, false, p);
                    c.drawLine(12 * u, 3 * u, 12 * u, 11 * u, p);
                    break;
                case BULB:
                    c.drawCircle(12 * u, 10 * u, 6 * u, p);
                    c.drawLine(9.5f * u, 18.5f * u, 14.5f * u, 18.5f * u, p);
                    c.drawLine(10.5f * u, 21.5f * u, 13.5f * u, 21.5f * u, p);
                    break;
                case WINDOWS:
                    p.setStyle(Paint.Style.FILL);
                    c.drawRoundRect(new RectF(4 * u, 4 * u, 11 * u, 11 * u), u, u, p);
                    c.drawRoundRect(new RectF(13 * u, 4 * u, 20 * u, 11 * u), u, u, p);
                    c.drawRoundRect(new RectF(4 * u, 13 * u, 11 * u, 20 * u), u, u, p);
                    c.drawRoundRect(new RectF(13 * u, 13 * u, 20 * u, 20 * u), u, u, p);
                    break;
                case PLANET:
                    c.drawCircle(12 * u, 12 * u, 8 * u, p);
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(12 * u, 4 * u, 2 * u, p);
                    c.drawCircle(18.9f * u, 16 * u, 2 * u, p);
                    c.drawCircle(5.1f * u, 16 * u, 2 * u, p);
                    break;
                case SHIELD:
                    path.reset();
                    path.moveTo(12 * u, 3 * u);
                    path.lineTo(20 * u, 6 * u);
                    path.lineTo(20 * u, 12 * u);
                    path.cubicTo(20 * u, 17 * u, 16 * u, 20 * u, 12 * u, 21.5f * u);
                    path.cubicTo(8 * u, 20 * u, 4 * u, 17 * u, 4 * u, 12 * u);
                    path.lineTo(4 * u, 6 * u);
                    path.close();
                    c.drawPath(path, p);
                    c.drawLine(9.5f * u, 9 * u, 9.5f * u, 15 * u, p);
                    c.drawLine(14.5f * u, 8.5f * u, 9.5f * u, 12 * u, p);
                    c.drawLine(10.5f * u, 12 * u, 14.5f * u, 15.5f * u, p);
                    break;
                case SEARCH:
                    c.drawCircle(10.5f * u, 10.5f * u, 6 * u, p);
                    c.drawLine(15 * u, 15 * u, 20 * u, 20 * u, p);
                    break;
            }
            c.restore();
        }
    }

    /**
     * Colorful, transparent, Papirus-inspired home tile icons.
     * Drawn in Java: no PNG files or external libraries required.
     */
    public static class PapirusIcon extends View {
        private final int type;
        private final Paint q = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        private static final int BLUE = 0xFF4C8DFF;
        private static final int CYAN = 0xFF65E0F5;
        private static final int PURPLE = 0xFFB49AFF;
        private static final int PINK = 0xFFFF83B7;
        private static final int YELLOW = 0xFFFFD166;
        private static final int GREEN = 0xFF72E0AE;
        private static final int INK = 0xFF202943;
        private static final int WHITE = 0xFFF5F7FF;
        private static final int ORANGE = 0xFFFFA45B;

        public PapirusIcon(Context c, int type) {
            super(c);
            this.type = type;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        private void fill(Canvas c, int color) {
            q.setShader(null);
            q.setStyle(Paint.Style.FILL);
            q.setColor(color);
        }

        private void line(Canvas c, int color, float width) {
            q.setShader(null);
            q.setStyle(Paint.Style.STROKE);
            q.setStrokeWidth(width);
            q.setStrokeCap(Paint.Cap.ROUND);
            q.setStrokeJoin(Paint.Join.ROUND);
            q.setColor(color);
        }

        private void round(Canvas c, float l, float t, float r, float b,
                           float radius, int color) {
            fill(c, color);
            c.drawRoundRect(l, t, r, b, radius, radius, q);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float side = Math.min(getWidth(), getHeight());
            if (side <= 0) return;

            c.save();
            c.translate((getWidth() - side) / 2f, (getHeight() - side) / 2f);
            c.scale(side / 24f, side / 24f);

            switch (type) {
                case TERMINAL:
                    round(c, 2, 3, 22, 21, 4, INK);
                    round(c, 2, 3, 22, 8, 3, BLUE);
                    line(c, CYAN, 1.8f);
                    path.reset();
                    path.moveTo(6, 11);
                    path.lineTo(9, 13.5f);
                    path.lineTo(6, 16);
                    c.drawPath(path, q);
                    line(c, WHITE, 1.6f);
                    c.drawLine(11, 16, 16, 16, q);
                    break;

                case DESKTOP:
                    round(c, 2, 3, 22, 17, 2.5f, PURPLE);
                    round(c, 3.5f, 4.5f, 20.5f, 15, 1.5f, BLUE);
                    round(c, 5, 6, 11, 12, 1, CYAN);
                    round(c, 12, 6, 19, 8, 0.7f, PINK);
                    round(c, 12, 9.5f, 17.5f, 11, 0.7f, GREEN);
                    fill(c, WHITE);
                    c.drawRect(10.5f, 17, 13.5f, 20, q);
                    round(c, 7, 19.5f, 17, 21, 0.7f, WHITE);
                    break;

                case SETTINGS:
                    line(c, BLUE, 2);
                    c.drawLine(3, 6, 21, 6, q);
                    c.drawLine(3, 12, 21, 12, q);
                    c.drawLine(3, 18, 21, 18, q);
                    fill(c, YELLOW); c.drawCircle(8, 6, 3, q);
                    fill(c, PINK); c.drawCircle(16, 12, 3, q);
                    fill(c, CYAN); c.drawCircle(10, 18, 3, q);
                    fill(c, INK); c.drawCircle(8, 6, 1.1f, q);
                    c.drawCircle(16, 12, 1.1f, q);
                    c.drawCircle(10, 18, 1.1f, q);
                    break;

                case TV:
                    round(c, 2, 6, 22, 20, 3, PURPLE);
                    round(c, 4, 8, 17, 17, 1.5f, CYAN);
                    round(c, 5, 9, 10, 15, 1, BLUE);
                    round(c, 11, 9, 15, 11, 0.6f, PINK);
                    fill(c, WHITE); c.drawCircle(19, 10, 0.8f, q);
                    c.drawCircle(19, 13, 0.8f, q);
                    line(c, PURPLE, 1.5f);
                    c.drawLine(8, 4, 12, 7, q);
                    c.drawLine(16, 4, 12, 7, q);
                    break;

                case RADIO:
                    line(c, ORANGE, 1.7f);
                    c.drawLine(6, 8, 17, 3, q);
                    round(c, 2, 8, 22, 21, 3, ORANGE);
                    round(c, 4, 10, 20, 19, 2, 0xFFFFC17C);
                    fill(c, INK); c.drawCircle(9, 14.5f, 3.1f, q);
                    fill(c, BLUE); c.drawCircle(9, 14.5f, 1.8f, q);
                    fill(c, WHITE); c.drawCircle(17, 12, 1, q);
                    c.drawCircle(17, 16, 1, q);
                    break;

                case GAME:
                    round(c, 2, 7, 22, 19, 6, BLUE);
                    round(c, 4, 8.5f, 20, 17.5f, 4, PURPLE);
                    fill(c, GREEN);
                    c.drawRoundRect(5, 11.5f, 11, 13.5f, 0.7f, 0.7f, q);
                    c.drawRoundRect(7, 9.5f, 9, 15.5f, 0.7f, 0.7f, q);
                    fill(c, YELLOW); c.drawCircle(16, 11, 1.2f, q);
                    fill(c, PINK); c.drawCircle(18.5f, 14, 1.2f, q);
                    break;

                case MUSIC:
                    line(c, PURPLE, 2.5f);
                    c.drawLine(14, 5, 14, 17, q);
                    c.drawLine(14, 5, 20, 7, q);
                    c.drawLine(20, 7, 20, 15, q);
                    fill(c, PINK); c.drawOval(8, 15, 14.5f, 20, q);
                    c.drawOval(14.5f, 13, 20.5f, 18, q);
                    break;

                case INFO:
                case PROFILE:
                    fill(c, CYAN); c.drawCircle(12, 8, 4, q);
                    fill(c, BLUE);
                    c.drawRoundRect(3, 13, 21, 22, 7, 7, q);
                    fill(c, PURPLE);
                    c.drawArc(4, 13, 20, 28, 180, 180, true, q);
                    fill(c, WHITE); c.drawCircle(12, 8, 1.4f, q);
                    break;

                case BULB:
                    fill(c, YELLOW); c.drawCircle(12, 10, 7, q);
                    fill(c, ORANGE); c.drawRect(9, 14, 15, 17, q);
                    round(c, 9, 16, 15, 19, 1, CYAN);
                    round(c, 10, 19, 14, 21, 0.7f, BLUE);
                    fill(c, WHITE); c.drawCircle(9, 7, 1.2f, q);
                    break;

                case WINDOWS:
                    round(c, 3, 3, 11, 11, 1.2f, CYAN);
                    round(c, 13, 3, 21, 11, 1.2f, GREEN);
                    round(c, 3, 13, 11, 21, 1.2f, PINK);
                    round(c, 13, 13, 21, 21, 1.2f, YELLOW);
                    break;

                case POWER:
                    line(c, PINK, 2.8f);
                    c.drawArc(4.5f, 4.5f, 19.5f, 19.5f, -48, 276, false, q);
                    line(c, GREEN, 3);
                    c.drawLine(12, 2.5f, 12, 11, q);
                    break;

                case PLANET:
                    fill(c, BLUE); c.drawCircle(12, 12, 7, q);
                    fill(c, CYAN); c.drawCircle(9, 9, 2.2f, q);
                    fill(c, PURPLE); c.drawCircle(15, 14, 2.6f, q);
                    line(c, PINK, 1.6f);
                    c.drawOval(1.5f, 8, 22.5f, 16, q);
                    break;

                case SHIELD:
                    path.reset();
                    path.moveTo(12, 2);
                    path.lineTo(21, 5.5f);
                    path.lineTo(20, 14);
                    path.cubicTo(19, 18, 15, 21, 12, 22);
                    path.cubicTo(8, 20, 4, 17, 4, 13);
                    path.lineTo(3, 5.5f);
                    path.close();
                    fill(c, BLUE); c.drawPath(path, q);
                    path.reset();
                    path.moveTo(12, 5);
                    path.lineTo(17, 7);
                    path.lineTo(16.5f, 13);
                    path.lineTo(12, 18);
                    path.lineTo(7.5f, 13);
                    path.lineTo(7, 7);
                    path.close();
                    fill(c, CYAN); c.drawPath(path, q);
                    fill(c, PURPLE); c.drawCircle(12, 11, 2, q);
                    break;

                default:
                    fill(c, CYAN); c.drawCircle(12, 12, 9, q);
                    fill(c, BLUE); c.drawCircle(12, 12, 5, q);
                    break;
            }
            c.restore();
        }
    }

}
