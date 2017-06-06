package com.gamaliev.notes.common.observers;

import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public interface Observer {

    void onNotify(
            final int resultCode,
            @Nullable final Bundle data);
}
