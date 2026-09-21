package com.example.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaUploaderTest {

    private fun apiReturning(uploadUrl: String?): SoundSpireApi {
        val body = if (uploadUrl != null) """{"uploadUrl":"$uploadUrl"}""" else "{}"
        val client = HttpClient(MockEngine {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            expectSuccess = true
            install(ContentNegotiation) { json(soundSpireJson) }
            defaultRequest { url.takeFrom("https://mock.local/") }
        }
        return SoundSpireApi(client)
    }

    private fun putClient(status: HttpStatusCode) = HttpClient(MockEngine { respond("", status) })

    @Test
    fun upload_success_returns_s3_path() = runBlocking {
        val uploader = MediaUploader(apiReturning("https://s3.mock/put-here"), putClient(HttpStatusCode.OK))
        val result = uploader.upload(byteArrayOf(1, 2, 3), "img_1.jpg", "image/jpeg")
        assertEquals("s3://soundspirewebsiteassets/img_1.jpg", result)
    }

    @Test
    fun upload_put_failure_returns_null() = runBlocking {
        val uploader = MediaUploader(apiReturning("https://s3.mock/put-here"), putClient(HttpStatusCode.Forbidden))
        assertEquals(null, uploader.upload(byteArrayOf(1), "img.jpg", "image/jpeg"))
    }

    @Test
    fun upload_missing_presigned_url_returns_null() = runBlocking {
        val uploader = MediaUploader(apiReturning(null), putClient(HttpStatusCode.OK))
        assertEquals(null, uploader.upload(byteArrayOf(1), "img.jpg", "image/jpeg"))
    }
}
