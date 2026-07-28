package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.remote.ApiClient
import com.example.data.remote.UploadUrlRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Uploads an image to S3 via the backend's presigned-URL flow:
 *   1. POST /api/upload {fileName, fileType} → {uploadUrl}
 *   2. PUT the bytes to uploadUrl with the correct Content-Type
 * Returns the canonical s3:// path that the backend stores, or null on failure.
 *
 * Failures are logged (tag "S3Uploader") instead of being swallowed silently — a
 * null return here otherwise looks like a successful save that later reverts, because
 * the caller keeps showing the local image preview.
 */
object S3Uploader {
    private const val TAG = "S3Uploader"
    private val client = OkHttpClient()

    suspend fun upload(context: Context, uri: Uri, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                Log.e(TAG, "Could not read image bytes from $uri")
                return@withContext null
            }

            val api = ApiClient.getService(context)
            // Presign step. A 401 here means the session cookie didn't ride along
            // (the /api/upload route now requires auth).
            val presigned = api.getUploadUrl(UploadUrlRequest(fileName = fileName, fileType = mimeType))
            val uploadUrl = presigned.uploadUrl
            if (uploadUrl == null) {
                Log.e(TAG, "Presign returned no uploadUrl for $fileName")
                return@withContext null
            }

            val request = Request.Builder()
                .url(uploadUrl)
                .put(bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                .header("Content-Type", mimeType)
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "S3 PUT failed: HTTP ${resp.code} — ${resp.body?.string()}")
                    return@withContext null
                }
            }

            "s3://soundspirewebsiteassets/$fileName"
        } catch (e: Exception) {
            Log.e(TAG, "Upload of $fileName failed", e)
            null
        }
    }
}
