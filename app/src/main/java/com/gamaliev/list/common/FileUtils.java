package com.gamaliev.list.common;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.list.R;
import com.gamaliev.list.list.ListDatabaseHelper;
import com.gamaliev.list.list.ListEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.gamaliev.list.common.CommonUtils.checkAndRequestPermissions;
import static com.gamaliev.list.common.CommonUtils.getDateFromISO8601String;
import static com.gamaliev.list.common.CommonUtils.getStringDateISO8601;
import static com.gamaliev.list.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_COLOR;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.list.list.ListActivity.RESULT_CODE_EXTRA_EXPORTED;
import static com.gamaliev.list.list.ListActivity.RESULT_CODE_EXTRA_IMPORTED;

/**
 * Class, for working with files, for exporting/importing entries from/to database.<br>
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
    private static final ImportExportLooperThread IMPORT_EXPORT_THREAD_LOOPER;


    /*
        Init
     */

    static {
        IMPORT_EXPORT_THREAD_LOOPER = new ImportExportLooperThread();
        IMPORT_EXPORT_THREAD_LOOPER.start();
    }

    private FileUtils() {}


    /*
        EXPORT
     */

    // TODO: handle
    // Before export is check for permission, and if necessary, making request to user.
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

    // TODO: handle
    public static void exportEntriesAsync(
            @NonNull final Activity activity,
            @NonNull final OnCompleteListener onCompleteListener) {

        IMPORT_EXPORT_THREAD_LOOPER.mHandler.post(new Runnable() {
            @Override
            public void run() {
                exportEntries(activity, onCompleteListener);
            }
        });
    }

    /**
     * Export entries from database to file with Json-format.<br>
     * @param activity Activity.
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
        final ImportExportProgressNotification notification =
                new ImportExportProgressNotification(
                        activity,
                        ImportExportProgressNotification.ACTION_EXPORT);

        // SingleThreadExecutor for panel notifications, because the bug.
        final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

        // Retrieve data from database in Json-format.
        final String result = getEntriesFromDatabase(
                activity,
                jsonArray,
                singleThreadExecutor,
                notification);

        // Save result Json-string to file.
        saveStringToFile(
                activity,
                jsonArray,
                result,
                onCompleteListener,
                singleThreadExecutor,
                notification);

    }

    // TODO: handle
    @Nullable
    private static String getEntriesFromDatabase(
            @NonNull final Activity activity,
            @NonNull final JSONArray jsonArray,
            @NonNull final ExecutorService singleThreadExecutor,
            @NonNull final ImportExportProgressNotification notification) {

        // Open database and get cursor.
        final ListDatabaseHelper dbHelper = new ListDatabaseHelper(activity);
        final Cursor cursor = dbHelper.getEntries(new DatabaseQueryBuilder());

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

                // SingleThreadExecutor, because the bug.
                singleThreadExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        notification.setProgress(100, percentNew);
                    }
                });
            }
        }

        // Finish him.
        cursor.close();
        dbHelper.close();

        //
        return jsonArray.toString();
    }

    // TODO: handle
    @NonNull
    private static JSONObject getJsonObject(
            @NonNull final Activity activity,
            @NonNull final Cursor cursor) throws IllegalStateException {

        // Get values from current row.
        final int indexTitle        = cursor.getColumnIndex(LIST_ITEMS_COLUMN_TITLE);
        final int indexDescription  = cursor.getColumnIndex(LIST_ITEMS_COLUMN_DESCRIPTION);
        final int indexColor        = cursor.getColumnIndex(LIST_ITEMS_COLUMN_COLOR);
        final int indexCreated      = cursor.getColumnIndex(LIST_ITEMS_COLUMN_CREATED);
        final int indexEdited       = cursor.getColumnIndex(LIST_ITEMS_COLUMN_EDITED);
        final int indexViewed       = cursor.getColumnIndex(LIST_ITEMS_COLUMN_VIEWED);

        final String title          = cursor.getString(indexTitle);
        final String color          = cursor.getString(indexColor);
        final String description    = cursor.getString(indexDescription);
        final String created        = cursor.getString(indexCreated);
        final String edited         = cursor.getString(indexEdited);
        final String viewed         = cursor.getString(indexViewed);

        // Create entry map
        final Map<String, String> entryMap = new HashMap<>();

        entryMap.put(LIST_ITEMS_COLUMN_TITLE,          title);
        entryMap.put(LIST_ITEMS_COLUMN_COLOR,          String.format(
                "#%06X",
                (0xFFFFFF & Integer.parseInt(color))));

        entryMap.put(LIST_ITEMS_COLUMN_DESCRIPTION,    description);
        entryMap.put(LIST_ITEMS_COLUMN_CREATED,        getStringDateISO8601(activity, created));
        entryMap.put(LIST_ITEMS_COLUMN_EDITED,         getStringDateISO8601(activity, edited));
        entryMap.put(LIST_ITEMS_COLUMN_VIEWED,         getStringDateISO8601(activity, viewed));

        //
        return new JSONObject(entryMap);
    }

    // TODO: handle
    private static void saveStringToFile(
            @NonNull final Activity activity,
            @NonNull final JSONArray jsonArray,
            @NonNull final String result,
            @NonNull final OnCompleteListener onCompleteListener,
            @NonNull final ExecutorService singleThreadExecutor,
            @NonNull final ImportExportProgressNotification notification) {

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
            // SingleThreadExecutor, because the bug.
            singleThreadExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    notification.endProgress();
                }
            });

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

    // TODO: handle
    public static void importEntriesAsync(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile,
            @NonNull final OnCompleteListener onCompleteListener) {

        //
        IMPORT_EXPORT_THREAD_LOOPER.mHandler.post(new Runnable() {
            @Override
            public void run() {
                importEntries(activity, selectedFile, onCompleteListener);
            }
        });
    }

    /**
     * Import entries from given file path.
     * @param activity      Activity.
     * @param selectedFile  Path to file.
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

        // Get Json-string from file.
        final String inputJson = getStringFromFile(activity, selectedFile);

        // Parse and save to database.
        parseAndSaveToDatabase(
                activity,
                inputJson,
                onCompleteListener);
    }

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

            String line = null;
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

    // TODO: handle
    private static void parseAndSaveToDatabase(
            @NonNull final Activity activity,
            @NonNull final String inputJson,
            @NonNull final OnCompleteListener onCompleteListener){

        try (   // Open database and start transaction.
                ListDatabaseHelper dbHelper = new ListDatabaseHelper(activity);
                SQLiteDatabase db = dbHelper.getWritableDatabase()) {

            // Init
            final JSONArray jsonArray = new JSONArray(inputJson);

            // Create progress notification.
            final ImportExportProgressNotification notification =
                    new ImportExportProgressNotification(
                            activity,
                            ImportExportProgressNotification.ACTION_IMPORT);

            // SingleThreadExecutor for panel notifications, because the bug.
            final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

            // Number of entries;
            final int size = jsonArray.length();
            int percent = 0;

            // Begin transaction
            db.beginTransaction();

            try {
                // Seek.
                for (int i = 0; i < size; i++) {

                    // Get next Json-object.
                    final String entryJson = jsonArray.getString(i);
                    final JSONObject jsonObject = new JSONObject(entryJson);

                    // Convert to entry.
                    final ListEntry entry = convertJsonToListEntry(activity, jsonObject);

                    // Insert
                    dbHelper.insertEntry(entry, db);

                    // Update progress. Without flooding. 0-100%
                    final int percentNew = i * 100 / size;
                    if (percentNew > percent) {
                        //
                        percent = percentNew;

                        // SingleThreadExecutor, because the bug.
                        singleThreadExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                notification.setProgress(100, percentNew);
                            }
                        });
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
                    singleThreadExecutor,
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

    @NonNull
    private static ListEntry convertJsonToListEntry(
            @NonNull final Activity activity,
            @NonNull final JSONObject jsonObject) {

        // Get strings
        final String title          = jsonObject.optString(LIST_ITEMS_COLUMN_TITLE, null);
        final int color             = Color.parseColor(jsonObject.optString(
                                                            LIST_ITEMS_COLUMN_COLOR, null));
        final String description    = jsonObject.optString(LIST_ITEMS_COLUMN_DESCRIPTION, null);
        final String created        = jsonObject.optString(LIST_ITEMS_COLUMN_CREATED, null);
        final String edited         = jsonObject.optString(LIST_ITEMS_COLUMN_EDITED, null);
        final String viewed         = jsonObject.optString(LIST_ITEMS_COLUMN_VIEWED, null);

        // Create entry model.
        final ListEntry entry = new ListEntry();
        entry.setTitle(title);
        entry.setColor(color);
        entry.setDescription(description);
        entry.setCreated(getDateFromISO8601String(activity, created));
        entry.setEdited(getDateFromISO8601String(activity, edited));
        entry.setViewed(getDateFromISO8601String(activity, viewed));

        return entry;
    }

    // TODO: handle
    private static void makeSuccessImportOperations(
            @NonNull final Activity activity,
            @NonNull final JSONArray jsonArray,
            @NonNull final OnCompleteListener onCompleteListener,
            @NonNull final ExecutorService singleThreadExecutor,
            @NonNull final ImportExportProgressNotification notification) {

        // Notification panel success.
        // SingleThreadExecutor, because the bug.
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                notification.endProgress();
            }
        });

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
        Inner Classes
     */

    // TODO: handle
    private static class ImportExportLooperThread extends Thread {
        @Nullable
        private Handler mHandler;

        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler();
            Looper.loop();
        }
    }

    // TODO: handle
    private static class ImportExportProgressNotification {
        @NonNull private final Context mContext;
        @NonNull private final String mAction;
        @NonNull private final NotificationManager mManager;
        @NonNull private final NotificationCompat.Builder mBuilder;

        private final int mId;

        private static final String ACTION_IMPORT = "ImportExportProgressNotification.ACTION_IMPORT";
        private static final String ACTION_EXPORT = "ImportExportProgressNotification.ACTION_EXPORT";

        private ImportExportProgressNotification(
                @NonNull final Context context,
                @NonNull final String action) {

            mContext = context;
            mAction = action;
            mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(context);

            final Random random = new Random();
            mId = random.nextInt();

            if (ACTION_IMPORT.equals(action)) {
                mBuilder
                        .setContentTitle(mContext.getString(
                        R.string.file_utils_import_notification_panel_title))

                        .setContentText(mContext.getString(
                        R.string.file_utils_import_notification_panel_text));

            } else if (ACTION_EXPORT.equals(action)) {
                mBuilder
                        .setContentTitle(mContext.getString(
                                R.string.file_utils_export_notification_panel_title))

                        .setContentText(mContext.getString(
                                R.string.file_utils_export_notification_panel_text));
            }

            mBuilder.setSmallIcon(getNotificationIcon());
        }

        private void setProgress(final int max, final int progress) {
            mBuilder.setProgress(max, progress, false);
            mManager.notify(mId, mBuilder.build());
        }

        private void endProgress() {
            if (ACTION_IMPORT.equals(mAction)) {
                mBuilder.setContentText(mContext.getString(
                        R.string.file_utils_import_notification_panel_finish));

            } else if (ACTION_EXPORT.equals(mAction)) {
                mBuilder.setContentText(mContext.getString(
                        R.string.file_utils_export_notification_panel_finish));
            }

            mBuilder.setProgress(0, 0, false);
            mManager.notify(mId, mBuilder.build());
        }

        // Fix bug with color icon.
        private int getNotificationIcon() {
            boolean useWhiteIcon =
                    (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
            return useWhiteIcon
                    ? R.drawable.ic_import_export_black_24dp
                    : R.drawable.ic_import_export_white_24dp;
        }
    }


    /*
        Getters
     */

    public static ImportExportLooperThread getImportExportThreadLooper() {
        return IMPORT_EXPORT_THREAD_LOOPER;
    }
}
