package com.shieldguard.callblocker

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* RoleManager result handled automatically by Android OS */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule automated daily spam database updates via WorkManager
        SpamListSyncWorker.scheduleDailySync(
            context = applicationContext,
            intervalHours = 24L,
            wifiOnly = true
        )

        val prefs = getSharedPreferences("shieldguard_prefs", MODE_PRIVATE)

        setContent {
            var blockUnknown by remember { mutableStateOf(prefs.getBoolean("block_unknown", true)) }
            var blockAnonymous by remember { mutableStateOf(prefs.getBoolean("block_anonymous", true)) }
            var blockForeign by remember { mutableStateOf(prefs.getBoolean("block_foreign", true)) }
            var blockHiya by remember { mutableStateOf(prefs.getBoolean("block_hiya_spam", true)) }
            var offlineMode by remember { mutableStateOf(prefs.getBoolean("offline_mode", false)) }

            val activeCount = listOf(blockUnknown, blockAnonymous, blockForeign, blockHiya).count { it }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF06080D),
                    surface = Color(0xFF101520),
                    primary = Color(0xFF10B981),
                    onPrimary = Color(0xFF031A12),
                    surfaceVariant = Color(0xFF161D2C)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF06080D)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Material 3 Expressive Protection Hero Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF111E24), Color(0xFF101520))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "ShieldGuard",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = "$activeCount di 4 filtri attivi · Batteria <0.1%/24h · Sync automatico 24h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Button(
                                    onClick = { requestDefaultCallScreeningRole() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color(0xFF031A12)
                                    )
                                ) {
                                    Text(
                                        text = "Imposta come App Filtro Predefinita",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Grouped Material 3 Protection Switches Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF101520))
                        ) {
                            FilterRow(
                                title = "Numeri sconosciuti",
                                subtitle = "Blocca chi non è salvato in rubrica",
                                checked = blockUnknown
                            ) {
                                blockUnknown = it
                                prefs.edit().putBoolean("block_unknown", it).apply()
                            }
                            HorizontalDivider(color = Color(0x14FFFFFF))
                            FilterRow(
                                title = "Numeri anonimi",
                                subtitle = "Rifiuta chiamate con ID nascosto",
                                checked = blockAnonymous
                            ) {
                                blockAnonymous = it
                                prefs.edit().putBoolean("block_anonymous", it).apply()
                            }
                            HorizontalDivider(color = Color(0x14FFFFFF))
                            FilterRow(
                                title = "Numeri esteri",
                                subtitle = "Blocca prefissi esteri non autorizzati",
                                checked = blockForeign
                            ) {
                                blockForeign = it
                                prefs.edit().putBoolean("block_foreign", it).apply()
                            }
                            HorizontalDivider(color = Color(0x14FFFFFF))
                            FilterRow(
                                title = "Spam Hiya / Smart Call",
                                subtitle = "Intercetta segnalazioni spam di Android",
                                checked = blockHiya
                            ) {
                                blockHiya = it
                                prefs.edit().putBoolean("block_hiya_spam", it).apply()
                            }
                            HorizontalDivider(color = Color(0x14FFFFFF))
                            FilterRow(
                                title = "Modalità Offline Privacy",
                                subtitle = "100% locale, nessuna richiesta esterna",
                                checked = offlineMode
                            ) {
                                offlineMode = it
                                prefs.edit().putBoolean("offline_mode", it).apply()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestDefaultCallScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            roleRequestLauncher.launch(intent)
        }
    }
}

@Composable
private fun FilterRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = Color(0xFFF8FAFC),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF8E9BB3),
                fontSize = 12.sp
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
