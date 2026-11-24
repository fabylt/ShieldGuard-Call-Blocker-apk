package com.shieldguard.callblocker

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
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

        val verificationFailed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            callDetails.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED
        } else {
            false
        }

        val decision = CallFilterEngine.evaluate(
            context = applicationContext,
            rawNumber = rawNumber,
            isAnonymous = isAnonymous,
            callerDisplayName = callDetails.callerDisplayName,
            verificationFailed = verificationFailed
        )

        val prefs = ShieldStorage.prefs(applicationContext)
        val blockAction = prefs.getString("block_action", "HANG_UP") ?: "HANG_UP"
        val notifStyle = prefs.getString("notification_style", "HEADS_UP") ?: "HEADS_UP"

        val response = CallResponse.Builder()
        val actionTaken = if (decision.shouldBlock) {
            when (blockAction) {
                "SILENCE" -> {
                    response.setDisallowCall(false)
                    response.setSilenceCall(true)
                    "SILENCE"
                }
                "VOICEMAIL" -> {
                    response.setDisallowCall(true)
                    response.setRejectCall(false)
                    "VOICEMAIL"
                }
                else -> {
                    response.setDisallowCall(true)
                    response.setRejectCall(true)
                    "HANG_UP"
                }
            }.also {
                response.setSkipCallLog(false)
                response.setSkipNotification(notifStyle == "GHOST_MODE")
            }
        } else {
            response.setDisallowCall(false)
            "RING"
        }

        // Persist real call event so Dashboard stats and Call Log tab update immediately
        ShieldStorage.appendCallLog(
            applicationContext,
            CallLogItem(
                id = "call-${System.currentTimeMillis()}",
                number = if (isAnonymous) "Numero Privato" else (rawNumber ?: "Sconosciuto"),
                callerName = decision.callerLabel,
                timestampMs = System.currentTimeMillis(),
                blocked = decision.shouldBlock,
                reasonCode = decision.reasonCode,
                reasonDescription = decision.reasonDescription,
                actionTaken = actionTaken
            )
        )

        respondToCall(callDetails, response.build())
    }
}
