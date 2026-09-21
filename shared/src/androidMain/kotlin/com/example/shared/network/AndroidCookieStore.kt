package com.example.shared.network

import android.content.Context

// Mirrors the app's PersistentCookieJar storage location/name.
class AndroidCookieStore(
    context: Context,
    prefsName: String = "soundspire_cookies",
) : CookieStore {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString("cookies_json", null)
    override fun write(data: String) { prefs.edit().putString("cookies_json", data).apply() }
    override fun clear() { prefs.edit().clear().apply() }
}
