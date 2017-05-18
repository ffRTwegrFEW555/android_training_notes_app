package com.gamaliev.notes.common.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class NetworkUtils {

    /* Logger */
    private static final String TAG = NetworkUtils.class.getSimpleName();

    /* ... */
    public static final int NETWORK_NO      = -1;
    public static final int NETWORK_MOBILE  = 0;
    public static final int NETWORK_WIFI    = 1;


    /*
        Init
     */

    private NetworkUtils() {}


    /*
        ...
     */

    /**
     * @param context   Context.
     * @return          -1 - no network; 0 - mobile; 1 - wi-fi;
     */
    public static int checkNetwork(@NonNull final Context context) {

        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();

        if (activeInfo != null && activeInfo.isConnected()) {
            final int type = activeInfo.getType();
            if (type == ConnectivityManager.TYPE_MOBILE) {
                return NETWORK_MOBILE;
            } else if (type == ConnectivityManager.TYPE_WIFI) {
                return NETWORK_WIFI;
            }
        }

        return NETWORK_NO;
    }
}
