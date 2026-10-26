package com.gass.gassnative;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.system.Os;
import android.text.Editable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
  static final String REPO = "library/debian";
  static final String TAG = "trixie-slim";
  static final String ACCEPT =
      "application/vnd.oci.image.index.v1+json,"
      + "application/vnd.docker.distribution.manifest.list.v2+json,"
      + "application/vnd.oci.image.manifest.v1+json,"
      + "application/vnd.docker.distribution.manifest.v2+json";
  static final int FG = Color.rgb(235, 235, 235);

  TextView out;
  LinearLayout terminalPanel, installerPanel;
  TextView installerStatus;
  ProgressBar installerProgress;
  Button terminalToggle;
  ImageView installerLogo;
  boolean terminalExpanded = false;
  ScrollView sv;
  EditText input;
  String nl;
  File root;
  volatile Process cur;
  volatile boolean busy;
  ArrayList<String> history = new ArrayList<String>();
  int hpos;
  float tsize;
  ScaleGestureDetector scaler;
  private MediaPlayer introPlayer;

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);

    boolean arm64Supported = false;
    for (String abi : android.os.Build.SUPPORTED_ABIS) {
      if ("arm64-v8a".equals(abi)) {
        arm64Supported = true;
        break;
      }
    }
    if (!arm64Supported || !android.os.Process.is64Bit()) {
      new android.app.AlertDialog.Builder(this)
          .setTitle("Device not compatible")
          .setMessage("GassNative requires a 64-bit ARM Android device (arm64-v8a).")
          .setCancelable(false)
          .setPositiveButton("Close", (dialog, which) -> finish())
          .show();
      return;
    }

    tsize = Cfg.sp(this).getFloat("tsize", 12f);
    nl = getApplicationInfo().nativeLibraryDir;
    root = Cfg.rootDir(this);

    scaler = new ScaleGestureDetector(this,
        new ScaleGestureDetector.SimpleOnScaleGestureListener() {
      public boolean onScale(ScaleGestureDetector d) {
        setSize(tsize * d.getScaleFactor());
        return true;
      }
    });

    LinearLayout col = new LinearLayout(this);
    col.setOrientation(LinearLayout.VERTICAL);
    col.setBackgroundColor(Color.BLACK);
    terminalPanel = new LinearLayout(this);
    terminalPanel.setOrientation(LinearLayout.VERTICAL);
    terminalPanel.setBackgroundColor(Color.BLACK);

    sv = new ScrollView(this) {
      @Override
      public boolean dispatchTouchEvent(MotionEvent e) {
        scaler.onTouchEvent(e);
        if (scaler.isInProgress()) return true;
        return super.dispatchTouchEvent(e);
      }
    };
    out = new TextView(this);
    out.setTextColor(FG);
    out.setBackgroundColor(Color.BLACK);
    out.setTypeface(Typeface.MONOSPACE);
    out.setTextIsSelectable(true);
    out.setPadding(16, 16, 16, 16);
    out.append("");
    setSize(tsize);
    sv.addView(out);
    terminalPanel.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

    HorizontalScrollView hs = new HorizontalScrollView(this);
    LinearLayout keys = new LinearLayout(this);
    hs.addView(keys);
    keys.addView(key("Y", new Runnable() { public void run() { sendLine("y"); } }));
    keys.addView(key("N", new Runnable() { public void run() { sendLine("n"); } }));
    keys.addView(key("Enter", new Runnable() { public void run() { sendLine(""); } }));
    keys.addView(key("Ctrl+C", new Runnable() { public void run() { stop(); } }));
    keys.addView(key("Ctrl+D", new Runnable() { public void run() { closeStdin(); } }));
    keys.addView(key("↑", new Runnable() { public void run() { hist(-1); } }));
    keys.addView(key("↓", new Runnable() { public void run() { hist(1); } }));
    keys.addView(key("Paste", new Runnable() { public void run() { paste(); } }));
    terminalPanel.addView(hs);

    LinearLayout row = new LinearLayout(this);
    input = new EditText(this);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    input.setSingleLine(true);
    input.setTextColor(Color.WHITE);
    input.setHintTextColor(Color.GRAY);
    input.setHint("command in Debian");
    input.setImeOptions(EditorInfo.IME_ACTION_SEND);
    input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      public boolean onEditorAction(TextView v, int id, KeyEvent e) {
        submit();
        return true;
      }
    });
    row.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
    row.addView(key("Run", new Runnable() { public void run() { submit(); } }));
    terminalPanel.addView(row);
    installerPanel = new LinearLayout(this);
    installerPanel.setOrientation(LinearLayout.VERTICAL);
    installerPanel.setGravity(Gravity.CENTER);
    installerPanel.setPadding(24, 24, 24, 24);
    installerPanel.setBackgroundColor(Color.rgb(10, 14, 24));

    installerLogo = new ImageView(this);
    installerLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
    try {
      java.io.InputStream logoStream =
          getAssets().open("gassnative-square.png");
      installerLogo.setImageBitmap(BitmapFactory.decodeStream(logoStream));
      logoStream.close();
    } catch (Exception ignored) { }

    int logoSize = (int)(144 * getResources().getDisplayMetrics().density);
    installerPanel.addView(installerLogo,
        new LinearLayout.LayoutParams(logoSize, logoSize));

    TextView installerTitle = new TextView(this);
    installerTitle.setText("GASSNATIVE");
    installerTitle.setTextColor(Color.WHITE);
    installerTitle.setTextSize(26);
    installerTitle.setGravity(Gravity.CENTER);
    installerTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    LinearLayout.LayoutParams titleLp =
        new LinearLayout.LayoutParams(-1, -2);
    titleLp.topMargin = 18;
    installerPanel.addView(installerTitle, titleLp);

    TextView subtitle = new TextView(this);
    subtitle.setText("Preparing your Linux desktop");
    subtitle.setTextColor(Color.rgb(130, 174, 255));
    subtitle.setTextSize(15);
    subtitle.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams subtitleLp =
        new LinearLayout.LayoutParams(-1, -2);
    subtitleLp.topMargin = 8;
    installerPanel.addView(subtitle, subtitleLp);

    installerStatus = new TextView(this);
    installerStatus.setText("Preparing Debian environment...");
    installerStatus.setTextColor(Color.LTGRAY);
    installerStatus.setTextSize(14);
    installerStatus.setGravity(Gravity.CENTER);
    installerStatus.setPadding(8, 24, 8, 18);
    installerPanel.addView(installerStatus,
        new LinearLayout.LayoutParams(-1, -2));

    installerProgress = new ProgressBar(this, null,
        android.R.attr.progressBarStyleHorizontal);
    installerProgress.setIndeterminate(true);
    installerPanel.addView(installerProgress,
        new LinearLayout.LayoutParams(-1,
            (int)(5 * getResources().getDisplayMetrics().density)));

    ObjectAnimator pulse =
        ObjectAnimator.ofFloat(installerLogo, "alpha", 0.65f, 1.0f);
    pulse.setDuration(1100);
    pulse.setRepeatMode(ValueAnimator.REVERSE);
    pulse.setRepeatCount(ValueAnimator.INFINITE);
    pulse.start();

    terminalToggle = key("▼ Show terminal output", new Runnable() {
      public void run() {
        terminalExpanded = !terminalExpanded;
        terminalPanel.setVisibility(
            terminalExpanded ? View.VISIBLE : View.GONE);
        terminalToggle.setText(terminalExpanded
            ? "▲ Hide terminal output" : "▼ Show terminal output");
      }
    });

    col.addView(installerPanel,
        new LinearLayout.LayoutParams(-1, 0, 1f));
    col.addView(terminalToggle,
        new LinearLayout.LayoutParams(-1, -2));
    col.addView(terminalPanel,
        new LinearLayout.LayoutParams(-1, 0, 1f));
    terminalPanel.setVisibility(View.GONE);

    Ui.addTopBar(this, col);
    Ui.styleTree(this, col);
    setContentView(col);

    startCinematicIntroAudio();


    new Thread(new Runnable() {
      public void run() { boot(); }
    }).start();
  }


  // Plays a bundled intro once when MainActivity is created.
  // The audio is optional: the app still starts normally if it is absent.
  private void startCinematicIntroAudio() {
    if (!Cfg.sp(this).getBoolean("intro_sound", true)) return;

    MediaPlayer player = null;
    try {
      File audioFile = new File(getCacheDir(), "gassnative_intro.mp3");

      try (InputStream in = getAssets().open("audio/gassnative_intro.mp3");
           FileOutputStream outFile = new FileOutputStream(audioFile)) {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) {
          outFile.write(buffer, 0, count);
        }
      }

      player = new MediaPlayer();
      player.setAudioAttributes(new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build());
      player.setVolume(0.85f, 0.85f);
      player.setDataSource(audioFile.getAbsolutePath());
      player.setOnCompletionListener(mp -> {
        mp.release();
        if (introPlayer == mp) introPlayer = null;
      });
      player.setOnErrorListener((mp, what, extra) -> {
        mp.release();
        if (introPlayer == mp) introPlayer = null;
        return true;
      });

      player.prepare();
      introPlayer = player;
      player.start();
    } catch (Exception e) {
      if (player != null) {
        try { player.release(); } catch (Exception ignored) { }
      }
      introPlayer = null;
      android.util.Log.w("GassNative", "Startup intro audio unavailable", e);
    }
  }

  Button key(String label, final Runnable r) {
    Button k = new Button(this);
    k.setText(label);
    k.setAllCaps(false);
    k.setTextColor(Color.WHITE);
    k.setBackgroundColor(0xFF2A2A2A);
    k.setMinWidth(0);
    k.setMinimumWidth(0);
    k.setPadding(36, 0, 36, 0);
    k.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) { r.run(); }
    });
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
    lp.setMargins(4, 4, 4, 4);
    k.setLayoutParams(lp);
    return k;
  }

  void setSize(float s) {
    tsize = Math.max(6f, Math.min(40f, s));
    out.setTextSize(TypedValue.COMPLEX_UNIT_SP, tsize);
    Cfg.sp(this).edit().putFloat("tsize", tsize).apply();
  }

  void toast(final String s) {
    runOnUiThread(new Runnable() {
      public void run() { Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show(); }
    });
  }

  void setHint(final boolean running) {
    runOnUiThread(new Runnable() {
      public void run() {
        input.setHint(running ? "input for the running command (y / n ...)" : "command in Debian");
      }
    });
  }

  void submit() {
    String c = input.getText().toString();
    input.setText("");
    if (busy) {
      sendLine(c);
      return;
    }
    c = c.trim();
    if (c.length() == 0) return;
    history.add(c);
    hpos = history.size();
    log("$ " + c + "\n");
    final String cmd = c;
    busy = true;
    new Thread(new Runnable() {
      public void run() {
        try {
          debian(cmd);
        } catch (Exception e) {
          log("ERROR: " + e + "\n");
        } finally {
          busy = false;
        }
      }
    }).start();
  }

  void sendLine(String s) {
    Process p = cur;
    if (p == null) {
      toast("No command is running");
      return;
    }
    try {
      OutputStream os = p.getOutputStream();
      os.write((s + "\n").getBytes("UTF-8"));
      os.flush();
      log("> " + s + "\n");
    } catch (IOException e) {
      log("(input closed)\n");
    }
  }

  void stop() {
    Process p = cur;
    if (p == null) return;
    log("^C\n");
    p.destroy();
    try {
      Runtime.getRuntime().exec(new String[] { "sh", "-c", "pkill -f libproot.so" });
    } catch (IOException e) { }
  }

  void closeStdin() {
    Process p = cur;
    if (p == null) return;
    try {
      p.getOutputStream().close();
      log("^D\n");
    } catch (IOException e) { }
  }

  void hist(int d) {
    if (history.isEmpty()) return;
    hpos = Math.max(0, Math.min(history.size(), hpos + d));
    input.setText(hpos == history.size() ? "" : history.get(hpos));
    input.setSelection(input.getText().length());
  }

  void paste() {
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
      String t = cm.getPrimaryClip().getItemAt(0).coerceToText(this).toString().replace("\n", " ");
      int at = Math.max(0, input.getSelectionStart());
      input.getText().insert(at, t);
    }
  }

  void copyAll() {
    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    cm.setPrimaryClip(ClipData.newPlainText("terminal", out.getText().toString()));
    toast("Copied");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu m) {
    m.add(0, 1, 0, "Profiles");
    m.add(0, 2, 0, "Settings");
    m.add(0, 3, 0, "About");
    m.add(0, 4, 0, "Copy all output");
    m.add(0, 5, 0, "Clear screen");
    m.add(0, 6, 0, "Text size +");
    m.add(0, 7, 0, "Text size -");
    m.add(0, 8, 0, "Desktop");
    m.add(0, 9, 0, "Music");
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem it) {
    switch (it.getItemId()) {
      case 1: startActivity(new Intent(this, ProfileActivity.class)); break;
      case 2: startActivity(new Intent(this, SettingsActivity.class)); break;
      case 3: startActivity(new Intent(this, AboutActivity.class)); break;
      case 4: copyAll(); break;
      case 5: ((Editable) out.getText()).clear(); break;
      case 6: setSize(tsize + 2); break;
      case 7: setSize(tsize - 2); break;
      case 8: startActivity(new Intent(this, DesktopActivity.class)); break;
      case 9: startActivity(new Intent(this, MusicActivity.class)); break;
    }
    return true;
  }

  void boot() {
    busy = true;
    final File targetRoot = root;
    final File staging = new File(targetRoot.getParentFile(),
        targetRoot.getName() + ".installing");
    try {
      boolean complete =
          new File(targetRoot, "usr/bin/bash").isFile()
          && new File(targetRoot, "usr/bin/env").isFile()
          && new File(targetRoot, "etc/os-release").isFile();

      if (!complete) {
        log("Debian is missing or incomplete. Preparing installation...\\n");

        if (staging.exists()) Cfg.rm(staging);
        if (!staging.mkdirs() && !staging.isDirectory())
          throw new IOException("Cannot create staging directory: " + staging);

        root = staging;
        try {
          pull();

          if (!new File(staging, "usr/bin/bash").isFile()
              || !new File(staging, "usr/bin/env").isFile()
              || !new File(staging, "etc/os-release").isFile()) {
            throw new IOException("Debian verification failed; existing root preserved.");
          }
        } catch (Exception e) {
          root = targetRoot;
          throw e;
        }

        root = targetRoot;
        File backup = null;
        if (targetRoot.exists()) {
          backup = new File(targetRoot.getParentFile(),
              targetRoot.getName() + ".backup-" + System.currentTimeMillis());
          if (!targetRoot.renameTo(backup))
            throw new IOException("Cannot move existing Debian root safely; it was preserved.");
        }

        if (!staging.renameTo(targetRoot)) {
          if (backup != null && backup.exists() && !backup.renameTo(targetRoot))
            throw new IOException("Install failed. Existing Debian root remains at: " + backup);
          throw new IOException("Could not activate the new Debian installation.");
        }
        if (backup != null && backup.exists()) Cfg.rm(backup);
      }

      root = targetRoot;
      write(new File(targetRoot, "etc/resolv.conf"),
          "nameserver " + Cfg.dns(this) + "\n");
      write(new File(targetRoot, "etc/apt/apt.conf.d/01-gassnative"),
          "APT::Sandbox::User \"root\";\n");
      setInstallerStatus("Checking Debian installation...");
      debian("head -2 /etc/os-release; uname -m");
      installDesktopPackagesIfNeeded();
      setInstallerStatus("Desktop installation completed!");
      showInstaller(false);
    } catch (Exception e) {
      root = targetRoot;
      setInstallerFailure("Setup failed. Terminal output is expanded below.");
      terminalExpanded = true;
      showInstaller(true);
      log("ERROR: " + e + "\\n");
    } finally {
      root = targetRoot;
      busy = false;
    }
  }



  void copyBundledHangoverArchive() throws Exception {
    File tmpDir = new File(root, "tmp");
    if (!tmpDir.isDirectory() && !tmpDir.mkdirs()) {
      throw new IOException("Cannot create Debian /tmp directory: " + tmpDir);
    }

    File dest = new File(tmpDir, "gass-hangover.tar");
    log("GASS-WINE: Copying bundled Hangover archive into Debian...\n");

    try (
        java.io.InputStream in =
            getAssets().open("hangover/hangover_11.16_debian13_trixie_arm64.tar");
        java.io.FileOutputStream out = new java.io.FileOutputStream(dest)
    ) {
      byte[] buffer = new byte[1024 * 1024];
      int n;
      while ((n = in.read(buffer)) != -1) {
        out.write(buffer, 0, n);
      }
      out.getFD().sync();
    }

    log("GASS-WINE: Bundled archive copied (" + dest.length() + " bytes).\n");
  }

  void installWindowsRuntimeIfNeeded() throws Exception {
    String script =
        "set -eu\n" +
        "export DEBIAN_FRONTEND=noninteractive\n" +
        "mkdir -p /var/lib /home/user/Desktop\n" +
        "if ! command -v zenity >/dev/null 2>&1; then apt-get update --error-on=any && apt-get install -y zenity; fi\n" +
        "if ! command -v wine >/dev/null 2>&1 || " +
        "   [ ! -f /usr/lib/wine/aarch64-windows/libarm64ecfex.dll ] || " +
        "   [ ! -f /usr/lib/wine/aarch64-windows/libwow64fex.dll ]; then\n" +
        "  echo 'GASS-WINE Installing bundled Hangover packages...'\n" +
        "  test -s /tmp/gass-hangover.tar || { echo 'ERROR: Bundled Hangover archive is missing from Debian /tmp'; exit 1; }\n" +
        "  apt-get update --error-on=any\n" +
        "  apt-get install -y ca-certificates python3 zenity\n" +
        "  mkdir -p /tmp/gass-hangover\n" +
        "  tar -xf /tmp/gass-hangover.tar -C /tmp/gass-hangover\n" +
        "  find /tmp/gass-hangover -type f -name '*.deb' -print > /tmp/gass-hangover-packages.txt\n" +
        "  echo 'GASS-WINE Packages found:'\n" +
        "  cat /tmp/gass-hangover-packages.txt\n" +
        "  [ \"$(wc -l < /tmp/gass-hangover-packages.txt)\" -ge 4 ] || { echo 'ERROR: Expected Hangover packages are missing from the bundled archive'; exit 1; }\n" +
        "  xargs -r apt-get install -y < /tmp/gass-hangover-packages.txt\n" +
        "  rm -rf /tmp/gass-hangover /tmp/gass-hangover-packages.txt\n" +
        "fi\n" +
        "rm -f /tmp/gass-hangover.tar\n" +
        "wine --version\n" +
        "test -f /usr/lib/wine/aarch64-windows/libarm64ecfex.dll\n" +
        "test -f /usr/lib/wine/aarch64-windows/libwow64fex.dll\n" +
        "cat > /usr/local/bin/gass-windows-app <<'GASS_WINE_EOF'\n" +
        "#!/bin/bash\n" +
        "export HOME=/home/user WINEPREFIX=/home/user/.gass-wine\n" +
        "export DISPLAY=${DISPLAY:-:1}\n" +
        "export HODLL64=libarm64ecfex.dll HODLL=libwow64fex.dll\n" +
                        "if [ -n \"${1:-}\" ]; then exec wine \"$1\"; fi\n" +
        "APP=$(zenity --file-selection --title='Choose a Windows application' --file-filter='Windows applications (*.exe) | *.exe') || exit 0\n" +
        "[ -n \"$APP\" ] && exec wine \"$APP\"\n" +
        "GASS_WINE_EOF\n" +
        "chmod 755 /usr/local/bin/gass-windows-app\n" +
        "mkdir -p /usr/share/applications /home/user/.local/share/applications /home/user/Desktop\n" +
        "cat > /usr/share/applications/gass-windows-exe.desktop <<'GASS_EXE_EOF'\n" +
        "[Desktop Entry]\n" +
        "Type=Application\n" +
        "Name=Windows Application (Hangover)\n" +
        "Exec=/usr/local/bin/gass-windows-app %f\n" +
        "Icon=application-x-ms-dos-executable\n" +
        "Terminal=false\n" +
        "MimeType=application/x-ms-dos-executable;application/x-msdownload;\n" +
        "Categories=System;\n" +
        "GASS_EXE_EOF\n" +
        "cat > /home/user/Desktop/Windows-Apps.desktop <<'GASS_DESKTOP_EOF'\n" +
        "[Desktop Entry]\n" +
        "Version=1.0\n" +
        "Type=Application\n" +
        "Name=Windows Apps\n" +
        "Exec=/usr/local/bin/gass-windows-app\n" +
        "Icon=application-x-ms-dos-executable\n" +
        "Terminal=false\n" +
        "Categories=System;\n" +
        "GASS_DESKTOP_EOF\n" +
        "cat > /home/user/.local/share/applications/mimeapps.list <<'GASS_MIME_EOF'\n" +
        "[Default Applications]\n" +
        "application/x-ms-dos-executable=gass-windows-exe.desktop\n" +
        "application/x-msdownload=gass-windows-exe.desktop\n" +
        "\n[Added Associations]\n" +
        "application/x-ms-dos-executable=gass-windows-exe.desktop;\n" +
        "application/x-msdownload=gass-windows-exe.desktop;\n" +
        "GASS_MIME_EOF\n" +
        "chown -R user:user /home/user/.local /home/user/Desktop\n" +
        "chmod 755 /home/user/Desktop/Windows-Apps.desktop\n" +
          "echo 'GASS-WINE Runtime and shortcut ready.'\n" +
          "touch /var/lib/gassnative-hangover-installed\n";
    if (!new File(root, "var/lib/gassnative-hangover-installed").isFile()) {
      copyBundledHangoverArchive();
    }
    debian(script);
  }


  void stageDxvkIfNeeded() throws Exception {
    File dxgi = new File(root,
        "opt/gassnative/dxvk-v2.7.1/arm64ec/dxgi.dll");
    if (dxgi.isFile() && dxgi.length() > 0) {
      log("GASS-DXVK: Bundled DXVK files already staged.\\n");
      return;
    }

    copyBundledHangoverArchive();
    String script =
        "set -eu\n" +
        "mkdir -p /opt/gassnative\n" +
        "test -s /tmp/gass-hangover.tar\n" +
        "tar -xOf /tmp/gass-hangover.tar dxvk-v2.7.1.tar.gz > /tmp/gass-dxvk.tar.gz\n" +
        "tar -tzf /tmp/gass-dxvk.tar.gz | grep -q '^dxvk-v2.7.1/arm64ec/dxgi.dll$'\n" +
        "tar -xzf /tmp/gass-dxvk.tar.gz -C /opt/gassnative\n" +
        "test -s /opt/gassnative/dxvk-v2.7.1/arm64ec/dxgi.dll\n" +
        "test -s /opt/gassnative/dxvk-v2.7.1/aarch64/dxgi.dll\n" +
        "rm -f /tmp/gass-dxvk.tar.gz /tmp/gass-hangover.tar\n" +
        "echo 'GASS-DXVK: DXVK 2.7.1 staged; not yet enabled.'\n";
    debian(script);
  }


  void installQemuIfNeeded() throws Exception {
    File marker = new File(root, "var/lib/gassnative-qemu-v1.done");

    if (marker.isFile()) {
      log("GASS-QEMU: Already installed.\n");
      return;
    }

    log("GASS-QEMU: Installing x86/x86-64 emulation...\n");

    String script =
        "set -eu\n" +
        "export DEBIAN_FRONTEND=noninteractive\n" +
        "echo 'GASS-QEMU Updating APT repositories...'\n" +
        "apt-get update --error-on=any\n" +
        "dpkg --configure -a\n" +
        "apt-get install -y qemu-system-x86 qemu-utils qemu-system-gui\n" +
        "echo 'GASS-QEMU Verifying binaries...'\n" +
        "command -v qemu-system-i386\n" +
        "command -v qemu-system-x86_64\n" +
        "command -v qemu-img\n" +
        "qemu-system-i386 --version\n" +
        "qemu-system-x86_64 --version\n" +
        "qemu-img --version\n" +
        "mkdir -p /var/lib\n" +
        "touch /var/lib/gassnative-qemu-v1.done\n" +
        "echo 'GASS-QEMU Installation completed.'\n";

    debian(script);

    if (!marker.isFile())
      throw new IOException("QEMU installation did not complete.");

    log("GASS-QEMU: x86/x86-64 emulator ready.\n");
  }

  void installDesktopPackagesIfNeeded() throws Exception {
    // Ensure the desktop account exists even when desktop packages
    // were installed by an earlier version of GassNative.
    debian("set -eu\n" +
        "command -v useradd >/dev/null 2>&1 || { echo 'ERROR: useradd is missing'; exit 1; }\n" +
        "if ! getent passwd user >/dev/null 2>&1; then useradd -M -U -d /home/user -s /bin/bash user; fi\n" +
        "getent group user >/dev/null 2>&1 || groupadd user\n" +
        "usermod -g user -d /home/user -s /bin/bash user\n" +
        "mkdir -p /home/user\n" +
        "chown -R user:user /home/user\n" +
        "chmod 700 /home/user\n" +
        "getent passwd user\n" +
        "echo 'GASS-SETUP Desktop account verified.'\n");

    installWindowsRuntimeIfNeeded();
    stageDxvkIfNeeded();
    installQemuIfNeeded();

    File marker = new File(root,
        "var/lib/gassnative-desktop-setup-v1.done");

    if (marker.isFile()) {
      log("Desktop installation already completed.\\n");
      return;
    }

    log("Starting automatic Debian desktop installation...\\n");

    String script =
        "set -eu\n" +
        "export DEBIAN_FRONTEND=noninteractive\n" +
        "echo 'GASS-SETUP Updating repositories...'\n" +
        "apt-get update --error-on=any\n" +
        "echo 'GASS-SETUP Recovering interrupted package configuration...'\n" +
        "dpkg --configure -a || { rc=$?; echo \"ERROR: dpkg --configure -a failed (exit $rc)\"; exit $rc; }\n" +
        "apt-get check || { rc=$?; echo \"ERROR: apt-get check failed (exit $rc)\"; exit $rc; }\n" +
        "echo 'GASS-SETUP Upgrading Debian...'\n" +
        "apt-get -y -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold upgrade\n" +
        "echo 'GASS-SETUP Installing XFCE4 and applications...'\n" +
        "apt-get install -y git wget tar nano sudo file man-db bash-completion dialog locales qemu-system-x86 qemu-utils qemu-system-gui xorg xwayland x11-xserver-utils x11-utils xauth xclip dbus dbus-x11 libx11-6 libx11-xcb1 libxcb1 libxcb-render0 libxcb-shm0 libxcb-xfixes0 libwayland-client0 libwayland-cursor0 libwayland-egl1 mesa-utils mesa-vulkan-drivers libvulkan1 vulkan-tools pulseaudio xfce4 xfce4-settings xfce4-goodies xfce4-terminal vlc firefox-esr w3m kitty feh chafa fortune-mod gimp mpv\n" +
        "for p in fastfetch lsd hardinfo2 freetuxtv; do\n" +
        "  if apt-cache show \"$p\" >/dev/null 2>&1; then\n" +
        "    apt-get install -y \"$p\" || echo \"Optional package failed: $p\"\n" +
        "  else\n" +
        "    echo \"Optional package unavailable: $p\"\n" +
        "  fi\n" +
        "done\n" +
        "test -x /usr/bin/startxfce4\n" +
        "test -x /usr/bin/Xwayland\n" +
        "test -x /usr/bin/dbus-launch\n" +
        "mkdir -p /var/lib\n" +
        "touch /var/lib/gassnative-desktop-setup-v1.done\n" +
        "echo 'GASS-SETUP Installation completed.'\n";

    debian(script);

    if (!marker.isFile())
      throw new IOException(
          "Desktop installation failed. Check the APT output above.");

    log("XFCE4 and desktop packages installed successfully.\\n");
  }


  void setInstallerStatus(final String message) {
    runOnUiThread(new Runnable() {
      public void run() {
        if (installerStatus != null) installerStatus.setText(message);
        if (installerProgress != null) installerProgress.setIndeterminate(true);
      }
    });
  }

  void setInstallerFailure(final String message) {
    runOnUiThread(new Runnable() {
      public void run() {
        if (installerStatus != null) {
          installerStatus.setText(message);
          installerStatus.setTextColor(Color.rgb(255, 150, 120));
        }
        if (installerProgress != null) {
          installerProgress.setIndeterminate(false);
          installerProgress.setProgress(0);
        }
      }
    });
  }

  void showInstaller(final boolean active) {
    runOnUiThread(new Runnable() {
      public void run() {
        if (installerPanel == null) return;
        if (active) {
          installerPanel.setVisibility(View.VISIBLE);
          terminalToggle.setVisibility(View.VISIBLE);
          terminalPanel.setVisibility(
              terminalExpanded ? View.VISIBLE : View.GONE);
          terminalToggle.setText(terminalExpanded
              ? "▲ Hide terminal output" : "▼ Show terminal output");
        } else {
          installerPanel.setVisibility(View.GONE);
          terminalToggle.setVisibility(View.GONE);
          terminalPanel.setVisibility(View.VISIBLE);
        }
      }
    });
  }

  void updateInstallerStatusFromLog(String message) {
    if (installerStatus == null || message == null) return;

    if (message.contains("Contacting Docker Hub")) {
      installerStatus.setText("Connecting to Docker Hub...");
    } else if (message.contains("Downloading layer")) {
      installerStatus.setText("Downloading Debian filesystem...");
    } else if (message.contains("Extracting")) {
      installerStatus.setText("Extracting Debian filesystem...");
    } else if (message.contains("GASS-SETUP Updating")) {
      installerStatus.setText("Updating Debian repositories...");
    } else if (message.contains("GASS-SETUP Upgrading")) {
      installerStatus.setText("Upgrading Debian packages...");
    } else if (message.contains("GASS-SETUP Installing")) {
      installerStatus.setText("Installing XFCE4 and desktop applications...");
    } else if (message.contains("Optional package")) {
      installerStatus.setText("Finishing optional applications...");
    } else if (message.contains("GASS-SETUP Installation completed")) {
      installerStatus.setText("Finalizing your Linux desktop...");
    }
  }

  void write(File f, String s) throws IOException {
    f.getParentFile().mkdirs();
    FileWriter w = new FileWriter(f);
    w.write(s);
    w.close();
  }

  static String q(String s) {
    return "'" + s.replace("'", "'\\''") + "'";
  }

  void debian(String cmd) throws Exception {
    sh("LD_LIBRARY_PATH=" + nl + " PROOT_LOADER=" + nl
        + "/libproot-loader.so PROOT_TMP_DIR=" + getCacheDir() + " "
        + nl + "/libproot.so --link2symlink -0 -r " + root
        + " -b /dev -b /proc -b /sys" + Cfg.binds(this)
        + " -w /root /usr/bin/env -i HOME=/root"
        + " TERM=xterm LANG=C.UTF-8"
        + " PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        + Cfg.env(this)
        + " /usr/bin/bash -c " + q(cmd));
  }

  void pull() throws Exception {
    String reg = "https://registry-1.docker.io/v2/" + REPO;
    log("Contacting Docker Hub (use Wi-Fi)...\n");
    String tok = new JSONObject(httpGet(
        "https://auth.docker.io/token?service=registry.docker.io&scope=repository:"
        + REPO + ":pull", null)).getString("token");
    JSONObject m = new JSONObject(httpGet(reg + "/manifests/" + TAG, tok));
    if (m.has("manifests")) {
      String dg = null;
      JSONArray ms = m.getJSONArray("manifests");
      for (int i = 0; i < ms.length(); i++) {
        JSONObject p = ms.getJSONObject(i).optJSONObject("platform");
        if (p != null && "linux".equals(p.optString("os"))
            && "arm64".equals(p.optString("architecture"))) {
          dg = ms.getJSONObject(i).getString("digest");
          break;
        }
      }
      if (dg == null) throw new IOException("no linux/arm64 image found");
      m = new JSONObject(httpGet(reg + "/manifests/" + dg, tok));
    }
    JSONArray layers = m.getJSONArray("layers");
    for (int i = 0; i < layers.length(); i++) {
      JSONObject l = layers.getJSONObject(i);
      if (l.getString("mediaType").contains("zstd"))
        throw new IOException("zstd layers not supported yet");
      File f = new File(getFilesDir(), "layer" + i + ".tar.gz");
      log("Downloading layer " + (i + 1) + "/" + layers.length() + "...\n");
      download(reg + "/blobs/" + l.getString("digest"), tok, f, l.optLong("size"));
      log("Extracting...\n");
      extract(f);
      f.delete();
    }
  }

  String httpGet(String u, String tok) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
    c.setConnectTimeout(15000);
    c.setReadTimeout(30000);
    c.setRequestProperty("Accept", ACCEPT);
    if (tok != null) c.setRequestProperty("Authorization", "Bearer " + tok);
    if (c.getResponseCode() != 200)
      throw new IOException("HTTP " + c.getResponseCode() + " " + u);
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    InputStream in = c.getInputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
    in.close();
    return bo.toString("UTF-8");
  }

  void download(String u, String tok, File dest, long total) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
    c.setConnectTimeout(15000);
    c.setReadTimeout(30000);
    c.setRequestProperty("Authorization", "Bearer " + tok);
    if (c.getResponseCode() != 200)
      throw new IOException("HTTP " + c.getResponseCode() + " (blob)");
    InputStream in = c.getInputStream();
    OutputStream os = new FileOutputStream(dest);
    byte[] buf = new byte[65536];
    long done = 0, last = 0;
    int n;
    while ((n = in.read(buf)) > 0) {
      os.write(buf, 0, n);
      done += n;
      if (done - last > 8 * 1048576) {
        last = done;
        log((done / 1048576) + " / " + (total / 1048576) + " MB\n");
      }
    }
    os.close();
    in.close();
  }

  private void copyTarAsset(String name, File dest) throws IOException {
    try (InputStream in = getAssets().open(name);
         OutputStream out = new FileOutputStream(dest)) {
      byte[] buf = new byte[65536];
      int n;
      while ((n = in.read(buf)) != -1) {
        out.write(buf, 0, n);
      }
      out.flush();
    }
  }


  void extract(File gz) throws Exception {
    if (!gz.isFile() || gz.length() == 0)
      throw new IOException("Docker layer archive is missing or empty");

    if (!root.isDirectory() && !root.mkdirs())
      throw new IOException("Cannot create Debian staging root");

    log("Extracting Debian layer inside APK (Java TAR reader)...\n");

    File canonicalRoot = root.getCanonicalFile();
    java.util.Map<String, String> globalPax =
        new java.util.HashMap<String, String>();
    java.util.Map<String, String> localPax =
        new java.util.HashMap<String, String>();
    java.util.List<File> directories = new java.util.ArrayList<File>();
    java.util.List<Integer> directoryModes =
        new java.util.ArrayList<Integer>();

    String longName = null;
    String longLink = null;
    int entries = 0;

    try (java.io.InputStream in = new java.util.zip.GZIPInputStream(
        new java.io.BufferedInputStream(new FileInputStream(gz), 65536),
        65536)) {

      byte[] header;
      while ((header = tarReadBlock(in)) != null) {
        if (tarZeroBlock(header)) break;

        String name = tarField(header, 0, 100);
        String prefix = tarField(header, 345, 155);
        if (prefix.length() != 0) name = prefix + "/" + name;

        String link = tarField(header, 157, 100);
        int type = header[156] & 255;
        long size = tarOctal(header, 124, 12);
        int mode = (int) tarOctal(header, 100, 8);

        if (size < 0)
          throw new IOException("Invalid TAR entry size: " + name);

        if (type == 'L' || type == 'K' || type == 'x' || type == 'g') {
          byte[] meta = tarReadBytes(in, size);
          tarSkipPadding(in, size);
          String value = new String(meta, "UTF-8");
          while (value.endsWith("\0") || value.endsWith("\n"))
            value = value.substring(0, value.length() - 1);

          if (type == 'L') {
            longName = value;
          } else if (type == 'K') {
            longLink = value;
          } else {
            java.util.Map<String, String> parsed = tarParsePax(meta);
            if (type == 'g') globalPax.putAll(parsed);
            else localPax.putAll(parsed);
          }
          continue;
        }

        String entryName = localPax.containsKey("path")
            ? localPax.get("path")
            : (longName != null ? longName : name);
        String entryLink = localPax.containsKey("linkpath")
            ? localPax.get("linkpath")
            : (longLink != null ? longLink : link);

        String sizeText = localPax.containsKey("size")
            ? localPax.get("size") : globalPax.get("size");
        if (sizeText != null) {
          try {
            size = Long.parseLong(sizeText);
          } catch (NumberFormatException e) {
            throw new IOException("Invalid PAX TAR size: " + sizeText);
          }
        }

        longName = null;
        longLink = null;
        localPax.clear();

        if (size < 0)
          throw new IOException("Negative TAR entry size: " + entryName);

        String rel = tarNormalize(entryName);
        if (rel.length() == 0) {
          tarSkipEntry(in, size);
          continue;
        }

        File dest = tarSafePath(canonicalRoot, rel);
        File parent = dest.getParentFile();

        if (type == 0 || type == '0' || type == '7') {
          if (!parent.isDirectory() && !parent.mkdirs()
              && !parent.isDirectory())
            throw new IOException("Cannot create parent directory: " + parent);

          if (dest.isDirectory())
            throw new IOException("File entry conflicts with directory: " + rel);

          if (dest.exists() || tarIsSymlink(dest)) Cfg.rm(dest);

          try (OutputStream out = new BufferedOutputStream(
              new FileOutputStream(dest), 65536)) {
            tarCopyExactly(in, out, size);
          }
          tarSkipPadding(in, size);

          int fileMode = mode & 0777;
          if (fileMode != 0) {
            try {
              android.system.Os.chmod(dest.getAbsolutePath(), fileMode);
            } catch (Exception e) {
              log("Permission note for " + rel + ": " + e.getMessage() + "\n");
            }
          }
          entries++;

        } else if (type == '5') {
          tarSkipEntry(in, size);
          if (dest.exists() && !dest.isDirectory()) Cfg.rm(dest);
          if (!dest.isDirectory() && !dest.mkdirs() && !dest.isDirectory())
            throw new IOException("Cannot create directory: " + rel);

          directories.add(dest);
          directoryModes.add(Integer.valueOf(mode & 0777));
          entries++;

        } else if (type == '2') {
          tarSkipEntry(in, size);
          if (!parent.isDirectory() && !parent.mkdirs()
              && !parent.isDirectory())
            throw new IOException("Cannot create symlink parent: " + parent);

          if (dest.exists() || tarIsSymlink(dest)) Cfg.rm(dest);
          android.system.Os.symlink(entryLink, dest.getAbsolutePath());
          entries++;

        } else if (type == '1') {
          tarSkipEntry(in, size);
          String targetRel = tarNormalize(entryLink);
          if (targetRel.length() == 0)
            throw new IOException("Invalid hard-link target: " + entryLink);

          File target = tarSafePath(canonicalRoot, targetRel);
          if (!parent.isDirectory() && !parent.mkdirs()
              && !parent.isDirectory())
            throw new IOException("Cannot create hard-link parent: " + parent);

          if (dest.exists() || tarIsSymlink(dest)) Cfg.rm(dest);

          // Android app storage may not permit hard links.
          // A relative symlink preserves the Debian filesystem reference.
          android.system.Os.symlink(
              tarRelativeLink(parent, target), dest.getAbsolutePath());
          entries++;

        } else {
          // Ignore device nodes, FIFOs and other special entries.
          tarSkipEntry(in, size);
        }

        if ((entries % 2000) == 0)
          log("Extracted " + entries + " TAR entries...\n");
      }
    }

    // Apply directory permissions only after extracting the layer.
    for (int i = directories.size() - 1; i >= 0; i--) {
      int dm = directoryModes.get(i).intValue();
      if (dm != 0) {
        try {
          android.system.Os.chmod(
              directories.get(i).getAbsolutePath(), dm);
        } catch (Exception e) {
          log("Directory permission note: " + e.getMessage() + "\n");
        }
      }
    }

    log("Java TAR extraction completed: " + entries + " entries.\n");
  }

  private byte[] tarReadBlock(InputStream in) throws IOException {
    byte[] b = new byte[512];
    int off = 0;
    while (off < b.length) {
      int n = in.read(b, off, b.length - off);
      if (n < 0) {
        if (off == 0) return null;
        throw new IOException("Truncated TAR header");
      }
      if (n == 0) continue;
      off += n;
    }
    return b;
  }

  private boolean tarZeroBlock(byte[] b) {
    for (int i = 0; i < b.length; i++)
      if (b[i] != 0) return false;
    return true;
  }

  private String tarField(byte[] b, int off, int len)
      throws IOException {
    int end = off;
    while (end < off + len && b[end] != 0) end++;
    try {
      return new String(b, off, end - off, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      throw new IOException(e);
    }
  }

  private long tarOctal(byte[] b, int off, int len)
      throws IOException {
    // Standard Docker TAR headers use octal numeric fields.
    int i = off;
    int end = off + len;
    while (i < end && (b[i] == 0 || b[i] == ' ')) i++;
    long value = 0;
    boolean found = false;
    for (; i < end && b[i] != 0 && b[i] != ' '; i++) {
      if (b[i] < '0' || b[i] > '7')
        throw new IOException("Unsupported TAR numeric field");
      found = true;
      if (value > (Long.MAX_VALUE - 7) / 8)
        throw new IOException("TAR numeric field overflow");
      value = value * 8 + (b[i] - '0');
    }
    return found ? value : 0;
  }

  private byte[] tarReadBytes(InputStream in, long size)
      throws IOException {
    if (size < 0 || size > 16L * 1024 * 1024)
      throw new IOException("TAR metadata is too large: " + size);
    byte[] data = new byte[(int) size];
    int off = 0;
    while (off < data.length) {
      int n = in.read(data, off, data.length - off);
      if (n < 0) throw new IOException("Truncated TAR metadata");
      if (n == 0) continue;
      off += n;
    }
    return data;
  }

  private void tarCopyExactly(InputStream in, OutputStream out, long size)
      throws IOException {
    byte[] buf = new byte[65536];
    long left = size;
    while (left > 0) {
      int n = in.read(buf, 0, (int) Math.min(buf.length, left));
      if (n < 0) throw new IOException("Truncated TAR file data");
      if (n == 0) continue;
      out.write(buf, 0, n);
      left -= n;
    }
  }

  private void tarSkipEntry(InputStream in, long size)
      throws IOException {
    byte[] buf = new byte[8192];
    long left = size;
    while (left > 0) {
      int n = in.read(buf, 0, (int) Math.min(buf.length, left));
      if (n < 0) throw new IOException("Truncated TAR entry");
      if (n == 0) continue;
      left -= n;
    }
    tarSkipPadding(in, size);
  }

  private void tarSkipPadding(InputStream in, long size)
      throws IOException {
    long pad = (512 - (size % 512)) % 512;
    while (pad > 0) {
      long n = in.skip(pad);
      if (n > 0) {
        pad -= n;
      } else {
        if (in.read() < 0) throw new IOException("Truncated TAR padding");
        pad--;
      }
    }
  }

  private java.util.Map<String, String> tarParsePax(byte[] data)
      throws IOException {
    java.util.Map<String, String> out =
        new java.util.HashMap<String, String>();
    int pos = 0;
    while (pos < data.length) {
      int sp = pos;
      while (sp < data.length && data[sp] != ' ') sp++;
      if (sp == data.length) break;
      int len;
      try {
        len = Integer.parseInt(new String(data, pos, sp - pos, "UTF-8"));
      } catch (Exception e) {
        throw new IOException("Invalid PAX record length");
      }
      if (len <= 0 || pos + len > data.length || sp + 1 >= data.length)
        throw new IOException("Invalid PAX record");

      int begin = sp + 1;
      int end = pos + len;
      if (data[end - 1] == '\n') end--;
      int eq = begin;
      while (eq < end && data[eq] != '=') eq++;
      if (eq < end) {
        String key = new String(data, begin, eq - begin, "UTF-8");
        String val = new String(data, eq + 1, end - eq - 1, "UTF-8");
        out.put(key, val);
      }
      pos += len;
    }
    return out;
  }

  private String tarNormalize(String name) throws IOException {
    if (name == null) throw new IOException("Missing TAR path");
    if (name.startsWith("/"))
      throw new IOException("Absolute TAR entry rejected: " + name);

    String s = name;
    while (s.startsWith("./")) s = s.substring(2);
    while (s.endsWith("/") && s.length() > 0)
      s = s.substring(0, s.length() - 1);

    if (s.length() == 0 || ".".equals(s)) return "";

    String[] parts = s.split("/");
    StringBuilder out = new StringBuilder();
    for (String part : parts) {
      if (part.length() == 0 || ".".equals(part)) continue;
      if ("..".equals(part))
        throw new IOException("TAR path traversal rejected: " + name);
      if (out.length() > 0) out.append('/');
      out.append(part);
    }
    return out.toString();
  }

  private File tarSafePath(File base, String rel) throws IOException {
    File dest = new File(base, rel);
    File parent = dest.getParentFile();
    String basePath = base.getCanonicalPath();
    String parentPath = parent.getCanonicalPath();
    if (!parentPath.equals(basePath)
        && !parentPath.startsWith(basePath + File.separator))
      throw new IOException("TAR path escapes staging root: " + rel);
    return dest;
  }

  private boolean tarIsSymlink(File f) {
    try {
      return (android.system.Os.lstat(f.getAbsolutePath()).st_mode
          & android.system.OsConstants.S_IFMT)
          == android.system.OsConstants.S_IFLNK;
    } catch (Exception e) {
      return false;
    }
  }

  private String tarRelativeLink(File parent, File target)
      throws IOException {
    String[] a = parent.getAbsolutePath().split("/");
    String[] b = target.getAbsolutePath().split("/");
    int i = 0;
    while (i < a.length && i < b.length && a[i].equals(b[i])) i++;
    StringBuilder r = new StringBuilder();
    for (int j = i; j < a.length; j++) {
      if (a[j].length() > 0) {
        if (r.length() > 0) r.append('/');
        r.append("..");
      }
    }
    for (int j = i; j < b.length; j++) {
      if (b[j].length() > 0) {
        if (r.length() > 0) r.append('/');
        r.append(b[j]);
      }
    }
    return r.length() == 0 ? "." : r.toString();
  }

  void sh(String cmd) throws Exception {
    Process p = new ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start();
    cur = p;
    setHint(true);
    try {
      InputStreamReader r = new InputStreamReader(p.getInputStream(), "UTF-8");
      char[] buf = new char[4096];
      int n;
      while ((n = r.read(buf)) > 0) log(new String(buf, 0, n));
      int rc = p.waitFor();
      log("exit=" + rc + "\n");
      if (rc != 0)
        throw new IOException("Command failed with exit code " + rc);
    } finally {
      cur = null;
      setHint(false);
    }
  }


  void log(final String s) {
    runOnUiThread(new Runnable() {
      public void run() {
        updateInstallerStatusFromLog(s);
        View c = sv.getChildAt(0);
        boolean bottom = c == null || c.getBottom() - (sv.getHeight() + sv.getScrollY()) < 300;
        String[] parts = s.replaceAll("\\u001B\\[[0-9;?]*[A-Za-z]", "")
            .replace("\\r\\n", "\\n").split("\\r", -1);
        for (int i = 0; i < parts.length; i++) {
          if (i > 0) {
            Editable e = (Editable) out.getText();
            int len = e.length(), st = len;
            while (st > 0 && e.charAt(st - 1) != '\n') st--;
            e.delete(st, len);
          }
          out.append(parts[i]);
        }
        Editable e = (Editable) out.getText();
        if (e.length() > 300000) e.delete(0, 100000);
        if (bottom) sv.post(new Runnable() {
          public void run() { sv.fullScroll(View.FOCUS_DOWN); }
        });
      }
    });
  }

  @Override
  protected void onDestroy() {
    if (introPlayer != null) {
      try {
        introPlayer.release();
      } catch (Exception ignored) { }
      introPlayer = null;
    }
    super.onDestroy();
  }


}
