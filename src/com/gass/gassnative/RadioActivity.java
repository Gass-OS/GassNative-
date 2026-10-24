package com.gass.gassnative;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.util.ArrayList;

public class RadioActivity extends Activity {
    private static final String[] SERVERS = {
        "https://de1.api.radio-browser.info", "https://de2.api.radio-browser.info",
        "https://nl1.api.radio-browser.info", "https://at1.api.radio-browser.info" };
    private static final String[] CHIPS = { "Top", "FR", "US", "GB", "DE", "ES", "IT", "TN", "DZ", "MA", "EG" };

    private LinearLayout list;
    private TextView status, now;
    private EditText search;
    private FrameLayout toggle;
    private MediaPlayer mp;
    private String[] current;
    private final ArrayList<TextView> chips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        int p = GassUi.dp(this, 16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        root.addView(GassUi.header(this, "Radio", GassUi.RADIO));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        search = new EditText(this);
        search.setHint("Search stations");
        search.setSingleLine(true);
        search.setTextColor(GassUi.TEXT);
        search.setHintTextColor(GassUi.MUTED);
        search.setBackground(GassUi.glass(this, 14));
        search.setPadding(p, 0, p, 0);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v, a, e) -> { load(search.getText().toString().trim(), null); return true; });
        row.addView(search, new LinearLayout.LayoutParams(0, GassUi.dp(this, 48), 1));
        FrameLayout sb = GassUi.iconButton(this, GassUi.SEARCH, 48, v -> load(search.getText().toString().trim(), null));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(GassUi.dp(this, 48), GassUi.dp(this, 48));
        slp.leftMargin = GassUi.dp(this, 8);
        row.addView(sb, slp);
        root.addView(row);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout cl = new LinearLayout(this);
        for (final String code : CHIPS) {
            final TextView ch = GassUi.chip(this, code);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = GassUi.dp(this, 8);
            ch.setOnClickListener(v -> {
                for (TextView t : chips) GassUi.chipSelect(t, t == ch);
                load("", code.equals("Top") ? null : code);
            });
            chips.add(ch);
            cl.addView(ch, lp);
        }
        hs.addView(cl);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
        hlp.topMargin = GassUi.dp(this, 10);
        hlp.bottomMargin = GassUi.dp(this, 6);
        root.addView(hs, hlp);

        status = GassUi.text(this, "", 12, GassUi.MUTED, false);
        root.addView(status);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(GassUi.glass(this, 24));
        bar.setPadding(p, GassUi.dp(this, 10), GassUi.dp(this, 10), GassUi.dp(this, 10));
        now = GassUi.text(this, "Pick a station", 14, GassUi.TEXT, true);
        now.setSingleLine(true);
        bar.addView(now, new LinearLayout.LayoutParams(0, -2, 1));
        toggle = GassUi.iconButton(this, GassUi.PLAY, 48, v -> {
            if (mp != null) stopPlayer(); else if (current != null) play(current);
        });
        bar.addView(toggle, new LinearLayout.LayoutParams(GassUi.dp(this, 48), GassUi.dp(this, 48)));
        root.addView(bar);

        setContentView(root);
        GassUi.chipSelect(chips.get(0), true);
        load("", null);
    }

    private void load(final String name, final String cc) {
        status.setText("Loading...");
        new Thread(() -> {
            ArrayList<String[]> res = new ArrayList<>();
            String err = null;
            StringBuilder q = new StringBuilder("/json/stations/search?limit=80&hidebroken=true&order=clickcount&reverse=true");
            try {
                if (name != null && name.length() > 0) q.append("&name=").append(URLEncoder.encode(name, "UTF-8"));
            } catch (Exception ignored) {}
            if (cc != null) q.append("&countrycode=").append(cc);
            String body = null;
            for (String s : SERVERS) {
                try { body = GassUi.httpGet(s + q); break; } catch (Exception e) { err = String.valueOf(e); }
            }
            if (body != null) {
                try {
                    JSONArray a = new JSONArray(body);
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject o = a.getJSONObject(i);
                        String u = o.optString("url_resolved", "");
                        if (u.length() == 0) continue;
                        res.add(new String[]{ o.optString("name", "").trim(), u, o.optString("country", ""),
                            o.optString("codec", "") + " " + o.optInt("bitrate", 0) + "k" });
                    }
                } catch (Exception e) { err = String.valueOf(e); }
            }
            final ArrayList<String[]> fr = res;
            final String fe = err;
            runOnUiThread(() -> show(fr, fe));
        }).start();
    }

    private void show(ArrayList<String[]> res, String err) {
        list.removeAllViews();
        if (res.isEmpty()) {
            status.setText(err != null ? "Network error: " + err : "No stations found");
            return;
        }
        status.setText(res.size() + " stations");
        for (final String[] s : res) {
            LinearLayout r = GassUi.row(this, s[0].isEmpty() ? "Station" : s[0], s[2] + "  " + s[3]);
            r.setOnClickListener(v -> play(s));
            list.addView(r);
        }
    }

    private void play(final String[] s) {
        stopPlayer();
        current = s;
        now.setText("Connecting: " + s[0]);
        try {
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mp.setDataSource(s[1]);
            mp.setOnPreparedListener(m -> { m.start(); now.setText(s[0]); });
            mp.setOnErrorListener((m, w, x) -> { now.setText("Unavailable: " + s[0]); stopPlayer(); return true; });
            mp.prepareAsync();
            ((GassUi.Icon) toggle.getTag()).setType(GassUi.STOP);
        } catch (Exception e) {
            now.setText("Error: " + e.getMessage());
            stopPlayer();
        }
    }

    private void stopPlayer() {
        if (mp != null) {
            try { mp.release(); } catch (Exception ignored) {}
            mp = null;
        }
        ((GassUi.Icon) toggle.getTag()).setType(GassUi.PLAY);
    }

    @Override
    protected void onDestroy() {
        stopPlayer();
        super.onDestroy();
    }
}
