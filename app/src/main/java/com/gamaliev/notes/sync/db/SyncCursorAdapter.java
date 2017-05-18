package com.gamaliev.notes.sync.db;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.db.DbHelper;

/**
 * @author Vadim Gamaliev
 * <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public final class SyncCursorAdapter extends CursorAdapter {

    public SyncCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        // Create new view, new view holder, and binding.
        final View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.activity_sync_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Get view holder.
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Get values from current row.
        final int indexFinished = cursor.getColumnIndex(DbHelper.SYNC_COLUMN_FINISHED);
        final int indexAction   = cursor.getColumnIndex(DbHelper.SYNC_COLUMN_ACTION);
        final int indexStatus   = cursor.getColumnIndex(DbHelper.SYNC_COLUMN_STATUS);
        final int indexAmount   = cursor.getColumnIndex(DbHelper.SYNC_COLUMN_AMOUNT);

        final String finished   = CommonUtils
                .convertUtcToLocal(context, cursor.getString(indexFinished));
        final int actionIndex   = Integer.parseInt(cursor.getString(indexAction));
        final String action     = context.getString(SyncDbHelper.ACTION_TEXT[actionIndex]);
        final int statusIndex   = Integer.parseInt(cursor.getString(indexStatus));
        final String status     = context.getString(SyncDbHelper.STATUS_TEXT[statusIndex]);
        final String amount     = cursor.getString(indexAmount);

        final String description =
                context.getString(R.string.activity_sync_item_status_prefix) + ": "
                        + action + ", "
                        + status + " ("
                        + amount + ")";

        // Fill view holder values.
        viewHolder.mFinishedView.setText(finished);
        viewHolder.mDescriptionView.setText(description);
    }

    // View holder, associated with activity_list_item.xml
    private static class ViewHolder {
        private final TextView mFinishedView;
        private final TextView mDescriptionView;

        ViewHolder(View view) {
            mFinishedView       = (TextView) view.findViewById(R.id.activity_sync_item_finished);
            mDescriptionView    = (TextView) view.findViewById(R.id.activity_sync_item_description);
        }
    }
}
