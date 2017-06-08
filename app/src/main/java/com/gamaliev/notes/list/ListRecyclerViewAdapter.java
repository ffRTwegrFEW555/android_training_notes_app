package com.gamaliev.notes.list;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
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

import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_LIST_FILTERED;
import static com.gamaliev.notes.common.observers.ObserverHelper.LIST_FILTER;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.SP_FILTER_PROFILE_MANUAL_ID;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.getSelectedIdForCurrentUser;
import static com.gamaliev.notes.common.shared_prefs.SpFilterProfiles.setSelectedForCurrentUser;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("NullableProblems")
final class ListRecyclerViewAdapter
        extends RecyclerView.Adapter<ListRecyclerViewAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final ListContract.Presenter mPresenter;
    @NonNull private final OnStartDragListener mDragStartListener;
    @SuppressWarnings("unused")
    private boolean mSwipeEnable;


    /*
        Init
     */

    ListRecyclerViewAdapter(
            @NonNull final FragmentActivity activity,
            @NonNull final ListContract.Presenter presenter,
            @NonNull final OnStartDragListener dragStartListener) {

        mContext = activity;
        mPresenter = presenter;
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
        final Cursor cursor = mPresenter.getCursor();
        if (cursor == null || !cursor.moveToPosition(position)) {
            return;
        }

        final int indexId           = cursor.getColumnIndex(DbHelper.BASE_COLUMN_ID);
        final int indexTitle        = cursor.getColumnIndex(DbHelper.LIST_ITEMS_COLUMN_TITLE);
        final int indexDescription  = cursor.getColumnIndex(DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION);
        final int indexEdited       = cursor.getColumnIndex(DbHelper.LIST_ITEMS_COLUMN_EDITED);
        final int indexColor        = cursor.getColumnIndex(DbHelper.LIST_ITEMS_COLUMN_COLOR);

        final long id               = cursor.getLong(indexId);
        final String title          = cursor.getString(indexTitle);
        final String description    = cursor.getString(indexDescription);
        final String localDate      = CommonUtils
                .convertUtcToLocal(mContext, cursor.getString(indexEdited));
        final String edited         = localDate == null ? null : localDate.split(" ")[0];
        final int color             = cursor.getInt(indexColor);

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
                        mContext.getString(R.string.shared_transition_name_color_box);
                final String viewTransName =
                        mContext.getString(R.string.shared_transition_name_layout);
                ViewCompat.setTransitionName(colorView, colorTransName);
                ViewCompat.setTransitionName(v, viewTransName);

                mPresenter.getSupportFragmentManager()
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
        return mPresenter.getItemCount();
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
        ItemTouchHelperAdapter
     */

    @Override
    public boolean swipeEnable() {
        return mSwipeEnable;
    }

    @Override
    public boolean dragDropEnable() {
        if (SP_FILTER_PROFILE_MANUAL_ID.equals(getSelectedIdForCurrentUser(mContext))) {
            return true;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder .setTitle(mContext.getString(R.string.fragment_list_drag_drop_dialog_title))
                .setMessage(mContext.getString(R.string.fragment_list_drag_drop_dialog_message))
                .setPositiveButton(mContext.getString(R.string.fragment_list_drag_drop_dialog_button_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                setSelectedForCurrentUser(mContext, SP_FILTER_PROFILE_MANUAL_ID);
                                notifyObservers(
                                        LIST_FILTER,
                                        RESULT_CODE_LIST_FILTERED,
                                        null);
                            }
                        })
                .setNegativeButton(
                        mContext.getString(R.string.fragment_list_drag_drop_dialog_button_cancel),
                        null)
                .create()
                .show();

        return false;
    }

    @Override
    public void onItemDismiss(final int position) {
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(final int fromPosition, final int toPosition) {
        if (!mPresenter.swapItems(fromPosition, toPosition)) {
            return false;
        }

        notifyItemMoved(fromPosition, toPosition);
        return true;
    }
}
