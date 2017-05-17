package com.gamaliev.notes.app;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.gamaliev.notes.common.FileUtils;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.shared_prefs.SpCommon;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class NotesApp extends Application {

    /* Logger */
    private static final String TAG = NotesApp.class.getSimpleName();

    /* ... */
    @NonNull private static Context appContext;


    /*
        Init
     */

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        appContext = getApplicationContext();

        initSharedPreferences();
        initDataBase();
        initFileUtils();
    }

    private void initSharedPreferences() {
        SpCommon.initSharedPreferences(getApplicationContext());
    }

    /**
     * Init database if not exist on first run application.
     * (The reason for the method is a bug, when after start app,
     * the database does not have time to fill when list view is refreshing)
     */
    private void initDataBase() {
        DbHelper dbHelper = DbHelper.getInstance(getApplicationContext());
        dbHelper.getWritableDatabase();
    }

    /**
     * To initialize the static fields of a FileUtils class, to fix the bug.<br>
     * Otherwise ImportExportLooperThread does not have time to execute the Run method,
     * when first accessing, and get NPE.
     */
    private void initFileUtils() {
        FileUtils.getImportExportHandlerLooperThread();
    }


    /*
        Getters
     */

    public static Context getAppContext() {
        return appContext;
    }
}
