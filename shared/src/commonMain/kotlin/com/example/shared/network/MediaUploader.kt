package com.example.shared.network

import com.example.shared.data.model.UploadUrlRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess

/**
 * Shared S3 upload flow (ported from the app's S3Uploader):
 *   1. POST /api/upload {fileName, fileType} → {uploadUrl}  (authed, via [api])
 *   2. PUT the bytes to the presigned uploadUrl with the right Content-Type
 * Returns the canonical `s3://` path the backend stores, or null on failure.
 *
 * Reading image bytes from a picked file is platform UI work (Android `Uri`, iOS
 * `PHPicker`), done in the native layer, which then hands the bytes here.
 *
 * The PUT uses a separate bare client so the session cookie isn't sent to S3 and the
 * API base URL doesn't interfere with the absolute presigned URL.
 */
class MediaUploader(
    private val api: SoundSpireApi,
    private val putClient: HttpClient = HttpClient(),
) {
    private val bucket = "soundspirewebsiteassets"

    suspend fun upload(bytes: ByteArray, fileName: String, mimeType: String = "image/jpeg"): String? {
        return try {
            val uploadUrl = api.getUploadUrl(UploadUrlRequest(fileName = fileName, fileType = mimeType)).uploadUrl
                ?: return null
            val resp = putClient.put(uploadUrl) {
                header(HttpHeaders.ContentType, mimeType)
                setBody(bytes)
            }
            if (resp.status.isSuccess()) "s3://$bucket/$fileName" else null
        } catch (e: Exception) {
            null
        }
    }
}
