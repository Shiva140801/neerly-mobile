package com.neerly.mobile.data.auth

import com.neerly.mobile.data.api.NeerlyApi
import com.neerly.mobile.data.dto.TokenPair
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TokenAuthenticatorTest {

    private val api: NeerlyApi = mockk()
    private val tokens: TokenStore = mockk(relaxed = true)
    private var access = "old-access"
    private val auth = TokenAuthenticator(tokens, { api }, Moshi.Builder().build())

    init {
        every { tokens.accessToken } answers { access }
        every { tokens.refreshToken } returns "refresh-1"
        every { tokens.deviceId() } returns "dev"
        every { tokens.updateAfterRefresh(any(), any(), any(), any()) } answers { access = firstArg() }
    }

    private fun unauthorized(sentWith: String) = Response.Builder()
        .request(Request.Builder().url("http://x/api/v1/driver/shifts/state").header("Authorization", "Bearer $sentWith").build())
        .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()

    @Test
    fun parallel401s_refreshOnce_andBothRetryWithTheNewToken() {
        coEvery { api.refresh(any()) } coAnswers {
            Thread.sleep(150)
            TokenPair("new-access", "refresh-2", "2026-01-01T00:00:00Z", "2026-01-08T00:00:00Z")
        }
        val pool = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        val results = (1..2).map { pool.submit<Request?> { start.await(); auth.authenticate(null, unauthorized("old-access")) } }
        start.countDown()
        val retried = results.map { it.get(5, TimeUnit.SECONDS) }
        pool.shutdown()

        coVerify(exactly = 1) { api.refresh(any()) }
        retried.forEach { assertEquals("Bearer new-access", it?.header("Authorization")) }
        verify(exactly = 0) { tokens.clear() }
    }

    @Test
    fun refreshFails_clearsSessionAndGivesUp() {
        coEvery { api.refresh(any()) } throws RuntimeException("401")
        assertNull(auth.authenticate(null, unauthorized("old-access")))
        verify { tokens.clear() }
    }
}
