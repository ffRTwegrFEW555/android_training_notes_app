package com.gamaliev.notes.list.filter_sort_dialog;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;

import com.gamaliev.notes.R;
import com.gamaliev.notes.main.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.gamaliev.notes.UtilsTest.changeOrientation;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.UtilsTest.sleepCurrentThread;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("NullableProblems")
public class FilterSortDialogFragmentTest {

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
        // Open -> Cancel.
        onView(withId(R.id.menu_list_filter_sort)).perform(click());
        onView(withId(R.id.fragment_list_filter_dialog_action_button_cancel)).perform(click());

        // Open -> Turn -> Filter.
        onView(withId(R.id.menu_list_filter_sort)).perform(click());
        changeOrientation(mDevice);
        onView(withId(R.id.fragment_list_filter_dialog_action_button_filter)).perform(click());
    }
}