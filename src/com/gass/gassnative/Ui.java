package com.gass.gassnative;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

public final class Ui {
  public static final int BG = Color.rgb(13, 17, 25);
  public static final int PANEL = Color.rgb(25, 33, 46);
  public static final int INPUT = Color.rgb(19, 25, 36);
  public static final int FG = Color.rgb(239, 243, 250);
  public static final int MUTED = Color.rgb(157, 171, 191);
  public static final int ACCENT = Color.rgb(80, 153, 255);

  private Ui() {}

  public static int dp(Activity a, int value) {
    return (int) (value *
        a.getResources().getDisplayMetrics().density + 0.5f);
  }

  private static float scale(Activity a) {
    float density = a.getResources().getDisplayMetrics().density;
    float widthDp =
        a.getResources().getDisplayMetrics().widthPixels / density;
    return Math.max(0.82f, Math.min(1.12f, widthDp / 400f));
  }

  private static int fit(Activity a, int value) {
    return Math.max(1, Math.round(value * scale(a)));
  }

  private static GradientDrawable shape(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(radius);
    return d;
  }

  private static TextView label(Activity a, String value, int size,
                                int color, boolean bold) {
    TextView t = new TextView(a);
    t.setText(value);
    t.setTextSize(fit(a, size));
    t.setTextColor(color);
    if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    return t;
  }

  public static void addTopBar(Activity a, LinearLayout parent) {
    a.getWindow().setStatusBarColor(BG);
    a.getWindow().setNavigationBarColor(BG);

    int pad = fit(a, 16);
    LinearLayout bar = new LinearLayout(a);
    bar.setOrientation(LinearLayout.HORIZONTAL);
    bar.setGravity(android.view.Gravity.CENTER_VERTICAL);
    bar.setPadding(dp(a, pad), dp(a, fit(a, 12)),
        dp(a, pad), dp(a, fit(a, 12)));
    bar.setBackgroundColor(Color.TRANSPARENT);

    LinearLayout brand = new LinearLayout(a);
    brand.setOrientation(LinearLayout.VERTICAL);
    brand.setGravity(android.view.Gravity.CENTER_VERTICAL);

    brand.addView(label(a, "▣  Debian ARM64", 16, FG, true));

    TextView subtitle =
        label(a, "GassNative Linux environment", 11, MUTED, false);
    LinearLayout.LayoutParams subLp =
        new LinearLayout.LayoutParams(-2, -2);
    subLp.topMargin = dp(a, 3);
    brand.addView(subtitle, subLp);

    LinearLayout clockBox = new LinearLayout(a);
    clockBox.setOrientation(LinearLayout.VERTICAL);
    clockBox.setGravity(
        android.view.Gravity.CENTER_VERTICAL
        | android.view.Gravity.RIGHT);

    TextClock time = new TextClock(a);
    time.setFormat12Hour("h:mm a");
    time.setFormat24Hour("HH:mm");
    time.setTextColor(FG);
    time.setTextSize(fit(a, 17));
    time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    time.setGravity(android.view.Gravity.RIGHT);
    clockBox.addView(time);

    TextClock date = new TextClock(a);
    date.setFormat12Hour("EEE, d MMM");
    date.setFormat24Hour("EEE, d MMM");
    date.setTextColor(MUTED);
    date.setTextSize(fit(a, 11));
    date.setGravity(android.view.Gravity.RIGHT);
    LinearLayout.LayoutParams dateLp =
        new LinearLayout.LayoutParams(-2, -2);
    dateLp.topMargin = dp(a, 2);
    clockBox.addView(date, dateLp);

    brand.setMinimumWidth(0);
    bar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));
    bar.addView(clockBox, new LinearLayout.LayoutParams(-2, -2));

    LinearLayout.LayoutParams barLp =
        new LinearLayout.LayoutParams(-1, -2);
    barLp.leftMargin = 0;
    barLp.rightMargin = 0;
    barLp.topMargin = dp(a, fit(a, 8));
    barLp.bottomMargin = dp(a, fit(a, 8));
    parent.addView(bar, 0, barLp);
  }

  private static void spaceAround(Activity a, View view,
                                  int top, int bottom) {
    ViewGroup.LayoutParams raw = view.getLayoutParams();
    if (!(raw instanceof ViewGroup.MarginLayoutParams)) return;

    ViewGroup.MarginLayoutParams lp =
        (ViewGroup.MarginLayoutParams) raw;
    lp.topMargin = Math.max(lp.topMargin, dp(a, fit(a, top)));
    lp.bottomMargin = Math.max(lp.bottomMargin, dp(a, fit(a, bottom)));
    view.setLayoutParams(lp);
  }

  public static void styleTree(Activity a, View view) {
    if (view instanceof Button) {
      Button b = (Button) view;
      b.setAllCaps(false);
      b.setTextColor(FG);
      b.setTextSize(fit(a, 14));
      b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
      b.setPadding(dp(a, fit(a, 14)), dp(a, fit(a, 9)),
          dp(a, fit(a, 14)), dp(a, fit(a, 9)));
      b.setBackground(shape(PANEL, dp(a, 13)));
      b.setMinHeight(dp(a, fit(a, 42)));
      spaceAround(a, b, 3, 5);
    } else if (view instanceof EditText) {
      EditText e = (EditText) view;
      e.setTextColor(FG);
      e.setHintTextColor(MUTED);
      e.setTextSize(fit(a, 15));
      e.setPadding(dp(a, fit(a, 12)), dp(a, fit(a, 10)),
          dp(a, fit(a, 12)), dp(a, fit(a, 10)));
      e.setBackground(shape(INPUT, dp(a, 12)));
      spaceAround(a, e, 4, 6);
    }

    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        styleTree(a, group.getChildAt(i));
      }
    }
  }
}
