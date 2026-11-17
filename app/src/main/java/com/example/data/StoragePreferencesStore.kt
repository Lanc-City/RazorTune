package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storageDataStore by preferencesDataStore(name = "razortune_storage")

class StoragePreferencesStore(private val context: Context) {

    private object Keys {
        val DOWNLOAD_OVER_WIFI_ONLY = booleanPreferencesKey("download_over_wifi_only")
    }

    val downloadOverWifiOnlyFlow: Flow<Boolean> = context.storageDataStore.data.map { prefs ->
        prefs[Keys.DOWNLOAD_OVER_WIFI_ONLY] ?: false
    }

    suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
        context.storageDataStore.edit { prefs ->
            prefs[Keys.DOWNLOAD_OVER_WIFI_ONLY] = enabled
        }
    }
}
