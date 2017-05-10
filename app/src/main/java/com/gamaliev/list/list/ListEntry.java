package com.gamaliev.list.list;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import static com.gamaliev.list.common.CommonUtils.getDateFromISO8601String;
import static com.gamaliev.list.common.CommonUtils.getStringDateISO8601;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_COLOR;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_IMAGE_URL;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.list.common.database.DatabaseHelper.LIST_ITEMS_COLUMN_VIEWED;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ListEntry implements Parcelable {

    /* Logger */
    private static final String TAG = ListEntry.class.getSimpleName();

    /* ... */
    @Nullable private Long      mId;
    @Nullable private String    mTitle;
    @Nullable private String    mDescription;
    @Nullable private Integer   mColor;
    @Nullable private String    mImageUrl;
    @Nullable private Date      mCreated;
    @Nullable private Date      mEdited;
    @Nullable private Date      mViewed;

    // What to Write and Read flags.
    private static final int RW_ID          = 1;
    private static final int RW_TITLE       = 2;
    private static final int RW_DESCRIPTION = 4;
    private static final int RW_COLOR       = 8;
    private static final int RW_IMAGE_URL   = 16;
    private static final int RW_CREATED     = 32;
    private static final int RW_EDITED      = 64;
    private static final int RW_VIEWED      = 128;

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
        if ((whatToRead & RW_IMAGE_URL) > 0)    mImageUrl = in.readString();
        if ((whatToRead & RW_CREATED) > 0)      mCreated = (Date) in.readSerializable();
        if ((whatToRead & RW_EDITED) > 0)       mEdited = (Date) in.readSerializable();
        if ((whatToRead & RW_VIEWED) > 0)       mViewed = (Date) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        // Compute what to write to parcel.
        int whatToWrite = 0;
        if (mId != null)            whatToWrite |= RW_ID;
        if (mTitle != null)         whatToWrite |= RW_TITLE;
        if (mDescription != null)   whatToWrite |= RW_DESCRIPTION;
        if (mColor != null)         whatToWrite |= RW_COLOR;
        if (mImageUrl != null)      whatToWrite |= RW_IMAGE_URL;
        if (mCreated != null)       whatToWrite |= RW_CREATED;
        if (mEdited != null)        whatToWrite |= RW_EDITED;
        if (mViewed != null)        whatToWrite |= RW_VIEWED;

        // Write computed to parcel.
        dest.writeInt(whatToWrite);
        if (mId != null)            dest.writeLong(mId);
        if (mTitle != null)         dest.writeString(mTitle);
        if (mDescription != null)   dest.writeString(mDescription);
        if (mColor != null)         dest.writeInt(mColor);
        if (mImageUrl != null)      dest.writeString(mImageUrl);
        if (mCreated != null)       dest.writeSerializable(mCreated);
        if (mEdited != null)        dest.writeSerializable(mEdited);
        if (mViewed != null)        dest.writeSerializable(mViewed);
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

    public void setId(@NonNull final Long id) {
        mId = id;
    }

    public void setTitle(@NonNull final String title) {
        mTitle = title;
    }

    public void setDescription(@NonNull final String description) {
        mDescription = description;
    }

    public void setColor(@NonNull final Integer color) {
        mColor = color;
    }

    public void setImageUrl(@NonNull final String imageUrl) {
        mImageUrl = imageUrl;
    }

    public void setCreated(@NonNull final Date created) {
        mCreated = created;
    }

    public void setEdited(@NonNull final Date edited) {
        mEdited = edited;
    }

    public void setViewed(@NonNull final Date viewed) {
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
    public String getImageUrl() {
        return mImageUrl;
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


    /*
        Util methods
     */

    /**
     * Get filled JSONObject with data from current cursor position.
     *
     * @param activity  Activity.
     * @param cursor    Cursor.
     *
     * @return Filled JSONObject with data from current cursor position.
     * @throws IllegalStateException Usually happens when the data has been changed.
     */
    @Nullable
    public static JSONObject getJsonObject(
            @NonNull final Activity activity,
            @NonNull final Cursor cursor) throws IllegalStateException {

        //
        final int indexTitle        = cursor.getColumnIndex(LIST_ITEMS_COLUMN_TITLE);
        final int indexDescription  = cursor.getColumnIndex(LIST_ITEMS_COLUMN_DESCRIPTION);
        final int indexColor        = cursor.getColumnIndex(LIST_ITEMS_COLUMN_COLOR);
        final int indexImageUrl     = cursor.getColumnIndex(LIST_ITEMS_COLUMN_IMAGE_URL);
        final int indexCreated      = cursor.getColumnIndex(LIST_ITEMS_COLUMN_CREATED);
        final int indexEdited       = cursor.getColumnIndex(LIST_ITEMS_COLUMN_EDITED);
        final int indexViewed       = cursor.getColumnIndex(LIST_ITEMS_COLUMN_VIEWED);

        final String title          = cursor.getString(indexTitle);
        final String color          = cursor.getString(indexColor);
        final String imageUrl       = cursor.getString(indexImageUrl);
        final String description    = cursor.getString(indexDescription);
        final String created        = cursor.getString(indexCreated);
        final String edited         = cursor.getString(indexEdited);
        final String viewed         = cursor.getString(indexViewed);

        //
        final JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(LIST_ITEMS_COLUMN_TITLE,         title);
            jsonObject.put(LIST_ITEMS_COLUMN_COLOR,         String.format(
                    "#%06X",
                    (0xFFFFFF & Integer.parseInt(color))));

            jsonObject.put(LIST_ITEMS_COLUMN_IMAGE_URL,    imageUrl);
            jsonObject.put(LIST_ITEMS_COLUMN_DESCRIPTION,  description);
            jsonObject.put(LIST_ITEMS_COLUMN_CREATED,      getStringDateISO8601(activity, created));
            jsonObject.put(LIST_ITEMS_COLUMN_EDITED,       getStringDateISO8601(activity, edited));
            jsonObject.put(LIST_ITEMS_COLUMN_VIEWED,       getStringDateISO8601(activity, viewed));

        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            return null;
        }

        //
        return jsonObject;
    }

    @NonNull
    public static ListEntry convertJsonToListEntry(
            @NonNull final Activity activity,
            @NonNull final JSONObject jsonObject) {

        //
        final String title          = jsonObject.optString(LIST_ITEMS_COLUMN_TITLE, null);
        final int color             = Color.parseColor(jsonObject.optString(
                LIST_ITEMS_COLUMN_COLOR, null));
        final String imageUrl       = jsonObject.optString(LIST_ITEMS_COLUMN_IMAGE_URL, null);
        final String description    = jsonObject.optString(LIST_ITEMS_COLUMN_DESCRIPTION, null);
        final String created        = jsonObject.optString(LIST_ITEMS_COLUMN_CREATED, null);
        final String edited         = jsonObject.optString(LIST_ITEMS_COLUMN_EDITED, null);
        final String viewed         = jsonObject.optString(LIST_ITEMS_COLUMN_VIEWED, null);

        //
        final ListEntry entry = new ListEntry();

        entry.setTitle(title);
        entry.setDescription(description);
        entry.setColor(color);
        entry.setImageUrl(imageUrl);
        entry.setCreated(getDateFromISO8601String(activity, created));
        entry.setEdited(getDateFromISO8601String(activity, edited));
        entry.setViewed(getDateFromISO8601String(activity, viewed));

        return entry;
    }


    /*
        Equals & Hashcode
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListEntry entry = (ListEntry) o;

        if (mId != null ? !mId.equals(entry.mId) : entry.mId != null) return false;
        if (mTitle != null ? !mTitle.equals(entry.mTitle) : entry.mTitle != null) return false;
        if (mDescription != null ? !mDescription.equals(entry.mDescription) : entry.mDescription != null)
            return false;
        if (mColor != null ? !mColor.equals(entry.mColor) : entry.mColor != null) return false;
        if (mImageUrl != null ? !mImageUrl.equals(entry.mImageUrl) : entry.mImageUrl != null)
            return false;
        if (mCreated != null ? !mCreated.equals(entry.mCreated) : entry.mCreated != null)
            return false;
        if (mEdited != null ? !mEdited.equals(entry.mEdited) : entry.mEdited != null) return false;
        return mViewed != null ? mViewed.equals(entry.mViewed) : entry.mViewed == null;

    }

    @Override
    public int hashCode() {
        int result = mId != null ? mId.hashCode() : 0;
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + (mDescription != null ? mDescription.hashCode() : 0);
        result = 31 * result + (mColor != null ? mColor.hashCode() : 0);
        result = 31 * result + (mImageUrl != null ? mImageUrl.hashCode() : 0);
        result = 31 * result + (mCreated != null ? mCreated.hashCode() : 0);
        result = 31 * result + (mEdited != null ? mEdited.hashCode() : 0);
        result = 31 * result + (mViewed != null ? mViewed.hashCode() : 0);
        return result;
    }
}
