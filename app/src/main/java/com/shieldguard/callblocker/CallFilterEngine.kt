package com.shieldguard.callblocker

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class FilterDecision(
    val shouldBlock: Boolean,
    val reasonCode: String,
    val reasonDescription: String,
    val callerLabel: String
)

object CallFilterEngine {
    private val EMERGENCY_NUMBERS = setOf("112", "113", "115", "118", "114", "1522", "911", "999")

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val stripped = raw.trim().replace(Regex("[\\s\\-()\\.]"), "")
        return if (stripped.startsWith("00")) "+" + stripped.substring(2) else stripped
    }

    fun evaluate(
        context: Context,
        rawNumber: String?,
        isAnonymous: Boolean,
        callerDisplayName: String?,
        verificationFailed: Boolean = false
    ): FilterDecision {
        ShieldStorage.ensureInitialized(context)
        val prefs = ShieldStorage.prefs(context)

        if (!prefs.getBoolean("protection_active", true)) {
            return FilterDecision(false, "DISABLED", "Protezione disattivata", rawNumber ?: "Chiamata")
        }

        val norm = normalize(rawNumber)
        if (norm in EMERGENCY_NUMBERS) {
            return FilterDecision(false, "EMERGENCY", "Numero di emergenza prioritario", "Emergenza $norm")
        }

        val whitelist = prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
        if (whitelist.any { w ->
                val wn = normalize(w)
                wn.isNotEmpty() && (norm == wn || (norm.length >= 7 && wn.length >= 7 && (norm.endsWith(wn) || wn.endsWith(norm))))
            }
        ) {
            return FilterDecision(false, "WHITELISTED", "Consentito da Whitelist", "Whitelist")
        }

        if (isAnonymous && prefs.getBoolean("block_anonymous", true)) {
            return FilterDecision(true, "ANONYMOUS_BLOCKED", "Chiamata anonima o numero privato", "Chiamata Anonima")
        }

        val blacklist = prefs.getStringSet("blacklist", setOf("+44*", "899*", "+355*")) ?: emptySet()
        for (rule in blacklist) {
            val pattern = normalize(rule)
            if (pattern.endsWith("*") && pattern.length > 1 && norm.startsWith(pattern.dropLast(1))) {
                return FilterDecision(true, "BLACKLIST_MATCH", "Regola Blacklist ($rule)", "Blacklist")
            } else if (pattern.isNotEmpty() && norm == pattern) {
                return FilterDecision(true, "BLACKLIST_MATCH", "Numero in Blacklist", "Blacklist")
            }
        }

        // Individual enabled public Spam lists lookup (<1ms)
        if (prefs.getBoolean("block_spam_db", true)) {
            val matchedFeed = ShieldStorage.findSpamMatch(context, norm)
            if (matchedFeed != null) {
                return FilterDecision(
                    shouldBlock = true,
                    reasonCode = "SPAM_DATABASE",
                    reasonDescription = "Lista Spam: ${matchedFeed.name}",
                    callerLabel = matchedFeed.name
                )
            }
        }

        // Hiya / Android Smart Call / STIR-SHAKEN spam metadata check
        val displayLower = (callerDisplayName ?: "").lowercase()
        val isHiyaFlagged = verificationFailed ||
            displayLower.contains("spam") ||
            displayLower.contains("hiya") ||
            displayLower.contains("frode") ||
            displayLower.contains("truffa") ||
            displayLower.contains("telemarketing") ||
            displayLower.contains("sospetto")
        if (prefs.getBoolean("block_hiya_spam", true) && isHiyaFlagged) {
            return FilterDecision(
                shouldBlock = true,
                reasonCode = "HIYA_SPAM_BLOCKED",
                reasonDescription = "Segnalato come spam da Hiya / Android Smart Call",
                callerLabel = callerDisplayName?.takeIf { it.isNotBlank() } ?: "Hiya Smart Call"
            )
        }

        // Foreign number check
        val homeCode = (prefs.getString("home_country_code", "39") ?: "39").replace(Regex("[^\\d]"), "")
        val allowedForeign = prefs.getStringSet("allowed_foreign_codes", setOf("41")) ?: setOf("41")
        if (prefs.getBoolean("block_foreign", true) && norm.startsWith("+") && homeCode.isNotEmpty() && !norm.startsWith("+$homeCode")) {
            val isAllowedForeign = allowedForeign.any { code -> code.isNotBlank() && norm.startsWith("+$code") }
            if (!isAllowedForeign) {
                return FilterDecision(
                    shouldBlock = true,
                    reasonCode = "FOREIGN_BLOCKED",
                    reasonDescription = "Prefisso estero non autorizzato",
                    callerLabel = "Numero Estero"
                )
            }
        }

        // Contact lookup
        val contactName = findContactName(context, norm)
        if (contactName != null) {
            return FilterDecision(
                shouldBlock = false,
                reasonCode = "ALLOWED_CONTACT",
                reasonDescription = "Contatto in rubrica ($contactName)",
                callerLabel = contactName
            )
        }

        if (prefs.getBoolean("block_unknown", true)) {
            return FilterDecision(
                shouldBlock = true,
                reasonCode = "UNKNOWN_NOT_IN_CONTACTS",
                reasonDescription = "Numero sconosciuto non presente in rubrica",
                callerLabel = "Sconosciuto"
            )
        }

        return FilterDecision(
            shouldBlock = false,
            reasonCode = "ALLOWED_NORMAL",
            reasonDescription = "Consentito dalle regole correnti",
            callerLabel = callerDisplayName?.takeIf { it.isNotBlank() } ?: "Chiamante"
        )
    }

    private fun findContactName(context: Context, normalizedNumber: String): String? {
        if (normalizedNumber.isBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalizedNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0) ?: "Rubrica"
                } else {
                    null
                }
            }
        } catch (_: SecurityException) {
            null
        }
    }
}
