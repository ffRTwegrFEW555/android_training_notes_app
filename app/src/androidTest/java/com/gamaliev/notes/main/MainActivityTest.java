package com.gamaliev.notes.main;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.gamaliev.notes.UtilsTest.changeOrientation;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.UtilsTest.sleepCurrentThread;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("NullableProblems")
public class MainActivityTest {

    /* ... */
    @NonNull private UiDevice mDevice;


    /*
        Init
     */

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void before() throws Exception {
        initDefaultPrefs();
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        sleepCurrentThread(-1);
    }


    /*
        Tests
     */

    @Test
    public void changeOrientationTest() throws Exception {
        changeOrientation(mDevice);
    }
}