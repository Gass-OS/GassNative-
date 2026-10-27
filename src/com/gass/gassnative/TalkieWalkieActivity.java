package com.gass.gassnative;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class TalkieWalkieActivity extends Activity {
    private int selected = 0;
    private TextView[] channelViews = new TextView[3];
    private final String[] channels = {"GLOBAL", "GAMING", "FRIENDS"};
    private final String[] descriptions = {
        "Open community channel",
        "Games, teams and sessions",
        "Your private group — setup pending"
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        GassUi.setup(this);

        int pad = GassUi.dp(this, 18);
        int teal = Color.rgb(25, 190, 190);
        int bg = Color.rgb(10, 16, 29);
        int panel = Color.rgb(22, 31, 49);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(pad, pad, pad, pad);
        page.setBackgroundColor(bg);

        page.addView(GassUi.header(this, "Talkie Walkie", GassUi.MIC));

        TextView intro = text("PUSH TO TALK", 12, teal, true);
        intro.setPadding(0, GassUi.dp(this, 18), 0, GassUi.dp(this, 5));
        page.addView(intro);

        TextView subtitle = text("Choose a channel and talk hands-free when online voice is available.", 15, Color.rgb(190, 202, 222), false);
        page.addView(subtitle);

        TextView status = text("●  VOICE SERVICE NOT CONNECTED", 12, Color.rgb(255, 190, 90), true);
        status.setPadding(0, GassUi.dp(this, 14), 0, GassUi.dp(this, 12));
        page.addView(status);

        TextView channelTitle = text("CHANNELS", 13, Color.WHITE, true);
        channelTitle.setPadding(0, GassUi.dp(this, 8), 0, GassUi.dp(this, 8));
        page.addView(channelTitle);

        for (int i = 0; i < channels.length; i++) {
            final int index = i;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(GassUi.dp(this, 15), GassUi.dp(this, 12),
                            GassUi.dp(this, 15), GassUi.dp(this, 12));
            card.setBackground(shape(panel, i == selected ? teal : Color.rgb(54, 67, 91), 18, i == selected ? 3 : 1));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = GassUi.dp(this, 9);
            page.addView(card, cp);

            TextView name = text(channels[i], 15, Color.WHITE, true);
            TextView desc = text(descriptions[i], 12, Color.rgb(180, 194, 216), false);
            card.addView(name);
            card.addView(desc);
            channelViews[i] = name;
            card.setOnClickListener(v -> selectChannel(index, panel, teal));
        }

        TextView ptt = text("HOLD TO TALK", 17, Color.WHITE, true);
        ptt.setGravity(Gravity.CENTER);
        ptt.setPadding(0, GassUi.dp(this, 24), 0, GassUi.dp(this, 24));
        ptt.setBackground(shape(Color.rgb(26, 95, 111), teal, 28, 2));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.topMargin = GassUi.dp(this, 12);
        page.addView(ptt, pp);
        ptt.setOnClickListener(v -> Toast.makeText(
            this,
            "Live voice is not active yet. No audio is being transmitted.",
            Toast.LENGTH_LONG
        ).show());

        
        TextView connect = text("CONNECT VIA WALKIE CLOUD  ↗", 15, Color.WHITE, true);
        connect.setGravity(Gravity.CENTER);
        connect.setPadding(0, GassUi.dp(this, 17), 0, GassUi.dp(this, 17));
        connect.setBackground(shape(Color.rgb(19, 117, 130), teal, 18, 2));
        LinearLayout.LayoutParams connectParams = new LinearLayout.LayoutParams(-1, -2);
        connectParams.topMargin = GassUi.dp(this, 12);
        page.addView(connect, connectParams);
        connect.setOnClickListener(v -> {
            Intent intent = new Intent(this, WalkieCloudActivity.class);
            intent.putExtra("channel_name", channels[selected]);
            startActivity(intent);
        });

        TextView note = text("Channels are a local preview in this build. Online rooms, user presence, and real-time voice still need a connected voice service.", 12, Color.rgb(150, 165, 189), false);
        note.setPadding(0, GassUi.dp(this, 14), 0, GassUi.dp(this, 8));
        page.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        GassUi.setContent(this, scroll);
    }

    private void selectChannel(int index, int panel, int teal) {
        selected = index;
        for (int i = 0; i < channelViews.length; i++) {
            View card = (View) channelViews[i].getParent();
            card.setBackground(shape(panel, i == selected ? teal : Color.rgb(54, 67, 91), 18, i == selected ? 3 : 1));
        }
        Toast.makeText(this, channels[index] + " selected (preview only)", Toast.LENGTH_SHORT).show();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private GradientDrawable shape(int fill, int stroke, int radiusDp, int widthDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(GassUi.dp(this, radiusDp));
        d.setStroke(GassUi.dp(this, widthDp), stroke);
        return d;
    }
}
