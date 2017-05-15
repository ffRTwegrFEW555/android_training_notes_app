package com.gamaliev.notes.common;

import android.Manifest;
import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.db.DbQueryBuilder;
import com.gamaliev.notes.list.db.ListDbHelper;
import com.gamaliev.notes.model.ListEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.gamaliev.notes.common.CommonUtils.checkAndRequestPermissions;
import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.notes.list.ListActivity.RESULT_CODE_EXTRA_EXPORTED;
import static com.gamaliev.notes.list.ListActivity.RESULT_CODE_EXTRA_IMPORTED;
import static com.gamaliev.notes.model.ListEntry.convertJsonToListEntry;
import static com.gamaliev.notes.model.ListEntry.getJsonObject;

/**
 * Class, for working with files, for exporting/importing entries from/to database.<br>
 * Supported asynchronous mode with message queue.<br>
 * If the operation time is longer than the specified time, then progress notification enable.<br>
 * <br>
 * The following file template is used:<br><br>
 *     {"title":"...",<br>
 *     "description":"...",<br>
 *     "color":"#...",<br>
 *     "created":"2017-04-24T12:00:00+05:00",<br>
 *     "edited":"...",<br>
 *     "viewed":"..."}<br>
 *     <br>
 *     Date format: YYYY-MM-DDThh:mm:ss±hh:mm (https://ru.wikipedia.org/wiki/ISO_8601)
 *
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class FileUtils {

    /* Logger */
    private static final String TAG = FileUtils.class.getSimpleName();

    /* ... */
    public static final int REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    public static final int REQUEST_CODE_PERMISSIONS_READ_EXTERNAL_STORAGE = 2;
    private static final String FILE_NAME = "itemlist.ili";

    /* Handler, Looper */
    private static final CommonUtils.LooperHandlerThread IMPORT_EXPORT_HANDLER_LOOPER_THREAD;


    /*
        Init
     */

    static {
        IMPORT_EXPORT_HANDLER_LOOPER_THREAD = new CommonUtils.LooperHandlerThread();
        IMPORT_EXPORT_HANDLER_LOOPER_THREAD.start();
    }

    private FileUtils() {}


    /*
        EXPORT
     */

    /**
     * Checking permission for export. If permission is granted, starting export,
     * otherwise requesting permission from user.
     *
     * @param activity              Activity.
     * @param onCompleteListener    Listener, who will be notified of the result.
     */
    public static void exportEntriesAsyncWithCheckPermission(
            @NonNull final Activity activity,
            @NonNull final OnCompleteListener onCompleteListener) {

        // Check writable. If denied, make request, then break.
        if (!checkAndRequestPermissions(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE)) {

            return;
        }

        // If ok.
        exportEntriesAsync(activity, onCompleteListener);
    }


    /**
     * Starting the export in asynchronous mode,
     * using a dedicated shared thread, with looper and message queue.<br>
     *
     * @param activity              Activity.
     * @param onCompleteListener    Listener, who will be notified of the result.
     */
    public static void exportEntriesAsync(
            @NonNull final Activity activity,
            @NonNull final OnCompleteListener onCompleteListener) {

        IMPORT_EXPORT_HANDLER_LOOPER_THREAD.getHandler().post(new Runnable() {
            @Override
            public void run() {
                exportEntries(activity, onCompleteListener);
            }
        });
    }

    /**
     * Export entries from database to file with Json-format.<br>
     * If the operation time is longer than the specified time, then progress notification enable.
     *
     * @param activity              Activity.
     * @param onCompleteListener    Listener, who will be notified of the result.
     */
    public static void exportEntries(
            @NonNull final Activity activity,
            @NonNull final OnCompleteListener onCompleteListener) {

        //
        showToastRunOnUiThread(
                activity,
                activity.getString(R.string.file_utils_export_notification_start),
                Toast.LENGTH_SHORT);

        // Create json array;
        final JSONArray jsonArray = new JSONArray();

        // Create progress notification.
        final ProgressNotificationHelper notification =
                new ProgressNotificationHelper(
                        activity,
                        activity.getString(R.string.file_utils_export_notification_panel_title),
                        activity.getString(R.string.file_utils_export_notification_panel_text),
                        activity.getString(R.string.file_utils_export_notification_panel_finish));

        // Timer for notification enable
        notification.startTimerToEnableNotification(
                activity.getResources().getInteger(
                        R.integer.activity_list_notification_panel_import_export_timer_enable));

        // Retrieve data from database in Json-format.
        final String result = getEntriesFromDatabase(
                activity,
                jsonArray,
                notification);

        // Save result Json-string to file.
        saveStringToFile(
                activity,
                jsonArray,
                result,
                onCompleteListener,
                notification);

    }

    /**
     * Get all entries from database.
     *
     * @param activity              Activity.
     * @param jsonArray             JSONArray-object to fill.
     * @param notification          Notification helper.
     *
     * @return String in needed Json-format, containing all entries from database.
     */
    @Nullable
    private static String getEntriesFromDatabase(
            @NonNull final Activity activity,
            @NonNull final JSONArray jsonArray,
            @NonNull final ProgressNotificationHelper notification) {

        // Get cursor.
        final Cursor cursor = ListDbHelper.getEntries(activity, new DbQueryBuilder());

        // Create json object;
        JSONObject jsonObject;

        // Number of entries;
        final int size = cursor.getCount();
        int percent = 0;
        int counter = 0;

        // Seek.
        while (true) {

            // Catch is used, because during the export there can be changes.
            try {
                if (!cursor.moveToNext()) {
                    // If end.
                    break;
                }

                // Get next Json-object.
                jsonObject = getJsonObject(activity, cursor);

            } catch (IllegalStateException e) {
                Log.e(TAG, e.toString());
                continue;
            }

            // Add to json array
            jsonArray.put(jsonObject);

            // Update progress. Without flooding. 0-100%
            final int percentNew = counter++ * 100 / size;
            if (percentNew > percent) {
                //
                percent = percentNew;
                //
                notification.setProgress(100, percentNew);
            }
        }

        // Finish him.
        cursor.close();

        //
        return jsonArray.toString();
    }

    /**
     * Saving given string to file, and notifying.
     */
    private static void saveStringToFile(
            @NonNull final Activity activity,
            @NonNull final JSONArray jsonArray,
            @NonNull final String result,
            @NonNull final OnCompleteListener onCompleteListener,
            @NonNull final ProgressNotificationHelper notification) {

        try {
            // Create file
            final File file = new File(Environment.getExternalStorageDirectory(), FILE_NAME);

            // Write to file
            final FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(result.getBytes());
            outputStream.close();

            // Notification success.
            showToastRunOnUiThread(
                    activity,
                    activity.getString(R.string.file_utils_export_toast_message_success)
                            + " : " + file.getPath()
                            + " (" + jsonArray.length() + ")",
                    Toast.LENGTH_LONG);

            // Notify activity.
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onCompleteListener.onComplete(RESULT_CODE_EXTRA_EXPORTED);
                }
            });

            // Notification panel success.
            notification.endProgress();

        } catch (IOException e) {
            Log.e(TAG, e.toString());
            // Notification failed.
            showToastRunOnUiThread(
                    activity,
                    activity.getString(R.string.file_utils_export_toast_message_failed),
                    Toast.LENGTH_SHORT);
        }
    }


    /*
        IMPORT
     */

    /**
     * Starting the import in asynchronous mode,
     * using a dedicated shared thread, with looper and message queue.<br>
     *
     * @param activity              Activity.
     * @param selectedFile          Selected file.
     * @param onCompleteListener    Listener, who will be notified of the result.
     */
    public static void importEntriesAsync(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile,
            @NonNull final OnCompleteListener onCompleteListener) {

        //
        IMPORT_EXPORT_HANDLER_LOOPER_THREAD.getHandler().post(new Runnable() {
            @Override
            public void run() {
                importEntries(activity, selectedFile, onCompleteListener);
            }
        });
    }

    /**
     * Import entries from file to database.<br>
     * If the operation time is longer than the specified time, then progress notification enable.
     *
     * @param activity              Activity.
     * @param selectedFile          Selected file.
     * @param onCompleteListener    Listener, who will be notified of the result.
     */
    public static void importEntries(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile,
            @NonNull final OnCompleteListener onCompleteListener){

        //
        showToastRunOnUiThread(
                activity,
                activity.getString(R.string.file_utils_import_notification_start),
                Toast.LENGTH_SHORT);

        // Create progress notification.
        final ProgressNotificationHelper notification =
                new ProgressNotificationHelper(
                        activity,
                        activity.getString(R.string.file_utils_import_notification_panel_title),
                        activity.getString(R.string.file_utils_import_notification_panel_text),
                        activity.getString(R.string.file_utils_import_notification_panel_finish));

        // Timer for notification enable
        notification.startTimerToEnableNotification(
                activity.getResources().getInteger(
                        R.integer.activity_list_notification_panel_import_export_timer_enable));

        // Get Json-string from file.
        final String inputJson = getStringFromFile(activity, selectedFile);

        // Parse and save to database.
        parseAndSaveToDatabase(
                activity,
                inputJson,
                onCompleteListener,
                notification);
    }

    /**
     * @param activity      Activity.
     * @param selectedFile  File with string.
     */
    @Nullable
    private static String getStringFromFile(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile) {

        //
        String inputJson = null;

        try {
            //
            final StringBuilder sb = new StringBuilder();

            // Read from file
            final BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    activity.getContentResolver().openInputStream(selectedFile)));

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            //
            inputJson = sb.toString();

        } catch (IOException e) {
            Log.e(TAG, e.toString());
            // Notification failed.
            showToastRunOnUiThread(
                    activity,
                    activity.getString(R.string.file_utils_import_toast_message_failed),
                    Toast.LENGTH_SHORT);
        }

        return inputJson;
    }

    /**
     * Parse given string in Json-format, and save to database.
     *
     * @param activity              Activity.
     * @param inputJson             String in Json-format.
     * @param onCompleteListener    Listener, who will be notified of the result.
     */
    private static void parseAndSaveToDatabase(
            @NonNull final Activity activity,
            @NonNull final String inputJson,
            @NonNull final OnCompleteListener onCompleteListener,
            @NonNull final ProgressNotificationHelper notification) {

        // Get database and start transaction.
        try {
            final SQLiteDatabase db = DbHelper
                    .getInstance(activity.getApplicationContext())
                    .getWritableDatabase();

            // Init
            final JSONArray jsonArray = new JSONArray(inputJson);

            // Number of entries;
            final int size = jsonArray.length();
            int percent = 0;

            // Begin transaction
            db.beginTransaction();

            try {
                // Seek.
                for (int i = 0; i < size; i++) {

                    // Get next Json-object.
                    final JSONObject jsonObject = jsonArray.getJSONObject(i);

                    // Convert to entry.
                    final ListEntry entry = convertJsonToListEntry(activity, jsonObject);

                    // Insert
                    ListDbHelper.insertEntry(activity, entry, db);

                    // Update progress. Without flooding. 0-100%
                    final int percentNew = i * 100 / size;
                    if (percentNew > percent) {
                        //
                        percent = percentNew;
                        //
                        notification.setProgress(100, percentNew);
                    }

                    //
                    if (i % 100 == 0) {
                        db.yieldIfContendedSafely();
                    }
                }

                // If ok.
                db.setTransactionSuccessful();

            } finally {
                db.endTransaction();
            }

            //
            makeSuccessImportOperations(
                    activity,
                    jsonArray,
                    onCompleteListener,
                    notification);

        } catch (JSONException | SQLiteException e) {
            Log.e(TAG, e.toString());
            // Notification failed.
            showToastRunOnUiThread(
                    activity,
                    activity.getString(R.string.file_utils_import_toast_message_failed),
                    Toast.LENGTH_SHORT);
        }
    }

    private static void makeSuccessImportOperations(
            @NonNull final Activity activity,
            @NonNull final JSONArray jsonArray,
            @NonNull final OnCompleteListener onCompleteListener,
            @NonNull final ProgressNotificationHelper notification) {

        // Notification panel success.
        notification.endProgress();

        // Notification success.
        showToastRunOnUiThread(
                activity,
                activity.getString(R.string.file_utils_import_toast_message_success)
                        + " (" + jsonArray.length() + ")",
                Toast.LENGTH_LONG);

        // Notify activity.
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onCompleteListener.onComplete(RESULT_CODE_EXTRA_IMPORTED);
            }
        });
    }


    /*
        Getters
     */

    /**
     * Typically used to initialize a variable, and running thread.
     */
    public static CommonUtils.LooperHandlerThread getImportExportHandlerLooperThread() {
        return IMPORT_EXPORT_HANDLER_LOOPER_THREAD;
    }
}
