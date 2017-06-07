package com.gamaliev.notes.item_details.pager_item;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import com.gamaliev.notes.entity.ListEntry;
import com.gamaliev.notes.list.db.ListDbHelper;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.CommonUtils.getDefaultColor;
import static com.gamaliev.notes.common.CommonUtils.getMainHandler;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.item_details.pager_item.ItemDetailsPagerItemFragment.ACTION_ENTRY_ADD;
import static com.gamaliev.notes.item_details.pager_item.ItemDetailsPagerItemFragment.ACTION_ENTRY_DELETE;
import static com.gamaliev.notes.item_details.pager_item.ItemDetailsPagerItemFragment.ACTION_ENTRY_EDIT;
import static com.gamaliev.notes.list.db.ListDbHelper.deleteEntry;
import static com.gamaliev.notes.list.db.ListDbHelper.insertUpdateEntry;
import static com.gamaliev.notes.list.db.ListDbHelper.updateEntry;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ItemDetailsPagerItemPresenter implements ItemDetailsPagerItemContract.Presenter {

    /* Logger */
    @NonNull private static final String TAG = ItemDetailsPagerItemPresenter.class.getSimpleName();

    /* Extra */
    @NonNull public static final String EXTRA_ENTRY = "ItemDetailsPagerItemPresenter.EXTRA_ENTRY";

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final ItemDetailsPagerItemContract.View mItemDetailsPagerItemView;

    @Nullable private ListEntry mEntry;
    private long mEntryId = -1;



    /*
        Init
     */

    public ItemDetailsPagerItemPresenter(
            @NonNull final ItemDetailsPagerItemContract.View itemDetailsPagerItemView) {

        mContext = getAppContext();
        mItemDetailsPagerItemView = itemDetailsPagerItemView;

        mItemDetailsPagerItemView.setPresenter(this);
    }


    /*
        ItemDetailsPagerItemContract.Presenter
     */

    @Override
    public void start() {}

    @NonNull
    @Override
    public ListEntry initializeNewEntry() {
        mEntry = new ListEntry();
        mEntry.setColor(getDefaultColor(mContext));
        return mEntry;
    }

    @Nullable
    @Override
    public ListEntry initializeEntryFromDb() {
        if (mEntryId == -1) {
            Log.e(TAG, "Entry id is undefined (-1).");
            return null;
        }
        return mEntry = ListDbHelper.getEntry(mContext, mEntryId);
    }

    @Nullable
    @Override
    public ListEntry getEntry() {
        return mEntry;
    }

    @Override
    public void setEntryId(final long entryId) {
        mEntryId = entryId;
        if (mEntry != null) {
            mEntry.setId(entryId);
        }
    }

    @Override
    public long getEntryId() {
        return mEntryId;
    }

    @Override
    public void refreshEntry(
            @NonNull final String title,
            @NonNull final String description,
            @NonNull final String imageUrl) {

        if (mEntry == null) {
            Log.e(TAG, "Entry is null.");
            return;
        }
        mEntry.setTitle(title);
        mEntry.setDescription(description);
        mEntry.setImageUrl(imageUrl);
    }

    @Override
    public void updateEntryViewed() {
        if (mEntry == null) {
            Log.e(TAG, "Entry is null.");
            return;
        }
        updateEntry(mContext, mEntry, LIST_ITEMS_COLUMN_VIEWED);
    }

    @Override
    public void loadImage(
            @NonNull final ImageView imageView,
            @NonNull final String pathToImage) {

        Picasso.with(mContext)
                .load(pathToImage)
                .fit()
                .centerInside()
                .into(imageView, new Callback() {

                    @Override
                    public void onSuccess() {
                        mItemDetailsPagerItemView.performSuccessLoadingImage();
                    }

                    @Override
                    public void onError() {
                        mItemDetailsPagerItemView.performErrorLoadingImage();
                    }
                });
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putParcelable(EXTRA_ENTRY, mEntry);
    }

    @Override
    public void setEntryFromSavedInstanceStateOrDefault(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            initializeNewEntry();
        } else {
            final ListEntry entry = savedInstanceState.getParcelable(EXTRA_ENTRY);
            if (entry == null) {
                initializeNewEntry();
            } else {
                mEntry = entry;
            }
        }
    }

    @Override
    public void startActionAsyncTask(@NonNull final String action) {
        mItemDetailsPagerItemView.showProgressBarAndHideMenuItems();

        if (mEntry == null) {
            Log.e(TAG, "Entry is null.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                mItemDetailsPagerItemView.refreshEntry();
                switch (action) {
                    case ACTION_ENTRY_ADD:
                        insertUpdateEntry(mContext, mEntry, false);
                        break;

                    case ACTION_ENTRY_EDIT:
                        updateEntryEdited();
                        break;

                    case ACTION_ENTRY_DELETE:
                        final Long id = mEntry.getId();
                        if (id == null) {
                            Log.e(TAG, "User id is null.");
                            break;
                        }
                        deleteEntry(mContext, id, true);
                        break;

                    default:
                        break;
                }

                getMainHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        mItemDetailsPagerItemView.finish(action);
                    }
                });
            }
        }).start();
    }


    /*
        ...
     */

    private void updateEntryEdited() {
        if (mEntry == null) {
            Log.e(TAG, "Entry is null.");
            return;
        }
        updateEntry(mContext, mEntry, LIST_ITEMS_COLUMN_EDITED);
    }
}
