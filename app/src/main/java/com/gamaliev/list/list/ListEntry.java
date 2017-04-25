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

    @Nullable private Integer   id;
    @Nullable private String    title;
    @Nullable private String    description;
    @Nullable private Integer   color;
    @Nullable private Date      created;
    @Nullable private Date      edited;
    @Nullable private Date      viewed;

    // What to Write and Read flags.
    private static final int RW_ID          = 1;
    private static final int RW_TITLE       = 2;
    private static final int RW_DESCRIPTION = 4;
    private static final int RW_COLOR       = 8;
    private static final int RW_CREATED     = 16;
    private static final int RW_EDITED      = 32;
    private static final int RW_VIEWED      = 64;

    ListEntry() {}


    /*
        Parcelable
     */

    protected ListEntry(Parcel in) {

        int whatToRead = in.readInt();
        if ((whatToRead & RW_ID) > 0)           id          = in.readInt();
        if ((whatToRead & RW_TITLE) > 0)        title       = in.readString();
        if ((whatToRead & RW_DESCRIPTION) > 0)  description = in.readString();
        if ((whatToRead & RW_COLOR) > 0)        color       = in.readInt();
        if ((whatToRead & RW_CREATED) > 0)      created     = (Date) in.readSerializable();
        if ((whatToRead & RW_EDITED) > 0)       edited      = (Date) in.readSerializable();
        if ((whatToRead & RW_VIEWED) > 0)       viewed      = (Date) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        int whatToWrite = 0;
        if (id != null)             whatToWrite |= RW_ID;
        if (title != null)          whatToWrite |= RW_TITLE;
        if (description != null)    whatToWrite |= RW_DESCRIPTION;
        if (color != null)          whatToWrite |= RW_COLOR;
        if (created != null)        whatToWrite |= RW_CREATED;
        if (edited != null)         whatToWrite |= RW_EDITED;
        if (viewed != null)         whatToWrite |= RW_VIEWED;

        dest.writeInt(whatToWrite);
        if (id != null)             dest.writeInt(id);
        if (title != null)          dest.writeString(title);
        if (description != null)    dest.writeString(description);
        if (color != null)          dest.writeInt(color);
        if (created != null)        dest.writeSerializable(created);
        if (edited != null)         dest.writeSerializable(edited);
        if (viewed != null)         dest.writeSerializable(viewed);
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

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    public void setColor(@NonNull Integer color) {
        this.color = color;
    }

    public void setCreated(@NonNull Date created) {
        this.created = created;
    }

    public void setEdited(@NonNull Date edited) {
        this.edited = edited;
    }

    public void setViewed(@NonNull Date viewed) {
        this.viewed = viewed;
    }


    /*
        Getters
     */

    @Nullable
    public Integer getId() {
        return id;
    }

    @Nullable
    public String getTitle() {
        return title;
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
    public Date getCreated() {
        return created;
    }

    @Nullable
    public Date getEdited() {
        return edited;
    }

    @Nullable
    public Date getViewed() {
        return viewed;
    }
}
