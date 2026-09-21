package com.example.shared.network

import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookiePersistenceTest {

    private class FakeStore(var data: String? = null) : CookieStore {
        override fun read(): String? = data
        override fun write(data: String) { this.data = data }
        override fun clear() { data = null }
    }

    @Test
    fun session_cookie_survives_a_restart() = runBlocking {
        val store = FakeStore()
        val url = Url("https://app.soundspire.online/")

        // First "launch": the server sets a session cookie.
        PersistentCookiesStorage(store).addCookie(
            url,
            Cookie(
                name = "session",
                value = "jwt.abc.def",
                domain = "app.soundspire.online",
                path = "/",
                expires = GMTDate(GMTDate().timestamp + 7L * 24 * 3600 * 1000), // +7 days
                httpOnly = true,
                secure = true,
            ),
        )

        // Second "launch": a brand-new storage from the SAME persisted blob.
        val cookies = PersistentCookiesStorage(store).get(url)
        val session = cookies.firstOrNull { it.name == "session" }
        assertTrue(session != null, "session cookie did not survive restart: $cookies")
        assertEquals("jwt.abc.def", session.value) // exact round-trip (the old logout-on-restart bug)
    }

    @Test
    fun expired_cookie_is_not_resurrected() = runBlocking {
        val store = FakeStore()
        val url = Url("https://app.soundspire.online/")
        PersistentCookiesStorage(store).addCookie(
            url,
            Cookie(
                name = "old", value = "x",
                domain = "app.soundspire.online", path = "/",
                expires = GMTDate(GMTDate().timestamp - 1000), // already expired
            ),
        )
        val cookies = PersistentCookiesStorage(store).get(url)
        assertTrue(cookies.none { it.name == "old" }, "expired cookie should not load")
    }
}
