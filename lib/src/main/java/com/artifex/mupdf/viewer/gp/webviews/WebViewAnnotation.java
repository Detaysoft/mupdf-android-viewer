package com.artifex.mupdf.viewer.gp.webviews;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.artifex.mupdf.viewer.gp.models.GPAnnotationInfo;
import com.artifex.mupdf.viewer.PageView;
import com.artifex.mupdf.viewer.ReaderView;
import com.artifex.mupdf.viewer.gp.CustomPulseProgress;

/**
 * Created by adem on 08/08/14.
 */
@SuppressLint("ViewConstructor")
public class WebViewAnnotation extends WebView {
    private final String TAG = "MyWebView";

    public float x1 , x2, y1 , y2;
    public float left, top ;
    public ReaderView readerView;
    public GPAnnotationInfo linkInfoExternal;
    private CustomPulseProgress loading;
    private Context context;

    /*
    * Video ve ses iceriklerinde(ozellikle autoplay olanlarda) sayfa yuklenmesi bitmeden diger sayfaya gecilirse
    * javascript ile video ve ses durdurulamiyor.
    * Bu parametre ile MuPDFPageView classinda stopAllWebAnnotationsMedia metodunda kontrol edilerek devam eden loading iptal edilecek.
    */
    public boolean isLoadingFinished = false;

    private class MyWebChromeClient extends WebChromeClient {
        /*
        * Bu iki metod override edildigi zaman videolar gorunmuyor
        * */
        /*@Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
        }
        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
        }*/
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.e(TAG, "shouldOverrideUrlLoading: " + url);
            // don't override URL so that stuff within iframe can work properly
            // view.loadUrl(url);
            if(isLoadingFinished) {
                Log.e(TAG, "redirect");
                Intent intent = new Intent(context, ExtraWebViewActivity.class);
                intent.putExtra("url", url);
                intent.putExtra("isMainActivitIntent", false);
                context.startActivity(intent);
                if(linkInfoExternal.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_AUDIO
                        || linkInfoExternal.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_VIDEO
                        || linkInfoExternal.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_WEB
                        || linkInfoExternal.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_ANIMATION)
                    isLoadingFinished = false;
                return true;
            }
            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            isLoadingFinished = true;
            Log.e(TAG, "finish");
            if(loading != null) {
                loading.setVisibility(GONE);
            }
            Log.e(TAG,""+url);
            view.setVisibility(VISIBLE);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            isLoadingFinished = false;
            Log.e(TAG, "start");
            if(loading != null) {
                loading.setVisibility(VISIBLE);
            }
            view.setVisibility(GONE);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            //view.loadUrl("file:///android_asset/annotation_not_loaded.html");
            view.loadUrl("about:blank");
            view.setVisibility(GONE);
            super.onReceivedError(view, errorCode, description, failingUrl);
            if(loading != null) {
                loading.setVisibility(GONE);
            }

            ((PageView)view.getParent()).removeView(view);

        }
    }

    public boolean isHorizontalScrolling, isDummyAction;
    private MotionEvent previousMotionEvent;
    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public WebViewAnnotation(Context context, GPAnnotationInfo lie, CustomPulseProgress loading) {
        super(context);

        Log.e("testtest", ""+lie.url);
        this.loading = loading;
        this.linkInfoExternal = lie;
        this.context = context;
        this.setWebChromeClient(new MyWebChromeClient());
        this.setWebViewClient(new MyWebViewClient());

        if(lie.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_VIDEO){
            this.setLayerType(WebView.LAYER_TYPE_HARDWARE,null);
        }
        else if(lie.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_WEB){
            this.setLayerType(WebView.LAYER_TYPE_HARDWARE,null);
        }
        else{
            this.setLayerType(WebView.LAYER_TYPE_SOFTWARE,null);
        }

        WebSettings s = getSettings();
        s.setBuiltInZoomControls(true);
        s.setPluginState(WebSettings.PluginState.ON);
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSaveFormData(true);
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); //false olarak set edilmedigi autoplay calismiyor.
        s.setAllowFileAccess(true);
        //s.setAppCacheEnabled(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setSupportZoom(true);


        this.setHorizontalScrollBarEnabled(false);
        this.setVerticalScrollBarEnabled(false);
        this.setBackgroundColor(Color.TRANSPARENT);
        setInitialScale(1);
        final WebViewAnnotation web = this;

        if(linkInfoExternal.mustHorizontalScrollLock()){
            this.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float dx,dy;
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        // Action DOWN
                        web.x1 = event.getX();
                        web.y1 = event.getY();
                        web.setPreviousMotionEvent(event);
                        web.isHorizontalScrolling = false;
                        return !web.isDummyAction;

                    }
                    else if(event.getAction() == MotionEvent.ACTION_MOVE){
                        // Action MOVE
                        web.x2 = event.getX();
                        web.y2 = event.getY();
                        dx = web.x2 - web.x1;
                        dy = web.y2 - web.y1;
                        if(Math.abs(dx) > 10 || Math.abs(dy) > 10){
                            if(!(Math.abs(dx) >  Math.abs(dy))) {
                                // vertical
                                web.isHorizontalScrolling = false;
                                return false;
                            }else {
                                // horizontal
                                web.isHorizontalScrolling = true;
                                if(web.getPreviousMotionEvent()!=null && web.getPreviousMotionEvent().getAction() != MotionEvent.ACTION_MOVE) {
                                    MotionEvent previousEvent = web.getPreviousMotionEvent();
                                    web.setPreviousMotionEvent(null);
                                    previousEvent.setLocation(previousEvent.getX() + left, previousEvent.getY() + top);
                                    readerView.onTouchEvent(previousEvent);
                                }
                                event.setLocation(event.getX() + left, event.getY() + top); // Webview size is not equal to page size. Optimize the location for page.
                                readerView.onTouchEvent(event);
                                return true;
                            }
                        }
                    }
                    else if(event.getAction() == MotionEvent.ACTION_UP){
                        // Action UP
                        if(web.isHorizontalScrolling){
                            web.isHorizontalScrolling = false;
                            event.setLocation(event.getX() + left, event.getY() + top); // Webview size is not equal to page size. Optimize the location for page.
                            readerView.onTouchEvent(event);
                            return true;
                        }
                        else{
                            if(web.getPreviousMotionEvent()!=null) {
                                MotionEvent previousEvent = web.getPreviousMotionEvent();
                                web.setPreviousMotionEvent(null);
                                web.isDummyAction = true;
                                web.onTouchEvent(previousEvent);
                            }
                            return false;
                        }
                    }
                    return false;
                }
            });
        }
    }

    public MotionEvent getPreviousMotionEvent() {
        return previousMotionEvent;
    }

    public void setPreviousMotionEvent(MotionEvent event) {
        if(event == null)
            this.previousMotionEvent = null;
        else
            this.previousMotionEvent = MotionEvent.obtain(event);
    }

    /*
    public boolean isLoadingFinished() {
        return isLoadingFinished;
    }

    public void setLoadingFinished(boolean loadingFinished) {
        isLoadingFinished = loadingFinished;
    }

     */
    public void loadSource(String url) {
        if (url == null) return;

        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            String videoId = extractVideoId(url);
            if (videoId != null) {
                String html = getHtmlWrapperForYouTube(videoId);
                // Use a valid HTTPS origin to satisfy YouTube's security checks
                this.loadDataWithBaseURL("https://www.galepress.com", html, "text/html", "UTF-8", null);
                return;
            }
        } else if (url.startsWith("file://") && (url.endsWith(".mp4") || url.endsWith(".mov") || url.endsWith(".m4v"))) {
             String html = getHtmlWrapperForLocalVideo(url);
             this.loadDataWithBaseURL("https://www.galepress.com", html, "text/html", "UTF-8", null);
             return;
        }

        // Fallback for other URLs
        this.loadUrl(url);
    }

    private String extractVideoId(String url) {
        String videoId = null;
        if (url != null && url.trim().length() > 0 && url.toString().startsWith("http")) {
            String expression = "^.*((youtu.be" + "\\/)" + "|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(expression, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(url);
            if (matcher.matches()) {
                String groupIndex1 = matcher.group(7);
                if (groupIndex1 != null && groupIndex1.length() == 11)
                    videoId = groupIndex1;
            }
        }
        return videoId;
    }

    private String getHtmlWrapperForYouTube(String videoId) {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0'>" +
                "<style>" +
                "html,body{margin:0;padding:0;background:#000;height:100%;}" +
                ".container{position:relative;width:100%;height:100%;display:flex;align-items:center;justify-content:center;}" +
                ".video{position:relative;width:100%;height:100%;}" +
                "iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0;}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='video'>" +
                "<iframe src='https://www.youtube.com/embed/" + videoId + "?playsinline=1&rel=0&enablejsapi=1&origin=https://www.galepress.com' " +
                "allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture' " +
                "allowfullscreen></iframe>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
        return html;
    }

    private String getHtmlWrapperForLocalVideo(String url) {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0'>" +
                "<style>" +
                "html,body{margin:0;padding:0;background:#000;height:100%;}" +
                ".container{display:flex;align-items:center;justify-content:center;height:100%;}" +
                "video{width:100%;height:100%;max-width:100%;max-height:100%;object-fit:contain;background:#000;}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<video controls playsinline preload='metadata'>" +
                "<source src='" + url + "' type='video/mp4'>" +
                "</video>" +
                "</div>" +
                "</body>" +
                "</html>";
        return html;
    }
}