package com.gamaliev.notes;

import android.support.annotation.NonNull;

public interface BaseView<T> {

    void setPresenter(@NonNull T presenter);

    /**
     * @return True, if view is active (attached or added), otherwise false.
     */
    boolean isActive();
}
