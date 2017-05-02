package com.gamaliev.list.common;

import android.content.Context;
import android.database.Cursor;
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
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class FileUtils {

    /* Logger */
    private static final String TAG = FileUtils.class.getSimpleName();

    private static final String FILE_NAME = "itemlist.ili";


    /*
        Init
     */

    private FileUtils() {}


    /*
        ...
     */

    public static void exportEntries(@NonNull final Context context) {

        /*
            {"title":"...",
            "description":"...",
            "color":"#...",
            "created":"2017-04-24T12:00:00+05:00",
            "edited":"...",
            "viewed":"..."}

            Формат дат: YYYY-MM-DDThh:mm:ss±hh:mm (https://ru.wikipedia.org/wiki/ISO_8601)
         */

        // Open database and get cursor.
        final ListDatabaseHelper dbHelper = new ListDatabaseHelper(context);
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
            entryMap.put(LIST_ITEMS_COLUMN_CREATED,        getStringDateISO8601(context, created));
            entryMap.put(LIST_ITEMS_COLUMN_EDITED,         getStringDateISO8601(context, edited));
            entryMap.put(LIST_ITEMS_COLUMN_VIEWED,         getStringDateISO8601(context, viewed));

            // Init json object
            jsonObject = new JSONObject(entryMap);

            // Add to json array
            jsonArray.put(jsonObject);
        }

        // Finish him.
        cursor.close();
        dbHelper.close();


        /*
            Save result
         */

        // Get result
        final String result = jsonArray.toString();

        // Check writable
        if (isExternalStorageReadable() && isExternalStorageWritable()) {

            try {
                // Create file
                final File file = new File(Environment.getExternalStorageDirectory(), FILE_NAME);

                // Write to file
                final FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(result.getBytes());
                outputStream.close();

                // Notification success.
                showToast(
                        context,
                        context.getString(R.string.file_utils_export_toast_message_success) + " : " + file.getPath(),
                        Toast.LENGTH_LONG);

            } catch (IOException e) {
                Log.e(TAG, e.toString());
                // Notification failed.
                showToast(
                        context,
                        context.getString(R.string.file_utils_export_toast_message_failed),
                        Toast.LENGTH_SHORT);
            }
        }
    }

    /**
     * Import entries from given file path.
     * @param context       Context.
     * @param selectedFile  Path to file.
     */
    public static void importEntries(
            @NonNull final Context context,
            @NonNull final Uri selectedFile) {

        // Get input string.
        String inputJson = null;

        // Check writable
        if (isExternalStorageReadable() && isExternalStorageWritable()) {

            try {
                //
                StringBuilder sb = new StringBuilder();

                // Read from file
                final BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        context.getContentResolver().openInputStream(selectedFile)));

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
                        context,
                        context.getString(R.string.file_utils_import_toast_message_failed),
                        Toast.LENGTH_SHORT);
            }
        }

        // Create
        JSONArray jsonArray = null;
        JSONObject jsonObject = null;

        try {
            // Init
            jsonArray = new JSONArray(inputJson);

            // Open database and start transaction.
            ListDatabaseHelper dbHelper = new ListDatabaseHelper(context);

            // Seek.
            for (int i = 0; i < jsonArray.length(); i++) {

                // Get string.
                final String entryJson = jsonArray.getString(i);

                // Init
                jsonObject = new JSONObject(entryJson);

                // Get strings
                final String title      = jsonObject.optString(LIST_ITEMS_COLUMN_TITLE, null);
                final int color         =
                        Color.parseColor(jsonObject.optString(LIST_ITEMS_COLUMN_COLOR, null));

                final String description = jsonObject.optString(LIST_ITEMS_COLUMN_DESCRIPTION, null);
                final String created    = jsonObject.optString(LIST_ITEMS_COLUMN_CREATED, null);
                final String edited     = jsonObject.optString(LIST_ITEMS_COLUMN_EDITED, null);
                final String viewed     = jsonObject.optString(LIST_ITEMS_COLUMN_VIEWED, null);

                // Create entry model.
                ListEntry entry = new ListEntry();
                entry.setTitle(title);
                entry.setColor(color);
                entry.setDescription(description);
                entry.setCreated(getDateFromISO8601String(context , created));
                entry.setEdited(getDateFromISO8601String(context , edited));
                entry.setViewed(getDateFromISO8601String(context , viewed));

                // Insert
                // TODO: add transaction.
                dbHelper.insertEntry(entry);
            }

            // Close.
            dbHelper.close();

            // Notification success.
            showToast(
                    context,
                    context.getString(R.string.file_utils_import_toast_message_success),
                    Toast.LENGTH_SHORT);



        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            // Notification failed.
            showToast(
                    context,
                    context.getString(R.string.file_utils_import_toast_message_failed),
                    Toast.LENGTH_SHORT);
        }
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
