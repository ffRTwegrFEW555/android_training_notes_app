package com.gamaliev.notes.entity;

import android.os.Parcel;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class SyncEntryTest {

    @Test
    public void settersGetters() throws Exception {
        final Date finished     = new Date(System.currentTimeMillis());
        final Integer action    = 2;
        final Integer status    = 3;
        final Integer amount    = 4;

        final SyncEntry syncEntry = new SyncEntry();
        syncEntry.setFinished(finished);
        syncEntry.setAction(action);
        syncEntry.setStatus(status);
        syncEntry.setAmount(amount);

        assertEquals(finished,  syncEntry.getFinished());
        assertEquals(action,    syncEntry.getAction());
        assertEquals(status,    syncEntry.getStatus());
        assertEquals(amount,    syncEntry.getAmount());
    }

    @Test
    public void parcelable() throws Exception {
        final Date finished     = new Date(System.currentTimeMillis());
        final Integer action    = 2;
        final Integer status    = 3;
        final Integer amount    = 4;

        final SyncEntry syncEntry = new SyncEntry();
        syncEntry.setFinished(finished);
        syncEntry.setAction(action);
        syncEntry.setStatus(status);
        syncEntry.setAmount(amount);

        final Parcel parcel = Parcel.obtain();
        syncEntry.writeToParcel(parcel, syncEntry.describeContents());
        parcel.setDataPosition(0);
        final SyncEntry parcelSyncEntry = SyncEntry.CREATOR.createFromParcel(parcel);

        // #1
        assertEquals(syncEntry, parcelSyncEntry);

        final SyncEntry syncEntryNotFull = new SyncEntry();
        syncEntryNotFull.setAction(action);
        syncEntryNotFull.setAmount(amount);

        final Parcel parcelNotFull = Parcel.obtain();
        syncEntryNotFull.writeToParcel(parcelNotFull, syncEntryNotFull.describeContents());
        parcelNotFull.setDataPosition(0);
        final SyncEntry parcelSyncEntryNotFull = SyncEntry.CREATOR.createFromParcel(parcelNotFull);

        // #2
        assertEquals(syncEntryNotFull, parcelSyncEntryNotFull);

        final SyncEntry[] listEntries = SyncEntry.CREATOR.newArray(5);

        // #3
        assertEquals(listEntries.length, 5);
    }

    @SuppressWarnings({"EqualsWithItself", "ObjectEqualsNull", "PMD.EqualsNull"})
    @Test
    public void equalsTest() throws Exception {
        final Date finished     = new Date(System.currentTimeMillis());
        final Integer action    = 2;
        final Integer status    = 3;
        final Integer amount    = 4;

        final SyncEntry syncEntry = new SyncEntry();
        syncEntry.setFinished(finished);
        syncEntry.setAction(action);
        syncEntry.setStatus(status);
        syncEntry.setAmount(amount);

        final SyncEntry syncEntryTwo = new SyncEntry();
        syncEntryTwo.setFinished(finished);
        syncEntryTwo.setAction(action);
        syncEntryTwo.setStatus(status);
        syncEntryTwo.setAmount(amount);

        final SyncEntry syncEntryThree = new SyncEntry();
        syncEntryThree.setFinished(finished);
        syncEntryThree.setAction(action);
        syncEntryThree.setStatus(status);

        assertTrue(syncEntry.equals(syncEntryTwo));
        assertTrue(syncEntry.equals(syncEntry));
        assertFalse(syncEntry.equals(syncEntryThree));
        assertFalse(syncEntry.equals(null));
    }

    @Test
    public void hashCodeTest() throws Exception {
        final Date finished     = new Date(System.currentTimeMillis());
        final Integer action    = 2;
        final Integer status    = 3;
        final Integer amount    = 4;

        final SyncEntry syncEntry = new SyncEntry();
        syncEntry.setFinished(finished);
        syncEntry.setAction(action);
        syncEntry.setStatus(status);
        syncEntry.setAmount(amount);

        final SyncEntry syncEntryTwo = new SyncEntry();
        syncEntryTwo.setFinished(finished);
        syncEntryTwo.setAction(action);
        syncEntryTwo.setStatus(status);
        syncEntryTwo.setAmount(amount);

        final SyncEntry syncEntryThree = new SyncEntry();
        syncEntryThree.setFinished(finished);
        syncEntryThree.setAction(action);
        syncEntryThree.setStatus(status);

        assertEquals(syncEntry.hashCode(), syncEntryTwo.hashCode());
        assertNotEquals(syncEntry.hashCode(), syncEntryThree.hashCode());
    }

    @Test
    public void toStringTest() throws Exception {
        assertNotNull(new SyncEntry().toString());
    }
}
