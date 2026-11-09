package com.neerly.mobile.feature.driver

import android.content.Context
import android.net.Uri
import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.PresignRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Why a door photo didn't upload — each has its own words on the hand-over screen. */
sealed class PhotoUploadResult {
    data class Uploaded(val s3Key: String) : PhotoUploadResult()
    /** 503 UPLOAD_STORAGE_UNAVAILABLE — Boondh's photo storage is down/unconfigured. Retry later. */
    data object StorageDown : PhotoUploadResult()
    data class Failed(val message: String) : PhotoUploadResult()
}

/** Seam so the view model can be tested without a camera or network. */
interface PhotoUploader {
    fun newPhotoFile(): File
    /** [scope]/[kind] pick the storage folder: ORDER_PROOF for doors and pickups, DRIVER_DOCS for papers. */
    suspend fun upload(file: File, scope: String = "ORDER_PROOF", kind: String = "door-photo"): PhotoUploadResult
}

/**
 * Proof-of-delivery photo: presign (scope ORDER_PROOF) → PUT the JPEG to the signed URL →
 * the s3Key goes on the deliver call. The old screen sent the literal "delivery-photo-pending",
 * so no photo was ever stored; this never sends a key it didn't upload.
 */
@Singleton
class S3PhotoUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: NeerlyApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : PhotoUploader {

    // A bare client: the signed URL carries its own auth; our bearer token must not go to storage.
    private val storage = OkHttpClient()

    override fun newPhotoFile(): File =
        File(File(context.cacheDir, "delivery-photos").apply { mkdirs() }, "door-${System.currentTimeMillis()}.jpg")

    override suspend fun upload(file: File, scope: String, kind: String): PhotoUploadResult = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext PhotoUploadResult.Failed(strings.get(com.neerly.mobile.R.string.drv_photo_not_saved))
        val signed = try {
            api.presign(PresignRequest(scope = scope, kind = kind, contentType = "image/jpeg"))
        } catch (e: HttpException) {
            return@withContext if (e.code() == 503) PhotoUploadResult.StorageDown
            else PhotoUploadResult.Failed(strings.get(com.neerly.mobile.R.string.drv_photo_prep_failed, e.code()))
        } catch (e: Exception) {
            return@withContext PhotoUploadResult.Failed(strings.get(com.neerly.mobile.R.string.drv_photo_no_internet))
        }
        val put = Request.Builder().url(signed.uploadUrl)
            .apply { signed.headers.forEach { (k, v) -> header(k, v) } }
            .put(file.readBytes().toRequestBody("image/jpeg".toMediaType()))
            .build()
        runCatching { storage.newCall(put).execute().use { it.isSuccessful } }.fold(
            onSuccess = { ok -> if (ok) PhotoUploadResult.Uploaded(signed.s3Key) else PhotoUploadResult.Failed(strings.get(com.neerly.mobile.R.string.drv_photo_not_uploaded)) },
            onFailure = { PhotoUploadResult.Failed(strings.get(com.neerly.mobile.R.string.drv_photo_no_internet)) }
        )
    }
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class PhotoUploaderModule {
    // Photos go to Boondh's own database for now; S3PhotoUploader comes back when S3 is set up.
    @dagger.Binds abstract fun bind(impl: DbPhotoUploader): PhotoUploader
}

/** content:// Uri for the camera to write into (FileProvider authority `<app>.photos`). */
fun photoUri(context: Context, file: File): Uri =
    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
