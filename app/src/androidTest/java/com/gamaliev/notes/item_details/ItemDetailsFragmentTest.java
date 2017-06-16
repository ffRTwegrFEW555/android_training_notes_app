package com.gamaliev.notes.item_details;

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
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.gamaliev.notes.UtilsTest.changeOrientation;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.UtilsTest.sleepCurrentThread;
import static org.hamcrest.Matchers.allOf;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("NullableProblems")
public class ItemDetailsFragmentTest {

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
        // Main -> Item details edit.
        onView(allOf(withId(R.id.fragment_list_rv), isDisplayed()))
                .perform(actionOnItemAtPosition(0, click()));

        changeOrientation(mDevice);

        // Back to main.
        pressBack();
    }
}