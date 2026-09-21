package com.example.shared.network

import com.example.shared.SharedConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Shared JSON config. ignoreUnknownKeys is required: the DTOs are subsets of the
// backend responses (see Phase 2). explicitNulls=false so absent fields serialize
// cleanly and defaults apply on decode.
val soundSpireJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

// KMP equivalent of the app's ApiClient. The HTTP engine is auto-selected from the
// classpath: OkHttp on Android, Darwin on iOS (both proven in the Phase 0 spike).
// Pass a [cookieStore] to persist the session cookie across app launches (Phase 4);
// omit it for stateless tests.
fun createHttpClient(cookieStore: CookieStore? = null): HttpClient = HttpClient {
    expectSuccess = true // non-2xx throws, mirroring Retrofit's HttpException path
    install(ContentNegotiation) {
        json(soundSpireJson)
    }
    if (cookieStore != null) {
        install(HttpCookies) {
            storage = PersistentCookiesStorage(cookieStore)
        }
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 30_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
    defaultRequest {
        // Base URL from .env-derived SharedConfig. Endpoint paths are relative
        // (no leading slash) so they resolve against this base.
        val base = SharedConfig.API_BASE_URL.trimEnd('/') + "/"
        url.takeFrom(base)
    }
}
