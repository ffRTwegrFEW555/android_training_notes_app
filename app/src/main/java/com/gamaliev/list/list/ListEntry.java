package com.gamaliev.list.list;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ListEntry implements Parcelable {

    @Nullable private Long      mId;
    @Nullable private String    mTitle;
    @Nullable private String    mDescription;
    @Nullable private Integer   mColor;
    @Nullable private Date      mCreated;
    @Nullable private Date      mEdited;
    @Nullable private Date      mViewed;

    // What to Write and Read flags.
    private static final int RW_ID          = 1;
    private static final int RW_TITLE       = 2;
    private static final int RW_DESCRIPTION = 4;
    private static final int RW_COLOR       = 8;
    private static final int RW_CREATED     = 16;
    private static final int RW_EDITED      = 32;
    private static final int RW_VIEWED      = 64;

    public ListEntry() {}


    /*
        Parcelable
     */

    protected ListEntry(Parcel in) {

        int whatToRead = in.readInt();
        if ((whatToRead & RW_ID) > 0)           mId = in.readLong();
        if ((whatToRead & RW_TITLE) > 0)        mTitle = in.readString();
        if ((whatToRead & RW_DESCRIPTION) > 0)  mDescription = in.readString();
        if ((whatToRead & RW_COLOR) > 0)        mColor = in.readInt();
        if ((whatToRead & RW_CREATED) > 0)      mCreated = (Date) in.readSerializable();
        if ((whatToRead & RW_EDITED) > 0)       mEdited = (Date) in.readSerializable();
        if ((whatToRead & RW_VIEWED) > 0)       mViewed = (Date) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        // Compute what to write to parcel.
        int whatToWrite = 0;
        if (mId != null)             whatToWrite |= RW_ID;
        if (mTitle != null)          whatToWrite |= RW_TITLE;
        if (mDescription != null)    whatToWrite |= RW_DESCRIPTION;
        if (mColor != null)          whatToWrite |= RW_COLOR;
        if (mCreated != null)        whatToWrite |= RW_CREATED;
        if (mEdited != null)         whatToWrite |= RW_EDITED;
        if (mViewed != null)         whatToWrite |= RW_VIEWED;

        // Write computed to parcel.
        dest.writeInt(whatToWrite);
        if (mId != null)             dest.writeLong(mId);
        if (mTitle != null)          dest.writeString(mTitle);
        if (mDescription != null)    dest.writeString(mDescription);
        if (mColor != null)          dest.writeInt(mColor);
        if (mCreated != null)        dest.writeSerializable(mCreated);
        if (mEdited != null)         dest.writeSerializable(mEdited);
        if (mViewed != null)         dest.writeSerializable(mViewed);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ListEntry> CREATOR = new Creator<ListEntry>() {
        @Override
        public ListEntry createFromParcel(Parcel in) {
            return new ListEntry(in);
        }

        @Override
        public ListEntry[] newArray(int size) {
            return new ListEntry[size];
        }
    };


    /*
        Setters
     */

    public void setId(@NonNull Long id) {
        mId = id;
    }

    public void setTitle(@NonNull String title) {
        mTitle = title;
    }

    public void setDescription(@NonNull String description) {
        mDescription = description;
    }

    public void setColor(@NonNull Integer color) {
        mColor = color;
    }

    public void setCreated(@NonNull Date created) {
        mCreated = created;
    }

    public void setEdited(@NonNull Date edited) {
        mEdited = edited;
    }

    public void setViewed(@NonNull Date viewed) {
        mViewed = viewed;
    }


    /*
        Getters
     */

    @Nullable
    public Long getId() {
        return mId;
    }

    @Nullable
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    public String getDescription() {
        return mDescription;
    }

    @Nullable
    public Integer getColor() {
        return mColor;
    }

    @Nullable
    public Date getCreated() {
        return mCreated;
    }

    @Nullable
    public Date getEdited() {
        return mEdited;
    }

    @Nullable
    public Date getViewed() {
        return mViewed;
    }
}
