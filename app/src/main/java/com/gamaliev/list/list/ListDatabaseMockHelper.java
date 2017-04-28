package com.gamaliev.list.list;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.gamaliev.list.colorpicker.ColorPickerDatabaseHelper;
import com.gamaliev.list.common.DatabaseHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static com.gamaliev.list.list.ListActivity.SP_FILTER_COLOR;
import static com.gamaliev.list.list.ListActivity.SP_FILTER_CREATED;
import static com.gamaliev.list.list.ListActivity.SP_FILTER_EDITED;
import static com.gamaliev.list.list.ListActivity.SP_FILTER_SORT_ID;
import static com.gamaliev.list.list.ListActivity.SP_FILTER_VIEWED;
import static com.gamaliev.list.list.ListActivity.SP_ORDER;
import static com.gamaliev.list.list.ListActivity.SP_ORDER_ASC_DESC;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ListDatabaseMockHelper extends DatabaseHelper {

    /* Logger */
    private static final String TAG = ListDatabaseMockHelper.class.getSimpleName();

    /* Mock data */
    public static final String[] LIST_MOCK_NAMES = {
            "Anastasia Aleksandrova", "Boris Babushkin", "Viktor Vasilyev", "Gennady Georgiyev",
            "Dmitry Dudinsky", "Yelena Yeremeyeva", "Pyotr Vorobyov", "Tatyana Terentyeva",
            "Svetlana Stasova", "Maria Timmerman"};

    public static final String[] LIST_MOCK_DESCRIPTION = {
            "Passion", "Smile", "Love", "Eternity", "Fantastic", "Destiny", "Freedom", "Liberty",
            "Tranquillity", "Peace", "Sunshine", "Gorgeous", "Hope", "Grace", "Rainbow",
            "Sunflower", "serendipity", "bliss", "cute", "hilarious", "aqua", "sentiment",
            "bubble", "banana", "paradox", "Blossom", "Cherish", "Enthusiasm", "lullaby",
            "renaissance", "cosy", "butterfly", "galaxy", "moment", "cosmopolitan", "lollipop"
    };

    public static final String[] LIST_MOCK_DATE = {
            "2017-04-25 21:25:35",
            "2017-04-24 21:25:35",
            "2017-04-23 21:25:35",
            "2017-04-22 21:25:35",
            "2017-04-21 21:25:35",
            "2017-04-20 21:25:35",
    };

    private static final String[][] SP_MOCK_DATA = new String[][] {
            {"0",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""},

            {"1",
                    "53248",
                    "2017-04-20 21:25:35#2017-04-21 21:25:35",
                    "2017-04-20 21:25:35#2017-04-21 21:25:35",
                    "2017-04-20 21:25:35#2017-04-21 21:25:35",
                    LIST_ITEMS_COLUMN_TITLE,
                    ORDER_ASCENDING},

            {"2",
                    "-16711824",
                    "2017-04-22 21:25:35#2017-04-23 21:25:35",
                    "2017-04-22 21:25:35#2017-04-23 21:25:35",
                    "2017-04-22 21:25:35#2017-04-23 21:25:35",
                    LIST_ITEMS_COLUMN_CREATED,
                    ORDER_DESCENDING},

            {"3",
                    "-4096",
                    "2017-04-24 21:25:35#2017-04-25 21:25:35",
                    "2017-04-24 21:25:35#2017-04-25 21:25:35",
                    "2017-04-24 21:25:35#2017-04-25 21:25:35",
                    LIST_ITEMS_COLUMN_EDITED,
                    ORDER_ASCENDING}
    };


    /*
        Init
     */

    ListDatabaseMockHelper(@NonNull final Context context) {
        super(context);
    }

    ListDatabaseMockHelper(@NonNull final Context context,
                       @NonNull final String name,
                       @NonNull final SQLiteDatabase.CursorFactory factory,
                       final int version) {
        super(context, name, factory, version);
    }

    ListDatabaseMockHelper(@NonNull final Context context,
                       @NonNull final String name,
                       @NonNull final SQLiteDatabase.CursorFactory factory,
                       final int version,
                       @NonNull final DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }


    /*
        Mock entries
     */

    /**
     * Add mock entries in given database, with given params.<br>
     * See: {@link com.gamaliev.list.list.ListActivity}
     *
     * @param entriesNumber Number of inserting entries.
     * @param db            Opened database.
     * @throws SQLiteException If insert error.
     */
    public static void addMockEntries(
            final int entriesNumber,
            @NonNull final SQLiteDatabase db) throws SQLiteException {

        final Random random = new Random();

        for (int i = 0; i < entriesNumber; i++) {
            // Content values.
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_TITLE,         getRandomMockName(random));
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   getRandomMockDescription(random));
            cv.put(LIST_ITEMS_COLUMN_COLOR,         getRandomFavoriteColor(random));
            cv.put(LIST_ITEMS_COLUMN_CREATED,       getRandomMockDate(random));
            cv.put(LIST_ITEMS_COLUMN_EDITED,        getRandomMockDate(random));
            cv.put(LIST_ITEMS_COLUMN_VIEWED,        getRandomMockDate(random));

            // Insert query.
            if (db.insert(LIST_ITEMS_TABLE_NAME, null, cv) == -1) {
                throw new SQLiteException("[ERROR] Add mock entries.");
            }
        }
    }

    /**
     * @param random Generator of pseudorandom numbers.
     * @return Random name from {@link #LIST_MOCK_NAMES}
     */
    @NonNull
    private static String getRandomMockName(@NonNull final Random random) {
        return LIST_MOCK_NAMES[random.nextInt(LIST_MOCK_NAMES.length)];
    }

    /**
     * @param random Generator of pseudorandom numbers.
     * @return Random description from {@link #LIST_MOCK_DESCRIPTION}
     */
    @NonNull
    private static String getRandomMockDescription(@NonNull final Random random) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(LIST_MOCK_DESCRIPTION[random.nextInt(LIST_MOCK_DESCRIPTION.length)]);
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * @param random Generator of pseudorandom numbers.
     * @return Random favorite color from
     * {@link com.gamaliev.list.colorpicker.ColorPickerDatabaseHelper#FAVORITE_COLORS_DEFAULT}
     */
    @NonNull
    private static String getRandomFavoriteColor(@NonNull final Random random) {
        final int[] colors = ColorPickerDatabaseHelper.FAVORITE_COLORS_DEFAULT;
        return String.valueOf(colors[random.nextInt(colors.length)]);
    }

    /**
     * @param random Generator of pseudorandom numbers.
     * @return Random date from {@link #LIST_MOCK_DATE}
     */
    @NonNull
    private static String getRandomMockDate(@NonNull final Random random) {
        return LIST_MOCK_DATE[random.nextInt(LIST_MOCK_DATE.length)];
    }


    /*
        Mock shared preferences.
     */

    /**
     * @return Set of mock profiles for shared preferences, in JSON-format.
     */
    @NonNull
    public static Set<String> getMockProfiles() {
        final Set<String> profiles = new HashSet<>();

        for (String[] entry : SP_MOCK_DATA) {
            try {
                final JSONObject jsonObject = new JSONObject();
                jsonObject.put(SP_FILTER_SORT_ID,   entry[0]);
                jsonObject.put(SP_FILTER_COLOR,     entry[1]);
                jsonObject.put(SP_FILTER_CREATED,   entry[2]);
                jsonObject.put(SP_FILTER_EDITED,    entry[3]);
                jsonObject.put(SP_FILTER_VIEWED,    entry[4]);
                jsonObject.put(SP_ORDER,            entry[5]);
                jsonObject.put(SP_ORDER_ASC_DESC,   entry[6]);

                profiles.add(jsonObject.toString());

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }

        return profiles;
    }
}
