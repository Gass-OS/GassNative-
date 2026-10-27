package com.gass.gassnative;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WalkieCloudActivity extends Activity {
    private WebView webView;
    private String channelName;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        GassUi.setup(this);

        channelName = getIntent().getStringExtra("channel_name");
        if (channelName == null || channelName.trim().isEmpty()) {
            channelName = "GLOBAL";
        }

        int pad = GassUi.dp(this, 12);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, 0);
        root.setBackgroundColor(Color.rgb(10, 16, 29));

        root.addView(GassUi.header(this, "Walkie Cloud", GassUi.MIC));

        TextView info = new TextView(this);
        info.setText("Selected channel: " + channelName
            + "\nThe channel name will be filled in if the website allows it. Review the page and press Join to connect.");
        info.setTextSize(13);
        info.setTextColor(Color.rgb(190, 202, 222));
        info.setPadding(0, pad, 0, pad);
        root.addView(info);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (!"https://walkie.cloud/".equals(url)
                        && !"https://walkie.cloud".equals(url)) return;

                // Fill the first text input only; user remains in control of Join.
                String safe = channelName.replace("\\", "\\\\").replace("'", "\\'");
                String js = "(function(){"
                    + "var i=document.querySelector('input');"
                    + "if(i){i.value='" + safe + "';"
                    + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                    + "i.dispatchEvent(new Event('change',{bubbles:true}));}"
                    + "})();";
                view.evaluateJavascript(js, null);
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1f));
        GassUi.setContent(this, root);
        webView.loadUrl("https://walkie.cloud/");
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
