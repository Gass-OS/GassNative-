package com.gass.gassnative;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IslandView extends LinearLayout {
    private final Context ctx;
    private LinearLayout musicRow, taskRow;
    private TextView mText, tText, tPct;
    private GassUi.Icon mBtn;
    private Eq eq;
    private Bar bar;
    private final Runnable upd = new Runnable() {
        @Override public void run() { refresh(); }
    };

    public IslandView(Context c) {
        super(c);
        ctx = c;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setPadding(0, dp(6), 0, 0);
        setVisibility(GONE);

        musicRow = pill();
        eq = new Eq(c);
        musicRow.addView(eq, new LayoutParams(dp(18), dp(18)));
        mText = txt(13, true);
        LayoutParams mlp = new LayoutParams(-2, -2);
        mlp.leftMargin = dp(10);
        musicRow.addView(mText, mlp);
        mText.setOnClickListener(v -> open(Island.musicTarget));
        FrameLayout tgl = iconBtn(GassUi.PAUSE, v -> MediaService.toggle(ctx));
        mBtn = (GassUi.Icon) tgl.getTag();
        LayoutParams blp = new LayoutParams(dp(32), dp(32));
        blp.leftMargin = dp(8);
        musicRow.addView(tgl, blp);
        musicRow.addView(iconBtn(GassUi.CLOSE, v -> MediaService.stop(ctx)), new LayoutParams(dp(32), dp(32)));
        addView(musicRow);

        taskRow = pill();
        tText = txt(12.5f, false);
        taskRow.addView(tText, new LayoutParams(-2, -2));
        bar = new Bar(c);
        LayoutParams barlp = new LayoutParams(dp(70), dp(5));
        barlp.leftMargin = dp(10);
        taskRow.addView(bar, barlp);
        tPct = txt(12, true);
        LayoutParams plp = new LayoutParams(-2, -2);
        plp.leftMargin = dp(8);
        plp.rightMargin = dp(8);
        taskRow.addView(tPct, plp);
        taskRow.setOnClickListener(v -> open(Island.taskTarget));
        LayoutParams tlp = new LayoutParams(-2, -2);
        tlp.topMargin = dp(6);
        addView(taskRow, tlp);
    }

    private int dp(float v) { return GassUi.dp(ctx, v); }

    private LinearLayout pill() {
        LinearLayout r = new LinearLayout(ctx);
        r.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xF2050810);
        g.setCornerRadius(dp(26));
        g.setStroke(dp(1), 0x44FFFFFF);
        r.setBackground(g);
        r.setPadding(dp(14), dp(6), dp(6), dp(6));
        r.setVisibility(GONE);
        return r;
    }

    private TextView txt(float sp, boolean bold) {
        TextView t = new TextView(ctx);
        t.setTextSize(sp);
        t.setTextColor(0xFFF4F7FF);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setMaxWidth(dp(190));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private FrameLayout iconBtn(int type, View.OnClickListener l) {
        FrameLayout f = new FrameLayout(ctx);
        GassUi.Icon i = new GassUi.Icon(ctx, type, 0xFFF4F7FF);
        f.addView(i, new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER));
        f.setOnClickListener(l);
        f.setTag(i);
        return f;
    }

    private void open(String target) {
        if (target == null) return;
        try {
            Class<?> k = Class.forName("com.gass.gassnative." + target);
            if (ctx.getClass() == k) return;
            ctx.startActivity(new Intent(ctx, k));
        } catch (Throwable ignored) {}
    }

    private void refresh() {
        boolean m = Island.musicTitle != null;
        boolean t = Island.taskLabel != null;
        boolean show = m || t;
        boolean was = getVisibility() == VISIBLE;
        setVisibility(show ? VISIBLE : GONE);
        musicRow.setVisibility(m ? VISIBLE : GONE);
        taskRow.setVisibility(t ? VISIBLE : GONE);
        if (m) {
            mText.setText(Island.musicTitle + (Island.musicPlaying ? "" : "  \u00B7  paused"));
            mBtn.setType(Island.musicPlaying ? GassUi.PAUSE : GassUi.PLAY);
            eq.setActive(Island.musicPlaying);
        }
        if (t) {
            tText.setText(Island.taskLabel);
            bar.set(Island.taskPct);
            tPct.setText(Island.taskPct >= 0 ? Island.taskPct + "%" : "");
        }
        if (show && !was) {
            setAlpha(0f);
            setTranslationY(-dp(12));
            animate().alpha(1f).translationY(0f).setDuration(260).start();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Island.addListener(upd);
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        Island.removeListener(upd);
        super.onDetachedFromWindow();
    }

    private static class Eq extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean on;

        Eq(Context c) { super(c); }

        void setActive(boolean a) {
            if (on != a) { on = a; invalidate(); }
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), bw = w / 5f;
            float t = System.nanoTime() / 1e9f;
            int[] col = {0xFF6EC1FF, 0xFFA78BFA, 0xFFF472B6};
            for (int i = 0; i < 3; i++) {
                float f = on ? 0.3f + 0.7f * (float) Math.abs(Math.sin(t * (3f + i * 1.7f) + i)) : 0.25f;
                float bh = h * f;
                p.setColor(col[i]);
                c.drawRoundRect(new RectF(i * 2 * bw, h - bh, i * 2 * bw + bw, h), bw / 2, bw / 2, p);
            }
            if (on) postInvalidateDelayed(70);
        }
    }

    private static class Bar extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int pct = -1;

        Bar(Context c) { super(c); }

        void set(int v) { pct = v; invalidate(); }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setShader(null);
            p.setColor(0x33FFFFFF);
            c.drawRoundRect(new RectF(0, 0, w, h), h / 2, h / 2, p);
            p.setShader(new LinearGradient(0, 0, w, 0, new int[]{0xFF6EC1FF, 0xFFA78BFA, 0xFFF472B6}, null, Shader.TileMode.CLAMP));
            if (pct >= 0) {
                c.drawRoundRect(new RectF(0, 0, Math.max(h, w * pct / 100f), h), h / 2, h / 2, p);
            } else {
                float t = (System.nanoTime() / 1e9f % 1.4f) / 1.4f;
                float x = -w * 0.4f + t * w * 1.4f;
                c.save();
                c.clipRect(0, 0, w, h);
                c.drawRoundRect(new RectF(x, 0, x + w * 0.4f, h), h / 2, h / 2, p);
                c.restore();
                postInvalidateDelayed(40);
            }
            p.setShader(null);
        }
    }
}
