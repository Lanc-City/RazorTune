package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "razortune_auth")

class AuthPreferencesStore(private val context: Context) {

    private object Keys {
        val YOUTUBE_COOKIE = stringPreferencesKey("youtube_cookie")
    }

    val cookieFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.YOUTUBE_COOKIE]?.takeIf { it.isNotBlank() }
    }

    suspend fun getCookie(): String? = cookieFlow.first()

    suspend fun saveCookie(cookie: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.YOUTUBE_COOKIE] = cookie
        }
    }

    suspend fun clearCookie() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.YOUTUBE_COOKIE)
        }
    }
}
