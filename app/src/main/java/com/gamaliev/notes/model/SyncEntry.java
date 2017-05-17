package com.gamaliev.notes.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class SyncEntry implements Parcelable {

    /* Logger */
    private static final String TAG = SyncEntry.class.getSimpleName();

    /* ... */
    @Nullable private Long      mId;
    @Nullable private Date      mFinished;
    @Nullable private Integer   mStatus;
    @Nullable private Integer   mAmount;

    // What to Write and Read flags.
    private static final int RW_ID          = 1;
    private static final int RW_FINISHED    = 2;
    private static final int RW_STATUS      = 4;
    private static final int RW_AMOUNT      = 8;

    public SyncEntry() {}


    /*
        Parcelable
     */

    protected SyncEntry(Parcel in) {

        int whatToRead = in.readInt();
        if ((whatToRead & RW_ID) > 0)           mId = in.readLong();
        if ((whatToRead & RW_FINISHED) > 0)     mFinished = (Date) in.readSerializable();
        if ((whatToRead & RW_STATUS) > 0)       mStatus = in.readInt();
        if ((whatToRead & RW_AMOUNT) > 0)       mAmount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        // Compute what to write to parcel.
        int whatToWrite = 0;
        if (mId != null)        whatToWrite |= RW_ID;
        if (mFinished != null)  whatToWrite |= RW_FINISHED;
        if (mStatus != null)    whatToWrite |= RW_STATUS;
        if (mAmount != null)    whatToWrite |= RW_AMOUNT;

        // Write computed to parcel.
        dest.writeInt(whatToWrite);
        if (mId != null)        dest.writeLong(mId);
        if (mFinished != null)  dest.writeSerializable(mFinished);
        if (mStatus != null)    dest.writeInt(mStatus);
        if (mAmount != null)    dest.writeInt(mAmount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SyncEntry> CREATOR = new Creator<SyncEntry>() {
        @Override
        public SyncEntry createFromParcel(Parcel in) {
            return new SyncEntry(in);
        }

        @Override
        public SyncEntry[] newArray(int size) {
            return new SyncEntry[size];
        }
    };


    /*
        Setters
     */

    public void setId(@NonNull final Long id) {
        mId = id;
    }

    public void setFinished(@NonNull final Date finished) {
        mFinished = finished;
    }

    public void setStatus(@NonNull final Integer status) {
        mStatus = status;
    }

    public void setAmount(@NonNull final Integer amount) {
        mAmount = amount;
    }


    /*
        Getters
     */

    @Nullable
    public Long getId() {
        return mId;
    }

    @Nullable
    public Date getFinished() {
        return mFinished;
    }

    @Nullable
    public Integer getStatus() {
        return mStatus;
    }

    @Nullable
    public Integer getAmount() {
        return mAmount;
    }
}
