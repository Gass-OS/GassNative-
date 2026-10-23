package com.gass.gassnative;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;

public class CrashReportsActivity extends Activity {
    private final int bg = Color.rgb(13, 17, 25);
    private final int card = Color.rgb(25, 31, 43);
    private final int blue = Color.rgb(105, 174, 255);
    private LinearLayout list;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 18, 20, 16);
        root.setBackgroundColor(bg);

        TextView title = new TextView(this);
        title.setText("Crash Reports");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setPadding(0, 8, 0, 8);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Local diagnostic logs stored by GassNative");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(14);
        root.addView(subtitle);

        Button refresh = new Button(this);
        refresh.setText("Refresh reports");
        refresh.setOnClickListener(v -> refreshList());
        root.addView(refresh);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 12, 0, 12);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1f));

        setContentView(root);
        refreshList();
    }

    private void refreshList() {
        list.removeAllViews();
        File dir = new File(getFilesDir(), "crash-reports");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            TextView empty = new TextView(this);
            empty.setText("No crash reports yet.\\n\\nThis is normal if the app has not recorded an uncaught Java exception.");
            empty.setTextColor(Color.LTGRAY);
            empty.setTextSize(16);
            empty.setPadding(16, 28, 16, 28);
            empty.setBackgroundColor(card);
            list.addView(empty);
            return;
        }

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File file : files) {
            TextView row = new TextView(this);
            row.setText(file.getName() + "\\n" +
                android.text.format.DateFormat.format(
                    "yyyy-MM-dd HH:mm", file.lastModified()) +
                "\\nTap to view or copy");
            row.setTextColor(Color.WHITE);
            row.setTextSize(15);
            row.setPadding(16, 16, 16, 16);
            row.setBackgroundColor(card);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                -1, -2);
            lp.setMargins(0, 0, 0, 10);
            list.addView(row, lp);
            row.setOnClickListener(v -> showReport(file));
        }
    }

    private void showReport(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String report = new String(bytes, StandardCharsets.UTF_8);
            TextView content = new TextView(this);
            content.setText(report);
            content.setTextColor(Color.WHITE);
            content.setTextSize(12);
            content.setTextIsSelectable(true);
            content.setPadding(20, 16, 20, 16);

            ScrollView scroll = new ScrollView(this);
            scroll.addView(content);

            new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setView(scroll)
                .setPositiveButton("Copy", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("Crash report", report));
                    Toast.makeText(this, "Report copied", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not read report: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }
}
