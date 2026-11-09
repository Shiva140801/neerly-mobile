package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

/** Answer to POST /api/v1/media — [key] is what the deliver / document calls take. */
@JsonClass(generateAdapter = true)
data class MediaUploadResponse(
    val key: String,
    val s3Key: String? = null,
    val url: String? = null,
    val contentType: String? = null,
    val sizeBytes: Int? = null
)
