package com.gamaliev.notes.color_picker;

import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.MotionEvents;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.gamaliev.notes.R;
import com.gamaliev.notes.main.MainActivity;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Field;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.doubleClick;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static com.gamaliev.notes.UtilsTest.changeOrientation;
import static com.gamaliev.notes.UtilsTest.initDefaultPrefs;
import static com.gamaliev.notes.UtilsTest.sleepCurrentThread;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("NullableProblems")
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class ColorPickerFragmentTest {

    /* Logger */
    @NonNull private static final String TAG = ColorPickerFragmentTest.class.getSimpleName();

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
        // Main -> Add new entry -> Change color.
        onView(allOf(withId(R.id.fragment_list_fab), isDisplayed()))
                .perform(click());
        onView(allOf(withId(R.id.fragment_item_details_color), isDisplayed()))
                .perform(click());

        changeOrientation(mDevice);

        // Back to main.
        pressBack();
        pressBack();
    }

    @Test
    public void mainTest() throws Exception {
        // Main -> Add new entry -> Change color.
        onView(allOf(withId(R.id.fragment_list_fab), isDisplayed()))
                .perform(click());
        onView(allOf(withId(R.id.fragment_item_details_color), isDisplayed()))
                .perform(click());


        /*
            Color palette.
         */

        // Swipes.
        onView(allOf(withId(R.id.fragment_color_picker_scroll_palette_bar), isDisplayed()))
                .perform(swipeLeft(), swipeLeft());

        // Color box: get tag.
        final Field tagField = ColorPickerFragment.class.getDeclaredField("TAG_COLOR_BOX");
        tagField.setAccessible(true);
        final String tagFieldValue = (String) tagField.get(null);
        final int colorBoxNumber = 29;
        final Object tagToFind = tagFieldValue + colorBoxNumber;

        // Color box: get object.
        final ViewInteraction colorBox = onView(allOf(withTagValue(equalTo(tagToFind)), isDisplayed()));

        // Color box: change color on move.
        colorBox.perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isDisplayed();
                    }

                    @Override
                    public String getDescription() {
                        return "Long touch";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        Paint paint;
                        PorterDuffColorFilter colorFilter;
                        Field colorField;
                        int color;
                        int newColor;

                        // Get view size.
                        final int width = view.getWidth();
                        final int height = view.getHeight();

                        // Get color.
                        try {
                            final Field paintField = ColorDrawable.class.getDeclaredField("mPaint");
                            paintField.setAccessible(true);
                            paint = (Paint) paintField.get(view.getBackground());

                            colorFilter = (PorterDuffColorFilter) paint.getColorFilter();

                            colorField = PorterDuffColorFilter.class.getDeclaredField("mColor");
                            colorField.setAccessible(true);

                            color = colorField.getInt(colorFilter);

                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            Log.e(TAG, e.toString());
                            throw new RuntimeException(e.toString());
                        }

                        // Get view absolute position
                        int[] location = new int[2];
                        view.getLocationOnScreen(location);

                        // Offset coordinates by view position
                        float[] coordinates = new float[] {location[0], location[1]};
                        float[] precision = new float[] {1f, 1f };

                        // Send down event, moving, and send up
                        // Down.
                        final MotionEvent down =
                                MotionEvents.sendDown(uiController, coordinates, precision).down;
                        uiController.loopMainThreadForAtLeast(2000);

                        // Bottom-left.
                        MotionEvents.sendMovement(
                                uiController,
                                down,
                                new float[] {coordinates[0], coordinates[1] + height});
                        uiController.loopMainThreadForAtLeast(1000);
                        // Bottom-left (Again, due bug).
                        MotionEvents.sendMovement(
                                uiController,
                                down,
                                new float[] {coordinates[0], coordinates[1] + height});
                        uiController.loopMainThreadForAtLeast(1000);

                        // #
                        newColor = getNewColor(paint, colorField);
                        assertNotEquals(color, newColor);
                        color = newColor;

                        // Bottom-right.
                        MotionEvents.sendMovement(
                                uiController,
                                down,
                                new float[] {coordinates[0] + width, coordinates[1] + height});
                        uiController.loopMainThreadForAtLeast(1000);

                        // #
                        newColor = getNewColor(paint, colorField);
                        assertNotEquals(color, newColor);
                        color = newColor;

                        // Top-right.
                        MotionEvents.sendMovement(
                                uiController,
                                down,
                                new float[] {coordinates[0] + width, coordinates[1]});
                        uiController.loopMainThreadForAtLeast(1000);

                        // #
                        newColor = getNewColor(paint, colorField);
                        assertNotEquals(color, newColor);
                        color = newColor;

                        // Top-left.
                        MotionEvents.sendMovement(
                                uiController,
                                down,
                                new float[] {coordinates[0], coordinates[1]});
                        uiController.loopMainThreadForAtLeast(1000);

                        // #
                        newColor = getNewColor(paint, colorField);
                        assertEquals(color, newColor);

                        // Up.
                        MotionEvents.sendUp(uiController, down, coordinates);
                    }
                });
        Thread.sleep(2000);

        // Color box: on single click.
        colorBox.perform(click());
        Thread.sleep(1000);
        // TODO asserts

        // Color box: on double click.
        colorBox.perform(doubleClick());
        Thread.sleep(1000);
        // TODO asserts

        // Color box: change orientation;
        changeOrientation(mDevice);
        Thread.sleep(1000);
        // TODO asserts

        // Back to main.
        pressBack();
        pressBack();
    }


    /*
        Utils
     */

    private static int getNewColor(
            @NonNull final Paint paint,
            @NonNull final Field colorField) {

        try {
            return colorField.getInt(paint.getColorFilter());
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.toString());
            throw new RuntimeException(e.toString());
        }
    }
}