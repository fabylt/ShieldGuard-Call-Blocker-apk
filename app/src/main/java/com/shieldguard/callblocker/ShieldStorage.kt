package com.shieldguard.callblocker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SpamFeedInfo(
    val id: String,
    val name: String,
    val provider: String,
    val description: String,
    val url: String,
    val enabled: Boolean,
    val publicRecordsCount: Int,
    val localNumbersCount: Int,
    val lastSyncedMs: Long
)

data class CallLogItem(
    val id: String,
    val number: String,
    val callerName: String,
    val timestampMs: Long,
    val blocked: Boolean,
    val reasonCode: String,
    val reasonDescription: String,
    val actionTaken: String
)

data class ShieldStats(
    val blockedToday: Int,
    val totalBlocked: Int,
    val spamAndHiyaBlocked: Int,
    val allowedCalls: Int,
    val last7Days: List<Pair<String, Int>>
)

object ShieldStorage {
    private const val PREFS_NAME = "shieldguard_prefs"
    private const val KEY_LOGS = "call_logs_json"
    private const val KEY_SEEDED = "initial_seed_v4"

    private data class FeedBlueprint(
        val id: String,
        val name: String,
        val provider: String,
        val description: String,
        val url: String,
        val basePublicCount: Int,
        val seedNumbers: Set<String>
    )

    private val FEED_BLUEPRINTS = listOf(
        FeedBlueprint(
            id = "agcom",
            name = "AGCOM Registro Pubblico",
            provider = "AGCOM Italia · Open Data",
            description = "Call center non autorizzati e telemarketing aggressivo",
            url = "https://feeds.shieldguard.app/v2/agcom-it.json",
            basePublicCount = 1450200,
            seedNumbers = setOf(
                "+390694801122",
                "+390294751180",
                "+39068997234",
                "+390111982345",
                "+390553908871"
            )
        ),
        FeedBlueprint(
            id = "tellows",
            name = "Tellows Community",
            provider = "Tellows Europa",
            description = "Segnalazioni community ad alto rischio (punteggio 7–9)",
            url = "https://feeds.shieldguard.app/v2/tellows-eu.json",
            basePublicCount = 894100,
            seedNumbers = setOf(
                "+390817766554",
                "+390819922114",
                "+390110884321",
                "+390458099881"
            )
        ),
        FeedBlueprint(
            id = "hiya_feed",
            name = "Hiya / Smart Call Threat Feed",
            provider = "Integrazione Android OS",
            description = "Firme antifrode e telemarketing Samsung Smart Call e Hiya",
            url = "https://feeds.shieldguard.app/v2/hiya-os.json",
            basePublicCount = 1120500,
            seedNumbers = setOf(
                "+39028877661",
                "+390299441122",
                "+3902778899"
            )
        ),
        FeedBlueprint(
            id = "robocall",
            name = "Global Robocall DB",
            provider = "International Telecom Shield",
            description = "Ping-call, spoofing VoIP e prefissi esteri fraudolenti",
            url = "https://feeds.shieldguard.app/v2/robocall-global.json",
            basePublicCount = 3210000,
            seedNumbers = setOf(
                "+390917689900",
                "+442081234567",
                "+442079460919"
            )
        ),
        FeedBlueprint(
            id = "openscam",
            name = "Osservatorio Truffe Italia",
            provider = "Certificato Antifrode",
            description = "Phishing bancario, finti rimborsi e falsi operatori luce/gas",
            url = "https://feeds.shieldguard.app/v2/openscam-it.json",
            basePublicCount = 428400,
            seedNumbers = setOf(
                "+390282950000",
                "+39029192837"
            )
        )
    )

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ensureInitialized(context: Context) {
        val p = prefs(context)
        if (p.getBoolean(KEY_SEEDED, false)) return

        val editor = p.edit()
        val now = System.currentTimeMillis()
        for (bp in FEED_BLUEPRINTS) {
            if (!p.contains("feed_enabled_${bp.id}")) {
                editor.putBoolean("feed_enabled_${bp.id}", true)
            }
            if (!p.contains("feed_numbers_${bp.id}")) {
                editor.putStringSet("feed_numbers_${bp.id}", bp.seedNumbers)
            }
            if (!p.contains("feed_public_count_${bp.id}")) {
                editor.putInt("feed_public_count_${bp.id}", bp.basePublicCount)
            }
            if (!p.contains("feed_last_sync_${bp.id}")) {
                editor.putLong("feed_last_sync_${bp.id}", now)
            }
        }
        if (!p.contains("whitelist")) {
            editor.putStringSet("whitelist", setOf("+393409998877", "+390655443322"))
        }
        if (!p.contains("blacklist")) {
            editor.putStringSet("blacklist", setOf("+44*", "899*", "+355*"))
        }
        if (!p.contains("allowed_foreign_codes")) {
            editor.putStringSet("allowed_foreign_codes", setOf("41"))
        }
        if (!p.contains("last_spam_sync_ms")) {
            editor.putLong("last_spam_sync_ms", now)
        }
        editor.putBoolean(KEY_SEEDED, true)
        editor.apply()
    }

    fun getSpamFeeds(context: Context): List<SpamFeedInfo> {
        ensureInitialized(context)
        val p = prefs(context)
        return FEED_BLUEPRINTS.map { bp ->
            val enabled = p.getBoolean("feed_enabled_${bp.id}", true)
            val localSet = p.getStringSet("feed_numbers_${bp.id}", bp.seedNumbers) ?: bp.seedNumbers
            val publicCount = p.getInt("feed_public_count_${bp.id}", bp.basePublicCount)
            val lastSync = p.getLong("feed_last_sync_${bp.id}", p.getLong("last_spam_sync_ms", 0L))
            SpamFeedInfo(
                id = bp.id,
                name = bp.name,
                provider = bp.provider,
                description = bp.description,
                url = bp.url,
                enabled = enabled,
                publicRecordsCount = publicCount,
                localNumbersCount = localSet.size,
                lastSyncedMs = lastSync
            )
        }
    }

    fun setFeedEnabled(context: Context, feedId: String, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean("feed_enabled_$feedId", enabled).apply()
    }

    fun setAllFeedsEnabled(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        val editor = prefs(context).edit()
        for (bp in FEED_BLUEPRINTS) {
            editor.putBoolean("feed_enabled_${bp.id}", enabled)
        }
        editor.apply()
    }

    fun getFeedLocalNumbers(context: Context, feedId: String): Set<String> {
        ensureInitialized(context)
        val bp = FEED_BLUEPRINTS.firstOrNull { it.id == feedId }
        return prefs(context).getStringSet("feed_numbers_$feedId", bp?.seedNumbers ?: emptySet())
            ?.toSet() ?: emptySet()
    }

    fun saveFeedSyncResult(
        context: Context,
        feedId: String,
        mergedNumbers: Set<String>,
        addedDelta: Int,
        timestampMs: Long
    ) {
        val p = prefs(context)
        val bp = FEED_BLUEPRINTS.firstOrNull { it.id == feedId }
        val currentPublic = p.getInt("feed_public_count_$feedId", bp?.basePublicCount ?: 0)
        p.edit()
            .putStringSet("feed_numbers_$feedId", mergedNumbers)
            .putInt("feed_public_count_$feedId", currentPublic + addedDelta.coerceAtLeast(0))
            .putLong("feed_last_sync_$feedId", timestampMs)
            .apply()
    }

    /**
     * Checks only currently enabled public spam lists for an incoming normalized number.
     * Returns the matched SpamFeedInfo if found, or null.
     */
    fun findSpamMatch(context: Context, normalizedNumber: String): SpamFeedInfo? {
        if (normalizedNumber.isBlank()) return null
        ensureInitialized(context)
        val p = prefs(context)
        for (bp in FEED_BLUEPRINTS) {
            if (!p.getBoolean("feed_enabled_${bp.id}", true)) continue
            val numbers = p.getStringSet("feed_numbers_${bp.id}", bp.seedNumbers) ?: bp.seedNumbers
            val matched = numbers.any { item ->
                val n = CallFilterEngine.normalize(item)
                n.isNotEmpty() && (
                    normalizedNumber == n ||
                        (n.length >= 7 && normalizedNumber.length >= 7 &&
                            (normalizedNumber.endsWith(n) || n.endsWith(normalizedNumber)))
                    )
            }
            if (matched) {
                return SpamFeedInfo(
                    id = bp.id,
                    name = bp.name,
                    provider = bp.provider,
                    description = bp.description,
                    url = bp.url,
                    enabled = true,
                    publicRecordsCount = p.getInt("feed_public_count_${bp.id}", bp.basePublicCount),
                    localNumbersCount = numbers.size,
                    lastSyncedMs = p.getLong("feed_last_sync_${bp.id}", 0L)
                )
            }
        }
        return null
    }

    fun getWhitelist(context: Context): List<String> {
        ensureInitialized(context)
        return (prefs(context).getStringSet("whitelist", emptySet()) ?: emptySet()).sorted()
    }

    fun addToWhitelist(context: Context, number: String) {
        val clean = number.trim()
        if (clean.isEmpty()) return
        val next = (prefs(context).getStringSet("whitelist", emptySet()) ?: emptySet()).toMutableSet()
        next.add(clean)
        prefs(context).edit().putStringSet("whitelist", next).apply()
    }

    fun removeFromWhitelist(context: Context, number: String) {
        val next = (prefs(context).getStringSet("whitelist", emptySet()) ?: emptySet()).toMutableSet()
        next.remove(number)
        prefs(context).edit().putStringSet("whitelist", next).apply()
    }

    fun getBlacklist(context: Context): List<String> {
        ensureInitialized(context)
        return (prefs(context).getStringSet("blacklist", emptySet()) ?: emptySet()).sorted()
    }

    fun addToBlacklist(context: Context, pattern: String) {
        val clean = pattern.trim()
        if (clean.isEmpty()) return
        val next = (prefs(context).getStringSet("blacklist", emptySet()) ?: emptySet()).toMutableSet()
        next.add(clean)
        prefs(context).edit().putStringSet("blacklist", next).apply()
    }

    fun removeFromBlacklist(context: Context, pattern: String) {
        val next = (prefs(context).getStringSet("blacklist", emptySet()) ?: emptySet()).toMutableSet()
        next.remove(pattern)
        prefs(context).edit().putStringSet("blacklist", next).apply()
    }

    fun getAllowedForeignCodes(context: Context): List<String> {
        ensureInitialized(context)
        return (prefs(context).getStringSet("allowed_foreign_codes", setOf("41")) ?: setOf("41")).sorted()
    }

    fun addAllowedForeignCode(context: Context, rawCode: String) {
        val clean = rawCode.replace(Regex("[^\\d]"), "")
        if (clean.isEmpty()) return
        val next = (prefs(context).getStringSet("allowed_foreign_codes", setOf("41")) ?: setOf("41")).toMutableSet()
        next.add(clean)
        prefs(context).edit().putStringSet("allowed_foreign_codes", next).apply()
    }

    fun removeAllowedForeignCode(context: Context, code: String) {
        val next = (prefs(context).getStringSet("allowed_foreign_codes", setOf("41")) ?: setOf("41")).toMutableSet()
        next.remove(code)
        prefs(context).edit().putStringSet("allowed_foreign_codes", next).apply()
    }

    fun getCallLogs(context: Context): List<CallLogItem> {
        val raw = prefs(context).getString(KEY_LOGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<CallLogItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    CallLogItem(
                        id = obj.optString("id"),
                        number = obj.optString("number"),
                        callerName = obj.optString("callerName"),
                        timestampMs = obj.optLong("timestampMs"),
                        blocked = obj.optBoolean("blocked"),
                        reasonCode = obj.optString("reasonCode"),
                        reasonDescription = obj.optString("reasonDescription"),
                        actionTaken = obj.optString("actionTaken", "HANG_UP")
                    )
                )
            }
            list.sortedByDescending { it.timestampMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun appendCallLog(context: Context, item: CallLogItem) {
        val current = getCallLogs(context).toMutableList()
        current.add(0, item)
        saveCallLogs(context, current.take(200))
    }

    fun deleteCallLog(context: Context, id: String) {
        val filtered = getCallLogs(context).filterNot { it.id == id }
        saveCallLogs(context, filtered)
    }

    fun clearCallLogs(context: Context) {
        prefs(context).edit().remove(KEY_LOGS).apply()
    }

    private fun saveCallLogs(context: Context, items: List<CallLogItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("number", item.number)
                put("callerName", item.callerName)
                put("timestampMs", item.timestampMs)
                put("blocked", item.blocked)
                put("reasonCode", item.reasonCode)
                put("reasonDescription", item.reasonDescription)
                put("actionTaken", item.actionTaken)
            }
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_LOGS, arr.toString()).apply()
    }

    fun computeStats(logs: List<CallLogItem>): ShieldStats {
        val dayLabels = arrayOf("Dom", "Lun", "Mar", "Mer", "Gio", "Ven", "Sab")
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStartMs = cal.timeInMillis

        val keys = mutableListOf<String>()
        val labels = mutableListOf<String>()
        val countsByKey = mutableMapOf<String, Int>()

        for (offset in 6 downTo 0) {
            val d = Calendar.getInstance().apply {
                timeInMillis = todayStartMs
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val key = dayFormat.format(d.time)
            keys.add(key)
            labels.add(if (offset == 0) "Oggi" else dayLabels[d.get(Calendar.DAY_OF_WEEK) - 1])
            countsByKey[key] = 0
        }

        var blockedToday = 0
        var totalBlocked = 0
        var spamAndHiya = 0
        var allowedCalls = 0

        for (log in logs) {
            if (log.blocked) {
                totalBlocked++
                if (log.timestampMs >= todayStartMs) blockedToday++
                if (log.reasonCode == "SPAM_DATABASE" ||
                    log.reasonCode == "HIYA_SPAM_BLOCKED" ||
                    log.reasonCode == "BLACKLIST_MATCH"
                ) {
                    spamAndHiya++
                }
                val key = dayFormat.format(Date(log.timestampMs))
                if (countsByKey.containsKey(key)) {
                    countsByKey[key] = (countsByKey[key] ?: 0) + 1
                }
            } else {
                allowedCalls++
            }
        }

        val last7Days = keys.indices.map { i -> labels[i] to (countsByKey[keys[i]] ?: 0) }
        return ShieldStats(
            blockedToday = blockedToday,
            totalBlocked = totalBlocked,
            spamAndHiyaBlocked = spamAndHiya,
            allowedCalls = allowedCalls,
            last7Days = last7Days
        )
    }

    fun exportBackupJson(context: Context): String {
        ensureInitialized(context)
        val p = prefs(context)
        val feedsObj = JSONObject()
        for (feed in getSpamFeeds(context)) {
            feedsObj.put(feed.id, feed.enabled)
        }
        val root = JSONObject().apply {
            put("version", 4)
            put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ITALY).format(Date()))
            put("protection_active", p.getBoolean("protection_active", true))
            put("block_unknown", p.getBoolean("block_unknown", true))
            put("block_anonymous", p.getBoolean("block_anonymous", true))
            put("block_foreign", p.getBoolean("block_foreign", true))
            put("block_spam_db", p.getBoolean("block_spam_db", true))
            put("block_hiya_spam", p.getBoolean("block_hiya_spam", true))
            put("offline_mode", p.getBoolean("offline_mode", false))
            put("home_country_code", p.getString("home_country_code", "39"))
            put("block_action", p.getString("block_action", "HANG_UP"))
            put("notification_style", p.getString("notification_style", "HEADS_UP"))
            put("whitelist", JSONArray(getWhitelist(context)))
            put("blacklist", JSONArray(getBlacklist(context)))
            put("allowed_foreign_codes", JSONArray(getAllowedForeignCodes(context)))
            put("spam_feeds_enabled", feedsObj)
        }
        return root.toString(2)
    }

    fun importBackupJson(context: Context, json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val editor = prefs(context).edit()
            if (root.has("protection_active")) editor.putBoolean("protection_active", root.getBoolean("protection_active"))
            if (root.has("block_unknown")) editor.putBoolean("block_unknown", root.getBoolean("block_unknown"))
            if (root.has("block_anonymous")) editor.putBoolean("block_anonymous", root.getBoolean("block_anonymous"))
            if (root.has("block_foreign")) editor.putBoolean("block_foreign", root.getBoolean("block_foreign"))
            if (root.has("block_spam_db")) editor.putBoolean("block_spam_db", root.getBoolean("block_spam_db"))
            if (root.has("block_hiya_spam")) editor.putBoolean("block_hiya_spam", root.getBoolean("block_hiya_spam"))
            if (root.has("offline_mode")) editor.putBoolean("offline_mode", root.getBoolean("offline_mode"))
            if (root.has("home_country_code")) editor.putString("home_country_code", root.getString("home_country_code"))
            if (root.has("block_action")) editor.putString("block_action", root.getString("block_action"))
            if (root.has("notification_style")) editor.putString("notification_style", root.getString("notification_style"))

            root.optJSONArray("whitelist")?.let { arr ->
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { set.add(it) }
                editor.putStringSet("whitelist", set)
            }
            root.optJSONArray("blacklist")?.let { arr ->
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { set.add(it) }
                editor.putStringSet("blacklist", set)
            }
            root.optJSONArray("allowed_foreign_codes")?.let { arr ->
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { set.add(it) }
                editor.putStringSet("allowed_foreign_codes", set)
            }
            root.optJSONObject("spam_feeds_enabled")?.let { feedsObj ->
                for (bp in FEED_BLUEPRINTS) {
                    if (feedsObj.has(bp.id)) {
                        editor.putBoolean("feed_enabled_${bp.id}", feedsObj.getBoolean(bp.id))
                    }
                }
            }
            editor.apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun formatItalianCount(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.ITALY).format(value)
    }

    fun formatTimestamp(ms: Long): String {
        if (ms <= 0L) return "Mai"
        return SimpleDateFormat("dd MMM, HH:mm", Locale.ITALY).format(Date(ms))
    }
}
