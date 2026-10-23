package app.droidnative.display;

import android.util.Log;
import android.view.Surface;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class Compositor {
    private static final String TAG = "GassNativeCompositor";

    static {
        System.loadLibrary("droidnative");
    }

    public interface Listener {
        void onToplevelNew(int id, String appId, String title, int pid);
        void onToplevelChanged(int id, String appId, String title);
        void onToplevelClosed(int id);
        void onTextInput(int id, boolean active);
        void onPointerLock(int id, boolean locked);
    }

    private volatile Listener listener;
    private volatile long handle;
    private Thread thread;

    public void setListener(Listener value) {
        listener = value;
    }

    public boolean start(
            final String socketPath,
            final String keymapPath,
            final String tmpDir,
            final int scale,
            final int width,
            final int height,
            final int physicalWidthMm,
            final int physicalHeightMm,
            final int refreshMhz,
            final int touchMode) {

        if (thread != null) return handle != 0;
        final CountDownLatch ready = new CountDownLatch(1);

        thread = new Thread(() -> {
            long h = nativeCreate(socketPath, keymapPath, tmpDir, scale,
                    width, height, physicalWidthMm, physicalHeightMm,
                    refreshMhz, touchMode);
            handle = h;
            ready.countDown();

            if (h == 0) {
                Log.e(TAG, "nativeCreate failed");
                return;
            }

            try {
                int result = nativeRun(h);
                Log.i(TAG, "nativeRun ended: " + result);
            } catch (Throwable t) {
                Log.e(TAG, "Compositor thread failed", t);
            } finally {
                handle = 0;
                nativeDestroy(h);
            }
        }, "gassnative-compositor");

        thread.start();
        try {
            if (!ready.await(10, TimeUnit.SECONDS)) {
                Log.e(TAG, "Timed out waiting for compositor startup");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return handle != 0;
    }

    public void stop() {
        long h = handle;
        if (h != 0) nativeStop(h);
        Thread t = thread;
        if (t != null && t != Thread.currentThread()) {
            try { t.join(5000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        thread = null;
    }

    public boolean isRunning() { return handle != 0; }

    public void attach(int id, Surface surface, int width, int height) {
        long h = handle;
        if (h != 0) nativeAttach(h, id, surface, width, height);
    }

    public void detach(int id) { withHandle(h -> nativeDetach(h, id)); }
    public void resize(int id, int w, int hgt) {
        withHandle(h -> nativeResize(h, id, w, hgt));
    }
    public void focus(int id, boolean focused) {
        withHandle(h -> nativeFocus(h, id, focused));
    }
    public void close(int id) { withHandle(h -> nativeClose(h, id)); }
    public void kill(int id) { withHandle(h -> nativeKill(h, id)); }
    public void setKeymap(String path) {
        withHandle(h -> nativeSetKeymap(h, path));
    }

    public void touch(int id, int action, int slot, float x, float y, int time) {
        withHandle(h -> nativeTouch(h, id, action, slot, x, y, time));
    }
    public void pointerMotion(int id, float x, float y, int time) {
        withHandle(h -> nativePointerMotion(h, id, x, y, time));
    }
    public void pointerButton(int id, int button, boolean pressed, int time) {
        withHandle(h -> nativePointerButton(h, id, button, pressed, time));
    }
    public void pointerAxis(int id, float dx, float dy, int time) {
        withHandle(h -> nativePointerAxis(h, id, dx, dy, time));
    }
    public void pointerRelative(int id, float dx, float dy, int time) {
        withHandle(h -> nativePointerRelative(h, id, dx, dy, time));
    }
    public void pointerWheel(int id, int x, int y, int time) {
        withHandle(h -> nativePointerWheel(h, id, x, y, time));
    }
    public void key(int id, int key, boolean pressed, int depressed, int locked, int time) {
        withHandle(h -> nativeKey(h, id, key, pressed, depressed, locked, time));
    }
    public void text(int id, String value, boolean preedit) {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        withHandle(h -> nativeText(h, id, bytes, preedit));
    }

    public long[] stats(int id) {
        long h = handle;
        if (h == 0) return null;
        long[] out = new long[11];
        return nativeStats(h, id, out) ? out : null;
    }

    private interface HandleCall { void run(long h); }
    private void withHandle(HandleCall call) {
        long h = handle;
        if (h != 0) call.run(h);
    }

    // Called by JNI. Keep these names and signatures unchanged.
    @SuppressWarnings("unused")
    private void onToplevelNew(int id, String appId, String title, int pid) {
        Listener l = listener;
        if (l != null) l.onToplevelNew(id, appId, title, pid);
    }

    @SuppressWarnings("unused")
    private void onToplevelChanged(int id, String appId, String title) {
        Listener l = listener;
        if (l != null) l.onToplevelChanged(id, appId, title);
    }

    @SuppressWarnings("unused")
    private void onToplevelClosed(int id) {
        Listener l = listener;
        if (l != null) l.onToplevelClosed(id);
    }

    @SuppressWarnings("unused")
    private void onTextInput(int id, boolean active) {
        Listener l = listener;
        if (l != null) l.onTextInput(id, active);
    }

    @SuppressWarnings("unused")
    private void onPointerLock(int id, boolean locked) {
        Listener l = listener;
        if (l != null) l.onPointerLock(id, locked);
    }

    private native long nativeCreate(String socket, String keymap, String tmp,
            int scale, int width, int height, int physWidth, int physHeight,
            int refreshMhz, int touchMode);
    private native int nativeRun(long h);
    private native void nativeStop(long h);
    private native void nativeDestroy(long h);
    private native void nativeAttach(long h, int id, Surface surface, int w, int hgt);
    private native void nativeDetach(long h, int id);
    private native void nativeResize(long h, int id, int w, int hgt);
    private native void nativeFocus(long h, int id, boolean focused);
    private native void nativeClose(long h, int id);
    private native void nativeKill(long h, int id);
    private native void nativeSetKeymap(long h, String path);
    private native void nativeTouch(long h, int id, int action, int slot,
            float x, float y, int time);
    private native void nativePointerMotion(long h, int id, float x, float y, int time);
    private native void nativePointerButton(long h, int id, int button, boolean pressed, int time);
    private native void nativePointerAxis(long h, int id, float dx, float dy, int time);
    private native void nativePointerRelative(long h, int id, float dx, float dy, int time);
    private native void nativePointerWheel(long h, int id, int x, int y, int time);
    private native void nativeKey(long h, int id, int key, boolean pressed,
            int depressed, int locked, int time);
    private native void nativeText(long h, int id, byte[] utf8, boolean preedit);
    private native boolean nativeStats(long h, int id, long[] out);
}
