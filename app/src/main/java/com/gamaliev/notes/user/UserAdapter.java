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
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import java.util.Map;
import java.util.Set;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class UserAdapter extends BaseAdapter {

    /* Logger */
    private static final String TAG = UserAdapter.class.getSimpleName();

    /* Intents */
    private static final int REQUEST_CODE_CONFIGURE_USER = 101;

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

        // Get or new.
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.activity_user_item, parent, false);

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Update.
        final Map<String, String> userProfile = SpUsers.get(parent.getContext(), mProfiles[position]);
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
                        parent.getContext(),
                        REQUEST_CODE_CONFIGURE_USER,
                        mProfiles[position]);
            }
        });

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
