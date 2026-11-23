package com.shieldguard.callblocker

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class FilterDecision(
    val shouldBlock: Boolean,
    val reasonCode: String,
    val reasonDescription: String
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
        callerDisplayName: String?
    ): FilterDecision {
        val prefs = context.getSharedPreferences("shieldguard_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("protection_active", true)) {
            return FilterDecision(false, "DISABLED", "Protezione disattivata")
        }

        val norm = normalize(rawNumber)
        if (norm in EMERGENCY_NUMBERS) {
            return FilterDecision(false, "EMERGENCY", "Numero di emergenza prioritario")
        }

        val whitelist = prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
        if (whitelist.any { w -> val wn = normalize(w); wn.isNotEmpty() && (norm == wn || norm.endsWith(wn)) }) {
            return FilterDecision(false, "WHITELISTED", "Consentito da Whitelist")
        }

        if (isAnonymous && prefs.getBoolean("block_anonymous", true)) {
            return FilterDecision(true, "ANONYMOUS_BLOCKED", "Chiamata anonima o privata")
        }

        val blacklist = prefs.getStringSet("blacklist", setOf("+44*", "899*", "+355*")) ?: emptySet()
        for (rule in blacklist) {
            val pattern = normalize(rule)
            if (pattern.endsWith("*") && norm.startsWith(pattern.dropLast(1))) {
                return FilterDecision(true, "BLACKLIST_MATCH", "Regola Blacklist ($rule)")
            } else if (pattern.isNotEmpty() && norm == pattern) {
                return FilterDecision(true, "BLACKLIST_MATCH", "Numero in Blacklist")
            }
        }

        // Hiya / Android Smart Call spam metadata check
        val displayLower = (callerDisplayName ?: "").lowercase()
        val isHiyaFlagged = displayLower.contains("spam") ||
            displayLower.contains("hiya") ||
            displayLower.contains("frode") ||
            displayLower.contains("telemarketing")
        if (prefs.getBoolean("block_hiya_spam", true) && isHiyaFlagged) {
            return FilterDecision(true, "HIYA_SPAM_BLOCKED", "Segnalato da Hiya / Smart Call")
        }

        // Automated daily local Spam DB lookup (<1ms HashSet lookup)
        if (prefs.getBoolean("block_spam_db", true)) {
            val spamSet = prefs.getStringSet("spam_db_numbers", emptySet()) ?: emptySet()
            if (norm in spamSet) {
                return FilterDecision(true, "SPAM_DATABASE", "Database Spam Pubblico Verificato")
            }
        }

        // Foreign number check
        val homeCode = prefs.getString("home_country_code", "39") ?: "39"
        val allowedForeign = prefs.getStringSet("allowed_foreign_codes", setOf("41")) ?: emptySet()
        if (prefs.getBoolean("block_foreign", true) && norm.startsWith("+") && !norm.startsWith("+$homeCode")) {
            val isAllowedForeign = allowedForeign.any { code -> norm.startsWith("+$code") }
            if (!isAllowedForeign) {
                return FilterDecision(true, "FOREIGN_BLOCKED", "Numero estero non autorizzato")
            }
        }

        // Contact lookup
        if (isInContacts(context, norm)) {
            return FilterDecision(false, "ALLOWED_CONTACT", "Contatto presente in rubrica")
        }

        if (prefs.getBoolean("block_unknown", true)) {
            return FilterDecision(true, "UNKNOWN_NOT_IN_CONTACTS", "Numero non presente in rubrica")
        }

        return FilterDecision(false, "ALLOWED_NORMAL", "Consentito")
    }

    private fun isInContacts(context: Context, normalizedNumber: String): Boolean {
        if (normalizedNumber.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalizedNumber)
            )
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { cursor -> cursor.moveToFirst() } ?: false
        } catch (_: SecurityException) {
            false
        }
    }
}
