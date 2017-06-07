package com.gamaliev.notes;

public interface BaseView<T> {

    void setPresenter(T presenter);

    /**
     * @return True, if view is active (attached or added), otherwise false.
     */
    boolean isActive();
}
