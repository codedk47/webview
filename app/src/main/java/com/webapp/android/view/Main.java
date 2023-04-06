package com.webapp.android.view;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;

import android.os.Build;
import android.os.Bundle;

import android.os.Message;
import android.provider.Settings;
import android.text.InputType;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.browser.customtabs.CustomTabsIntent;

import java.io.File;
import java.util.Objects;

public class Main extends Activity
{
    private WebView webview, open_window;
    private static final int REQUEST_CODE_CHROME_CUSTOM_TAB = 0;
    public static final int REQUEST_CODE_INPUT_FILE = 1;
    private ValueCallback<Uri[]> mFilePathCallback;
    @SuppressLint({"SetJavaScriptEnabled", "UnsafeDynamicallyLoadedCode"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //System.loadLibrary("libchrome");


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        webview = new WebView(this);

        webview.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webview, String url)
            {
                return false;
            }
        });
        webview.setWebChromeClient(new WebChromeClient()
        {
            //对话框去掉标题
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result)
            {
                new AlertDialog.Builder(view.getContext())
                        .setMessage(message)
                        .setPositiveButton("OK", (DialogInterface dialog, int which) -> result.confirm())
                        .setOnDismissListener((DialogInterface dialog) -> result.confirm())
                        .create()
                        .show();
                return true;
            }
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result)
            {
                new AlertDialog.Builder(view.getContext())
                        .setMessage(message)
                        .setPositiveButton("OK", (DialogInterface dialog, int which) -> result.confirm())
                        .setNegativeButton("NO", (DialogInterface dialog, int which) -> result.cancel())
                        .setOnDismissListener((DialogInterface dialog) -> result.cancel())
                        .create()
                        .show();
                return true;
            }
            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result)
            {
                final EditText input = new EditText(view.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(defaultValue);
                new AlertDialog.Builder(view.getContext())
                        .setMessage(message)
                        .setView(input)
                        .setPositiveButton("OK", (DialogInterface dialog, int which) -> result.confirm(input.getText().toString()))
                        .setNegativeButton("NO", (DialogInterface dialog, int which) -> result.cancel())
                        .setOnDismissListener((DialogInterface dialog) -> result.cancel())
                        .create()
                        .show();
                return true;
            }
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

                ((FrameLayout)getWindow().getDecorView()).addView(fullscreen,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                fullscreen.setVisibility(View.VISIBLE);
            }
            //打开新窗口
            @Override
            public boolean onCreateWindow(WebView webview, boolean isDialog, boolean isUserGesture, Message resultMsg)
            {
                WebView target = new WebView(Main.this);
                //webview.addView(window);
                WebView.WebViewTransport transport = (WebView.WebViewTransport)resultMsg.obj;
                transport.setWebView(target);
                resultMsg.sendToTarget();
                target.setWebViewClient(new WebViewClient()
                {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView webview, String url) {
                        open(url, webview);
                        return true;
                    }
                });
                return true;
            }
            //选择文件
            @Override
            public boolean onShowFileChooser(WebView webview, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams)
            {
                if(mFilePathCallback != null)
                {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;
                startActivityForResult(fileChooserParams.createIntent(), REQUEST_CODE_INPUT_FILE);
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
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_CODE_CHROME_CUSTOM_TAB && open_window != null)
        {
            open_window.destroy();
            open_window = null;
            return;
        }
        if(requestCode == REQUEST_CODE_INPUT_FILE && mFilePathCallback != null)
        {
            Uri[] results = null;
            if(resultCode == Activity.RESULT_OK && data != null)
            {
                String dataString = data.getDataString();
                if (dataString != null)
                {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void open(String url, WebView webview)
    {
        try
        {
            Uri uri = Uri.parse(url);
            if (uri.getScheme().toLowerCase().startsWith("http"))
            {
                CustomTabsIntent tab = new CustomTabsIntent.Builder().build();
                tab.intent.setPackage("com.android.chrome");
                tab.intent.setData(uri);
                open_window = webview;
                startActivityForResult(tab.intent, REQUEST_CODE_CHROME_CUSTOM_TAB);
            }
            else
            {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        }
        catch (Exception e)
        {
            new AlertDialog.Builder(Main.this).setMessage(e.getMessage()).show();
        }
    }
    @SuppressLint("HardwareIds")
    private String did()
    {
        String did;
        try
        {
            did = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        catch (Exception e)
        {
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
    private String cid()
    {
        try
        {
            return Objects.requireNonNull(ChannelReader.get(new File(this.getApplicationInfo().sourceDir))).getChannel();
        }
        catch (Exception e)
        {
            return "";
        }
    }
}
