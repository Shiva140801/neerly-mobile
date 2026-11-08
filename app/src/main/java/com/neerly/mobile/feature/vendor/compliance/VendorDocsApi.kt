package com.neerly.mobile.feature.vendor.compliance

import com.neerly.mobile.R
import android.content.Context
import android.net.Uri
import com.squareup.moshi.JsonClass
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Licences and papers (boards V03, V36, V37, V48): list with expiry, submit a document, and
 * the upload presign. The shared `PresignRequest` in NeerlyApi sends `purpose/fileName`,
 * which the backend doesn't read — this one matches the server (`scope/kind/contentType`).
 */
interface VendorDocsApi {
    @GET("api/v1/vendor/compliance")
    suspend fun docs(): List<DocRow>

    @POST("api/v1/vendor/compliance")
    suspend fun submit(@Body body: SubmitDocBody): DocRow

    @POST("api/v1/uploads/presign")
    suspend fun presign(@Body body: PresignBody): PresignResult
}

@Module
@InstallIn(SingletonComponent::class)
object VendorDocsApiModule {
    @Provides @Singleton
    fun provide(retrofit: Retrofit): VendorDocsApi = retrofit.create(VendorDocsApi::class.java)
}

@JsonClass(generateAdapter = true)
data class DocRow(
    val id: String,
    val docType: String,
    val docNumber: String? = null,
    val status: String,                     // SUBMITTED / APPROVED / REJECTED / EXPIRED
    val issuedDate: String? = null,
    val expiresDate: String? = null,
    val rejectionReason: String? = null,
    val uploadedAt: String,
    val expiresInDays: Int? = null,
    val renewSoon: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SubmitDocBody(
    val docType: String,
    val docNumber: String? = null,
    val s3Key: String,
    val issuedDate: String? = null,
    val expiresDate: String? = null
)

@JsonClass(generateAdapter = true)
data class PresignBody(val scope: String, val kind: String, val contentType: String)

@JsonClass(generateAdapter = true)
data class PresignResult(
    val uploadUrl: String,
    val s3Key: String,
    val headers: Map<String, String> = emptyMap(),
    val expiresInSeconds: Long? = null
)

/** The papers a water shop needs (V01 list). `required` ones block going live. */
data class DocKind(val type: String, @androidx.annotation.StringRes val label: Int, @androidx.annotation.StringRes val hint: Int?,
                   val required: Boolean, val hasNumber: Boolean, val hasExpiry: Boolean)

val SHOP_DOCS = listOf(
    DocKind("FSSAI_LICENSE", R.string.vend_doc_fssai, R.string.vend_dh_fssai, required = true, hasNumber = true, hasExpiry = true),
    DocKind("BIS_CERTIFICATE", R.string.vend_doc_bis, R.string.vend_dh_bis, required = true, hasNumber = true, hasExpiry = true),
    DocKind("PAN", R.string.vend_doc_pan, R.string.vend_dh_pan, required = true, hasNumber = true, hasExpiry = false),
    DocKind("WATER_TEST", R.string.vend_doc_water, R.string.vend_dh_water, required = true, hasNumber = false, hasExpiry = false),
    DocKind("GSTIN", R.string.vend_doc_gstin, R.string.vend_dh_gstin, required = false, hasNumber = true, hasExpiry = false)
)

/** Presign → PUT the bytes → the s3 key to submit. Throws [UploadUnavailable] when storage isn't set up. */
@Singleton
class DocUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: VendorDocsApi
) {
    class UploadUnavailable(cause: Throwable?) : Exception(
        "Document upload isn't available right now. Your details are kept — try again later.", cause)

    /** Something wrong with the picked file; [res] says what, in the shop's language. */
    class FileProblem(@androidx.annotation.StringRes val res: Int, message: String) : IllegalStateException(message)

    private val http = OkHttpClient()

    suspend fun upload(uri: Uri, kind: String): String {
        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: throw FileProblem(R.string.vend_cant_read_file, "Couldn't read that file. Pick it again.")
        if (bytes.size > MAX_BYTES) throw FileProblem(R.string.vend_file_too_big, "That file is over 10 MB. Take a photo instead.")
        val signed = runCatching { api.presign(PresignBody("VENDOR_DOCS", kind.lowercase(), type)) }
            .getOrElse { throw UploadUnavailable(it) }
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(signed.uploadUrl)
                .put(bytes.toRequestBody(type.toMediaTypeOrNull()))
                .apply { signed.headers.forEach { (k, v) -> header(k, v) } }
                .build()
            http.newCall(req).execute().use { if (!it.isSuccessful) throw UploadUnavailable(null) }
        }
        return signed.s3Key
    }

    companion object { const val MAX_BYTES = 10 * 1024 * 1024 }
}
