package com.gamaliev.notes.common;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.list.db.ListDbHelper;
import com.gamaliev.notes.model.ListEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_NOTES_EXPORTED;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_NOTES_IMPORTED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;
import static com.gamaliev.notes.common.db.DbHelper.getEntries;
import static com.gamaliev.notes.common.observers.ObserverHelper.FILE_EXPORT;
import static com.gamaliev.notes.common.observers.ObserverHelper.FILE_IMPORT;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getProgressNotificationTimerForCurrentUser;
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
    public static final String FILE_NAME_EXPORT_DEFAULT = "itemlist.ili";
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
     * Starting the export in asynchronous mode,
     * using a dedicated shared thread, with looper and message queue.<br>
     *
     * @param activity              Activity.
     * @param selectedFile          Selected file.
     */
    public static void exportEntriesAsync(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile) {

        IMPORT_EXPORT_HANDLER_LOOPER_THREAD.getHandler().post(new Runnable() {
            @Override
            public void run() {
                exportEntries(activity, selectedFile);
            }
        });
    }

    /**
     * Export entries from database to file with Json-format.<br>
     * If the operation time is longer than the specified time, then progress notification enable.
     *
     * @param activity              Activity.
     * @param selectedFile          Selected file.
     */
    public static void exportEntries(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile) {

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
                getProgressNotificationTimerForCurrentUser(activity.getApplicationContext()),
                false);

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
                selectedFile,
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
        final Cursor cursor = getEntries(
                activity,
                LIST_ITEMS_TABLE_NAME,
                null);

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
            @NonNull final Uri selectedFile,
            @NonNull final ProgressNotificationHelper notification) {

        try {
            //
            final OutputStream os = activity
                    .getContentResolver()
                    .openOutputStream(selectedFile);

            os.write(result.getBytes());
            os.close();

            // Notification success.
            showToastRunOnUiThread(
                    activity,
                    activity.getString(R.string.file_utils_export_toast_message_success)
                            + " (" + jsonArray.length() + ")",
                    Toast.LENGTH_LONG);

            // Notify.
            notifyObservers(
                    FILE_EXPORT,
                    RESULT_CODE_NOTES_EXPORTED,
                    null);

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
     */
    public static void importEntriesAsync(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile) {

        //
        IMPORT_EXPORT_HANDLER_LOOPER_THREAD.getHandler().post(new Runnable() {
            @Override
            public void run() {
                importEntries(activity, selectedFile);
            }
        });
    }

    /**
     * Import entries from file to database.<br>
     * If the operation time is longer than the specified time, then progress notification enable.
     *
     * @param activity              Activity.
     * @param selectedFile          Selected file.
     */
    public static void importEntries(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile){

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
                getProgressNotificationTimerForCurrentUser(activity.getApplicationContext()),
                false);

        // Get Json-string from file.
        final String inputJson = getStringFromFile(activity, selectedFile);

        // Parse and save to database.
        parseAndSaveToDatabase(
                activity,
                inputJson,
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
            final InputStream is = activity
                    .getContentResolver()
                    .openInputStream(selectedFile);

            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            int count;
            while ((count = is.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }

            bytes.close();
            is.close();

            //
            inputJson = bytes.toString();

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
     */
    private static void parseAndSaveToDatabase(
            @NonNull final Activity activity,
            @NonNull final String inputJson,
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
                    ListDbHelper.insertUpdateEntry(activity, entry, db, false);

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
        notifyObservers(
                FILE_IMPORT,
                RESULT_CODE_NOTES_IMPORTED,
                null);
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
