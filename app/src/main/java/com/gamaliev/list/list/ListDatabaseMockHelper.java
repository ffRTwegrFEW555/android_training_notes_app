package com.gamaliev.list.list;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;

import com.gamaliev.list.colorpicker.ColorPickerDatabaseHelper;
import com.gamaliev.list.common.DatabaseHelper;

import java.util.Random;

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
            "2017-04-25T21:25:35+05:00",
            "2017-04-24T21:25:35+05:00",
            "2017-04-23T21:25:35+05:00",
            "2017-04-22T21:25:35+05:00",
            "2017-04-21T21:25:35+05:00",
            "2017-04-20T21:25:35+05:00",
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
        Methods
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

    private static String getRandomMockName(@NonNull final Random random) {
        return LIST_MOCK_NAMES[random.nextInt(LIST_MOCK_NAMES.length)];
    }

    private static String getRandomMockDescription(@NonNull final Random random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(LIST_MOCK_DESCRIPTION[random.nextInt(LIST_MOCK_DESCRIPTION.length)]);
            sb.append(" ");
        }
        return sb.toString();
    }

    private static String getRandomFavoriteColor(@NonNull final Random random) {
        int[] colors = ColorPickerDatabaseHelper.FAVORITE_COLORS_DEFAULT;
        return String.valueOf(colors[random.nextInt(colors.length)]);
    }

    private static String getRandomMockDate(@NonNull final Random random) {
        return LIST_MOCK_DATE[random.nextInt(LIST_MOCK_DATE.length)];
    }
}
