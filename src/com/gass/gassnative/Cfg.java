package com.gass.gassnative;

import android.content.Context;
import android.content.SharedPreferences;
import android.system.Os;
import android.system.OsConstants;
import java.io.File;

public class Cfg {
  static SharedPreferences sp(Context c) { return c.getSharedPreferences("gass", 0); }
  static String current(Context c) { return sp(c).getString("profile", "default"); }
  static String[] profiles(Context c) { return sp(c).getString("profiles", "default").split(","); }

  static boolean addProfile(Context c, String n) {
    n = n.replaceAll("[^A-Za-z0-9_-]", "");
    if (n.length() == 0) return false;
    for (String p : profiles(c)) if (p.equals(n)) return false;
    sp(c).edit().putString("profiles", sp(c).getString("profiles", "default") + "," + n).apply();
    return true;
  }

  static void select(Context c, String n) { sp(c).edit().putString("profile", n).apply(); }

  static File rootDir(Context c) {
    String p = current(c);
    return p.equals("default") ? new File(c.getFilesDir(), "debian")
        : new File(c.getFilesDir(), "profiles/" + p);
  }

  static String get(Context c, String k, String d) {
    return sp(c).getString("p." + current(c) + "." + k, d);
  }

  static void put(Context c, String k, String v) {
    sp(c).edit().putString("p." + current(c) + "." + k, v).apply();
  }

  static String dns(Context c) { return sp(c).getString("dns", "1.1.1.1"); }

  static String q(String s) { return "'" + s.replace("'", "'\\''") + "'"; }

  static String binds(Context c) {
    StringBuilder b = new StringBuilder();
    for (String l : get(c, "binds", "").split("\n")) {
      l = l.trim();
      if (l.length() > 0) b.append(" -b ").append(q(l));
    }
    return b.toString();
  }

  static String env(Context c) {
    StringBuilder b = new StringBuilder();
    for (String l : get(c, "env", "").split("\n")) {
      l = l.trim();
      if (l.indexOf('=') > 0) b.append(" ").append(q(l));
    }
    return b.toString();
  }

  static void rm(File f) {
    f.setWritable(true, true);
    boolean link = false;
    try {
      link = (Os.lstat(f.getPath()).st_mode & OsConstants.S_IFMT) == OsConstants.S_IFLNK;
    } catch (Exception e) { }
    File[] k = link ? null : f.listFiles();
    if (k != null) for (File x : k) rm(x);
    f.delete();
  }
}
