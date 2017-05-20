package com.gamaliev.notes.user;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import java.util.Map;
import java.util.Set;

import static com.gamaliev.notes.user.UserActivity.REQUEST_CODE_CONFIGURE_USER;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class UserAdapter extends BaseAdapter {

    /* Logger */
    private static final String TAG = UserAdapter.class.getSimpleName();

    /* ... */
    @NonNull private final String[] mProfiles;


    /*
        Init
     */

    public UserAdapter(@NonNull final Context context) {
        final Set<String> profiles = SpUsers.getProfiles(context);
        mProfiles = new String[profiles.size()];
        profiles.toArray(mProfiles);
    }


    /*
        ...
     */

    @Override
    public int getCount() {
        return mProfiles.length;
    }

    @Override
    public Object getItem(int position) {
        return mProfiles[position];
    }

    @Override
    public long getItemId(int position) {
        return Long.parseLong(mProfiles[position]);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        final Context context = parent.getContext();

        // Get or new.
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater
                    .from(context)
                    .inflate(R.layout.activity_user_item, parent, false);

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Update.
        final Map<String, String> userProfile = SpUsers.get(context, mProfiles[position]);
        holder.mTitleView.setText(
                        userProfile.get(SpUsers.SP_USER_FIRST_NAME) + " " +
                        userProfile.get(SpUsers.SP_USER_LAST_NAME) + " " +
                        userProfile.get(SpUsers.SP_USER_MIDDLE_NAME));
        holder.mDescriptionEmailView.setText(userProfile.get(SpUsers.SP_USER_EMAIL));
        holder.mDescriptionView.setText(userProfile.get(SpUsers.SP_USER_DESCRIPTION));
        holder.mConfigureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserPreferenceActivity.startIntent(
                        context,
                        REQUEST_CODE_CONFIGURE_USER,
                        mProfiles[position]);
            }
        });

        // Coloring current user
        final String selected = SpUsers.getSelected(context);
        final String fromProfile = userProfile.get(SpUsers.SP_USER_ID);
        if (selected.equals(fromProfile)) {
            holder.mTitleView.setTextColor(
                    CommonUtils.getResourceColorApi(context, R.color.colorPrimary));
        }

        return convertView;
    }

    private static class ViewHolder {
        private final TextView      mTitleView;
        private final TextView      mDescriptionEmailView;
        private final TextView      mDescriptionView;
        private final ImageButton   mConfigureImageButton;

        ViewHolder(View view) {
            mTitleView              = (TextView) view.findViewById(R.id.activity_user_item_title);
            mDescriptionEmailView   = (TextView) view.findViewById(R.id.activity_user_item_description_email);
            mDescriptionView        = (TextView) view.findViewById(R.id.activity_user_item_description);
            mConfigureImageButton   = (ImageButton) view.findViewById(R.id.activity_user_item_configure_button);
        }
    }
}
