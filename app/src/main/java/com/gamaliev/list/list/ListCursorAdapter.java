package com.gamaliev.list.list;

import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
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
        String name         = cursor.getString(1);
        String description  = cursor.getString(2);
        int color           = cursor.getInt(3);

        viewHolder.id = id;
        viewHolder.nameView.setText(name);
        viewHolder.descriptionView.setText(description);
        viewHolder.iconView
                .getBackground()
                .setColorFilter(color, PorterDuff.Mode.SRC);
    }

    private static class ViewHolder {
        private Integer id;
        private final TextView nameView;
        private final TextView descriptionView;
        private final View iconView;

        ViewHolder(View view) {
            nameView = (TextView) view.findViewById(R.id.activity_list_item_name);
            descriptionView = (TextView) view.findViewById(R.id.activity_list_item_description);
            iconView = view.findViewById(R.id.activity_list_item_icon);
        }
    }
}
