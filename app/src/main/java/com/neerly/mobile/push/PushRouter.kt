package com.neerly.mobile.push

import com.neerly.mobile.data.auth.TokenStore
import com.neerly.mobile.feature.driver.DriverLocationTicker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one entry point for every push — FCM ([NeerlyMessagingService]), the debug Push lab and
 * the adb broadcast all land here, so what you test is what ships.
 */
@Singleton
class PushRouter @Inject constructor(
    private val tokens: TokenStore,
    private val ticker: DriverLocationTicker,
    private val duty: DriverDutyState,
    private val notifier: PushNotifier,
    private val links: DeepLinkBus
) {
    fun handle(data: Map<String, String>, forceBackground: Boolean = false): PushPlanner.Decision {
        val payload = PushPayload.parse(data)
        val env = PushPlanner.Env(
            activeRole = PushRole.of(tokens.activeRole),
            appForeground = AppForeground.isForeground && !forceBackground,
            nowMs = System.currentTimeMillis(),
            speedMps = ticker.freshFix()?.takeIf { it.hasSpeed() }?.speed,
            onDuty = duty.onDuty.value
        )
        val decision = PushPlanner.decide(payload, env)
        Timber.d("push %s → %s", payload.type, decision::class.simpleName)
        when (decision) {
            is PushPlanner.Decision.Ignore -> {}
            is PushPlanner.Decision.Cancel -> {
                notifier.cancel(decision.tag, decision.id)
                RingSession.stop(decision.tag)
            }
            is PushPlanner.Decision.InApp -> links.open(decision.route)
            is PushPlanner.Decision.Show -> notifier.post(decision.n, payload)
        }
        return decision
    }
}
