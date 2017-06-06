package com.gamaliev.notes.common.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.gamaliev.notes.sync.SyncUtils;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class NetworkBroadcastReceiver extends BroadcastReceiver {

    /* ... */
    private static BroadcastReceiver sInstance;


    /*
        Init
     */

    /**
     * @return Broadcast receiver, that monitors changes in the network.
     */
    @Nullable
    public static synchronized BroadcastReceiver getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkBroadcastReceiver();
        }
        return sInstance;
    }


    /*
        ...
     */

    @Override
    public void onReceive(Context context, Intent intent) {
        SyncUtils.checkPendingSyncAndStart(context);
    }
}
