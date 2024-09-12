package com.artifex.mupdf.viewer.gp.models;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.viewer.DocumentActivity;

public class GPAnnotationInfo {
    public String url;
    private String sourceUrl;
    public Link muPdfLink;
    private static String  baseUrlType = "baseUrlType";

    public static final int COMPONENT_TYPE_ID_VIDEO 		=1;
    public static final int COMPONENT_TYPE_ID_AUDIO 		=2;
    public static final int COMPONENT_TYPE_ID_MAP    		=3;
    public static final int COMPONENT_TYPE_ID_WEBLINK       =4;
    public static final int COMPONENT_TYPE_ID_WEB			=5;
    //public static final int COMPONENT_TYPE_ID_TOOLTIP		=6;
    //public static final int COMPONENT_TYPE_ID_SCROLLER		=7;
    private static final int COMPONENT_TYPE_ID_SLIDESHOW		=8;
    //private static final int COMPONENT_TYPE_ID_360			=9;
    public static final int COMPONENT_TYPE_ID_BOOKMARK		=9;
    public static final int COMPONENT_TYPE_ID_ANIMATION		=10;

    private static final int  MAP_TYPE_STANDART = 0;
    private static final int  MAP_TYPE_HYBRID = 1;
    private static final int  MAP_TYPE_SATELLITE = 2;

    public int componentAnnotationTypeId = -1;
    public boolean isModal = false;
    public boolean isInternal = true;
    public int webViewId = -1;
    public Location location;
    public float zoom;
    public int mapType = 0;
    public boolean isMailto = false;
    public boolean isSuitabale = true;
    public int internalLinkPageIndex = 0;

    public GPAnnotationInfo(Link muPdfLink, String mBaseUrlType) {
        this.muPdfLink = muPdfLink;
        baseUrlType = mBaseUrlType;

        if (muPdfLink.uri == null) {
            componentAnnotationTypeId = COMPONENT_TYPE_ID_BOOKMARK;
            isInternal = true;
            //internalLinkPageIndex = muPdfLink.page;
            return;
        }

        url = muPdfLink.uri;
        Uri uri = Uri.parse(url);

        if(uri.isHierarchical()) {

            String modalQueryParameterValue = uri.getQueryParameter("modal");
            if(modalQueryParameterValue!=null && !modalQueryParameterValue.isEmpty()){
                int modalValue = Integer.parseInt(modalQueryParameterValue);
                if(modalValue == 1){
                    isModal = true;
                }
                removeQueryParameter("modal", modalQueryParameterValue);
            }

            String componentTypeQueryParameterValue = uri.getQueryParameter("componentTypeID");
            if(componentTypeQueryParameterValue!=null && !componentTypeQueryParameterValue.isEmpty()){
                componentAnnotationTypeId = Integer.parseInt(componentTypeQueryParameterValue);
                removeQueryParameter("componentTypeID", componentTypeQueryParameterValue);
                isSuitabale = true;
            } else {
                if(url.contains("www") || url.contains("http://")) {
                    componentAnnotationTypeId = COMPONENT_TYPE_ID_WEBLINK;
                } else if(url.contains("@")){
                    componentAnnotationTypeId = COMPONENT_TYPE_ID_WEBLINK;
                    isMailto = true;
                } else {
                    isSuitabale = false;
                    return;
                }
            }


            if(componentAnnotationTypeId == COMPONENT_TYPE_ID_MAP){
                uri = Uri.parse(url);
                location = new Location("");

                double lat = 41.0053215;
                if(uri.getQueryParameter("lat") != null && !uri.getQueryParameter("lat").isEmpty())
                    lat = Double.valueOf(uri.getQueryParameter("lat"));

                double lon = 29.0121795;
                if(uri.getQueryParameter("lon") != null && !uri.getQueryParameter("lon").isEmpty())
                    lon = Double.valueOf(uri.getQueryParameter("lon"));
                location.setLatitude(lat);
                location.setLongitude(lon);
                Double zoomValue = Double.valueOf(uri.getQueryParameter("slon"));
                float zoomlevel = 12 - (int)(zoomValue / Double.parseDouble("0.01"));
                zoom = (zoomlevel / 2) + 12;
                if(url.contains("standard")){
                    mapType = MAP_TYPE_STANDART;
                }
                else if(url.contains("hybrid")){
                    mapType = MAP_TYPE_HYBRID;
                }
                else if(url.contains("satellite")){
                    mapType = MAP_TYPE_SATELLITE;
                }
            }
            else if(isWebAnnotation()){
                if(url.length() == 8){ // Eger servisten bos url gelmisse "ylweb://"
                    isInternal = false;
                    sourceUrl = "";
                } else {
                    try{
                        if (url.substring(0,17).equals("ylweb://localhost")){
                            isInternal = true;
                            sourceUrl = url.substring(18);
                        }
                        else{
                            String currentUrl;
                            isInternal = false;
                            switch (baseUrlType) {
                                case "2":
                                    if (url.contains("ylweb://www.galepress.com"))
                                        currentUrl = url.replace("ylweb://www.galepress.com", "https://www.galepress.com/catalog/api");
                                    else
                                        currentUrl = url.replace("ylweb", "http");
                                    break;
                                case "3":
                                    if (url.contains("ylweb://www.galepress.com"))
                                        currentUrl = url.replace("ylweb://www.galepress.com", "https://www.galepress.com/api");
                                    else
                                        currentUrl = url.replace("ylweb", "http");
                                    break;
                                default:
                                    if (url.contains("ylweb://www.galepress.com"))
                                        currentUrl = url.replace("ylweb://www.galepress.com", "https://www.galepress.com");
                                    else
                                        currentUrl = url.replace("ylweb", "http");
                                    break;
                            }
                            Log.d("GPAnnotationInfo", "a: " + currentUrl);
                            sourceUrl = currentUrl;
                        }
                    } catch (Exception e){ //Url hatalı
                        isInternal = false;
                        sourceUrl = "";
                    }

                }

            }

        } else {
            if(url.startsWith("mailto:")) {
                isMailto = true;
                componentAnnotationTypeId = COMPONENT_TYPE_ID_WEBLINK;
            } else {
                isSuitabale = false;
            }
        }

    }

    public boolean isWebAnnotation(){
        switch (componentAnnotationTypeId){
            case COMPONENT_TYPE_ID_MAP:
                return false;
            case COMPONENT_TYPE_ID_WEBLINK:
                return false;
            default:
                return true;
        }
    }

    private void removeQueryParameter(String paramName, String paramValue) {
        String temp = paramName+"="+paramValue;
        if(url.contains(temp)){
            if(url.contains("?"+temp+"&")){
                url = url.replace("?"+temp+"&", "?");
            }
            else if(url.contains("&"+temp+"&")){
                url = url.replace("&"+temp+"&", "&");
            }
            else if(url.contains("?"+temp)){
                url = url.replace("?"+temp, "");
            }
            else if(url.contains("&"+temp)){
                url = url.replace("&"+temp, "");
            }
        }
    }

    public String getSourceUrlPath(Context context){
        if(isInternal){
            try {
                return "file://" + context.getFilesDir().getAbsolutePath() +
                        "/" +
                        ((DocumentActivity) context).getContentId() +
                        "/" +
                        sourceUrl;
            }
            catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        else{
            return sourceUrl;
        }
    }

    public boolean mustHorizontalScrollLock() {
        return componentAnnotationTypeId != COMPONENT_TYPE_ID_MAP &&
                /*componentAnnotationTypeId != COMPONENT_TYPE_ID_360 &&*/
                componentAnnotationTypeId != COMPONENT_TYPE_ID_SLIDESHOW &&
                componentAnnotationTypeId != COMPONENT_TYPE_ID_VIDEO;
    }
}
