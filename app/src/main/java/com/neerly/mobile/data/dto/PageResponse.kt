package com.neerly.mobile.data.dto

import com.squareup.moshi.JsonClass

/**
 * The paging envelope the backend wraps list endpoints in:
 *
 * ```json
 * {"content":[…],"page":0,"size":20,"totalElements":1,"totalPages":1,
 *  "hasNext":false,"hasPrevious":false}
 * ```
 *
 * Several `NeerlyApi` methods declared a bare `List<T>` for these, so Moshi
 * failed with "Expected BEGIN_ARRAY but was BEGIN_OBJECT at path $" the moment
 * a request finally went out. The contract drift check only compares *paths*,
 * which is why this never showed up in CI.
 *
 * Repositories unwrap `content`, so callers keep working with plain lists.
 */
@JsonClass(generateAdapter = true)
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)
