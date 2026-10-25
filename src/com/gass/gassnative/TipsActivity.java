package com.gass.gassnative;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import java.util.ArrayList;

public class TipsActivity extends Activity {
    private static final String[] CATS = { "Tips", "Linux", "Games", "Android", "Software", "Radio/TV", "Privacy", "Code", "Media" };
    private static final String[][][] ITEMS = {
        { {"Keyboard", "", "Tap the keyboard button on the desktop to type. A hardware keyboard works too."},
          {"Mouse gestures", "", "Drag one finger to move the pointer and tap to click. Hold, then drag, to move things. Two fingers scroll, and a quick two-finger tap right-clicks."},
          {"Your files", "", "Android storage is /sdcard inside Linux, and your Download folder is ~/Download."},
          {"Windows games", "", "Put .exe files in Download/Games, then open Window Games."},
          {"Power off", "", "Use Power Off on the home screen to stop the desktop and free memory."} },
        { {"Linux Journey", "https://linuxjourney.com", "Free, beginner-friendly Linux lessons"},
          {"tldr pages", "https://tldr.sh", "Short practical examples for common commands"},
          {"explainshell", "https://explainshell.com", "Paste a command and see what each part does"},
          {"Debian Wiki", "https://wiki.debian.org", "Official Debian how-tos"},
          {"Arch Wiki", "https://wiki.archlinux.org", "Excellent docs that apply to most distros"},
          {"The Linux Command Line", "https://linuxcommand.org", "Free book about the shell"},
          {"LinuxFr.org", "https://linuxfr.org", "French Linux and free-software news"},
          {"Debian-Facile", "https://debian-facile.org", "French Debian help and tutorials"} },
        { {"WineHQ AppDB", "https://appdb.winehq.org", "Check how well a program runs on Wine"},
          {"ProtonDB", "https://www.protondb.com", "Community reports for Windows games"},
          {"Lutris", "https://lutris.net", "Game install scripts and guides"},
          {"FEX-Emu", "https://fex-emu.com", "The x86 emulator for Windows apps on ARM"},
          {"Hangover", "https://github.com/AndreRH/hangover", "Wine for ARM with FEX, used by GassNative"},
          {"itch.io", "https://itch.io", "Indie and free games, many for Windows"} },
        { {"Termux", "https://termux.dev", "Terminal environment for Android"},
          {"Termux wiki", "https://wiki.termux.com", "Guides and package help"},
          {"F-Droid", "https://f-droid.org", "Open-source Android app store"},
          {"DroidNative", "https://github.com/DroidNative/DroidNativeApp", "The Wayland bridge behind the desktop display"},
          {"Android Developers", "https://developer.android.com", "Official Android documentation"} },
        { {"Flathub", "https://flathub.org", "Catalog of Flatpak apps"},
          {"AlternativeTo", "https://alternativeto.net", "Open alternatives to popular apps"},
          {"Internet Archive", "https://archive.org", "Free books, software, music and video"},
          {"VideoLAN", "https://www.videolan.org", "VLC and other media tools"},
          {"GitHub", "https://github.com", "Source code and releases"} },
        { {"Radio Browser", "https://www.radio-browser.info", "Station database used by the Radio screen"},
          {"iptv-org", "https://github.com/iptv-org/iptv", "Public channel playlists used by the TV screen"} },
        { {"Privacy Guides", "https://www.privacyguides.org", "Tools and advice for private computing"},
          {"Have I Been Pwned", "https://haveibeenpwned.com", "Check if your email appeared in a breach"},
          {"VirusTotal", "https://www.virustotal.com", "Scan files and links before opening them"},
          {"EFF Self-Defense", "https://ssd.eff.org", "Guides to protect yourself online"} },
        { {"freeCodeCamp", "https://www.freecodecamp.org", "Free coding courses"},
          {"MDN Web Docs", "https://developer.mozilla.org", "Web development reference"},
          {"roadmap.sh", "https://roadmap.sh", "Learning paths for developers"},
          {"DevDocs", "https://devdocs.io", "Fast API docs in one place"},
          {"regex101", "https://regex101.com", "Test and explain regular expressions"} },
        { {"Pexels", "https://www.pexels.com", "Free photos and videos"},
          {"Wikimedia Commons", "https://commons.wikimedia.org", "Free-to-use images and media"},
          {"Freesound", "https://freesound.org", "Free sound effects and samples"},
          {"Free Music Archive", "https://freemusicarchive.org", "Free music downloads"} } };

    private LinearLayout list;
    private final ArrayList<TextView> chips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        GassUi.setup(this);
        int p = GassUi.dp(this, 16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(p, p, p, p);
        root.addView(GassUi.header(this, "Useful tips", GassUi.INFO));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout cl = new LinearLayout(this);
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            final TextView ch = GassUi.chip(this, CATS[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = GassUi.dp(this, 8);
            ch.setOnClickListener(v -> {
                for (TextView t : chips) GassUi.chipSelect(t, t == ch);
                show(idx);
            });
            chips.add(ch);
            cl.addView(ch, lp);
        }
        hs.addView(cl);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
        hlp.bottomMargin = GassUi.dp(this, 10);
        root.addView(hs, hlp);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        GassUi.setContent(this, root);
        GassUi.chipSelect(chips.get(0), true);
        show(0);
    }

    private void show(int cat) {
        list.removeAllViews();
        for (final String[] it : ITEMS[cat]) {
            LinearLayout r = GassUi.row(this, it[0], it[2]);
            if (it[1].length() > 0) {
                String host = Uri.parse(it[1]).getHost();
                r.addView(GassUi.text(this, host == null ? it[1] : host, 11, GassUi.BLUE, false));
                r.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(it[1])));
                    } catch (Exception e) {
                        Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            list.addView(r);
        }
    }
}
