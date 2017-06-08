package com.gamaliev.notes.main;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.gamaliev.notes.R;
import com.gamaliev.notes.common.FileUtils;
import com.gamaliev.notes.common.ProgressNotificationHelper;
import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.list.db.ListDbHelper;

import java.util.HashMap;
import java.util.Map;

import static com.gamaliev.notes.app.NotesApp.getAppContext;
import static com.gamaliev.notes.common.CommonUtils.showToastRunOnUiThread;
import static com.gamaliev.notes.common.codes.ResultCode.RESULT_CODE_MOCK_ENTRIES_ADDED;
import static com.gamaliev.notes.common.observers.ObserverHelper.ENTRIES_MOCK;
import static com.gamaliev.notes.common.observers.ObserverHelper.notifyObservers;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

class MainPresenter implements MainContract.Presenter {

    /* Logger */
    @NonNull private static final String TAG = MainPresenter.class.getSimpleName();

    /* ... */
    @NonNull private final Context mContext;
    @NonNull private final MainContract.View mMainView;


    /*
        Init
     */

    MainPresenter(@NonNull final MainContract.View mainView) {
        mContext = getAppContext();
        mMainView = mainView;

        mMainView.setPresenter(this);
    }


    /*
        MainContract.Presenter
     */

    @Override
    public void start() {}

    @Override
    public void initUserInfo() {
        final String userId = SpUsers.getSelected(mContext);
        Map<String, String> userProfile;
        if (userId == null) {
            Log.e(TAG, "User ID is null.");
            userProfile = new HashMap<>();
        } else {
            userProfile = SpUsers.get(mContext, userId);
        }
        mMainView.updateUserInfo(userProfile);
    }

    @Override
    public void importEntriesAsync(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile) {

        FileUtils.importEntriesAsync(activity, selectedFile);
    }

    @Override
    public void exportEntriesAsync(
            @NonNull final Activity activity,
            @NonNull final Uri selectedFile) {

        FileUtils.exportEntriesAsync(activity, selectedFile);
    }

    @Override
    public void addMockEntries(final int entriesCount) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                showToastRunOnUiThread(
                        mContext.getString(R.string.activity_main_notification_add_mock_entries_start),
                        Toast.LENGTH_SHORT);

                final ProgressNotificationHelper notification =
                        new ProgressNotificationHelper(
                                mContext,
                                mContext.getString(R.string.activity_main_notification_add_mock_title),
                                mContext.getString(R.string.activity_main_notification_add_mock_text),
                                mContext.getString(R.string.activity_main_notification_add_mock_finish));
                notification.startTimerToEnableNotification(
                        SpUsers.getProgressNotificationTimerForCurrentUser(mContext),
                        false);

                final int added = ListDbHelper.addMockEntries(
                        mContext,
                        notification,
                        entriesCount);

                showToastRunOnUiThread(
                        added > -1
                                ? mContext.getString(R.string.activity_main_notification_add_mock_entries_success)
                                + " (" + added + ")"
                                : mContext.getString(R.string.activity_main_notification_add_mock_entries_failed),
                        Toast.LENGTH_SHORT);

                notifyObservers(
                        ENTRIES_MOCK,
                        RESULT_CODE_MOCK_ENTRIES_ADDED,
                        null);
            }
        }).start();
    }


    /*
        ...
     */

    // --Commented out by Inspection START:
    //    private void deleteAllEntries() {
    //        new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                final boolean success = ListDbHelper.removeAllEntries(MainActivity.this);
    //                showToastRunOnUiThread(
    //                        success
    //                                ? getString(R.string.activity_main_notification_delete_all_entries_success)
    //                                : getString(R.string.activity_main_notification_delete_all_entries_failed),
    //                        Toast.LENGTH_SHORT);
    //            }
    //        }).start();
    //    }
    // --Commented out by Inspection STOP
}
