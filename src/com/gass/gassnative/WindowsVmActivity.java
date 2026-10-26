package com.gass.gassnative;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class WindowsVmActivity extends Activity {
    private static final String DISK_URL =
        "https://www.mediafire.com/file/qltz6n680wwtz3r/w7.qcow2/file";

    private Uri selectedDisk;
    private TextView diskStatus;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(0, dp(5), 0, dp(5));
        return v;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(13, 20, 35));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(24));
        scroll.addView(root);

        TextView title = text("Windows 7 VM", 28, Color.WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title);

        root.addView(text(
            "Run a Windows 7 virtual machine using QEMU on ARM64.",
            15, Color.LTGRAY));

        root.addView(text("VIRTUAL MACHINE CONFIGURATION", 12,
            Color.rgb(92, 190, 255)));

        root.addView(text(
            "Machine: pc-1.3\n" +
            "CPU: qemu64 · 2 cores\n" +
            "Memory: 1024 MB\n" +
            "Disk: w7.qcow2\n" +
            "Video: std · Audio: ac97\n" +
            "Network: User mode · e1000\n" +
            "Boot: Hard disk",
            15, Color.WHITE));

        root.addView(text("DISK IMAGE", 12, Color.rgb(92, 190, 255)));

        diskStatus = text("No disk image selected.", 14, Color.LTGRAY);
        root.addView(diskStatus);

        Button select = button("Select w7.qcow2");
        select.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, 71);
        });
        root.addView(select);

        Button download = button("Get Windows 7 disk image");
        download.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DISK_URL)));
            } catch (Exception e) {
                Toast.makeText(this, "Could not open download page.",
                    Toast.LENGTH_LONG).show();
            }
        });
        root.addView(download);

        root.addView(text("QEMU ENGINE", 12, Color.rgb(92, 190, 255)));
        root.addView(text(
            "Starts Windows 7 using Debian QEMU. Keep w7.qcow2 in Download " +
            "and install the QEMU engine inside Debian if it is missing.",
            14, Color.LTGRAY));

        Button launch = button("Start Windows 7");
        launch.setOnClickListener(v -> showLaunchStatus());
        root.addView(launch);

        Button back = button("Back to GassNative");
        back.setOnClickListener(v -> finish());
        root.addView(back);

        setContentView(scroll);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 71 && resultCode == RESULT_OK && data != null) {
            selectedDisk = data.getData();
            if (selectedDisk != null) {
                try {
                    getContentResolver().takePersistableUriPermission(
                        selectedDisk, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) { }

                diskStatus.setText("Selected disk:\n" + selectedDisk);
            }
        }
    }

    private void showLaunchStatus() {
        File disk = new File(
            android.os.Environment.getExternalStorageDirectory(),
            "Download/w7.qcow2");

        if (!disk.isFile() || !disk.canRead()) {
            new AlertDialog.Builder(this)
                .setTitle("Windows disk not found")
                .setMessage("Place w7.qcow2 in your phone's Download folder, "
                    + "and allow GassNative access to shared storage.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        String command =
            "export DISPLAY=:1; unset WAYLAND_DISPLAY; "
          + "export GDK_BACKEND=x11 QT_QPA_PLATFORM=xcb HOME=/home/user USER=user; "
          + "export XDG_RUNTIME_DIR=/tmp/runtime-user; "
          + "mkdir -p \"$XDG_RUNTIME_DIR\"; chmod 700 \"$XDG_RUNTIME_DIR\"; "
          + "command -v qemu-system-i386 >/dev/null 2>&1 || "
          + "{ echo 'QEMU missing: apt install qemu-system-x86 qemu-system-gui'; exit 127; }; "
          + "for i in $(seq 1 60); do "
          + "[ -S /tmp/.X11-unix/X1 ] && break; sleep 1; done; "
          + "[ -S /tmp/.X11-unix/X1 ] || "
          + "{ echo 'X11 display :1 did not start'; exit 1; }; "
          + "[ -r /home/user/Download/w7.qcow2 ] || "
          + "{ echo 'Cannot read /home/user/Download/w7.qcow2'; exit 1; }; "
          + "exec qemu-system-i386 -machine pc -cpu qemu64 -smp 2 -m 1024 "
          + "-drive file=/home/user/Download/w7.qcow2,format=qcow2 "
          + "-vga std -device AC97 "
          + "-netdev user,id=net0 -device e1000,netdev=net0 "
          + "-usb -device usb-tablet -boot c "
          + "-display gtk,zoom-to-fit=on,window-close=on";

        Intent intent = new Intent(this, DesktopActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("x11_command", command);

        try {
            startActivity(intent);
            Toast.makeText(this,
                "Opening desktop and starting Windows 7…",
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                .setTitle("Could not start Windows")
                .setMessage(e.toString())
                .setPositiveButton("OK", null)
                .show();
        }
    }
}
