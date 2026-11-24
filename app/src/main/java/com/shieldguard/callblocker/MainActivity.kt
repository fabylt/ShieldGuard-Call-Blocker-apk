package com.shieldguard.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private data class NavDestination(
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {

    private var refreshCounter by mutableIntStateOf(0)

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshCounter++
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshCounter++
    }

    override fun onResume() {
        super.onResume()
        refreshCounter++
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = 0xFF06080D.toInt()
        window.navigationBarColor = 0xFF0B0F18.toInt()

        ShieldStorage.ensureInitialized(applicationContext)
        val prefs = ShieldStorage.prefs(applicationContext)

        // Ensure WorkManager periodic spam sync matches saved preferences
        SpamListSyncWorker.scheduleSync(
            context = applicationContext,
            enabled = prefs.getBoolean("auto_sync_enabled", true),
            intervalHours = prefs.getLong("sync_interval_hours", 24L),
            wifiOnly = prefs.getBoolean("sync_wifi_only", true),
            chargingOnly = prefs.getBoolean("sync_charging_only", false)
        )

        setContent {
            val scope = rememberCoroutineScope()
            val tick = refreshCounter

            var selectedTab by remember { mutableIntStateOf(0) }
            var showTestDialog by remember { mutableStateOf(false) }

            var protectionActive by remember { mutableStateOf(prefs.getBoolean("protection_active", true)) }
            var blockUnknown by remember { mutableStateOf(prefs.getBoolean("block_unknown", true)) }
            var blockAnonymous by remember { mutableStateOf(prefs.getBoolean("block_anonymous", true)) }
            var blockForeign by remember { mutableStateOf(prefs.getBoolean("block_foreign", true)) }
            var blockSpamDb by remember { mutableStateOf(prefs.getBoolean("block_spam_db", true)) }
            var blockHiya by remember { mutableStateOf(prefs.getBoolean("block_hiya_spam", true)) }
            var offlineMode by remember { mutableStateOf(prefs.getBoolean("offline_mode", false)) }

            var blockAction by remember { mutableStateOf(prefs.getString("block_action", "HANG_UP") ?: "HANG_UP") }
            var notificationStyle by remember { mutableStateOf(prefs.getString("notification_style", "HEADS_UP") ?: "HEADS_UP") }
            var homeCountryCode by remember { mutableStateOf(prefs.getString("home_country_code", "39") ?: "39") }

            var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("auto_sync_enabled", true)) }
            var syncIntervalHours by remember { mutableLongStateOf(prefs.getLong("sync_interval_hours", 24L)) }
            var wifiOnly by remember { mutableStateOf(prefs.getBoolean("sync_wifi_only", true)) }
            var chargingOnly by remember { mutableStateOf(prefs.getBoolean("sync_charging_only", false)) }
            var lastSyncMs by remember { mutableLongStateOf(prefs.getLong("last_spam_sync_ms", System.currentTimeMillis())) }
            var isSyncing by remember { mutableStateOf(false) }

            var spamFeeds by remember(tick) { mutableStateOf(ShieldStorage.getSpamFeeds(applicationContext)) }
            var callLogs by remember(tick) { mutableStateOf(ShieldStorage.getCallLogs(applicationContext)) }
            var whitelist by remember(tick) { mutableStateOf(ShieldStorage.getWhitelist(applicationContext)) }
            var blacklist by remember(tick) { mutableStateOf(ShieldStorage.getBlacklist(applicationContext)) }
            var allowedForeignCodes by remember(tick) { mutableStateOf(ShieldStorage.getAllowedForeignCodes(applicationContext)) }

            val isDefaultApp = remember(tick) { checkIsDefaultCallScreeningApp() }
            val hasContactsPermission = remember(tick) {
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            }
            val stats = remember(callLogs) { ShieldStorage.computeStats(callLogs) }

            val destinations = remember {
                listOf(
                    NavDestination("Scudo", Icons.Rounded.Shield),
                    NavDestination("Registro", Icons.Rounded.History),
                    NavDestination("Liste", Icons.Rounded.Checklist),
                    NavDestination("Spam", Icons.Rounded.Storage),
                    NavDestination("Opzioni", Icons.Rounded.Settings)
                )
            }

            fun rescheduleWorker(enabled: Boolean, hours: Long, wifi: Boolean, charging: Boolean) {
                prefs.edit()
                    .putBoolean("auto_sync_enabled", enabled)
                    .putLong("sync_interval_hours", hours)
                    .putBoolean("sync_wifi_only", wifi)
                    .putBoolean("sync_charging_only", charging)
                    .apply()
                SpamListSyncWorker.scheduleSync(applicationContext, enabled, hours, wifi, charging)
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BgOled,
                    surface = SurfaceCard,
                    primary = AccentEmerald,
                    onPrimary = Color(0xFF031A12),
                    surfaceVariant = SurfaceElevated
                )
            ) {
                Scaffold(
                    containerColor = BgOled,
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF0B0F18),
                            tonalElevation = 0.dp
                        ) {
                            destinations.forEachIndexed { index, dest ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentEmeraldLight,
                                        selectedTextColor = TextPrimary,
                                        indicatorColor = AccentEmeraldSoft,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> ShieldDashboardScreen(
                                isDefaultApp = isDefaultApp,
                                protectionActive = protectionActive,
                                blockUnknown = blockUnknown,
                                blockAnonymous = blockAnonymous,
                                blockForeign = blockForeign,
                                blockSpamDb = blockSpamDb,
                                blockHiya = blockHiya,
                                offlineMode = offlineMode,
                                homeCountryCode = homeCountryCode,
                                allowedForeignCodes = allowedForeignCodes,
                                spamFeeds = spamFeeds,
                                stats = stats,
                                onRequestDefaultRole = { requestDefaultCallScreeningRole() },
                                onToggleProtection = {
                                    protectionActive = it
                                    prefs.edit().putBoolean("protection_active", it).apply()
                                },
                                onToggleUnknown = {
                                    blockUnknown = it
                                    prefs.edit().putBoolean("block_unknown", it).apply()
                                },
                                onToggleAnonymous = {
                                    blockAnonymous = it
                                    prefs.edit().putBoolean("block_anonymous", it).apply()
                                },
                                onToggleForeign = {
                                    blockForeign = it
                                    prefs.edit().putBoolean("block_foreign", it).apply()
                                },
                                onToggleSpamDb = {
                                    blockSpamDb = it
                                    prefs.edit().putBoolean("block_spam_db", it).apply()
                                },
                                onToggleHiya = {
                                    blockHiya = it
                                    prefs.edit().putBoolean("block_hiya_spam", it).apply()
                                },
                                onOpenSpamTab = { selectedTab = 3 },
                                onOpenTestDialog = { showTestDialog = true }
                            )

                            1 -> CallLogsScreen(
                                logs = callLogs,
                                onPromoteToWhitelist = { number ->
                                    ShieldStorage.addToWhitelist(applicationContext, number)
                                    whitelist = ShieldStorage.getWhitelist(applicationContext)
                                    Toast.makeText(this@MainActivity, "$number aggiunto alla Whitelist", Toast.LENGTH_SHORT).show()
                                },
                                onDeleteLog = { id ->
                                    ShieldStorage.deleteCallLog(applicationContext, id)
                                    callLogs = ShieldStorage.getCallLogs(applicationContext)
                                },
                                onClearLogs = {
                                    ShieldStorage.clearCallLogs(applicationContext)
                                    callLogs = emptyList()
                                },
                                onOpenTestDialog = { showTestDialog = true }
                            )

                            2 -> ListsManagementScreen(
                                whitelist = whitelist,
                                blacklist = blacklist,
                                allowedForeignCodes = allowedForeignCodes,
                                onAddWhitelist = {
                                    ShieldStorage.addToWhitelist(applicationContext, it)
                                    whitelist = ShieldStorage.getWhitelist(applicationContext)
                                },
                                onRemoveWhitelist = {
                                    ShieldStorage.removeFromWhitelist(applicationContext, it)
                                    whitelist = ShieldStorage.getWhitelist(applicationContext)
                                },
                                onAddBlacklist = {
                                    ShieldStorage.addToBlacklist(applicationContext, it)
                                    blacklist = ShieldStorage.getBlacklist(applicationContext)
                                },
                                onRemoveBlacklist = {
                                    ShieldStorage.removeFromBlacklist(applicationContext, it)
                                    blacklist = ShieldStorage.getBlacklist(applicationContext)
                                },
                                onAddForeignCode = {
                                    ShieldStorage.addAllowedForeignCode(applicationContext, it)
                                    allowedForeignCodes = ShieldStorage.getAllowedForeignCodes(applicationContext)
                                },
                                onRemoveForeignCode = {
                                    ShieldStorage.removeAllowedForeignCode(applicationContext, it)
                                    allowedForeignCodes = ShieldStorage.getAllowedForeignCodes(applicationContext)
                                }
                            )

                            3 -> SpamListsScreen(
                                feeds = spamFeeds,
                                autoSyncEnabled = autoSyncEnabled,
                                syncIntervalHours = syncIntervalHours,
                                wifiOnly = wifiOnly,
                                chargingOnly = chargingOnly,
                                offlineMode = offlineMode,
                                lastSyncMs = lastSyncMs,
                                isSyncing = isSyncing,
                                onToggleFeed = { id, enabled ->
                                    ShieldStorage.setFeedEnabled(applicationContext, id, enabled)
                                    spamFeeds = ShieldStorage.getSpamFeeds(applicationContext)
                                },
                                onToggleAllFeeds = { enabled ->
                                    ShieldStorage.setAllFeedsEnabled(applicationContext, enabled)
                                    spamFeeds = ShieldStorage.getSpamFeeds(applicationContext)
                                },
                                onToggleAutoSync = { enabled ->
                                    autoSyncEnabled = enabled
                                    val hrs = if (enabled && syncIntervalHours <= 0L) 24L else syncIntervalHours
                                    syncIntervalHours = hrs
                                    rescheduleWorker(enabled, hrs, wifiOnly, chargingOnly)
                                },
                                onSelectIntervalHours = { hours ->
                                    if (hours <= 0L) {
                                        autoSyncEnabled = false
                                        syncIntervalHours = 0L
                                        rescheduleWorker(false, 0L, wifiOnly, chargingOnly)
                                    } else {
                                        autoSyncEnabled = true
                                        syncIntervalHours = hours
                                        rescheduleWorker(true, hours, wifiOnly, chargingOnly)
                                    }
                                },
                                onToggleWifiOnly = {
                                    wifiOnly = it
                                    rescheduleWorker(autoSyncEnabled, syncIntervalHours, it, chargingOnly)
                                },
                                onToggleChargingOnly = {
                                    chargingOnly = it
                                    rescheduleWorker(autoSyncEnabled, syncIntervalHours, wifiOnly, it)
                                },
                                onSyncNow = {
                                    scope.launch {
                                        isSyncing = true
                                        val result = SpamListSyncWorker.runSyncNow(applicationContext)
                                        spamFeeds = ShieldStorage.getSpamFeeds(applicationContext)
                                        lastSyncMs = prefs.getLong("last_spam_sync_ms", System.currentTimeMillis())
                                        isSyncing = false
                                        val msg = when {
                                            result.skippedOffline -> "Modalità Offline attiva: sync saltato"
                                            result.feedsSynced == 0 -> "Seleziona almeno una lista spam"
                                            else -> "${result.feedsSynced} liste sincronizzate"
                                        }
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            4 -> SettingsScreen(
                                blockAction = blockAction,
                                notificationStyle = notificationStyle,
                                offlineMode = offlineMode,
                                homeCountryCode = homeCountryCode,
                                hasContactsPermission = hasContactsPermission,
                                onSelectBlockAction = {
                                    blockAction = it
                                    prefs.edit().putString("block_action", it).apply()
                                },
                                onSelectNotificationStyle = {
                                    notificationStyle = it
                                    prefs.edit().putString("notification_style", it).apply()
                                },
                                onToggleOfflineMode = {
                                    offlineMode = it
                                    prefs.edit().putBoolean("offline_mode", it).apply()
                                },
                                onChangeHomeCountry = {
                                    homeCountryCode = it
                                    prefs.edit().putString("home_country_code", it).apply()
                                },
                                onRequestPermissions = { requestAppPermissions() },
                                onShareBackup = { shareBackupJson() },
                                onImportBackupJson = { json ->
                                    val ok = ShieldStorage.importBackupJson(applicationContext, json)
                                    if (ok) {
                                        protectionActive = prefs.getBoolean("protection_active", true)
                                        blockUnknown = prefs.getBoolean("block_unknown", true)
                                        blockAnonymous = prefs.getBoolean("block_anonymous", true)
                                        blockForeign = prefs.getBoolean("block_foreign", true)
                                        blockSpamDb = prefs.getBoolean("block_spam_db", true)
                                        blockHiya = prefs.getBoolean("block_hiya_spam", true)
                                        offlineMode = prefs.getBoolean("offline_mode", false)
                                        blockAction = prefs.getString("block_action", "HANG_UP") ?: "HANG_UP"
                                        notificationStyle = prefs.getString("notification_style", "HEADS_UP") ?: "HEADS_UP"
                                        homeCountryCode = prefs.getString("home_country_code", "39") ?: "39"
                                        refreshCounter++
                                        Toast.makeText(this@MainActivity, "Backup ripristinato", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "JSON backup non valido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                if (showTestDialog) {
                    CallSimulatorDialog(
                        onDismiss = { showTestDialog = false },
                        onSimulate = { number, isAnon, isHiya ->
                            val decision = CallFilterEngine.evaluate(
                                context = applicationContext,
                                rawNumber = number,
                                isAnonymous = isAnon,
                                callerDisplayName = if (isHiya) "Sospetto Spam Hiya" else null,
                                verificationFailed = isHiya
                            )
                            ShieldStorage.appendCallLog(
                                applicationContext,
                                CallLogItem(
                                    id = "sim-${System.currentTimeMillis()}",
                                    number = if (isAnon) "Numero Privato" else number,
                                    callerName = decision.callerLabel,
                                    timestampMs = System.currentTimeMillis(),
                                    blocked = decision.shouldBlock,
                                    reasonCode = decision.reasonCode,
                                    reasonDescription = decision.reasonDescription,
                                    actionTaken = if (decision.shouldBlock) blockAction else "RING"
                                )
                            )
                            callLogs = ShieldStorage.getCallLogs(applicationContext)
                            decision
                        }
                    )
                }
            }
        }
    }

    private fun checkIsDefaultCallScreeningApp(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }

    private fun requestDefaultCallScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            roleRequestLauncher.launch(intent)
        }
    }

    private fun requestAppPermissions() {
        val toRequest = mutableListOf(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(toRequest.toTypedArray())
    }

    private fun shareBackupJson() {
        val json = ShieldStorage.exportBackupJson(applicationContext)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "ShieldGuard Backup")
            putExtra(Intent.EXTRA_TEXT, json)
        }
        startActivity(Intent.createChooser(sendIntent, "Salva o condividi Backup ShieldGuard"))
    }
}
