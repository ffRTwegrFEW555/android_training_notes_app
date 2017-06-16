package com.gamaliev.notes.entity;

import android.content.Context;
import android.database.MatrixCursor;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import com.gamaliev.notes.common.shared_prefs.SpCommon;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.gamaliev.notes.common.CommonUtils.getDateFromIso8601String;
import static com.gamaliev.notes.common.CommonUtils.getStringDateFormatSqlite;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_COLOR;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_IMAGE_URL;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ListEntryTest {

    @Test
    public void settersGetters() throws Exception {
        final Long id           = 1L;
        final Long syncId       = 2L;
        final String title      = "title";
        final String description = "description";
        final Integer color     = 123456;
        final String imageUrl   = "http://jpg.jpg.jpg";
        final Date created      = new Date(System.currentTimeMillis());
        final Date edited       = new Date(System.currentTimeMillis());
        final Date viewed       = new Date(System.currentTimeMillis());

        final ListEntry listEntry = new ListEntry();
        listEntry.setId(id);
        listEntry.setSyncId(syncId);
        listEntry.setTitle(title);
        listEntry.setDescription(description);
        listEntry.setColor(color);
        listEntry.setImageUrl(imageUrl);
        listEntry.setCreated(created);
        listEntry.setEdited(edited);
        listEntry.setViewed(viewed);

        assertEquals(id,        listEntry.getId());
        assertEquals(syncId,    listEntry.getSyncId());
        assertEquals(title,     listEntry.getTitle());
        assertEquals(description, listEntry.getDescription());
        assertEquals(color,     listEntry.getColor());
        assertEquals(imageUrl,  listEntry.getImageUrl());
        assertEquals(created,   listEntry.getCreated());
        assertEquals(edited,    listEntry.getEdited());
        assertEquals(viewed,    listEntry.getViewed());
    }

    @Test
    public void parcelable() throws Exception {
        final Long id           = 1L;
        final Long syncId       = 2L;
        final String title      = "title";
        final String description = "description";
        final Integer color     = 123456;
        final String imageUrl   = "http://jpg.jpg.jpg";
        final Date created      = new Date(System.currentTimeMillis());
        final Date edited       = new Date(System.currentTimeMillis());
        final Date viewed       = new Date(System.currentTimeMillis());

        final ListEntry listEntry = new ListEntry();
        listEntry.setId(id);
        listEntry.setSyncId(syncId);
        listEntry.setTitle(title);
        listEntry.setDescription(description);
        listEntry.setColor(color);
        listEntry.setImageUrl(imageUrl);
        listEntry.setCreated(created);
        listEntry.setEdited(edited);
        listEntry.setViewed(viewed);

        final Parcel parcel = Parcel.obtain();
        listEntry.writeToParcel(parcel, listEntry.describeContents());
        parcel.setDataPosition(0);
        final ListEntry parcelListEntry = ListEntry.CREATOR.createFromParcel(parcel);

        // #1
        assertEquals(listEntry, parcelListEntry);

        final ListEntry listEntryNotFull = new ListEntry();
        listEntryNotFull.setId(id);
        listEntryNotFull.setTitle(title);
        listEntryNotFull.setColor(color);
        listEntryNotFull.setImageUrl(imageUrl);
        listEntryNotFull.setViewed(viewed);

        final Parcel parcelNotFull = Parcel.obtain();
        listEntryNotFull.writeToParcel(parcelNotFull, listEntryNotFull.describeContents());
        parcelNotFull.setDataPosition(0);
        final ListEntry parcelListEntryNotFull = ListEntry.CREATOR.createFromParcel(parcelNotFull);

        // #2
        assertEquals(listEntryNotFull, parcelListEntryNotFull);

        final ListEntry[] listEntries = ListEntry.CREATOR.newArray(5);

        // #3
        assertEquals(listEntries.length, 5);
    }

    @Test
    public void getJsonObjectFromCursor() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();

        final String[] columns = {
                LIST_ITEMS_COLUMN_TITLE,
                LIST_ITEMS_COLUMN_DESCRIPTION,
                LIST_ITEMS_COLUMN_COLOR,
                LIST_ITEMS_COLUMN_IMAGE_URL,
                LIST_ITEMS_COLUMN_CREATED,
                LIST_ITEMS_COLUMN_EDITED,
                LIST_ITEMS_COLUMN_VIEWED};

        // Database format.
        final String title      = "title";
        final String description = "description";
        final String color      = "123456";
        final String imageUrl   = "imageUrl";
        final String created    = "2017-05-10 21:25:35";
        final String edited     = "2017-05-10 21:25:35";
        final String viewed     = "2017-05-10 21:25:35";

        // Json format.
        final String colorExpected = "#01E240";

        final Map<String, String> entryExpectedMap = new HashMap<>();
        entryExpectedMap.put(LIST_ITEMS_COLUMN_TITLE,      title);
        entryExpectedMap.put(LIST_ITEMS_COLUMN_DESCRIPTION, description);
        entryExpectedMap.put(LIST_ITEMS_COLUMN_COLOR,      colorExpected);
        entryExpectedMap.put(LIST_ITEMS_COLUMN_IMAGE_URL,  imageUrl);
        entryExpectedMap.put(LIST_ITEMS_COLUMN_CREATED,    created);
        entryExpectedMap.put(LIST_ITEMS_COLUMN_EDITED,     edited);
        entryExpectedMap.put(LIST_ITEMS_COLUMN_VIEWED,     viewed);

        // Fake cursor.
        final MatrixCursor matrixCursor = new MatrixCursor(columns);
        matrixCursor.addRow(new String[] {
                title,
                description,
                color,
                imageUrl,
                created,
                edited,
                viewed});
        matrixCursor.moveToNext();

        // Convert cursor to json.
        final JSONObject jsonObject = ListEntry.getJsonObjectFromCursor(
                InstrumentationRegistry.getTargetContext(),
                matrixCursor);
        if (jsonObject == null) {
            throw new Exception("jsonObject is null.");
        }
        final Map<String, String> entryActualMap =
                SpCommon.convertJsonToMap(jsonObject.toString());
        if (entryActualMap == null) {
            throw new Exception("entryActualMap is null.");
        }

        // Convert dates.
        final Date createdDate =
                getDateFromIso8601String(context, entryActualMap.get(LIST_ITEMS_COLUMN_CREATED));
        final Date editedDate =
                getDateFromIso8601String(context, entryActualMap.get(LIST_ITEMS_COLUMN_EDITED));
        final Date viewedDate =
                getDateFromIso8601String(context, entryActualMap.get(LIST_ITEMS_COLUMN_VIEWED));

        if (createdDate == null || editedDate == null || viewedDate == null) {
            throw new IllegalArgumentException();
        }

        final String createdString = getStringDateFormatSqlite(context, createdDate, true);
        final String editedString = getStringDateFormatSqlite(context, editedDate, true);
        final String viewedString = getStringDateFormatSqlite(context, viewedDate, true);

        entryActualMap.put(LIST_ITEMS_COLUMN_CREATED, createdString);
        entryActualMap.put(LIST_ITEMS_COLUMN_EDITED, editedString);
        entryActualMap.put(LIST_ITEMS_COLUMN_VIEWED, viewedString);

        // #1
        assertEquals(entryExpectedMap, entryActualMap);

        // Check null.
        final MatrixCursor matrixCursorNull = new MatrixCursor(columns);
        matrixCursorNull.addRow(new String[] {
                title,
                description,
                "abc",
                imageUrl,
                created,
                edited,
                viewed});
        matrixCursorNull.moveToNext();
        final JSONObject jsonObjectNull = ListEntry.getJsonObjectFromCursor(
                InstrumentationRegistry.getTargetContext(),
                matrixCursorNull);

        // #2
        assertNull(jsonObjectNull);
    }

    @Test
    public void convertJsonStringToListEntry() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();

        final String note = getNoteInstance();

        // #1
        assertNotNull(ListEntry.convertJsonToListEntry(context, note));

        // #2
        assertNull(ListEntry.convertJsonToListEntry(context, "123"));
    }

    @Test
    public void convertJsonObjectToListEntry() throws Exception {
        final Context context = InstrumentationRegistry.getTargetContext();

        final String note = getNoteInstance();

        ListEntry.convertJsonToListEntry(context, new JSONObject(note));
    }

    @SuppressWarnings({"ObjectEqualsNull", "EqualsWithItself", "PMD.EqualsNull"})
    @Test
    public void equalsTest() throws Exception {
        final Long id           = 1L;
        final Long syncId       = 2L;
        final String title      = "title";
        final String description = "description";
        final Integer color     = 123456;
        final String imageUrl   = "http://jpg.jpg.jpg";
        final Date created      = new Date(System.currentTimeMillis());
        final Date edited       = new Date(System.currentTimeMillis());
        final Date viewed       = new Date(System.currentTimeMillis());

        final ListEntry listEntry = new ListEntry();
        listEntry.setId(id);
        listEntry.setSyncId(syncId);
        listEntry.setTitle(title);
        listEntry.setDescription(description);
        listEntry.setColor(color);
        listEntry.setImageUrl(imageUrl);
        listEntry.setCreated(created);
        listEntry.setEdited(edited);
        listEntry.setViewed(viewed);

        final ListEntry listEntryTwo = new ListEntry();
        listEntryTwo.setId(id);
        listEntryTwo.setSyncId(syncId);
        listEntryTwo.setTitle(title);
        listEntryTwo.setDescription(description);
        listEntryTwo.setColor(color);
        listEntryTwo.setImageUrl(imageUrl);
        listEntryTwo.setCreated(created);
        listEntryTwo.setEdited(edited);
        listEntryTwo.setViewed(viewed);

        final ListEntry listEntryThree = new ListEntry();
        listEntryTwo.setId(id);
        listEntryTwo.setSyncId(syncId);
        listEntryTwo.setTitle(title);
        listEntryTwo.setDescription(description);
        listEntryTwo.setColor(color);
        listEntryTwo.setImageUrl(imageUrl);
        listEntryTwo.setCreated(created);
        listEntryTwo.setEdited(edited);

        assertTrue(listEntry.equals(listEntryTwo));
        assertTrue(listEntry.equals(listEntry));
        assertFalse(listEntry.equals(listEntryThree));
        assertFalse(listEntry.equals(null));
    }

    @Test
    public void hashCodeTest() throws Exception {
        final Long id           = 1L;
        final Long syncId       = 2L;
        final String title      = "title";
        final String description = "description";
        final Integer color     = 123456;
        final String imageUrl   = "http://jpg.jpg.jpg";
        final Date created      = new Date(System.currentTimeMillis());
        final Date edited       = new Date(System.currentTimeMillis());
        final Date viewed       = new Date(System.currentTimeMillis());

        final ListEntry listEntry = new ListEntry();
        listEntry.setId(id);
        listEntry.setSyncId(syncId);
        listEntry.setTitle(title);
        listEntry.setDescription(description);
        listEntry.setColor(color);
        listEntry.setImageUrl(imageUrl);
        listEntry.setCreated(created);
        listEntry.setEdited(edited);
        listEntry.setViewed(viewed);

        final ListEntry listEntryTwo = new ListEntry();
        listEntryTwo.setId(id);
        listEntryTwo.setSyncId(syncId);
        listEntryTwo.setTitle(title);
        listEntryTwo.setDescription(description);
        listEntryTwo.setColor(color);
        listEntryTwo.setImageUrl(imageUrl);
        listEntryTwo.setCreated(created);
        listEntryTwo.setEdited(edited);
        listEntryTwo.setViewed(viewed);

        final ListEntry listEntryThree = new ListEntry();
        listEntryTwo.setId(id);
        listEntryTwo.setSyncId(syncId);
        listEntryTwo.setTitle(title);
        listEntryTwo.setDescription(description);
        listEntryTwo.setColor(color);
        listEntryTwo.setImageUrl(imageUrl);
        listEntryTwo.setCreated(created);
        listEntryTwo.setEdited(edited);

        assertEquals(listEntry.hashCode(), listEntryTwo.hashCode());
        assertNotEquals(listEntry.hashCode(), listEntryThree.hashCode());
    }


    /*
        Utils
     */

    @NonNull
    private static String getNoteInstance() {
        return "{\"title\": \"Yelena Yeremeyeva\", "
                + "\"color\": \"#0010FF\", "
                + "\"imageUrl\": \"https:\\/\\/developer.android.com\\/images\\/brand\\/Android_Robot_100.png\", "
                + "\"description\": \"butterfly galaxy Peace Smile bubble cosmopolitan cosy Love \", "
                + "\"created\": \"2017-05-05T02:25:35+05:00\", "
                + "\"edited\": \"2017-05-05T02:25:35+05:00\", "
                + "\"viewed\": \"2017-05-08T02:25:35+05:00\"}";
    }
}
