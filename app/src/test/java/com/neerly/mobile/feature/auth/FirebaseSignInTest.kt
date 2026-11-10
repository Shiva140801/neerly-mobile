package com.neerly.mobile.feature.auth

import com.neerly.mobile.data.auth.AuthRepository
import com.neerly.mobile.data.dto.ExchangeResponse
import com.neerly.mobile.data.dto.UserSummary
import com.neerly.mobile.support.TestStrings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Scripted stand-in for Firebase Phone Auth. */
class FakePhoneAuth(
    var sendOutcome: SendOutcome = SendOutcome.CodeSent,
    var verifyResult: Result<String> = Result.success("id-token")
) : PhoneAuthGateway {
    val sends = mutableListOf<Pair<String, Boolean>>()
    var signedOut = false
    private val auto = MutableStateFlow<String?>(null)
    override val autoVerified: StateFlow<String?> = auto
    fun autoVerify(token: String) { auto.value = token }
    override suspend fun sendCode(phoneE164: String, resend: Boolean): SendOutcome {
        sends += phoneE164 to resend
        if (sendOutcome is SendOutcome.Verified) auto.value = (sendOutcome as SendOutcome.Verified).idToken
        return sendOutcome
    }
    override suspend fun verifyCode(code: String): Result<String> = verifyResult
    override fun consumeAutoVerified(): String? = auto.value.also { auto.value = null }
    override fun signOut() { signedOut = true }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseSignInTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: AuthRepository = mockk(relaxed = true)
    private val sender: OtpSender = mockk()
    private val fake = FakePhoneAuth()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = AuthViewModel(repo, TestStrings, sender, AuthMode.FIREBASE, fake)

    private fun resp(isNew: Boolean = false, roles: List<String> = listOf("CUSTOMER")) = ExchangeResponse(
        accessToken = "a", refreshToken = "r", accessExpiresAt = "2026-11-10T00:00:00Z", refreshExpiresAt = "2026-11-17T00:00:00Z",
        isNewUser = isNew, activeRole = roles.first(), grantedRoles = roles,
        user = UserSummary(id = "u", displayName = "U", phoneMask = "+91******0123", email = null, preferredLanguage = "en", status = "ACTIVE"))

    private fun http(status: Int, code: String) = HttpException(Response.error<Any>(status,
        """{"error":{"code":"$code","message":"nope"}}""".toResponseBody("application/json".toMediaType())))

    @Test fun codeSent_movesToTheCodeScreen_withoutTheDevBackend() = runTest(dispatcher) {
        val vm = vm()
        var sentTo: String? = null
        vm.sendOtp("9876500123") { phone, _ -> sentTo = phone }
        advanceUntilIdle()
        assertEquals("+919876500123", sentTo)
        assertEquals(listOf("+919876500123" to false), fake.sends)
        coVerify(exactly = 0) { repo.sendDevOtp(any(), any()) }
        assertFalse(vm.isDevMode)
    }

    @Test fun rightCode_exchangesTheIdToken_andSignsIn() = runTest(dispatcher) {
        coEvery { repo.exchange("id-token", "CUSTOMER", any(), any()) } returns resp(isNew = true)
        val vm = vm()
        var done: Pair<Boolean, List<String>>? = null
        vm.verifyOtp("+919876500123", "123456") { isNew, roles -> done = isNew to roles }
        advanceUntilIdle()
        assertEquals(true to listOf("CUSTOMER"), done)
        assertNull(vm.state.value.error)
    }

    @Test fun wrongCode_saysSo_andDoesNotCallTheBackend() = runTest(dispatcher) {
        fake.verifyResult = Result.failure(PhoneAuthException(PhoneAuthError.INVALID_CODE))
        val vm = vm()
        vm.verifyOtp("+919876500123", "000000") { _, _ -> error("must not sign in") }
        advanceUntilIdle()
        assertEquals(TestStrings.get(com.neerly.mobile.R.string.core_auth_err_wrong_code), vm.state.value.error)
        assertFalse(vm.state.value.verifying)
        coVerify(exactly = 0) { repo.exchange(any(), any(), any(), any()) }
    }

    @Test fun tooManyRequests_onSend_showsTheWaitMessage() = runTest(dispatcher) {
        fake.sendOutcome = SendOutcome.Failed(PhoneAuthError.TOO_MANY_REQUESTS)
        val vm = vm()
        vm.sendOtp("9876500123") { _, _ -> error("must not advance") }
        advanceUntilIdle()
        assertEquals(TestStrings.get(com.neerly.mobile.R.string.core_auth_err_too_many), vm.state.value.error)
        assertFalse(vm.state.value.sending)
    }

    @Test fun instantVerification_signsInWhenTheCodeScreenOpens() = runTest(dispatcher) {
        fake.sendOutcome = SendOutcome.Verified("instant-token")
        coEvery { repo.exchange("instant-token", "CUSTOMER", any(), any()) } returns resp()
        val phoneVm = vm()
        var advanced = false
        phoneVm.sendOtp("9876500123") { _, _ -> advanced = true }
        advanceUntilIdle()
        // The code screen has its own view model; the gateway carries the token across.
        val codeVm = vm()
        var signedIn = false
        codeVm.otpScreenShown("CUSTOMER") { _, _ -> signedIn = true }
        advanceUntilIdle()
        assertEquals(true, advanced)
        assertEquals(true, signedIn)
        assertNull(fake.autoVerified.value)
    }

    @Test fun smsAutoRetrievedLater_alsoSignsIn() = runTest(dispatcher) {
        coEvery { repo.exchange("auto-token", "CUSTOMER", any(), any()) } returns resp()
        val vm = vm()
        var signedIn = false
        vm.otpScreenShown("CUSTOMER") { _, _ -> signedIn = true }
        advanceUntilIdle()
        assertFalse(signedIn)
        fake.autoVerify("auto-token")
        advanceUntilIdle()
        assertEquals(true, signedIn)
    }

    @Test fun exchangeFailure_keepsTheCustomerOnTheCodeScreen_withAMessage() = runTest(dispatcher) {
        coEvery { repo.exchange(any(), any(), any(), any()) } throws http(422, "USER_SUSPENDED")
        val vm = vm()
        vm.verifyOtp("+919876500123", "123456") { _, _ -> error("must not sign in") }
        advanceUntilIdle()
        assertEquals("nope", vm.state.value.error)
        assertFalse(vm.state.value.verifying)
    }

    @Test fun driverNotInvitedYet_signsInAsCustomerInstead() = runTest(dispatcher) {
        coEvery { repo.exchange("id-token", "DRIVER", any(), any()) } throws http(403, "AUTH_ROLE_NOT_GRANTED")
        coEvery { repo.exchange("id-token", "CUSTOMER", any(), any()) } returns resp()
        val vm = vm()
        vm.sendOtp("9876500123", role = "DRIVER") { _, _ -> }
        advanceUntilIdle()
        var roles: List<String>? = null
        vm.verifyOtp("+919876500123", "123456") { _, r -> roles = r }
        advanceUntilIdle()
        assertEquals(listOf("CUSTOMER"), roles)
        coVerify { repo.exchange("id-token", "CUSTOMER", any(), any()) }
    }

    @Test fun resend_usesTheResendToken_path() = runTest(dispatcher) {
        val vm = vm()
        vm.resendOtp("+919876500123", "CUSTOMER")
        advanceUntilIdle()
        assertEquals(listOf("+919876500123" to true), fake.sends)
        assertEquals(TestStrings.get(com.neerly.mobile.R.string.core_otp_resent), vm.state.value.resendNotice)
    }
}
