package com.gamaliev.notes.common.observers;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ObserverHelper {

    /* Observers */
    private static final Map<String, Map<String, Observer>> OBSERVERS;

    /* Types of observation */
    @SuppressWarnings("WeakerAccess")
    public static final String COMMON           = "COMMON";
    public static final String FILE_EXPORT      = "FILE_EXPORT";
    public static final String FILE_IMPORT      = "FILE_IMPORT";
    public static final String COLOR_PICKER     = "COLOR_PICKER";
    public static final String ENTRY            = "ENTRY";
    public static final String ENTRIES_MOCK     = "ENTRIES_MOCK";
    public static final String LIST_FILTER      = "LIST_FILTER";
    public static final String SYNC             = "SYNC";
    public static final String USERS            = "USERS";
    public static final String CONFLICT         = "CONFLICT";


    /*
        Init
     */

    static {
        OBSERVERS = new HashMap<>();
        addType(COMMON);
    }

    private ObserverHelper() {}


    /*
        Registrations
     */

    public static void registerObserver(
            @NonNull final String[] observationTypes,
            @NonNull final String name,
            @NonNull final Observer observer) {

        for (String type : observationTypes) {
            if (OBSERVERS.containsKey(type)) {
                OBSERVERS.get(type).put(name, observer);
            } else {
                addType(type);
                OBSERVERS.get(type).put(name, observer);
            }
        }
    }

    public static void unregisterObserver(
            @NonNull final String[] observationTypes,
            @NonNull final String name) {

        for (String type : observationTypes) {
            final Map<String, Observer> observers = OBSERVERS.get(type);
            if (observers != null) {
                observers.remove(name);
            }
        }
    }

    private static void addType(@NonNull final String type) {
        OBSERVERS.put(
                type,
                new ConcurrentHashMap<String, Observer>());
    }


    /*
        Notifying
     */

    @SuppressWarnings("SameParameterValue")
    public static void notifyAllObservers(
            final int resultCode,
            @Nullable final Bundle data) {

        for (Map<String, Observer> observerTypes : OBSERVERS.values()) {
            for (Observer observer : observerTypes.values()) {
                observer.onNotify(resultCode, data);
            }
        }
    }

    public static void notifyObservers(
            @NonNull final String type,
            final int resultCode,
            @Nullable final Bundle data) {

        final Map<String, Observer> observers = OBSERVERS.get(type);
        if (observers != null) {
            for (Observer observer : observers.values()) {
                observer.onNotify(resultCode, data);
            }
        }
    }
}
