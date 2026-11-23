package com.shieldguard.callblocker

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager

/**
 * Native Android CallScreeningService (Android 10+).
 * Invoked directly by Telecom framework only on incoming calls:
 * zero background polling, zero wake-locks, <0.1% daily battery usage.
 */
class ShieldCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val rawNumber = callDetails.handle?.schemeSpecificPart
        val isAnonymous = rawNumber.isNullOrBlank() ||
            callDetails.handlePresentation == TelecomManager.PRESENTATION_RESTRICTED ||
            callDetails.handlePresentation == TelecomManager.PRESENTATION_UNKNOWN

        val decision = CallFilterEngine.evaluate(
            context = applicationContext,
            rawNumber = rawNumber,
            isAnonymous = isAnonymous,
            callerDisplayName = callDetails.callerDisplayName
        )

        val prefs = getSharedPreferences("shieldguard_prefs", MODE_PRIVATE)
        val blockAction = prefs.getString("block_action", "HANG_UP")
        val notifStyle = prefs.getString("notification_style", "HEADS_UP")

        val response = CallResponse.Builder()
        if (decision.shouldBlock) {
            when (blockAction) {
                "SILENCE" -> {
                    response.setDisallowCall(false)
                    response.setSilenceCall(true)
                }
                else -> {
                    response.setDisallowCall(true)
                    response.setRejectCall(true)
                }
            }
            response.setSkipCallLog(false)
            response.setSkipNotification(notifStyle == "GHOST_MODE")
        } else {
            response.setDisallowCall(false)
        }

        respondToCall(callDetails, response.build())
    }
}
