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

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

final class ListCursorAdapter extends CursorAdapter {

    ListCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags); // TODO: flags
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater
                .from(context)
                .inflate(R.layout.activity_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int id              = cursor.getInt(0);
        String title        = cursor.getString(1);
        String description  = cursor.getString(2);
        int color           = cursor.getInt(3);

        viewHolder.id = id;
        viewHolder.titleView.setText(title);
        viewHolder.descriptionView.setText(description);
        viewHolder.iconView
                .getBackground()
                .setColorFilter(color, PorterDuff.Mode.SRC);
    }

    private static class ViewHolder {
        private Integer         id;
        private final TextView  titleView;
        private final TextView  descriptionView;
        private final View      iconView;

        ViewHolder(View view) {
            titleView       = (TextView) view.findViewById(R.id.activity_list_item_title);
            descriptionView = (TextView) view.findViewById(R.id.activity_list_item_description);
            iconView        = view.findViewById(R.id.activity_list_item_color);
        }
    }
}
