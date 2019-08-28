package com.artifex.mupdf.viewer.gp;

import android.app.Application;

import com.artifex.mupdf.viewer.DocumentActivity;

public class MuPDFLibrary extends Application {

    public final static String TAG = "MuPDFLibrary";

    public interface ApplicationInterface {
         void setMuPDFActivity(DocumentActivity documentActivity);
         void fullTextSearchForReader(String searchText, String contentId, DocumentActivity documentActivity);
        // public abstract void commitStatisticsToDB(L_Statistic statistic);
    }

    private static ApplicationInterface myApp = null;

    public static void registerApp(ApplicationInterface applicationInterface) {
        myApp = applicationInterface;
    }

    public static ApplicationInterface getAppInstance() {
        return myApp;
    }
}
