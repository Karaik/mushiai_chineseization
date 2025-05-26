package com.karaik.scripteditor.util;

import java.io.File;
import java.util.prefs.Preferences;

public class AppPreferenceHelper {

    private static final Preferences preferences = Preferences.userNodeForPackage(AppPreferenceHelper.class);

    private static final String PREF_KEY_LAST_FILE = "lastOpenedFile";
    private static final String PREF_KEY_ITEMS_PER_PAGE = "itemsPerPage";
    private static final String PREF_KEY_LAST_PAGE_INDEX = "lastPageIndex";
    private static final String PREF_KEY_ALWAYS_ON_TOP = "alwaysOnTop";

    public static void saveLastFile(File file) {
        if (file != null) {
            preferences.put(PREF_KEY_LAST_FILE, file.getAbsolutePath());
        } else {
            preferences.remove(PREF_KEY_LAST_FILE);
        }
    }

    public static File loadLastFile() {
        String path = preferences.get(PREF_KEY_LAST_FILE, null);
        return path != null ? new File(path) : null;
    }

    public static void saveItemsPerPage(int value) {
        preferences.putInt(PREF_KEY_ITEMS_PER_PAGE, value);
    }

    public static int loadItemsPerPage(int defaultValue) {
        return preferences.getInt(PREF_KEY_ITEMS_PER_PAGE, defaultValue);
    }

    public static void saveLastPageIndex(int index) {
        preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, index);
    }

    public static int loadLastPageIndex() {
        return preferences.getInt(PREF_KEY_LAST_PAGE_INDEX, 0);
    }

    public static void saveAlwaysOnTop(boolean value) {
        preferences.putBoolean(PREF_KEY_ALWAYS_ON_TOP, value);
    }

    public static boolean loadAlwaysOnTop() {
        return preferences.getBoolean(PREF_KEY_ALWAYS_ON_TOP, false);
    }
}
