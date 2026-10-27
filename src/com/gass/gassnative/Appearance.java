package com.gass.gassnative;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;

public final class Appearance {
    public static final String[] THEME_IDS = {
        "gass_orange", "kali_blue", "matrix_green",
        "retro_terminal", "glass"
    };

    public static final String[] THEME_LABELS = {
        "Gass Orange", "Kali Blue", "Matrix Green",
        "Retro Terminal", "Glass"
    };

    public static final String[] FONT_IDS = {
        "system", "sans", "rounded", "medium",
        "monospace", "serif", "condensed", "black"
    };

    public static final String[] FONT_LABELS = {
        "System", "Modern Sans", "Rounded", "Medium",
        "Monospace", "Serif", "Condensed", "Bold Display"
    };

    private Appearance() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("gass_appearance", Context.MODE_PRIVATE);
    }

    public static String theme(Context c) {
        return prefs(c).getString("theme", "gass_orange");
    }

    public static String font(Context c) {
        return prefs(c).getString("font", "system");
    }

    public static int textSize(Context c) {
        return Math.max(80, Math.min(125,
            prefs(c).getInt("text_size", 100)));
    }

    public static float scale(Context c) {
        return textSize(c) / 100f;
    }

    public static Typeface typeface(Context c, boolean bold) {
        String f = font(c);
        Typeface base;
        switch (f) {
            case "sans":
                base = Typeface.create("sans-serif", Typeface.NORMAL);
                break;
            case "rounded":
                base = Typeface.create("sans-serif-rounded", Typeface.NORMAL);
                break;
            case "medium":
                base = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                break;
            case "monospace":
                base = Typeface.MONOSPACE;
                break;
            case "serif":
                base = Typeface.SERIF;
                break;
            case "condensed":
                base = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
                break;
            case "black":
                base = Typeface.create("sans-serif-black", Typeface.NORMAL);
                break;
            default:
                base = Typeface.DEFAULT;
                break;
        }
        return Typeface.create(base, bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    public static int bg(Context c) {
        switch (theme(c)) {
            case "kali_blue": return Color.rgb(8, 16, 35);
            case "matrix_green": return Color.rgb(0, 10, 5);
            case "retro_terminal": return Color.rgb(10, 17, 10);
            case "glass": return Color.rgb(12, 18, 29);
            default: return Color.rgb(15, 12, 18);
        }
    }

    public static int panel(Context c) {
        switch (theme(c)) {
            case "kali_blue": return Color.rgb(20, 32, 58);
            case "matrix_green": return Color.rgb(8, 28, 17);
            case "retro_terminal": return Color.rgb(17, 31, 17);
            case "glass": return Color.rgb(28, 38, 55);
            default: return Color.rgb(32, 25, 37);
        }
    }

    public static int input(Context c) {
        switch (theme(c)) {
            case "kali_blue": return Color.rgb(12, 23, 43);
            case "matrix_green":
            case "retro_terminal": return Color.rgb(2, 12, 6);
            case "glass": return Color.rgb(14, 22, 36);
            default: return Color.rgb(21, 18, 26);
        }
    }

    public static int fg(Context c) {
        if ("retro_terminal".equals(theme(c))) {
            return Color.rgb(190, 255, 165);
        }
        return Color.rgb(240, 245, 252);
    }

    public static int muted(Context c) {
        switch (theme(c)) {
            case "kali_blue": return Color.rgb(159, 185, 225);
            case "matrix_green":
            case "retro_terminal": return Color.rgb(115, 190, 130);
            default: return Color.rgb(165, 178, 199);
        }
    }

    public static int accent(Context c) {
        switch (theme(c)) {
            case "kali_blue": return Color.rgb(95, 160, 255);
            case "matrix_green": return Color.rgb(45, 255, 120);
            case "retro_terminal": return Color.rgb(165, 255, 100);
            case "glass": return Color.rgb(110, 190, 255);
            default: return Color.rgb(255, 145, 75);
        }
    }
}
