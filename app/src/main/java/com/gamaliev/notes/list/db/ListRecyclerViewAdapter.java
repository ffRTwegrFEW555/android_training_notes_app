package com.gamaliev.notes.list.db;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.db.DbHelper;
import com.gamaliev.notes.common.recycler_view_item_touch_helper.ItemTouchHelperAdapter;
import com.gamaliev.notes.common.recycler_view_item_touch_helper.ItemTouchHelperViewHolder;
import com.gamaliev.notes.common.recycler_view_item_touch_helper.OnStartDragListener;
import com.gamaliev.notes.item_details.ItemDetailsFragment;

import java.util.Map;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_LIST_FILTERED;
import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.findColumnValueByCursorPosition;
import static com.gamaliev.notes.common.observers.ObserverHelper.LIST_FILTER;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_MANUAL_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.getSelectedIdForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.setSelectedForCurrentUser;
import static com.gamaliev.notes.list.db.ListDbHelper.getCursorWithParams;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public final class ListRecyclerViewAdapter
        extends RecyclerView.Adapter<ListRecyclerViewAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    /* ... */
    @NonNull private final Fragment mFragment;
    @NonNull private final OnStartDragListener mDragStartListener;
    @Nullable private Cursor mCursor;
    @NonNull private String mConstraint;
    @NonNull private Map<String, String> mFilterProfileMap;
    private boolean mSwipeEnable;


    /*
        Init
     */

    public ListRecyclerViewAdapter(
            @NonNull final Fragment fragment,
            @NonNull final OnStartDragListener dragStartListener) {

        mFragment = fragment;
        mDragStartListener = dragStartListener;
    }


    /*
        Lifecycle
     */

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (mCursor == null || !mCursor.moveToPosition(position)) {
            return;
        }

        final Context context       = holder.mParentView.getContext();

        final int indexId           = mCursor.getColumnIndex(DbHelper.BASE_COLUMN_ID);
        final int indexTitle        = mCursor.getColumnIndex(DbHelper.LIST_ITEMS_COLUMN_TITLE);
        final int indexDescription  = mCursor.getColumnIndex(DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION);
        final int indexEdited       = mCursor.getColumnIndex(DbHelper.LIST_ITEMS_COLUMN_EDITED);
        final int indexColor        = mCursor.getColumnIndex(DbHelper.LIST_ITEMS_COLUMN_COLOR);

        final long id               = mCursor.getLong(indexId);
        final String title          = mCursor.getString(indexTitle);
        final String description    = mCursor.getString(indexDescription);
        final String edited         = CommonUtils
                .convertUtcToLocal(context, mCursor.getString(indexEdited))
                .split(" ")[0];
        final int color             = mCursor.getInt(indexColor);

        holder.mTitleView       .setText(title);
        holder.mDescriptionView .setText(description);
        holder.mEditedView      .setText(edited);
        holder.mColorView
                .getBackground()
                .setColorFilter(color, PorterDuff.Mode.SRC);

        holder.mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ItemDetailsFragment fragment =
                        ItemDetailsFragment.newInstance(id);

                // Transitions.
                final View colorView = v.findViewById(R.id.fragment_list_item_color);
                final String colorTransName =
                        context.getString(R.string.shared_transition_name_color_box);
                final String viewTransName =
                        context.getString(R.string.shared_transition_name_layout);
                ViewCompat.setTransitionName(colorView, colorTransName);
                ViewCompat.setTransitionName(v, viewTransName);

                mFragment.getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .addSharedElement(v, viewTransName)
                        .addSharedElement(colorView, colorTransName)
                        .replace(R.id.activity_main_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Drag & Drop
        holder.mParentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                if (dragDropEnable()) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor == null || mCursor.isClosed() ? 0 : mCursor.getCount();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        closeCursor();
    }


    /*
        ViewHolder
     */

    static class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder {

        private final View      mParentView;
        private final TextView  mTitleView;
        private final TextView  mDescriptionView;
        private final TextView  mEditedView;
        private final View      mColorView;

        private ViewHolder(@NonNull final View view) {
            super(view);

            mParentView         = view;
            mTitleView          = (TextView) view.findViewById(R.id.fragment_list_item_title);
            mDescriptionView    = (TextView) view.findViewById(R.id.fragment_list_item_description);
            mEditedView         = (TextView) view.findViewById(R.id.fragment_list_item_edited);
            mColorView          = view.findViewById(R.id.fragment_list_item_color);
        }


        /*
            ItemTouchHelperViewHolder
         */

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }


    /*
        ...
     */

    public void updateCursor(
            @NonNull final String constraint,
            @NonNull final Map<String, String> filterProfileMap) {

        mConstraint = constraint;
        mFilterProfileMap = filterProfileMap;
        updateCursor();
    }

    private void updateCursor() {
        closeCursor();
        mCursor = getCursorWithParams(
                getAppContext(),
                mConstraint,
                mFilterProfileMap);
    }

    private void closeCursor() {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }


    /*
        ItemTouchHelperAdapter
     */

    @Override
    public boolean swipeEnable() {
        return mSwipeEnable;
    }

    @Override
    public boolean dragDropEnable() {
        final Context context = mFragment.getContext();

        if (!SP_FILTER_PROFILE_MANUAL_ID.equals(getSelectedIdForCurrentUser(context))) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder .setTitle(context.getString(R.string.fragment_list_drag_drop_dialog_title))
                    .setMessage(context.getString(R.string.fragment_list_drag_drop_dialog_message))
                    .setPositiveButton(context.getString(R.string.fragment_list_drag_drop_dialog_button_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    setSelectedForCurrentUser(context, SP_FILTER_PROFILE_MANUAL_ID);
                                    notifyObservers(
                                            LIST_FILTER,
                                            RESULT_CODE_LIST_FILTERED,
                                            null);
                                }
                            })
                    .setNegativeButton(
                            context.getString(R.string.fragment_list_drag_drop_dialog_button_cancel),
                            null)
                    .create()
                    .show();

            return false;
        }

        return true;
    }

    @Override
    public void onItemDismiss(final int position) {
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(final int fromPosition, final int toPosition) {
        if (mCursor == null || mCursor.isClosed()) {
            return false;
        }

        final String entryIdFrom = findColumnValueByCursorPosition(
                mCursor,
                BASE_COLUMN_ID,
                fromPosition);
        if (entryIdFrom == null) {
            return false;
        }

        final String entryIdTo = findColumnValueByCursorPosition(
                mCursor,
                BASE_COLUMN_ID,
                toPosition);
        if (entryIdTo == null) {
            return false;
        }

        ListDbHelper.swapManuallyColumnValue(
                getAppContext(),
                entryIdFrom,
                entryIdTo);

        updateCursor();
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }
}
