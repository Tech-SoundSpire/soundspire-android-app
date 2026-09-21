package com.example.shared.util

import com.example.shared.SharedConfig

// Ported from the app's ImageUtils. Converts stored s3://... paths and relative paths
// into backend image-proxy URLs. Shared so Android and iOS resolve identically.
object ImageUrls {
    private val base: String get() = SharedConfig.API_BASE_URL.trimEnd('/')

    fun resolve(s3Path: String?): String? {
        if (s3Path.isNullOrBlank()) return null
        if (s3Path.startsWith("http")) return s3Path
        if (s3Path.startsWith("/api/")) return "$base$s3Path"
        if (s3Path.startsWith("s3://")) {
            val match = Regex("^s3://[^/]+/(.+)$").find(s3Path)
            if (match != null) {
                var path = match.groupValues[1]
                if (!path.startsWith("images/")) path = "images/$path"
                return "$base/api/$path"
            }
        }
        return "$base/api/images/$s3Path"
    }

    fun defaultProfile(): String = "$base/api/images/images/placeholder.jpg"
    fun logo(): String = "$base/api/images/assets/ss_logo.png"
}
