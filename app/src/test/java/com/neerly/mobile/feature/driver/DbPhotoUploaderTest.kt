package com.neerly.mobile.feature.driver

import com.neerly.mobile.support.TestStrings
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DbPhotoUploaderTest {
    private val uploader = DbPhotoUploader(mockk(relaxed = true), mockk(relaxed = true), TestStrings)

    @Test
    fun sampleSize_decodesBigCameraShotsSmaller_butNeverBelowTheEdgeLimit() {
        assertEquals(2, DbPhotoUploader.sampleSize(4000, 3000))   // 2000 px ≥ 1600, 1000 < 1600
        assertEquals(1, DbPhotoUploader.sampleSize(1600, 1200))
        assertEquals(4, DbPhotoUploader.sampleSize(8000, 6000))
    }

    @Test
    fun scaledSize_fitsTheLongSide_keepsAspect_leavesSmallPhotosAlone() {
        assertEquals(1600 to 1200, DbPhotoUploader.scaledSize(2000, 1500))
        assertEquals(1200 to 1600, DbPhotoUploader.scaledSize(1500, 2000))
        assertEquals(800 to 600, DbPhotoUploader.scaledSize(800, 600))
    }

    @Test
    fun outcomeFor_mapsServerRefusalsToTheDriversWords() {
        assertEquals(PhotoUploadResult.StorageDown, uploader.outcomeFor(503))
        val big = uploader.outcomeFor(413) as PhotoUploadResult.Failed
        assertTrue(big.message.contains("too big"))
        val type = uploader.outcomeFor(415) as PhotoUploadResult.Failed
        assertTrue(type.message.contains("camera"))
        assertTrue((uploader.outcomeFor(500) as PhotoUploadResult.Failed).message.contains("didn't upload"))
    }
}
