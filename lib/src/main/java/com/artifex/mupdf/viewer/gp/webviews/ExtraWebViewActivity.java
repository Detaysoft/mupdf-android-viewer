package com.artifex.mupdf.viewer.gp.webviews;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.artifex.mupdf.viewer.gp.GPWebViewClient;
import com.artifex.mupdf.viewer.R;

/**
 * Created by adem on 15/04/14.
 */
public class ExtraWebViewActivity extends Activity {
    private WebView webView;
    public String url = "http://www.google.com";
    public boolean isMainActivitIntent = false;
    private boolean isModal = false;
    ProgressBar progressBar;
    ImageButton forwardButton, backButton, refreshButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extra_web_view_layout);
        progressBar = (ProgressBar) findViewById(R.id.extra_web_view_load_progress_bar);

        if(getIntent().getExtras().containsKey("isModal"))
            isModal = this.getIntent().getExtras().getBoolean("isModal");

        forwardButton = (ImageButton) findViewById(R.id.extra_web_view_ileri_button);
        forwardButton.setBackgroundColor(Color.GRAY);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });

        backButton = (ImageButton) findViewById(R.id.extra_web_view_geri_button);
        backButton.setBackgroundColor(Color.GRAY);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(webView.canGoBack()){
                    webView.goBack();
                }
            }
        });

        refreshButton = (ImageButton) findViewById(R.id.extra_web_view_refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });
        refreshButton.setBackgroundColor(Color.WHITE);

        if(isModal){
            forwardButton.setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);
            refreshButton.setVisibility(View.GONE);
        }

        ImageButton closeButton = (ImageButton) findViewById(R.id.extra_web_view_close_button);
        closeButton.setBackgroundColor(Color.WHITE);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                if (isMainActivitIntent)
                    overridePendingTransition(0, R.animator.right_to_left_translate);
            }
        });

        url = (String)this.getIntent().getExtras().get("url");
        if(getIntent().getExtras().containsKey("isMainActivitIntent"))
            isMainActivitIntent = this.getIntent().getExtras().getBoolean("isMainActivitIntent");


        webView = (WebView) findViewById(R.id.webview);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin,true, false);
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setGeolocationEnabled(true);

        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);

        webView.setWebViewClient(new GPWebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setIndeterminate(true);
                progressBar.getIndeterminateDrawable().setColorFilter(0xFF00D0FF, android.graphics.PorterDuff.Mode.MULTIPLY);
                ((LinearLayout) progressBar.getParent()).setVisibility(View.VISIBLE);
                refreshButton.setBackgroundColor(Color.GRAY);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                ((LinearLayout) progressBar.getParent()).setVisibility(View.GONE);
                enableDisableNavigationButtons(view);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                ((LinearLayout) progressBar.getParent()).setVisibility(View.GONE);
            }
        });
        webView.loadUrl(url);

        ((LinearLayout)progressBar.getParent()).bringToFront();
        ((LinearLayout)progressBar.getParent()).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(webView!=null) {
            //webView.pauseTimers();
            webView.destroy();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(webView != null){
            webView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        webView.onPause();

    }

    public void enableDisableNavigationButtons(WebView webView) {
        // if has previous page, enable the back button
        if(webView.canGoBack()){
            backButton.setBackgroundColor(Color.WHITE);
        }
        // if has next page, enable the next button
        if(webView.canGoForward()){
            forwardButton.setBackgroundColor(Color.WHITE);
        } else{
            forwardButton.setBackgroundColor(Color.GRAY);
        }

        refreshButton.setBackgroundColor(Color.WHITE);

    }


    /*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }
    */
}