package com.example.util

import com.example.BuildConfig

private val BASE_URL = BuildConfig.SOUNDSPIRE_API_BASE_URL.trimEnd('/')

fun resolveImageUrl(s3Path: String?): String? {
    if (s3Path.isNullOrBlank()) return null
    if (s3Path.startsWith("http")) return s3Path
    if (s3Path.startsWith("/api/")) return "$BASE_URL$s3Path"

    if (s3Path.startsWith("s3://")) {
        val match = Regex("^s3://[^/]+/(.+)$").find(s3Path)
        if (match != null) {
            var path = match.groupValues[1]
            if (!path.startsWith("images/")) path = "images/$path"
            return "$BASE_URL/api/$path"
        }
    }

    if (s3Path.startsWith("assets/") || s3Path.startsWith("reviews/") || s3Path.startsWith("images/")) {
        return "$BASE_URL/api/images/$s3Path"
    }

    return "$BASE_URL/api/images/$s3Path"
}

fun defaultProfileImageUrl(): String = "$BASE_URL/api/images/images/placeholder.jpg"

fun soundSpireLogoUrl(): String = "$BASE_URL/api/images/assets/ss_logo.png"
