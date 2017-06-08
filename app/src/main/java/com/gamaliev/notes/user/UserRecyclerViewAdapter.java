package com.gamaliev.notes.user;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.CommonUtils;
import com.gamaliev.notes.common.shared_prefs.SpUsers;

import java.util.Map;

import static com.gamaliev.notes.app.NotesApp.getAppContext;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class UserRecyclerViewAdapter
        extends RecyclerView.Adapter<UserRecyclerViewAdapter.ViewHolder> {

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final UserContract.Presenter mPresenter;


    /*
        Init
     */

    UserRecyclerViewAdapter(@NonNull final UserContract.Presenter presenter) {
        mContext = getAppContext();
        mPresenter = presenter;
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
        final String userId = mPresenter.getUserId(position);
        final Map<String, String> userProfile = mPresenter.getUserProfile(userId);

        holder.mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.selectUser(userId);
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
                mPresenter.startUserPreferenceFragment(userId);
            }
        });

        // Colorize current user.
        if (userId.equals(mPresenter.getSelectedUserId())) {
            holder.mTitleView.setTextColor(
                    CommonUtils.getResourceColorApi(mContext, R.color.colorPrimary));
        }
    }

    @Override
    public int getItemCount() {
        return mPresenter.getItemCount();
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
}
