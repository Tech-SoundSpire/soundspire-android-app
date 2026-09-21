package com.example.shared.network

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Platform-swappable persistent key-value blob store for the cookie jar.
 * Android backs it with SharedPreferences; iOS with UserDefaults.
 * (Interface + platform classes instead of expect/actual class, because the
 * Android implementation needs a Context in its constructor.)
 */
interface CookieStore {
    fun read(): String?
    fun write(data: String)
    fun clear()
}

// One persisted cookie. Serializes every field so it round-trips exactly, including
// the encoding and expiry — the app's past "logged out on restart" bug came from
// dropping fields / re-parsing against a dummy host, so we keep them all here.
@Serializable
private data class PersistedCookie(
    val name: String,
    val value: String,
    val encoding: String,
    val maxAge: Int,
    val expiresMs: Long?,
    val domain: String?,
    val path: String?,
    val secure: Boolean,
    val httpOnly: Boolean,
    val requestUrl: String,
) {
    fun key() = "$name@${domain ?: ""}"
    fun toCookie() = Cookie(
        name = name,
        value = value,
        encoding = CookieEncoding.valueOf(encoding),
        maxAge = maxAge,
        expires = expiresMs?.let { GMTDate(it) },
        domain = domain,
        path = path,
        secure = secure,
        httpOnly = httpOnly,
    )

    companion object {
        fun from(requestUrl: Url, c: Cookie) = PersistedCookie(
            name = c.name,
            value = c.value,
            encoding = c.encoding.name,
            maxAge = c.maxAge,
            expiresMs = c.expires?.timestamp,
            domain = c.domain,
            path = c.path,
            secure = c.secure,
            httpOnly = c.httpOnly,
            requestUrl = requestUrl.toString(),
        )
    }
}

/**
 * Ktor CookiesStorage that persists across app launches. Cookie matching, default
 * filling, and expiry are delegated to Ktor's AcceptAllCookiesStorage; this class
 * only adds durable storage of the raw cookie fields via [CookieStore].
 */
class PersistentCookiesStorage(private val store: CookieStore) : CookiesStorage {
    private val delegate = AcceptAllCookiesStorage()
    private val byKey = mutableMapOf<String, PersistedCookie>()
    private val mutex = Mutex()
    private var loaded = false
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val nowMs = GMTDate().timestamp
            store.read()?.let { blob ->
                runCatching { json.decodeFromString<List<PersistedCookie>>(blob) }
                    .getOrDefault(emptyList())
                    .filter { it.expiresMs == null || it.expiresMs > nowMs } // skip already-expired
                    .forEach { pc ->
                        byKey[pc.key()] = pc
                        delegate.addCookie(Url(pc.requestUrl), pc.toCookie())
                    }
            }
            loaded = true
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        ensureLoaded()
        return delegate.get(requestUrl)
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        ensureLoaded()
        delegate.addCookie(requestUrl, cookie)
        mutex.withLock {
            val pc = PersistedCookie.from(requestUrl, cookie)
            byKey[pc.key()] = pc
            store.write(json.encodeToString(byKey.values.toList()))
        }
    }

    override fun close() = delegate.close()
}
