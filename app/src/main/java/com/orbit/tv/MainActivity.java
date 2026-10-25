package com.orbit.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            web = new WebView(this);
            web.setBackgroundColor(0xFF0B132B);
            WebSettings s = web.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setMediaPlaybackRequiresUserGesture(false);
            web.setWebViewClient(new WebViewClient());
            web.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams fp) {
                    if (fileCb != null) fileCb.onReceiveValue(null);
                    fileCb = cb;
                    try {
                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("image/*");
                        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        startActivityForResult(Intent.createChooser(i, "Choose photos"), 7);
                    } catch (Throwable t) {
                        fileCb = null;
                        cb.onReceiveValue(null);
                    }
                    return true;
                }
            });
            web.addJavascriptInterface(new Bridge(), "Android");
            setContentView(web);
            web.loadUrl("file:///android_asset/index.html");
            web.requestFocus();
        } catch (Throwable t) {
            web = null;
            TextView tv = new TextView(this);
            tv.setText("Orbit TV needs Android System WebView to run.\nInstall or enable it in the Play Store, then reopen this app.\n\n" + t);
            tv.setTextSize(26);
            tv.setTextColor(0xFFFFFFFF);
            tv.setBackgroundColor(0xFF0B132B);
            tv.setPadding(90, 90, 90, 90);
            setContentView(tv);
        }
    }

    @Override
    protected void onActivityResult(int rc, int res, Intent d) {
        if (rc == 7 && fileCb != null) {
            Uri[] r = null;
            if (res == RESULT_OK && d != null) {
                if (d.getClipData() != null) {
                    int n = d.getClipData().getItemCount();
                    r = new Uri[n];
                    for (int i = 0; i < n; i++) r[i] = d.getClipData().getItemAt(i).getUri();
                } else if (d.getData() != null) {
                    r = new Uri[]{d.getData()};
                }
            }
            fileCb.onReceiveValue(r);
            fileCb = null;
        } else {
            super.onActivityResult(rc, res, d);
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (web != null) web.evaluateJavascript("window.scrollTo(0,0)", null);
    }

    @Override
    public void onBackPressed() {
        // A home screen should not close on Back; let the page close dialogs.
        if (web != null) web.evaluateJavascript("window.__back&&window.__back()", null);
    }

    private String icon(Drawable d) {
        int n = 96;
        Bitmap bm = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        d.setBounds(0, 0, n, n);
        d.draw(c);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, bo);
        return "data:image/png;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP);
    }

    class Bridge {
        @JavascriptInterface
        public String getApps() {
            try {
                PackageManager pm = getPackageManager();
                Map<String, JSONObject> out = new LinkedHashMap<>();
                String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
                for (String c : cats) {
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(c);
                    for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
                        String pkg = r.activityInfo.packageName;
                        if (pkg.equals(getPackageName()) || out.containsKey(pkg)) continue;
                        try {
                            JSONObject o = new JSONObject();
                            o.put("pkg", pkg);
                            o.put("name", r.loadLabel(pm).toString());
                            out.put(pkg, o);
                        } catch (Exception e) { }
                    }
                }
                return new JSONArray(out.values()).toString();
            } catch (Throwable t) {
                return "[]";
            }
        }

        @JavascriptInterface
        public String getIcon(String pkg) {
            try {
                PackageManager pm = getPackageManager();
                Drawable d = pm.getApplicationIcon(pkg);
                Drawable banner = null;
                try { banner = pm.getApplicationBanner(pkg); } catch (Exception e) { }
                return icon(d != null ? d : banner);
            } catch (Throwable t) {
                return "";
            }
        }

        @JavascriptInterface
        public void launch(final String pkg) {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        PackageManager pm = getPackageManager();
                        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
                        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
                        if (i != null) {
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                        }
                    } catch (Throwable t) { }
                }
            });
        }

        @JavascriptInterface
        public void assistant() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Intent[] tries = {
                        new Intent(Intent.ACTION_VOICE_COMMAND),
                        new Intent(Intent.ACTION_ASSIST),
                        getPackageManager().getLeanbackLaunchIntentForPackage("com.google.android.katniss")
                    };
                    for (Intent i : tries) {
                        if (i == null) continue;
                        try {
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            return;
                        } catch (Throwable t) { }
                    }
                }
            });
        }

        @JavascriptInterface
        public void openSettings() {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Throwable t) { }
                }
            });
        }
    }
}
