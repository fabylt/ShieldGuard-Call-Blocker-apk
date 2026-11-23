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

/**
 * Automated daily WorkManager worker that downloads verified public spam lists
 * only under battery-safe constraints (Wi-Fi + Battery Not Low) and respects Offline Mode.
 */
class SpamListSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("shieldguard_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("offline_mode", false)) {
            return@withContext Result.success()
        }

        val feedUrls = listOf(
            "https://feeds.shieldguard.app/v2/agcom-it.json",
            "https://feeds.shieldguard.app/v2/tellows-eu.json",
            "https://feeds.shieldguard.app/v2/hiya-os.json",
            "https://feeds.shieldguard.app/v2/robocall-global.json"
        )

        val currentNumbers = prefs.getStringSet("spam_db_numbers", emptySet())?.toMutableSet()
            ?: mutableSetOf()

        for (feedUrl in feedUrls) {
            try {
                val conn = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val number = CallFilterEngine.normalize(arr.optString(i))
                        if (number.isNotEmpty()) currentNumbers.add(number)
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
                // Continue with remaining verified feeds
            }
        }

        prefs.edit()
            .putStringSet("spam_db_numbers", currentNumbers)
            .putLong("last_spam_sync_ms", System.currentTimeMillis())
            .apply()

        Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "shieldguard_daily_spam_sync"

        fun scheduleDailySync(context: Context, intervalHours: Long = 24L, wifiOnly: Boolean = true) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SpamListSyncWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
