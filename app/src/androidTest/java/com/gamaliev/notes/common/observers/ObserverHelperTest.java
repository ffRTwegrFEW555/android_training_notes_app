package com.gamaliev.notes.common.observers;

import android.os.Bundle;
import android.support.annotation.Nullable;

import org.junit.Test;

import static com.gamaliev.notes.common.observers.ObserverHelper.COLOR_PICKER;
import static com.gamaliev.notes.common.observers.ObserverHelper.FILE_EXPORT;
import static org.junit.Assert.assertEquals;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class ObserverHelperTest {

    /* ... */
    private static final String EXTRA_BUNDLE = "EXTRA_BUNDLE";


    /*
        Tests
     */

    @Test
    public void registerObserver() throws Exception {
        final int[] forCheck = {-1, -1};

        final String[] observed = {COLOR_PICKER};
        final String observerName = "observerName";
        final Observer observer = new Observer() {
            @Override
            public void onNotify(final int resultCode, @Nullable final Bundle data) {
                forCheck[0] = resultCode;
                if (data == null) {
                    throw new IllegalArgumentException("Bundle data is null.");
                }
                forCheck[1] = data.getInt(EXTRA_BUNDLE, -1);
            }
        };

        ObserverHelper.registerObserver(observed, observerName, observer);

        // First test.
        ObserverHelper.notifyObservers(FILE_EXPORT, 1, null);

        // Second test.
        final Bundle data = new Bundle();
        data.putInt(EXTRA_BUNDLE, 777);
        ObserverHelper.notifyObservers(COLOR_PICKER, 1, data);
        assertEquals(forCheck[0], 1);
        assertEquals(forCheck[1], 777);

        ObserverHelper.unregisterObserver(observed, observerName);
    }

    @Test
    public void unregisterObserver() throws Exception {
        final int[] forCheck = {-1, -1};

        final String[] observed = {COLOR_PICKER};
        final String observerName = "observerName";
        final Observer observer = new Observer() {
            @Override
            public void onNotify(final int resultCode, @Nullable final Bundle data) {
                forCheck[0] = resultCode;
                if (data == null) {
                    throw new IllegalArgumentException("Bundle data is null.");
                }
                forCheck[1] = data.getInt(EXTRA_BUNDLE, -1);
            }
        };

        ObserverHelper.registerObserver(observed, observerName, observer);
        final Bundle data = new Bundle();
        data.putInt(EXTRA_BUNDLE, 777);
        ObserverHelper.notifyObservers(COLOR_PICKER, 1, data);
        assertEquals(forCheck[0], 1);
        assertEquals(forCheck[1], 777);

        ObserverHelper.unregisterObserver(observed, observerName);
        final Bundle dataTwo = new Bundle();
        data.putInt(EXTRA_BUNDLE, 888);
        ObserverHelper.notifyObservers(COLOR_PICKER, 2, dataTwo);
        assertEquals(forCheck[0], 1);
        assertEquals(forCheck[1], 777);
    }

    @Test
    public void notifyAllObservers() throws Exception {
        final int[] forCheck = {-1, -1, -1, -1};

        final String[] observed = {COLOR_PICKER};
        final String observerName = "observerName";
        final Observer observer = new Observer() {
            @Override
            public void onNotify(final int resultCode, @Nullable final Bundle data) {
                forCheck[0] = resultCode;
                if (data == null) {
                    throw new IllegalArgumentException("Bundle data is null.");
                }
                forCheck[1] = data.getInt(EXTRA_BUNDLE, -1);
            }
        };

        final String[] observedTwo = {FILE_EXPORT};
        final String observerNameTwo = "observerNameTwo";
        final Observer observerTwo = new Observer() {
            @Override
            public void onNotify(final int resultCode, @Nullable final Bundle data) {
                forCheck[2] = resultCode;
                if (data == null) {
                    throw new IllegalArgumentException("Bundle data is null.");
                }
                forCheck[3] = data.getInt(EXTRA_BUNDLE, -1);
            }
        };

        ObserverHelper.registerObserver(observed, observerName, observer);
        ObserverHelper.registerObserver(observedTwo, observerNameTwo, observerTwo);

        final Bundle data = new Bundle();
        data.putInt(EXTRA_BUNDLE, 777);
        ObserverHelper.notifyAllObservers(1, data);

        assertEquals(forCheck[0], 1);
        assertEquals(forCheck[1], 777);
        assertEquals(forCheck[2], 1);
        assertEquals(forCheck[3], 777);

        ObserverHelper.unregisterObserver(observed, observerName);
        ObserverHelper.unregisterObserver(observedTwo, observerNameTwo);
    }

    @Test
    public void notifyObservers() throws Exception {
        final int[] forCheck = {-1, -1};

        final String[] observed = {COLOR_PICKER};
        final String observerName = "observerName";
        final Observer observer = new Observer() {
            @Override
            public void onNotify(final int resultCode, @Nullable final Bundle data) {
                forCheck[0] = resultCode;
                if (data == null) {
                    throw new IllegalArgumentException("Bundle data is null.");
                }
                forCheck[1] = data.getInt(EXTRA_BUNDLE, -1);
            }
        };

        ObserverHelper.registerObserver(observed, observerName, observer);

        // First test.
        ObserverHelper.notifyObservers(FILE_EXPORT, 1, null);

        // Second test.
        final Bundle data = new Bundle();
        data.putInt(EXTRA_BUNDLE, 777);
        ObserverHelper.notifyObservers(COLOR_PICKER, 1, data);

        // #
        assertEquals(forCheck[0], 1);
        assertEquals(forCheck[1], 777);

        ObserverHelper.unregisterObserver(observed, observerName);
    }
}
