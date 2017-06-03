package com.gamaliev.notes.app;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.gamaliev.notes.common.FileUtils;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.observers.ObserverHelper;
import com.gamaliev.notes.common.shared_prefs.SpCommon;
import com.gamaliev.notes.sync.SyncUtils;

import static com.gamaliev.notes.conflict.ConflictUtils.checkConflictExistsAndShowStatusBarNotification;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class NotesApp extends Application {

    /* ... */
    @SuppressWarnings("NullableProblems")
    @NonNull private static Context sAppContext;


    /*
        Lifecycle
     */

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }


    /*
        ...
     */

    private void init() {
        sAppContext = getApplicationContext();

        initObserverHelper();
        initSharedPreferences();
        initDataBase();
        initFileUtils();
        initSync();
    }

    private void initObserverHelper() {
        ObserverHelper.notifyAllObservers(0, null);
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
        final DbHelper dbHelper = DbHelper.getInstance(getApplicationContext());
        if (dbHelper != null) {
            dbHelper.getWritableDatabase();
        }
    }

    /**
     * To initialize the static fields of a FileUtils class, to fix the bug.<br>
     * Otherwise ImportExportLooperThread does not have time to execute the Run method,
     * when first accessing, and get NPE.
     */
    private void initFileUtils() {
        FileUtils.getImportExportHandlerLooperThread();
    }

    private void initSync() {
        SyncUtils.checkPendingSyncAndStart(sAppContext);
        checkConflictExistsAndShowStatusBarNotification(sAppContext);
    }


    /*
        Getters
     */

    public static Context getAppContext() {
        return sAppContext;
    }
}
