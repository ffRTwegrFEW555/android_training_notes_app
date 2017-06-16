package com.gamaliev.notes;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;

import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.shared_prefs.SpCommon;

import static android.content.Context.MODE_PRIVATE;
import static com.gamaliev.notes.common.shared_prefs.SpCommon.SP_MAIN;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.getPreferencesName;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class UtilsTest {

    /*
        Init
     */

    private UtilsTest() {}


    /*
        Utils
     */

    /**
     * Clear main preferences of application. Delete all databases. Init default settings.
     */
    public static void initDefaultPrefs() {
        final Context context = InstrumentationRegistry.getTargetContext();

        final SharedPreferences sp = context.getSharedPreferences(
                SP_MAIN,
                MODE_PRIVATE);
        sp.edit().clear().apply();

        deleteAllDatabase();

        SpCommon.initSharedPreferences(context);
    }

    /**
     * Clear user preferences, with specified id.
     * @param userId User id.
     */
    public static void clearUserPrefs(@NonNull final String userId) {
        final Context context = InstrumentationRegistry.getTargetContext();
        final SharedPreferences sp = context.getSharedPreferences(
                getPreferencesName(userId),
                MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    private static void deleteAllDatabase() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String[] dbList = context.databaseList();
        for (String dbName : dbList) {
            deleteDatabase(dbName);
        }
    }

    private static boolean deleteDatabase(@NonNull final String dbName) {
        final Context context = InstrumentationRegistry.getTargetContext();
        boolean result = context.deleteDatabase(dbName);
        DbHelper.clearInstances();
        return result;
    }

    /**
     * Sleep the current thread.
     * @param millis        Time to sleep. if '-1', then default value is '1000'.
     * @throws Exception    Thread.sleep() exceptions.
     */
    public static void sleepCurrentThread(final int millis) throws Exception {
        if (millis == -1) {
            Thread.sleep(1000);
        } else {
            Thread.sleep(millis);
        }
    }


    /*
        Utils. UI
     */

    /**
     * Turn left -> Thread.sleep(1000) -> Turn right -> Thread.sleep(1000) -> Turn natural.
     * @param device        UI device.
     * @throws Exception    Thread.sleep() exceptions.
     */
    public static void changeOrientation(@NonNull final UiDevice device) throws Exception {
        device.setOrientationLeft();
        sleepCurrentThread(-1);
        device.setOrientationRight();
        sleepCurrentThread(-1);
        device.setOrientationNatural();
        sleepCurrentThread(-1);
    }
}
