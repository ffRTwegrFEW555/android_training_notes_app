package com.gamaliev.notes.user.user_preference;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.view.View;

import com.gamaliev.notes.R;
import com.gamaliev.notes.main.MainActivity;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.gamaliev.notes.UtilsTest.changeOrientation;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.UtilsTest.sleepCurrentThread;
import static org.hamcrest.Matchers.allOf;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("NullableProblems")
public class UserPreferenceFragmentTest {

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
        // Nav drawer -> Change user -> Edit user
        onView(allOf(
                withContentDescription("Open navigation drawer"),
                withParent(withId(R.id.activity_main_toolbar)),
                isDisplayed()))
                .perform(click());
        onView(allOf(withId(R.id.design_menu_item_text), withText("Change user"), isDisplayed()))
                .perform(click());

        /*
        mActivityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((RecyclerView) mActivityTestRule.getActivity().findViewById(R.id.fragment_user_rv))
                        .getChildAt(2)
                        .findViewById(R.id.fragment_user_item_configure_button)
                        .performClick();
            }
        });
        */

        onView(withId(R.id.fragment_user_rv)).perform(
                RecyclerViewActions.actionOnItemAtPosition(
                        2,
                        new ViewAction() {
                            @Override
                            public Matcher<View> getConstraints() {
                                return null;
                            }

                            @Override
                            public String getDescription() {
                                return "Click on configure user button";
                            }

                            @Override
                            public void perform(UiController uiController, View view) {
                                view    .findViewById(R.id.fragment_user_item_configure_button)
                                        .performClick();
                            }
                        })
                );
        sleepCurrentThread(-1);

        changeOrientation(mDevice);

        // Back to main.
        pressBack();
        pressBack();
    }
}