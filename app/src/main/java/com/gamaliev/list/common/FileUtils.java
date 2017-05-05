package com.gamaliev.list.common;

import android.Manifest;
import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
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

import static com.gamaliev.list.common.CommonUtils.checkAndRequestPermissions;
import static com.gamaliev.list.common.CommonUtils.getDateFromISO8601String;
import static com.gamaliev.list.common.CommonUtils.getStringDateISO8601;
import static com.gamaliev.list.common.CommonUtils.showToast;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_COLOR;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.list.common.DatabaseHelper.LIST_ITEMS_COLUMN_VIEWED;

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


    /*
        Init
     */

    private FileUtils() {}


    /*
        ...
     */


    /**
     * Export entries from database to file with Json-format.<br>
     * Before export is check for permission, and if necessary, making request to user.
     * @param activity Activity.
     */
    public static void exportEntries(@NonNull final Activity activity) {

        // Check writable. If denied, make request, then break.
        if (!checkAndRequestPermissions(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE)) {

            return;
        }


        /*
            Retrieve data from database and create result Json-string.
         */

        // Open database and get cursor.
        final ListDatabaseHelper dbHelper = new ListDatabaseHelper(activity);
        final Cursor cursor = dbHelper.getEntries(new DatabaseQueryBuilder());

        // Create json array;
        final JSONArray jsonArray = new JSONArray();

        // Create json object;
        JSONObject jsonObject;

        // Seek.
        while (cursor.moveToNext()) {

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

            // Init json object
            jsonObject = new JSONObject(entryMap);

            // Add to json array
            jsonArray.put(jsonObject);
        }

        // Finish him.
        cursor.close();
        dbHelper.close();


        /*
            Save result Json-string to file.
         */

        // Get result
        final String result = jsonArray.toString();

        try {
            // Create file
            final File file = new File(Environment.getExternalStorageDirectory(), FILE_NAME);

            // Write to file
            final FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(result.getBytes());
            outputStream.close();

            // Notification success.
            showToast(
                    activity,
                    activity.getString(R.string.file_utils_export_toast_message_success)
                            + " : " + file.getPath()
                            + " (" + jsonArray.length() + ")",
                    Toast.LENGTH_LONG);

        } catch (IOException e) {
            Log.e(TAG, e.toString());
            // Notification failed.
            showToast(
                    activity,
                    activity.getString(R.string.file_utils_export_toast_message_failed),
                    Toast.LENGTH_SHORT);
        }
    }

    /**
     * Import entries from given file path.
     * @param activity      Activity.
     * @param selectedFile  Path to file.
     */
    public static void importEntries(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile) {

        /*
            Get Json-string from file.
         */

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
            showToast(
                    activity,
                    activity.getString(R.string.file_utils_import_toast_message_failed),
                    Toast.LENGTH_SHORT);
        }


        /*
            Parse and save to database.
         */

        try (   // Open database and start transaction.
                ListDatabaseHelper dbHelper = new ListDatabaseHelper(activity);
                SQLiteDatabase db = dbHelper.getWritableDatabase()) {

            // Init
            final JSONArray jsonArray = new JSONArray(inputJson);
            JSONObject jsonObject = null;

            // Begin transaction
            db.beginTransaction();

            try {
                // Seek.
                for (int i = 0; i < jsonArray.length(); i++) {

                    // Get string.
                    final String entryJson = jsonArray.getString(i);

                    // Init
                    jsonObject = new JSONObject(entryJson);

                    // Get strings
                    final String title = jsonObject.optString(LIST_ITEMS_COLUMN_TITLE, null);
                    final int color =
                            Color.parseColor(jsonObject.optString(LIST_ITEMS_COLUMN_COLOR, null));

                    final String description = jsonObject.optString(LIST_ITEMS_COLUMN_DESCRIPTION, null);
                    final String created = jsonObject.optString(LIST_ITEMS_COLUMN_CREATED, null);
                    final String edited = jsonObject.optString(LIST_ITEMS_COLUMN_EDITED, null);
                    final String viewed = jsonObject.optString(LIST_ITEMS_COLUMN_VIEWED, null);

                    // Create entry model.
                    final ListEntry entry = new ListEntry();
                    entry.setTitle(title);
                    entry.setColor(color);
                    entry.setDescription(description);
                    entry.setCreated(getDateFromISO8601String(activity, created));
                    entry.setEdited(getDateFromISO8601String(activity, edited));
                    entry.setViewed(getDateFromISO8601String(activity, viewed));

                    // Insert
                    dbHelper.insertEntry(entry, db);
                }

                // If ok.
                db.setTransactionSuccessful();

            } finally {
                db.endTransaction();
            }

            // Notification success.
            showToast(
                    activity,
                    activity.getString(R.string.file_utils_import_toast_message_success)
                        + " (" + jsonArray.length() + ")",
                    Toast.LENGTH_SHORT);

        } catch (JSONException | SQLiteException e) {
            Log.e(TAG, e.toString());
            // Notification failed.
            showToast(
                    activity,
                    activity.getString(R.string.file_utils_import_toast_message_failed),
                    Toast.LENGTH_SHORT);
        }
    }
}
