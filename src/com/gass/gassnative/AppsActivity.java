package com.gass.gassnative;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AppsActivity extends Activity {
    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(16));
        return d;
    }

    private void addApp(LinearLayout col, String name, String command,
                        String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(17), dp(14), dp(17), dp(14));
        card.setBackground(shape(Color.rgb(32, 35, 43)));
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        col.addView(card, lp);

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextSize(17);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView detail = new TextView(this);
        detail.setText(description + "\nCommand: " + command);
        detail.setTextSize(12);
        detail.setTextColor(Color.LTGRAY);
        detail.setPadding(0, dp(5), 0, 0);
        card.addView(detail);

        card.setOnClickListener(v -> {
            Intent i = new Intent(this, DesktopActivity.class);
            i.putExtra("linux_command", command);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            Toast.makeText(this, "Opening " + name, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(17, 19, 25));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(22), dp(20), dp(24));
        scroll.addView(col);

        TextView heading = new TextView(this);
        heading.setText("Linux Apps");
        heading.setTextSize(28);
        heading.setTextColor(Color.WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        col.addView(heading);

        TextView note = new TextView(this);
        note.setText("Start Desktop first. An app must already be installed in the selected Debian profile.");
        note.setTextSize(13);
        note.setTextColor(Color.LTGRAY);
        note.setPadding(0, dp(6), 0, dp(18));
        col.addView(note);

        addApp(col, "Terminal", "xfce4-terminal", "XFCE terminal emulator");
        addApp(col, "File Manager", "thunar", "Browse Debian files");
        addApp(col, "Text Editor", "mousepad", "Edit text files");
        addApp(col, "Web Browser", "firefox", "Firefox, if installed");
        addApp(col, "XFCE Settings", "xfce4-settings-manager", "Desktop configuration");
        addApp(col, "XTerm", "xterm", "Alternative terminal emulator");

        setContentView(scroll);
    }
}
