package com.neerly.mobile.feature.driver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.neerly.mobile.R
import com.neerly.mobile.data.api.NeerlyApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends the photo straight to Boondh (`POST /api/v1/media`), which keeps it in its own
 * database until object storage is switched on. The camera's full-size shot is shrunk to
 * at most [MAX_EDGE_PX] on the long side and re-encoded as JPEG first: a door photo goes
 * from ~4 MB to ~250 KB, which matters on a driver's mobile data and stays well under the
 * server's upload limit.
 */
@Singleton
class DbPhotoUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: NeerlyApi,
    private val strings: com.neerly.mobile.core.util.Strings
) : PhotoUploader {

    override fun newPhotoFile(): File =
        File(File(context.cacheDir, "delivery-photos").apply { mkdirs() }, "door-${System.currentTimeMillis()}.jpg")

    override suspend fun upload(file: File, scope: String, kind: String): PhotoUploadResult = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext PhotoUploadResult.Failed(strings.get(R.string.drv_photo_not_saved))
        val bytes = runCatching { shrink(file) }.getOrNull() ?: file.readBytes()
        val jpeg = "image/jpeg".toMediaType()
        val text = "text/plain".toMediaType()
        try {
            val res = api.uploadMedia(
                MultipartBody.Part.createFormData("file", file.name, bytes.toRequestBody(jpeg)),
                scope.toRequestBody(text),
                kind.toRequestBody(text)
            )
            PhotoUploadResult.Uploaded(res.key)
        } catch (e: HttpException) {
            outcomeFor(e.code())
        } catch (e: Exception) {
            PhotoUploadResult.Failed(strings.get(R.string.drv_photo_no_internet))
        }
    }

    /** What a refused upload means for the driver, by HTTP status. */
    fun outcomeFor(code: Int): PhotoUploadResult = when (code) {
        503 -> PhotoUploadResult.StorageDown
        413 -> PhotoUploadResult.Failed(strings.get(R.string.drv_photo_too_big))
        415 -> PhotoUploadResult.Failed(strings.get(R.string.drv_photo_wrong_type))
        else -> PhotoUploadResult.Failed(strings.get(R.string.drv_photo_not_uploaded))
    }

    /** Decodes at a reduced size, turns it upright from EXIF, scales to the edge limit, JPEG [QUALITY]. */
    private fun shrink(file: File): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val decoded = BitmapFactory.decodeFile(file.path,
            BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }) ?: return null
        val (w, h) = scaledSize(decoded.width, decoded.height)
        val rotation = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val matrix = Matrix().apply {
            postScale(w.toFloat() / decoded.width, h.toFloat() / decoded.height)
            if (rotation != 0f) postRotate(rotation)
        }
        val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        return ByteArrayOutputStream().use { s ->
            out.compress(Bitmap.CompressFormat.JPEG, QUALITY, s)
            if (out !== decoded) out.recycle()
            decoded.recycle()
            s.toByteArray()
        }
    }

    companion object {
        const val MAX_EDGE_PX = 1600
        const val QUALITY = 80

        /** Largest power of two that keeps the decoded image at least [MAX_EDGE_PX] on its long side. */
        fun sampleSize(width: Int, height: Int): Int {
            var s = 1
            while (maxOf(width, height) / (s * 2) >= MAX_EDGE_PX) s *= 2
            return s
        }

        /** Size after fitting the long side into [MAX_EDGE_PX]; smaller images are left alone. */
        fun scaledSize(width: Int, height: Int): Pair<Int, Int> {
            val long = maxOf(width, height)
            if (long <= MAX_EDGE_PX) return width to height
            val f = MAX_EDGE_PX.toDouble() / long
            return maxOf(1, Math.round(width * f).toInt()) to maxOf(1, Math.round(height * f).toInt())
        }
    }
}
