package com.example.shared.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Proves the ported DTOs parse real-world payloads: missing fields fall back to
// defaults, explicit nulls are accepted, and unknown backend keys are ignored.
// The Json config here is what the Phase 3 Ktor client must use.
class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun decodes_with_missing_and_unknown_fields() {
        // Most fields absent + an unknown key a future backend might add.
        val v = json.decodeFromString<AppVersionResponse>(
            """{"latestVersionCode":12,"someFutureField":true}"""
        )
        assertEquals(12, v.latestVersionCode)
        assertEquals(null, v.versionName) // absent -> default
    }

    @Test
    fun decodes_explicit_null_on_optional() {
        val m = json.decodeFromString<ForumMessage>(
            """{"forum_post_id":"p1","parent_post_id":null,"reactions":null}"""
        )
        assertEquals("p1", m.forum_post_id)
        assertTrue(m.reactions == null)
        assertTrue(m.media_urls == null)
    }

    @Test
    fun decodes_nested_and_lists() {
        val feed = json.decodeFromString<ReviewsFeedResponse>(
            """{"reviews":[{"review_id":"r1","spotify_track_id":"t1","user":{"username":"amy"}}]}"""
        )
        assertEquals(1, feed.reviews.size)
        assertEquals("amy", feed.reviews[0].user?.username)
    }

    @Test
    fun encodes_request_body() {
        val body = json.encodeToString(LoginRequest("a@b.com", "hash"))
        assertTrue(body.contains("password_hash"), "expected snake_case key in: $body")
    }
}
