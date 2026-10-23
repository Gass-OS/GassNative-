package com.gass.gassnative;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileActivity extends Activity {
  private final int bg = Color.rgb(18, 18, 20);
  private final int cardBg = Color.rgb(31, 32, 36);
  private final int fg = Color.rgb(245, 245, 247);
  private final int muted = Color.rgb(175, 178, 187);
  private final int accent = Color.rgb(130, 170, 255);

  private int dp(float n) {
    return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
  }

  private GradientDrawable shape(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  private LinearLayout vertical() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  private TextView text(String value, int size, int color) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    return t;
  }

  private void space(LinearLayout parent, int height) {
    View v = new View(this);
    parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
  }

  private LinearLayout card(LinearLayout parent, String heading) {
    LinearLayout c = vertical();
    c.setPadding(dp(18), dp(16), dp(18), dp(16));
    c.setBackground(shape(cardBg, 20));

    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(14);
    parent.addView(c, lp);

    TextView h = text(heading, 17, fg);
    h.setTypeface(null, Typeface.BOLD);
    c.addView(h);
    space(c, 12);
    return c;
  }

  private void paragraph(LinearLayout parent, String value) {
    TextView t = text(value, 14, muted);
    t.setLineSpacing(dp(3), 1f);
    parent.addView(t);
  }

  private void link(LinearLayout parent, String label, final String url) {
    TextView t = text(label + "  ↗", 15, accent);
    t.setPadding(0, dp(9), 0, dp(9));
    t.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        try {
          Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
          startActivity(i);
        } catch (Exception e) {
          Toast.makeText(ProfileActivity.this,
              "Unable to open link", Toast.LENGTH_SHORT).show();
        }
      }
    });
    parent.addView(t);
  }

  private void sectionTitle(LinearLayout parent, String value) {
    TextView t = text(value.toUpperCase(), 12, muted);
    t.setTypeface(null, Typeface.BOLD);
    t.setLetterSpacing(0.08f);
    t.setPadding(dp(4), dp(8), 0, dp(12));
    parent.addView(t);
  }

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);

    ScrollView sv = new ScrollView(this);
    sv.setFillViewport(true);
    sv.setBackgroundColor(bg);

    LinearLayout root = vertical();
    root.setPadding(dp(20), dp(22), dp(20), dp(28));
    sv.addView(root);

    TextView title = text("About Me", 29, fg);
    title.setTypeface(null, Typeface.BOLD);
    root.addView(title);

    space(root, 6);
    paragraph(root, "Developer profile, projects and Debian environments");
    space(root, 22);

    // Developer identity
    LinearLayout identity = vertical();
    identity.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
    identity.setPadding(dp(20), dp(24), dp(20), dp(22));
    identity.setBackground(shape(cardBg, 24));
    root.addView(identity, new LinearLayout.LayoutParams(-1, -2));

    ImageView avatar = new ImageView(this);
    int imageId = getResources().getIdentifier(
        "profile", "drawable", getPackageName());

    if (imageId != 0) {
      avatar.setImageResource(imageId);
      avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
    } else {
      avatar.setImageDrawable(shape(Color.rgb(55, 76, 120), 60));
      avatar.setBackground(shape(Color.rgb(55, 76, 120), 60));
    }

    avatar.setBackground(shape(Color.rgb(55, 76, 120), 60));
    avatar.setClipToOutline(true);
    LinearLayout.LayoutParams imageLp =
        new LinearLayout.LayoutParams(dp(112), dp(112));
    imageLp.bottomMargin = dp(16);
    identity.addView(avatar, imageLp);

    TextView name = text("Aymen Gharsalli", 24, fg);
    name.setTypeface(null, Typeface.BOLD);
    name.setGravity(android.view.Gravity.CENTER);
    identity.addView(name);

    space(identity, 7);
    TextView role = text("Systems & Embedded Software Engineer",
        14, accent);
    role.setGravity(android.view.Gravity.CENTER);
    identity.addView(role);

    space(identity, 12);
    TextView brand = text("Gass-OS  •  Independent Developer",
        13, muted);
    brand.setGravity(android.view.Gravity.CENTER);
    identity.addView(brand);

    space(root, 24);

    sectionTitle(root, "Developer");

    LinearLayout about = card(root, "About Me");
    paragraph(about,
        "Hello! I'm Aymen Gharsalli, an independent developer "
        + "interested in operating systems, embedded software, "
        + "Linux environments and native Android development.\n\n"
        + "My work explores bringing Linux desktop experiences "
        + "to Android devices, building ARM64 software environments, "
        + "and making system tools easier to use.");

    LinearLayout skills = card(root, "Interests & Technologies");
    paragraph(skills,
        "• Linux and GNU userland\n"
        + "• Android and native application development\n"
        + "• ARM64 and embedded systems\n"
        + "• Termux and Debian environments\n"
        + "• Desktop environments and graphical interfaces\n"
        + "• Open-source software and system integration");

    sectionTitle(root, "Projects");

    LinearLayout projects = card(root, "My Projects");
    TextView gass = text("GassNative", 16, fg);
    gass.setTypeface(null, Typeface.BOLD);
    projects.addView(gass);
    paragraph(projects,
        "An Android application for running a Debian environment "
        + "and accessing a Linux desktop.");
    space(projects, 12);

    TextView os = text("Gass-OS / GNOME-Termux", 16, fg);
    os.setTypeface(null, Typeface.BOLD);
    projects.addView(os);
    paragraph(projects,
        "Experiments and development focused on native Linux "
        + "desktop software, GNOME and graphics on Android.");
    space(projects, 8);
    link(projects, "Gass-OS on GitHub",
        "https://github.com/Gass-OS");
    link(projects, "GNOME-Termux project",
        "https://github.com/Gass-OS/GNOME-Termux");
    link(projects, "Project website",
        "https://gass-os.github.io/GNOME-Termux/");

    sectionTitle(root, "Licensing");

    LinearLayout license = card(root, "License & Open Source");
    paragraph(license,
        "GassNative is an independent project. The license for "
        + "the GassNative-specific source code is not specified "
        + "by this notice; consult the project's published "
        + "LICENSE file or repository for its actual terms.\n\n"
        + "Debian, Android platform components, libraries and "
        + "other bundled third-party software remain subject "
        + "to their respective licenses and copyright notices. "
        + "This application does not relicense those components.");
    space(license, 8);
    link(license, "Review project repository",
        "https://github.com/Gass-OS");

    sectionTitle(root, "Environment Profiles");

    LinearLayout profiles = card(root, "Debian Profiles");
    paragraph(profiles,
        "Each profile has its own Debian system. Select a profile "
        + "to switch environments. A new profile downloads its "
        + "own Debian system at first start.");
    space(profiles, 12);

    for (final String p : Cfg.profiles(this)) {
      Button bt = new Button(this);
      bt.setText((p.equals(Cfg.current(this)) ? "●  " : "○  ") + p);
      bt.setAllCaps(false);
      bt.setTextColor(fg);

      LinearLayout.LayoutParams lp =
          new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(5);
      profiles.addView(bt, lp);

      bt.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Cfg.select(ProfileActivity.this, p);
          Intent i = new Intent(ProfileActivity.this, MainActivity.class);
          i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(i);
          finish();
        }
      });
    }

    final EditText newName = new EditText(this);
    newName.setSingleLine(true);
    newName.setTextColor(fg);
    newName.setHintTextColor(muted);
    newName.setHint("New profile name");
    newName.setBackground(shape(bg, 12));
    newName.setPadding(dp(12), dp(8), dp(12), dp(8));
    profiles.addView(newName, new LinearLayout.LayoutParams(-1, -2));

    Button add = new Button(this);
    add.setText("Create Debian profile");
    add.setAllCaps(false);
    profiles.addView(add);
    add.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (Cfg.addProfile(ProfileActivity.this,
            newName.getText().toString())) {
          recreate();
        } else {
          Toast.makeText(ProfileActivity.this,
              "Invalid or existing name", Toast.LENGTH_SHORT).show();
        }
      }
    });

    TextView footer = text(
        "GassNative • Version 0.1\n"
        + "Made by Aymen Gharsalli\n"
        + "Linux on Android • ARM64",
        12, muted);
    footer.setGravity(android.view.Gravity.CENTER);
    footer.setPadding(0, dp(12), 0, dp(12));
    root.addView(footer);

    setTitle("About Me");
    Ui.addTopBar(this, root);
    Ui.styleTree(this, root);
    setContentView(sv);
  }
}
