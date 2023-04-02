package com.webapp.android.view;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.app.AlertDialog;
import android.content.Intent;

import android.net.Uri;

import android.os.Build;
import android.os.Bundle;

import android.os.Message;
import android.provider.Settings;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.File;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Activity {
    private WebView webview;
    public static final int INPUT_FILE_REQUEST_CODE = 1;
    private ValueCallback<Uri[]> mFilePathCallback;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        webview = new WebView(this);
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webview, String url) {
                Matcher urls = Pattern.compile("^\\w+:\\/\\/").matcher(url);
                if (urls.find() && !urls.group(0).toLowerCase().startsWith("http")) {
                    open(url);
                    return true;
                }
                return false;
            }
        });

        webview.setWebChromeClient(new WebChromeClient() {
            //视频全屏
            View fullscreen = null;
            @Override
            public void onHideCustomView()
            {
                fullscreen.setVisibility(View.GONE);
                webview.setVisibility(View.VISIBLE);
            }
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback)
            {
                webview.setVisibility(View.GONE);
                if(fullscreen != null)
                {
                    ((FrameLayout)getWindow().getDecorView()).removeView(fullscreen);
                }
                fullscreen = view;
                ((FrameLayout)getWindow().getDecorView()).addView(fullscreen, new FrameLayout.LayoutParams(-1, -1));
                fullscreen.setVisibility(View.VISIBLE);
            }
            //打开新窗口
            @Override
            public boolean onCreateWindow(WebView webview, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView window = new WebView(Main.this);
                webview.addView(window);
                WebView.WebViewTransport transport = (WebView.WebViewTransport)resultMsg.obj;
                transport.setWebView(window);
                resultMsg.sendToTarget();
                window.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView webview, String url) {
                        open(url);
                        return true;
                    }
                });
                return true;
            }
            //选择文件
            @Override
            public boolean onShowFileChooser(WebView webview, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if(mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
                return true;
            }
        });
        WebSettings config = webview.getSettings();
        config.setAllowContentAccess(true);
        config.setAllowFileAccess(true);
        config.setAllowFileAccessFromFileURLs(true);
        config.setAllowUniversalAccessFromFileURLs(true);
        config.setCacheMode(WebSettings.LOAD_DEFAULT);
        config.setDatabaseEnabled(true);
        config.setDomStorageEnabled(true);
        config.setJavaScriptCanOpenWindowsAutomatically(true);
        config.setJavaScriptEnabled(true);
        config.setLoadsImagesAutomatically(true);
        config.setMediaPlaybackRequiresUserGesture(true);
        config.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        config.setSupportMultipleWindows(true);
        config.setTextZoom(100);
        config.setSupportZoom(false);
        config.setLoadWithOverviewMode(true);
        config.setUserAgentString(config.getUserAgentString() +
                "; DID/".concat(this.did()) +
                "; CID/".concat(this.cid()));

//        deleteDatabase("WebView.db");
//        deleteDatabase("WebViewCache.db");
//        webview.clearCache(true);
//        webview.clearFormData();
//        getCacheDir().delete();

        webview.loadUrl("file:///android_asset/index.html");
        setContentView(webview);
    }
    private void open(String url)
    {
        try{
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            new AlertDialog.Builder(Main.this).setMessage(e.getMessage()).show();
        }
    }
    @SuppressLint("HardwareIds")
    private String did() {
        String did;
        try {
            did = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception ex) {
            did = Build.BOARD+
                    Build.BRAND +
                    Build.DEVICE +
                    Build.HARDWARE +
                    Build.ID +
                    Build.MODEL +
                    Build.PRODUCT +
                    Build.SERIAL;
        }
        long hash = 5381;
        int len = did.length();
        if (did.length() > 0)
        {
            for (int i = 0; i < len; ++i)
            {
                hash += (hash << 5) + did.charAt(i);
            }
        }
        did = "000000000000000" + Long.toHexString(hash);
        return did.substring(did.length() - 16);
    }
    private String cid() {
        try {
            return ChannelReader.get(new File(this.getApplicationInfo().sourceDir)).getChannel();
        } catch (Exception ex) {
            return "";
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
            if(data == null) {
                // If there is not data, then we may have taken a photo
//                if(mCameraPhotoPath != null) {
//                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
//                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }
}
