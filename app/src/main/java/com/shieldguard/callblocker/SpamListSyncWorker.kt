package com.shieldguard.callblocker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class SyncOutcome(
    val skippedOffline: Boolean,
    val feedsSynced: Int,
    val newEntriesAdded: Int
)

/**
 * Automated periodic WorkManager worker that downloads only the individually enabled
 * public spam lists under battery-safe constraints and respects Offline Privacy Mode.
 */
class SpamListSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        runSyncNow(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "shieldguard_daily_spam_sync"

        suspend fun runSyncNow(context: Context): SyncOutcome = withContext(Dispatchers.IO) {
            ShieldStorage.ensureInitialized(context)
            val prefs = ShieldStorage.prefs(context)
            if (prefs.getBoolean("offline_mode", false)) {
                return@withContext SyncOutcome(
                    skippedOffline = true,
                    feedsSynced = 0,
                    newEntriesAdded = 0
                )
            }

            val enabledFeeds = ShieldStorage.getSpamFeeds(context).filter { it.enabled }
            if (enabledFeeds.isEmpty()) {
                return@withContext SyncOutcome(
                    skippedOffline = false,
                    feedsSynced = 0,
                    newEntriesAdded = 0
                )
            }

            val now = System.currentTimeMillis()
            var totalAdded = 0

            for (feed in enabledFeeds) {
                val merged = ShieldStorage.getFeedLocalNumbers(context, feed.id).toMutableSet()
                val beforeSize = merged.size
                try {
                    val conn = (URL(feed.url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        requestMethod = "GET"
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            val number = CallFilterEngine.normalize(arr.optString(i))
                            if (number.isNotEmpty()) merged.add(number)
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {
                    // Keep verified local feed cache intact when remote endpoint is unreachable
                }

                val added = (merged.size - beforeSize).coerceAtLeast(0)
                totalAdded += added
                ShieldStorage.saveFeedSyncResult(
                    context = context,
                    feedId = feed.id,
                    mergedNumbers = merged,
                    addedDelta = added,
                    timestampMs = now
                )
            }

            prefs.edit().putLong("last_spam_sync_ms", now).apply()

            SyncOutcome(
                skippedOffline = false,
                feedsSynced = enabledFeeds.size,
                newEntriesAdded = totalAdded
            )
        }

        fun scheduleSync(
            context: Context,
            enabled: Boolean,
            intervalHours: Long = 24L,
            wifiOnly: Boolean = true,
            chargingOnly: Boolean = false
        ) {
            val wm = WorkManager.getInstance(context)
            if (!enabled || intervalHours <= 0L) {
                wm.cancelUniqueWork(UNIQUE_WORK_NAME)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(chargingOnly)
                .build()

            val request = PeriodicWorkRequestBuilder<SpamListSyncWorker>(
                intervalHours.coerceAtLeast(6L),
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            wm.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
