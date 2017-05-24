package com.gamaliev.notes.list.db;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import java.util.Collections;
import java.util.Map;

import static com.gamaliev.notes.item_details.ItemDetailsPagerItemFragment.ACTION_EDIT;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class ListRecyclerViewAdapter
        extends RecyclerView.Adapter<ListRecyclerViewAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {

    /* Logger */
    private static final String TAG = ListRecyclerViewAdapter.class.getSimpleName();

    /* ... */
    @NonNull private final Fragment mFragment;
    @NonNull private final OnStartDragListener mDragStartListener;
    @Nullable private Cursor mCursor;



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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        final View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        mCursor.moveToPosition(position);
        final Context context = holder.mParentView.getContext();

        // Get values from current row.
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

        // Fill view holder values.
        holder.mTitleView       .setText(title);
        holder.mDescriptionView .setText(description);
        holder.mEditedView      .setText(edited);
        holder.mColorView
                .getBackground()
                .setColorFilter(color, PorterDuff.Mode.SRC);

        //
        holder.mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Start item details fragment, with edit action.
                final ItemDetailsFragment fragment =
                        ItemDetailsFragment.newInstance(ACTION_EDIT, id);

                // Init transitions.
                final View colorView = v.findViewById(R.id.fragment_list_item_color);
                final String colorTransName =
                        context.getString(R.string.shared_transition_name_color_box);
                final String viewTransName =
                        context.getString(R.string.shared_transition_name_layout);
                ViewCompat.setTransitionName(colorView, colorTransName);
                ViewCompat.setTransitionName(v, viewTransName);

                mFragment.setExitTransition(new Fade());
                fragment.setEnterTransition(new Fade());
                fragment.setSharedElementEnterTransition(new AutoTransition());
                fragment.setSharedElementReturnTransition(new AutoTransition());

                // Start fragment.
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

        //
        holder.mParentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mDragStartListener.onStartDrag(holder);
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
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

        private ViewHolder(View view) {
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
            @NonNull final Context context,
            @NonNull final String constraint,
            @NonNull final Map<String, String> filterProfileMap) {

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = ListDbHelper.getCursorWithParams(
                context,
                constraint,
                filterProfileMap);
    }


    /*
        ItemTouchHelperAdapter
     */

    @Override
    public void onItemDismiss(int position) {
//        mItems.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
//        Collections.swap(mItems, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }
}
