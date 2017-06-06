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

public final class ObserverHelper {

    /* Observers */
    @NonNull private static final Map<String, Map<String, Observer>> OBSERVERS;

    /* Types of observation */
    @SuppressWarnings("WeakerAccess")
    @NonNull public static final String COMMON          = "COMMON";
    @NonNull public static final String FILE_EXPORT     = "FILE_EXPORT";
    @NonNull public static final String FILE_IMPORT     = "FILE_IMPORT";
    @NonNull public static final String COLOR_PICKER    = "COLOR_PICKER";
    @NonNull public static final String ENTRY           = "ENTRY";
    @NonNull public static final String ENTRIES_MOCK    = "ENTRIES_MOCK";
    @NonNull public static final String LIST_FILTER     = "LIST_FILTER";
    @NonNull public static final String SYNC            = "SYNC";
    @NonNull public static final String USERS           = "USERS";
    @NonNull public static final String CONFLICT        = "CONFLICT";


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

    /**
     * Register observer, who will be notified for signed events.
     * @param observationTypes  Types of observation (events).
     * @param name              Unique name of observer.
     * @param observer          Observer, whose implements {@link Observer}.
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

    /**
     * Unregister observer from given events.
     * @param observationTypes  Type of observation (events).
     * @param name              Unique name of observer.
     */
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

    /**
     * Notify all registered observers, regardless of the type of signed events.
     * @param resultCode    Result code of event.
     * @param data          Data of event.
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

    /**
     * Notify registered observers, for given event.
     * @param type          Type of observation (event).
     * @param resultCode    Result code of event.
     * @param data          Data of event.
     */
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
