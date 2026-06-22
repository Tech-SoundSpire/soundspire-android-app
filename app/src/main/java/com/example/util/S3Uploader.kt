package com.example.util

import android.content.Context
import android.net.Uri
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
 */
object S3Uploader {
    private val client = OkHttpClient()

    suspend fun upload(context: Context, uri: Uri, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null

            val api = ApiClient.getService(context)
            val presigned = api.getUploadUrl(UploadUrlRequest(fileName = fileName, fileType = mimeType))
            val uploadUrl = presigned.uploadUrl ?: return@withContext null

            val request = Request.Builder()
                .url(uploadUrl)
                .put(bytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                .header("Content-Type", mimeType)
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
            }

            "s3://soundspirewebsiteassets/$fileName"
        } catch (e: Exception) {
            null
        }
    }
}
