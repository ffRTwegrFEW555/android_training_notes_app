package com.gamaliev.notes.user;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import java.util.Map;
import java.util.Set;

import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_USER_SELECTED;
import static com.gamaliev.notes.common.observers.ObserverHelper.USERS;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;
import static com.gamaliev.notes.common.shared_prefs.SpUsers.SP_USER_ID;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class UserRecyclerViewAdapter
        extends RecyclerView.Adapter<UserRecyclerViewAdapter.ViewHolder> {

    /* Logger */
    @NonNull private static final String TAG = UserRecyclerViewAdapter.class.getSimpleName();

    /* ... */
    @NonNull private final Fragment mFragment;
    @SuppressWarnings("NullableProblems")
    @NonNull private String[] mProfiles;


    /*
        Init
     */

    UserRecyclerViewAdapter(@NonNull final Fragment fragment) {
        mFragment = fragment;
        updateProfiles();
    }


    /*
        Lifecycle
     */

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.fragment_user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Context context = mFragment.getContext();
        final String userId = mProfiles[position];

        final Map<String, String> userProfile = SpUsers.get(context, userId);
        holder.mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpUsers.setSelected(context, userId);
                notifyObservers(USERS, RESULT_CODE_USER_SELECTED, null);
            }
        });
        //noinspection SetTextI18n
        holder.mTitleView.setText(
                userProfile.get(SpUsers.SP_USER_FIRST_NAME) + " "
                        + userProfile.get(SpUsers.SP_USER_LAST_NAME) + " "
                        + userProfile.get(SpUsers.SP_USER_MIDDLE_NAME));
        holder.mDescriptionEmailView.setText(userProfile.get(SpUsers.SP_USER_EMAIL));
        holder.mDescriptionView.setText(userProfile.get(SpUsers.SP_USER_DESCRIPTION));
        holder.mConfigureImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final UserPreferenceFragment fragment
                        = UserPreferenceFragment.newInstance(userId);
                mFragment.getActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.activity_main_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Colorize current user.
        final String selectedId = SpUsers.getSelected(context);
        if (selectedId == null) {
            Log.e(TAG, "Selected User ID is null.");
        } else {
            final String currentId = userProfile.get(SP_USER_ID);
            if (selectedId.equals(currentId)) {
                holder.mTitleView.setTextColor(
                        CommonUtils.getResourceColorApi(context, R.color.colorPrimary));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mProfiles.length;
    }


    /*
        ViewHolder
     */

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View          mParentView;
        private final TextView      mTitleView;
        private final TextView      mDescriptionEmailView;
        private final TextView      mDescriptionView;
        private final ImageButton   mConfigureImageButton;

        private ViewHolder(@NonNull final View view) {
            super(view);

            mParentView             = view;
            mTitleView              = (TextView) view.findViewById(R.id.fragment_user_item_title);
            mDescriptionEmailView   = (TextView) view.findViewById(R.id.fragment_user_item_description_email);
            mDescriptionView        = (TextView) view.findViewById(R.id.fragment_user_item_description);
            mConfigureImageButton   = (ImageButton) view.findViewById(R.id.fragment_user_item_configure_button);
        }
    }


    /*
        ...
     */

    private void updateProfiles() {
        final Set<String> profiles = SpUsers.getProfiles(mFragment.getContext());
        mProfiles = new String[profiles.size()];
        profiles.toArray(mProfiles);
    }
}
