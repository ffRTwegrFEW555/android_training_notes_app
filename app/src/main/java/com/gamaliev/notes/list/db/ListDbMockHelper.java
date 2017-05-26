package com.gamaliev.notes.list.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gamaliev.notes.R;
import com.gamaliev.notes.colorpicker.db.ColorPickerDbHelper;
import com.gamaliev.notes.common.ProgressNotificationHelper;

import java.util.Random;

import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_COLOR;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_IMAGE_URL;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_TABLE_NAME;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ListDbMockHelper {

    /* Mock data */
    private static final String[] LIST_MOCK_NAMES = {
            "Anastasia Aleksandrova", "Boris Babushkin", "Viktor Vasilyev", "Gennady Georgiyev",
            "Dmitry Dudinsky", "Yelena Yeremeyeva", "Pyotr Vorobyov", "Tatyana Terentyeva",
            "Svetlana Stasova", "Maria Timmerman"};

    private static final String[] LIST_MOCK_DESCRIPTION = {
            "Passion", "Smile", "Love", "Eternity", "Fantastic", "Destiny", "Freedom", "Liberty",
            "Tranquillity", "Peace", "Sunshine", "Gorgeous", "Hope", "Grace", "Rainbow",
            "Sunflower", "serendipity", "bliss", "cute", "hilarious", "aqua", "sentiment",
            "bubble", "banana", "paradox", "Blossom", "Cherish", "Enthusiasm", "lullaby",
            "renaissance", "cosy", "butterfly", "galaxy", "moment", "cosmopolitan", "lollipop"
    };

    private static final String[] LIST_MOCK_DATE = {
            "2017-05-10 21:25:35",
            "2017-05-09 21:25:35",
            "2017-05-08 21:25:35",
            "2017-05-07 21:25:35",
            "2017-05-06 21:25:35",
            "2017-05-05 21:25:35",
            "2017-05-04 21:25:35",
            "2017-05-03 21:25:35",
            "2017-05-02 21:25:35",
            "2017-05-01 21:25:35",
    };


    /*
        Init
     */

    private ListDbMockHelper() {}


    /*
        Mock entries
     */

    /**
     * Add mock entries in given database, with given params.<br>
     *
     * @param entriesNumber Number of inserting entries.
     * @param db            Opened database.
     * @throws SQLiteException If insert error.
     */
    public static void addMockEntries(
            @NonNull    final Context           context,
                        final int               entriesNumber,
            @NonNull    final SQLiteDatabase    db,
            @Nullable   final ProgressNotificationHelper notification,
                        final boolean           yieldIfContendedSafely) throws SQLiteException {

        final Random random = new Random();

        // Number of entries;
        int percent = 0;

        for (int i = 0; i < entriesNumber; i++) {
            // Content values.
            final ContentValues cv = new ContentValues();
            cv.put(LIST_ITEMS_COLUMN_TITLE,         getRandomMockName(random));
            cv.put(LIST_ITEMS_COLUMN_DESCRIPTION,   getRandomMockDescription(random));
            cv.put(LIST_ITEMS_COLUMN_COLOR,         getRandomFavoriteColor(random));
            cv.put(LIST_ITEMS_COLUMN_IMAGE_URL,     getRandomImageUrl(context));
            cv.put(LIST_ITEMS_COLUMN_CREATED,       getRandomMockDate(random));
            cv.put(LIST_ITEMS_COLUMN_EDITED,        getRandomMockDate(random));
            cv.put(LIST_ITEMS_COLUMN_VIEWED,        getRandomMockDate(random));

            // Insert query.
            if (db.insert(LIST_ITEMS_TABLE_NAME, null, cv) == -1) {
                throw new SQLiteException("[ERROR] Add mock entries.");
            }

            // Update progress. Without flooding. 0-100%
            if (notification != null) {
                final int percentNew = i * 100 / entriesNumber;
                if (percentNew > percent) {
                    //
                    percent = percentNew;
                    //
                    notification.setProgress(100, percentNew);
                }
            }

            //
            if (yieldIfContendedSafely && i % 100 == 0) {
                db.yieldIfContendedSafely();
            }
        }

        // Notification panel success.
        if (notification != null) {
            notification.endProgress();
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
     * {@link ColorPickerDbHelper#FAVORITE_COLORS_DEFAULT}
     */
    @NonNull
    private static String getRandomFavoriteColor(@NonNull final Random random) {
        final int[] colors = ColorPickerDbHelper.FAVORITE_COLORS_DEFAULT;
        return String.valueOf(colors[random.nextInt(colors.length)]);
    }

    @NonNull
    private static String getRandomImageUrl(@NonNull final Context context) {
        return context.getString(R.string.mock_entries_default_image);
    }

    /**
     * @param random Generator of pseudorandom numbers.
     * @return Random date from {@link #LIST_MOCK_DATE}
     */
    @NonNull
    private static String getRandomMockDate(@NonNull final Random random) {
        return LIST_MOCK_DATE[random.nextInt(LIST_MOCK_DATE.length)];
    }
}
