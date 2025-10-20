package com.artifex.mupdf.viewer.gp;

import android.app.Application;

import com.artifex.mupdf.viewer.DocumentActivity;
import com.artifex.mupdf.viewer.gp.models.GPNote;

import java.util.List;


public class MuPDFLibrary extends Application {

    public final static String TAG = "MuPDFLibrary";

    public interface ApplicationInterface {
        void setMuPDFActivity(DocumentActivity documentActivity);

        void fullTextSearchForReader(String searchText, String contentId, DocumentActivity documentActivity);

        void onNoteRequested(String contentId, int pageIndex, String noteText);

        boolean isPageFavorite(String contentId, int pageIndex);

        void onFavoritePageRequested(String contentId, int pageIndex, boolean add);

        List<GPNote> getNotes(String contentId, int pageIndex);

        int getNoteCount(String contentId, int pageIndex);

        void updateNoteText(int noteId, String newText);

        void removeNote(int noteId);
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
