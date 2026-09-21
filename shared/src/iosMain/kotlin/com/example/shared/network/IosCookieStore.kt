package com.example.shared.network

import platform.Foundation.NSUserDefaults

// UserDefaults matches Android's SharedPreferences security level (the app stores its
// cookie in plain SharedPreferences). Swap for Keychain if stronger at-rest protection
// is needed later.
class IosCookieStore(
    private val key: String = "soundspire_cookies",
) : CookieStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(): String? = defaults.stringForKey(key)
    override fun write(data: String) { defaults.setObject(data, key) }
    override fun clear() { defaults.removeObjectForKey(key) }
}
