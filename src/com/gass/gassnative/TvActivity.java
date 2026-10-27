package com.gass.gassnative;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class TvActivity extends Activity {
    private static final String BASE = "https://iptv-org.github.io/iptv/";
    private static final String[][] SRC = {
        {"FR", "countries/fr"}, {"US", "countries/us"}, {"UK", "countries/gb"},
        {"DE", "countries/de"}, {"ES", "countries/es"}, {"IT", "countries/it"},
        {"TN", "countries/tn"}, {"DZ", "countries/dz"}, {"MA", "countries/ma"},
        {"EG", "countries/eg"}, {"LU", "countries/lu"}, {"BE", "countries/be"},
        {"CH", "countries/ch"}, {"CA", "countries/ca"}, {"NL", "countries/nl"},
        {"AT", "countries/at"}, {"PT", "countries/pt"}, {"SE", "countries/se"},
        {"PL", "countries/pl"}, {"GR", "countries/gr"}, {"AE", "countries/ae"},
        {"SA", "countries/sa"}, {"QA", "countries/qa"}, {"JO", "countries/jo"},
        {"LB", "countries/lb"}, {"IQ", "countries/iq"}, {"LY", "countries/ly"},
        {"PS", "countries/ps"},
        {"TR", "countries/tr"}, {"GR", "countries/gr"},
        {"RO", "countries/ro"}, {"IN", "countries/in"},
        {"JP", "countries/jp"}, {"BR", "countries/br"},
        {"MX", "countries/mx"}, {"AU", "countries/au"},
        {"ZA", "countries/za"}, {"NG", "countries/ng"},
        {"KR", "countries/kr"}, {"ID", "countries/id"},
        {"PK", "countries/pk"}, {"AF", "countries/af"},
        {"KW", "countries/kw"}, {"OM", "countries/om"},
        {"BH", "countries/bh"}, {"SY", "countries/sy"},
        {"IR", "countries/ir"}, {"CY", "countries/cy"},
        {"CZ", "countries/cz"}, {"HU", "countries/hu"},
        {"HR", "countries/hr"}, {"RS", "countries/rs"},
        {"UA", "countries/ua"}, {"IE", "countries/ie"},
        {"NO", "countries/no"}, {"DK", "countries/dk"},
        {"FI", "countries/fi"}, {"AR", "countries/ar"},
        {"CL", "countries/cl"}, {"CO", "countries/co"},
        {"Worldwide", "regions/ww"},
        {"Arab World", "regions/arab"},
        {"Maghreb", "regions/maghreb"},
        {"Middle East", "regions/mideast"},
        {"Benelux", "regions/benelux"},
        {"Europe", "regions/europe"}, {"Africa", "regions/africa"},
        {"Asia", "regions/asia"}, {"Americas", "regions/americas"},
        {"Arabic", "languages/ara"}, {"French", "languages/fra"},
        {"English", "languages/eng"}, {"Italian", "languages/ita"},
        {"German", "languages/deu"}, {"Turkish", "languages/tur"},
        {"Spanish", "languages/spa"}, {"Portuguese", "languages/por"},
        {"Hindi", "languages/hin"}, {"Japanese", "languages/jpn"},
        {"News", "categories/news"}, {"Sports", "categories/sports"},
        {"Entertainment", "categories/entertainment"},
        {"General", "categories/general"},
        {"Animation", "categories/animation"},
        {"Series", "categories/series"},
        {"Family", "categories/family"},
        {"Comedy", "categories/comedy"},
        {"Culture", "categories/culture"},
        {"Education", "categories/education"},
        {"Documentary", "categories/documentary"},
        {"Science", "categories/science"},
        {"Travel", "categories/travel"},
        {"Cooking", "categories/cooking"},
        {"Business", "categories/business"},
        {"Lifestyle", "categories/lifestyle"},
        {"Kids", "categories/kids"}, {"Music", "categories/music"},
        {"Movies", "categories/movies"},
        {"Religious", "categories/religious"},
        {"Weather", "categories/weather"},
        {"Classic", "categories/classic"},
        {"Public", "categories/public"}
    };

    private LinearLayout root, list;
    private View header, chipsScroll, listScroll;
    private FrameLayout frame;
    private VideoView video;
    private ProgressBar spinner;
    private TextView status;
    private boolean full;
    private int normalH, pad;
    private final ArrayList<TextView> chips = new ArrayList<>();

    private String sourceIcon(String label) {
        switch (label) {
            case "FR": return "🇫🇷";
            case "US": return "🇺🇸";
            case "UK": return "🇬🇧";
            case "DE": return "🇩🇪";
            case "ES": return "🇪🇸";
            case "IT": return "🇮🇹";
            case "TN": return "🇹🇳";
            case "DZ": return "🇩🇿";
            case "MA": return "🇲🇦";
            case "EG": return "🇪🇬";
            case "LU": return "🇱🇺";
            case "BE": return "🇧🇪";
            case "CH": return "🇨🇭";
            case "CA": return "🇨🇦";
            case "NL": return "🇳🇱";
            case "AT": return "🇦🇹";
            case "PT": return "🇵🇹";
            case "SE": return "🇸🇪";
            case "PL": return "🇵🇱";
            case "GR": return "🇬🇷";
            case "TR": return "🇹🇷";
            case "RO": return "🇷🇴";
            case "IN": return "🇮🇳";
            case "JP": return "🇯🇵";
            case "BR": return "🇧🇷";
            case "MX": return "🇲🇽";
            case "AU": return "🇦🇺";
            case "ZA": return "🇿🇦";
            case "NG": return "🇳🇬";
            case "KR": return "🇰🇷";
            case "PK": return "🇵🇰";
            case "SA": return "🇸🇦";
            case "AE": return "🇦🇪";
            case "QA": return "🇶🇦";
            case "KW": return "🇰🇼";
            case "OM": return "🇴🇲";
            case "BH": return "🇧🇭";
            case "JO": return "🇯🇴";
            case "LB": return "🇱🇧";
            case "IQ": return "🇮🇶";
            case "LY": return "🇱🇾";
            case "PS": return "🇵🇸";
            case "CY": return "🇨🇾";
            case "CZ": return "🇨🇿";
            case "HU": return "🇭🇺";
            case "HR": return "🇭🇷";
            case "RS": return "🇷🇸";
            case "UA": return "🇺🇦";
            case "IE": return "🇮🇪";
            case "NO": return "🇳🇴";
            case "DK": return "🇩🇰";
            case "FI": return "🇫🇮";
            case "AR": return "🇦🇷";
            case "CL": return "🇨🇱";
            case "CO": return "🇨🇴";
            case "Europe": return "🌍";
            case "Africa": return "🌍";
            case "Asia": return "🌏";
            case "Americas": return "🌎";
            case "Worldwide": return "🌐";
            case "Arab World": return "🌙";
            case "Maghreb": return "🌙";
            case "Middle East": return "🕌";
            case "Benelux": return "🏙️";
            case "Arabic": return "🗣️";
            case "French": return "🥐";
            case "English": return "🔤";
            case "Italian": return "🍕";
            case "German": return "🗣️";
            case "Turkish": return "🧿";
            case "Spanish": return "💃";
            case "Portuguese": return "🎶";
            case "Hindi": return "🗣️";
            case "Japanese": return "🗾";
            case "News": return "📰";
            case "Sports": return "⚽";
            case "Kids": return "🧸";
            case "Music": return "🎵";
            case "Movies": return "🎬";
            case "Docs":
            case "Documentary": return "🎥";
            case "Entertainment": return "🎭";
            case "General": return "📺";
            case "Animation": return "🎨";
            case "Series": return "🍿";
            case "Family": return "👨‍👩‍👧‍👦";
            case "Comedy": return "😂";
            case "Culture": return "🎭";
            case "Education": return "🎓";
            case "Science": return "🔬";
            case "Travel": return "✈️";
            case "Cooking": return "🍳";
            case "Business": return "💼";
            case "Lifestyle": return "✨";
            case "Religious": return "🕊️";
            case "Weather": return "🌦️";
            case "Classic": return "📻";
            case "Public": return "📡";
            default: return "📺";
        }
    }


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        pad = GassUi.dp(this, 16);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        normalH = (int) ((Math.min(dm.widthPixels, dm.heightPixels) - 2 * pad) * 9f / 16f);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        header = GassUi.header(this, "TV", GassUi.TV);
        root.addView(header);

        frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);
        video = new VideoView(this);
        frame.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        spinner = new ProgressBar(this);
        spinner.setVisibility(View.GONE);
        frame.addView(spinner, new FrameLayout.LayoutParams(GassUi.dp(this, 40), GassUi.dp(this, 40), Gravity.CENTER));
        video.setOnPreparedListener(m -> { spinner.setVisibility(View.GONE); m.start(); });
        video.setOnErrorListener((m, w, x) -> {
            spinner.setVisibility(View.GONE);
            status.setText("Channel unavailable (offline or geo-blocked)");
            return true;
        });
        video.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_UP) setFull(!full);
            return true;
        });
        root.addView(frame, new LinearLayout.LayoutParams(-1, normalH));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout cl = new LinearLayout(this);
        for (final String[] src : SRC) {
            final TextView ch = GassUi.chip(this, sourceIcon(src[0]) + "  " + src[0]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = GassUi.dp(this, 8);
            ch.setOnClickListener(v -> {
                for (TextView t : chips) GassUi.chipSelect(t, t == ch);
                loadPlaylist(src[1]);
            });
            chips.add(ch);
            cl.addView(ch, lp);
        }
        hs.addView(cl);
        chipsScroll = hs;
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
        hlp.topMargin = GassUi.dp(this, 12);
        hlp.bottomMargin = GassUi.dp(this, 6);
        root.addView(hs, hlp);

        status = GassUi.text(this, "Pick a country or category. Tap the video for full screen.", 12, GassUi.MUTED, false);
        root.addView(status);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        listScroll = sv;
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        GassUi.setContent(this, root);
        GassUi.chipSelect(chips.get(0), true);
        loadPlaylist(SRC[0][1]);
    }

    private void loadPlaylist(final String path) {
        status.setText("Loading channels...");
        list.removeAllViews();
        new Thread(() -> {
            ArrayList<String[]> out = new ArrayList<>();
            String err = null;
            try { out = parse(GassUi.httpGet(BASE + path + ".m3u")); } catch (Exception e) { err = String.valueOf(e); }
            final ArrayList<String[]> fo = out;
            final String fe = err;
            runOnUiThread(() -> showChannels(fo, fe));
        }).start();
    }

    private ArrayList<String[]> parse(String body) {
        ArrayList<String[]> out = new ArrayList<>();
        String title = null;
        for (String line : body.split("\n")) {
            line = line.trim();
            if (line.startsWith("#EXTINF")) {
                int i = line.lastIndexOf(',');
                title = i >= 0 ? line.substring(i + 1).trim() : "Channel";
            } else if (line.length() > 0 && !line.startsWith("#") && title != null) {
                if (line.startsWith("http")) out.add(new String[]{ title, line });
                title = null;
            }
        }
        return out;
    }

    private void showChannels(ArrayList<String[]> ch, String err) {
        list.removeAllViews();
        if (ch.isEmpty()) {
            status.setText(err != null ? "Network error: " + err : "No channels in this list");
            return;
        }
        status.setText(ch.size() + " channels");
        int n = 0;
        for (final String[] c : ch) {
            if (n++ >= 300) break;
            LinearLayout r = GassUi.row(this, c[0], null);
            r.setOnClickListener(v -> play(c[0], c[1]));
            list.addView(r);
        }
    }

    private void play(String name, String url) {
        status.setText(name);
        spinner.setVisibility(View.VISIBLE);
        video.stopPlayback();
        video.setVideoURI(Uri.parse(url));
    }

    private void setFull(boolean f) {
        full = f;
        int vis = f ? View.GONE : View.VISIBLE;
        header.setVisibility(vis);
        chipsScroll.setVisibility(vis);
        status.setVisibility(vis);
        listScroll.setVisibility(vis);
        root.setPadding(f ? 0 : pad, f ? 0 : pad, f ? 0 : pad, f ? 0 : pad);
        android.view.ViewGroup.LayoutParams lp = frame.getLayoutParams();
        lp.height = f ? android.view.ViewGroup.LayoutParams.MATCH_PARENT : normalH;
        frame.setLayoutParams(lp);
        setRequestedOrientation(f ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().getDecorView().setSystemUiVisibility(f
            ? (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN)
            : 0);
    }

    @Override
    public void onBackPressed() {
        if (full) setFull(false); else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        video.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        video.stopPlayback();
        super.onDestroy();
    }
}
