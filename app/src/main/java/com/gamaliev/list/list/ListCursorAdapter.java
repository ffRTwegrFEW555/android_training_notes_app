package com.gamaliev.list.list;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gamaliev.list.R;
import com.gamaliev.list.common.DatabaseHelper;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class ListCursorAdapter extends CursorAdapter {

    ListCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        // Create new view, new view holder, and binding.
        final View view = LayoutInflater
                .from(context)
                .inflate(R.layout.activity_list_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Get view holder.
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Get values from current row.
        final int indexId           = cursor.getColumnIndex(DatabaseHelper.BASE_COLUMN_ID);
        final int indexTitle        = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_TITLE);
        final int indexDescription  = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_DESCRIPTION);
        final int indexEdited       = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_EDITED);
        final int indexColor        = cursor.getColumnIndex(DatabaseHelper.LIST_ITEMS_COLUMN_COLOR);

        final long id               = cursor.getLong(   indexId);
        final String title          = cursor.getString( indexTitle);
        final String description    = cursor.getString( indexDescription);
        final String edited         = cursor.getString( indexEdited).split(" ")[0];
        final int color             = cursor.getInt(    indexColor);

        // Fill view holder values.
        viewHolder.mId = id;
        viewHolder.mTitleView.setText(title);
        viewHolder.mDescriptionView.setText(description);
        viewHolder.mEditedView.setText(edited);
        viewHolder.mIconView
                .getBackground()
                .setColorFilter(color, PorterDuff.Mode.SRC);
    }

    // View holder, associated with activity_list_item.xml
    private static class ViewHolder {
        private Long            mId;
        private final TextView  mTitleView;
        private final TextView  mDescriptionView;
        private final TextView  mEditedView;
        private final View      mIconView;

        ViewHolder(View view) {
            mTitleView          = (TextView) view.findViewById(R.id.activity_list_item_title);
            mDescriptionView    = (TextView) view.findViewById(R.id.activity_list_item_description);
            mEditedView         = (TextView) view.findViewById(R.id.activity_list_item_edited);
            mIconView           = view.findViewById(R.id.activity_list_item_color);
        }
    }
}
