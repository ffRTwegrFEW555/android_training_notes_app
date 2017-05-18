package com.gamaliev.notes.common.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.gamaliev.notes.common.shared_prefs.SpUsers;
import com.gamaliev.notes.sync.SyncUtils;

import static com.gamaliev.notes.common.shared_prefs.SpUsers.getPendingSyncStatusForCurrentUser;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class NetworkBroadcastReceiver extends BroadcastReceiver {

    private static BroadcastReceiver sInstance;

    @Nullable
    public static synchronized BroadcastReceiver getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkBroadcastReceiver();
        }

        return sInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (getPendingSyncStatusForCurrentUser(context)
                .equals(SpUsers.SP_USER_SYNC_PENDING_TRUE)) {
            SyncUtils.synchronize(context);
        }
    }
}
