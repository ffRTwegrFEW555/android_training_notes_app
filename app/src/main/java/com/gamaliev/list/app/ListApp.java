package com.gamaliev.list.app;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.gamaliev.list.common.FileUtils;
import com.gamaliev.list.common.database.DatabaseHelper;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ListApp extends Application {

    /* Logger */
    private static final String TAG = ListApp.class.getSimpleName();


    /*
        Init
     */

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        initDataBase();
        initFileUtils();
    }

    /**
     * Init database if not exist on first run application.
     * (The reason for the method is a bug, when after start app,
     * the database does not have time to fill when list view is refreshing)
     */
    private void initDataBase() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
    }

    /**
     * To initialize the static fields of a FileUtils class, to fix the bug.<br>
     * Otherwise ImportExportLooperThread does not have time to execute the Run method,
     * when first accessing, and get NPE.
     */
    private void initFileUtils() {
        FileUtils.getImportExportHandlerLooperThread();
    }
}
