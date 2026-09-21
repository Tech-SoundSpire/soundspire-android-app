package com.example.shared.viewmodel

import com.example.shared.network.CookieStore
import com.example.shared.network.SoundSpireApi
import com.example.shared.network.soundSpireJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Ktor's MockEngine does real async work, so these run on runBlocking (real time) with a
// real Main dispatcher, awaiting the StateFlow rather than a virtual scheduler.
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private class FakeStore(var data: String? = "seed") : CookieStore {
        override fun read() = data
        override fun write(data: String) { this.data = data }
        override fun clear() { data = null }
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    // Builds a SoundSpireApi whose HTTP is faked by MockEngine, matched on path.
    private fun api(handler: (path: String) -> Pair<HttpStatusCode, String>): SoundSpireApi {
        val client = HttpClient(MockEngine { request ->
            val (status, body) = handler(request.url.encodedPath)
            respond(body, status, jsonHeaders())
        }) {
            expectSuccess = true
            install(ContentNegotiation) { json(soundSpireJson) }
            defaultRequest { url.takeFrom("https://mock.local/") }
        }
        return SoundSpireApi(client)
    }

    @BeforeTest fun setup() { Dispatchers.setMain(Dispatchers.Default) }
    @AfterTest fun teardown() { Dispatchers.resetMain() }

    @Test
    fun login_success_sets_logged_in_and_routes() = runBlocking {
        val store = FakeStore()
        val vm = AuthViewModel(
            api { path ->
                when (path) {
                    "/api/users/login" -> HttpStatusCode.OK to """{"message":"Logged In Success"}"""
                    "/api/auth/session" -> HttpStatusCode.OK to """{"user":{"id":"u1","role":"fan","email":"a@b.com"}}"""
                    "/api/profile" -> HttpStatusCode.OK to """{"full_name":"Amy","gender":"f","date_of_birth":"2000-01-01","mobile_number":"1","city":"NY","country":"US"}"""
                    "/api/preferences/check" -> HttpStatusCode.OK to """{"hasPreferences":true}"""
                    else -> HttpStatusCode.OK to "{}"
                }
            },
            store,
        )
        // Await the login callback itself (init's checkSession also flips isLoggedIn, so
        // awaiting the flag alone would race ahead of the login flow's onSuccess).
        val routed = CompletableDeferred<Unit>()
        vm.login("a@b.com", "pw") { routed.complete(Unit) }
        withTimeout(5000) { routed.await() }

        assertTrue(vm.isLoggedIn.value)
        assertEquals("u1", vm.currentUser.value?.id)
        assertFalse(vm.needsCompleteProfile.value, "complete profile → false")
        assertFalse(vm.needsPreferences.value, "prefs set → false")
        assertEquals(null, vm.authError.value)
    }

    @Test
    fun login_failure_surfaces_backend_message() = runBlocking {
        val vm = AuthViewModel(
            api { path ->
                when (path) {
                    "/api/auth/session" -> HttpStatusCode.OK to """{"user":null}"""
                    "/api/users/login" -> HttpStatusCode.Unauthorized to """{"message":"Invalid email or password"}"""
                    else -> HttpStatusCode.OK to "{}"
                }
            },
            FakeStore(),
        )
        vm.login("a@b.com", "wrong") { }
        val err = withTimeout(5000) { vm.authError.filterNotNull().first() }
        assertEquals("Invalid email or password", err)
        assertFalse(vm.isLoggedIn.value)
    }

    @Test
    fun logout_clears_session_and_cookies() = runBlocking {
        val store = FakeStore(data = "cookie-blob")
        val vm = AuthViewModel(
            api { path ->
                when (path) {
                    "/api/auth/session" -> HttpStatusCode.OK to """{"user":{"id":"u1","role":"fan","email":"a@b.com"}}"""
                    "/api/profile" -> HttpStatusCode.OK to """{"full_name":"Amy","gender":"f","date_of_birth":"2000-01-01","mobile_number":"1","city":"NY","country":"US"}"""
                    "/api/preferences/check" -> HttpStatusCode.OK to """{"hasPreferences":true}"""
                    "/api/auth/logout" -> HttpStatusCode.OK to "{}"
                    else -> HttpStatusCode.OK to "{}"
                }
            },
            store,
        )
        withTimeout(5000) { vm.isLoggedIn.first { it } } // logged in via init/session

        val done = CompletableDeferred<Unit>()
        vm.logout { done.complete(Unit) }
        withTimeout(5000) { done.await() }
        assertFalse(vm.isLoggedIn.value)
        assertEquals(null, vm.currentUser.value)
        assertEquals(null, store.data, "cookie store should be cleared")
    }
}
