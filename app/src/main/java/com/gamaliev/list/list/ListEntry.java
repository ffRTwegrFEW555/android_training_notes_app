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

class ListEntry implements Parcelable {

    @Nullable private Integer id;
    @Nullable private String name;
    @Nullable private String description;
    @Nullable private Integer color;
    @Nullable private Date createdDate;
    @Nullable private Date modifiedDate;

    // What to Write and Read flags.
    private static final int W_ID           = 1;
    private static final int W_NAME         = 2;
    private static final int W_DESCRIPTION  = 4;
    private static final int W_COLOR        = 8;
    private static final int W_CREATED_DATE = 16;
    private static final int W_MODIFIED_DATE = 32;

    ListEntry() {}


    /*
        Parcelable
     */

    protected ListEntry(Parcel in) {

        int whatToRead = in.readInt();
        if ((whatToRead & W_ID) > 0)            id = in.readInt();
        if ((whatToRead & W_NAME) > 0)          name = in.readString();
        if ((whatToRead & W_DESCRIPTION) > 0)   description = in.readString();
        if ((whatToRead & W_COLOR) > 0)         color = in.readInt();
        if ((whatToRead & W_CREATED_DATE) > 0)  createdDate = (Date) in.readSerializable();
        if ((whatToRead & W_MODIFIED_DATE) > 0) modifiedDate = (Date) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        int whatToWrite = 0;
        if (id != null)             whatToWrite |= W_ID;
        if (name != null)           whatToWrite |= W_NAME;
        if (description != null)    whatToWrite |= W_DESCRIPTION;
        if (color != null)          whatToWrite |= W_COLOR;
        if (createdDate != null)    whatToWrite |= W_CREATED_DATE;
        if (modifiedDate != null)   whatToWrite |= W_MODIFIED_DATE;

        dest.writeInt(whatToWrite);
        if (id != null)             dest.writeInt(id);
        if (name != null)           dest.writeString(name);
        if (description != null)    dest.writeString(description);
        if (color != null)          dest.writeInt(color);
        if (createdDate != null)    dest.writeSerializable(createdDate);
        if (modifiedDate != null)   dest.writeSerializable(modifiedDate);
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

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    public void setColor(@NonNull Integer color) {
        this.color = color;
    }

    public void setCreatedDate(@NonNull Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setModifiedDate(@NonNull Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }


    /*
        Getters
     */

    @Nullable
    public Integer getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public Integer getColor() {
        return color;
    }

    @Nullable
    public Date getCreatedDate() {
        return createdDate;
    }

    @Nullable
    public Date getModifiedDate() {
        return modifiedDate;
    }
}
