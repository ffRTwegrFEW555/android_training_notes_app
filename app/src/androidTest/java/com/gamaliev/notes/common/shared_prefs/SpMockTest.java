package com.gamaliev.notes.common.shared_prefs;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static com.gamaliev.notes.TestUtils.initDefaultPrefs;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class SpMockTest {

    /*
        Init
     */

    @Before
    public void before() throws Exception {
        initDefaultPrefs();
    }


    /*
        Tests
     */

    @Test
    public void addMockData() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();
        SpMock.addMockData(context);
    }

    @Test
    public void getMockFilterProfiles() throws Exception {
        final Set<String> profiles = SpMock.getMockFilterProfiles();

        assertNotNull(profiles);
        assertTrue(profiles.size() > 0);
    }
}