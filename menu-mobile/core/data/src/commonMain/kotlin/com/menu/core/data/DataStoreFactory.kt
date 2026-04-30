package com.menu.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

internal const val DATA_STORE_FILE_NAME = "menu_prefs.preferences_pb"

fun createDataStore(parentPath: String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath {
        "$parentPath/$DATA_STORE_FILE_NAME".toPath()
    }
}
