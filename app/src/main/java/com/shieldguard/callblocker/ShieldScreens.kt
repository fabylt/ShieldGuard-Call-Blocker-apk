package com.shieldguard.callblocker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgOled = Color(0xFF06080D)
val SurfaceCard = Color(0xFF101520)
val SurfaceElevated = Color(0xFF161D2C)
val BorderSubtle = Color(0x16FFFFFF)
val AccentEmerald = Color(0xFF10B981)
val AccentEmeraldLight = Color(0xFF34D399)
val AccentEmeraldSoft = Color(0x2610B981)
val DangerRose = Color(0xFFFB7185)
val DangerRoseSoft = Color(0x24FB7185)
val WarningAmber = Color(0xFFFBBF24)
val InfoSky = Color(0xFF38BDF8)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

@Composable
fun ShieldSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF031A12),
            checkedTrackColor = AccentEmerald,
            uncheckedThumbColor = Color(0xFF94A3B8),
            uncheckedTrackColor = SurfaceElevated,
            uncheckedBorderColor = Color(0xFF33415E)
        )
    )
}

@Composable
fun ShieldDashboardScreen(
    isDefaultApp: Boolean,
    protectionActive: Boolean,
    blockUnknown: Boolean,
    blockAnonymous: Boolean,
    blockForeign: Boolean,
    blockSpamDb: Boolean,
    blockHiya: Boolean,
    offlineMode: Boolean,
    homeCountryCode: String,
    allowedForeignCodes: List<String>,
    spamFeeds: List<SpamFeedInfo>,
    stats: ShieldStats,
    onRequestDefaultRole: () -> Unit,
    onToggleProtection: (Boolean) -> Unit,
    onToggleUnknown: (Boolean) -> Unit,
    onToggleAnonymous: (Boolean) -> Unit,
    onToggleForeign: (Boolean) -> Unit,
    onToggleSpamDb: (Boolean) -> Unit,
    onToggleHiya: (Boolean) -> Unit,
    onOpenSpamTab: () -> Unit,
    onOpenTestDialog: () -> Unit
) {
    val activeFilters = listOf(
        blockUnknown,
        blockAnonymous,
        blockForeign,
        blockSpamDb,
        blockHiya
    ).count { it }
    val enabledFeeds = spamFeeds.filter { it.enabled }
    val totalActivePublicNumbers = enabledFeeds.sumOf { it.publicRecordsCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    if (protectionActive) {
                        Brush.verticalGradient(listOf(Color(0xFF112321), SurfaceCard))
                    } else {
                        Brush.verticalGradient(listOf(SurfaceElevated, SurfaceCard))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (protectionActive) Color(0x3D10B981) else BorderSubtle,
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (protectionActive) AccentEmeraldSoft else SurfaceElevated)
                                .border(
                                    2.dp,
                                    if (protectionActive) AccentEmerald else TextMuted,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = "Scudo ShieldGuard",
                                tint = if (protectionActive) AccentEmeraldLight else TextMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (protectionActive) "Protezione attiva" else "Scudo in pausa",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$activeFilters/5 filtri · Batteria <0.1%/24h" +
                                    if (offlineMode) " · Offline" else "",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }

                    ShieldSwitch(
                        checked = protectionActive,
                        onCheckedChange = onToggleProtection
                    )
                }

                // Default Call Screening Role Action (never clips text)
                if (isDefaultApp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AccentEmeraldSoft)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = AccentEmeraldLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "App Filtro Predefinita attiva",
                                color = AccentEmeraldLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = onOpenTestDialog,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Testa", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRequestDefaultRole,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentEmerald,
                                contentColor = Color(0xFF031A12)
                            )
                        ) {
                            Text(
                                text = "Imposta App Predefinita",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenTestDialog,
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Testa", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 4 KPI Metric Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiTile("Bloccate oggi", stats.blockedToday.toString(), TextPrimary, Modifier.weight(1f))
            KpiTile("Totale bloccate", stats.totalBlocked.toString(), DangerRose, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiTile("Spam e Hiya", stats.spamAndHiyaBlocked.toString(), TextPrimary, Modifier.weight(1f))
            KpiTile("Consentite", stats.allowedCalls.toString(), AccentEmeraldLight, Modifier.weight(1f))
        }

        // 7-Day Visual Statistics Chart Card
        WeeklyStatsChartCard(stats.last7Days)

        // Grouped Quick Filters Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtri di protezione",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "$activeFilters di 5 attivi",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = BorderSubtle)

            FilterToggleRow(
                icon = Icons.Rounded.PersonOff,
                title = "Numeri sconosciuti",
                subtitle = "Blocca chi non è presente in rubrica o whitelist",
                checked = blockUnknown,
                enabled = protectionActive,
                onCheckedChange = onToggleUnknown
            )
            HorizontalDivider(color = BorderSubtle)

            FilterToggleRow(
                icon = Icons.Rounded.Block,
                title = "Numeri anonimi",
                subtitle = "Rifiuta chiamate con ID chiamante nascosto",
                checked = blockAnonymous,
                enabled = protectionActive,
                onCheckedChange = onToggleAnonymous
            )
            HorizontalDivider(color = BorderSubtle)

            val foreignSub = "Prefissi diversi da +$homeCountryCode" +
                if (allowedForeignCodes.isNotEmpty()) {
                    " · eccetto +${allowedForeignCodes.joinToString(", +")}"
                } else {
                    ""
                }
            FilterToggleRow(
                icon = Icons.Rounded.Public,
                title = "Numeri esteri",
                subtitle = foreignSub,
                checked = blockForeign,
                enabled = protectionActive,
                onCheckedChange = onToggleForeign
            )
            HorizontalDivider(color = BorderSubtle)

            FilterToggleRow(
                icon = Icons.Rounded.Storage,
                title = "Liste spam pubbliche",
                subtitle = "${enabledFeeds.size}/${spamFeeds.size} liste attive · ${ShieldStorage.formatItalianCount(totalActivePublicNumbers)} numeri",
                checked = blockSpamDb,
                enabled = protectionActive,
                onCheckedChange = onToggleSpamDb,
                onRowClick = onOpenSpamTab
            )
            HorizontalDivider(color = BorderSubtle)

            FilterToggleRow(
                icon = Icons.Rounded.Smartphone,
                title = "Spam Hiya / Smart Call",
                subtitle = "Blocca chiamate segnalate come spam da Android",
                checked = blockHiya,
                enabled = protectionActive,
                onCheckedChange = onToggleHiya
            )
        }
    }
}

@Composable
private fun KpiTile(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WeeklyStatsChartCard(last7Days: List<Pair<String, Int>>) {
    val weekTotal = last7Days.sumOf { it.second }
    val maxVal = (last7Days.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Statistiche ultimi 7 giorni",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chiamate bloccate per giorno",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = weekTotal.toString(),
                    color = AccentEmeraldLight,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "ultimi 7 gg", color = TextMuted, fontSize = 11.sp)
            }
        }

        // Native Compose Canvas Bar Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            for ((label, count) in last7Days) {
                val fraction = if (count == 0) 0.08f else (count.toFloat() / maxVal.toFloat()).coerceIn(0.16f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = count.toString(),
                        color = if (count > 0) AccentEmeraldLight else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Canvas(
                        modifier = Modifier
                            .width(22.dp)
                            .height(78.dp)
                    ) {
                        val trackRadius = CornerRadius(12f, 12f)
                        drawRoundRect(
                            color = Color(0xFF161D2C),
                            topLeft = Offset.Zero,
                            size = size,
                            cornerRadius = trackRadius
                        )
                        val barH = size.height * fraction
                        drawRoundRect(
                            color = if (count > 0) AccentEmerald else Color(0xFF28334A),
                            topLeft = Offset(0f, size.height - barH),
                            size = Size(size.width, barH),
                            cornerRadius = trackRadius
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = label,
                        color = if (label == "Oggi") TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (label == "Oggi") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (onRowClick != null) onRowClick() else onCheckedChange(!checked)
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (checked && enabled) AccentEmeraldSoft else SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked && enabled) AccentEmeraldLight else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        ShieldSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun CallLogsScreen(
    logs: List<CallLogItem>,
    onPromoteToWhitelist: (String) -> Unit,
    onDeleteLog: (String) -> Unit,
    onClearLogs: () -> Unit,
    onOpenTestDialog: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filters = listOf(
        "ALL" to "Tutte",
        "BLOCKED" to "Bloccate",
        "SPAM" to "Spam",
        "HIYA" to "Hiya",
        "ANONYMOUS" to "Anonime",
        "FOREIGN" to "Estere"
    )

    val filtered = remember(logs, selectedFilter) {
        logs.filter { log ->
            when (selectedFilter) {
                "BLOCKED" -> log.blocked
                "SPAM" -> log.reasonCode == "SPAM_DATABASE" || log.reasonCode == "BLACKLIST_MATCH"
                "HIYA" -> log.reasonCode == "HIYA_SPAM_BLOCKED"
                "ANONYMOUS" -> log.reasonCode == "ANONYMOUS_BLOCKED"
                "FOREIGN" -> log.reasonCode == "FOREIGN_BLOCKED"
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Registro Chiamate", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${filtered.size} chiamate registrate", color = TextSecondary, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenTestDialog,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Simula", color = AccentEmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                if (logs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearLogs,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Svuota", color = DangerRose, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((code, label) in filters) {
                val active = selectedFilter == code
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (active) AccentEmeraldSoft else SurfaceCard)
                        .border(1.dp, if (active) AccentEmerald else BorderSubtle, CircleShape)
                        .clickable { selectedFilter = code }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = label,
                        color = if (active) AccentEmeraldLight else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nessuna chiamata nel registro", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Le chiamate reali bloccate appariranno qui automaticamente. Puoi anche provare il pulsante Simula.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
            ) {
                filtered.forEachIndexed { idx, log ->
                    if (idx > 0) HorizontalDivider(color = BorderSubtle)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (log.blocked) DangerRoseSoft else AccentEmeraldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.blocked) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (log.blocked) DangerRose else AccentEmeraldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.number,
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${log.reasonDescription} · ${ShieldStorage.formatTimestamp(log.timestampMs)}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { onDeleteLog(log.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Elimina",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (log.blocked && log.reasonCode != "ANONYMOUS_BLOCKED") {
                            Row(modifier = Modifier.padding(start = 50.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(AccentEmeraldSoft)
                                        .clickable { onPromoteToWhitelist(log.number) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "+ Consenti in Whitelist",
                                        color = AccentEmeraldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListsManagementScreen(
    whitelist: List<String>,
    blacklist: List<String>,
    allowedForeignCodes: List<String>,
    onAddWhitelist: (String) -> Unit,
    onRemoveWhitelist: (String) -> Unit,
    onAddBlacklist: (String) -> Unit,
    onRemoveBlacklist: (String) -> Unit,
    onAddForeignCode: (String) -> Unit,
    onRemoveForeignCode: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Liste di Esclusione", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // 3-Segment pill switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, CircleShape)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "Whitelist (${whitelist.size})",
                "Blacklist (${blacklist.size})",
                "Esteri (${allowedForeignCodes.size})"
            ).forEachIndexed { index, title ->
                val selected = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (selected) AccentEmeraldSoft else Color.Transparent)
                        .clickable {
                            activeTab = index
                            inputText = ""
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (selected) AccentEmeraldLight else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        // Add Form Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = when (activeTab) {
                    0 -> "Aggiungi numero sempre consentito"
                    1 -> "Aggiungi numero o prefisso (* wildcard, es. +44*)"
                    else -> "Aggiungi prefisso estero consentito (es. 41, 33)"
                },
                color = TextSecondary,
                fontSize = 12.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = when (activeTab) {
                                0 -> "+39 340 1234567"
                                1 -> "+44* oppure 899*"
                                else -> "41"
                            },
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = BgOled,
                        unfocusedContainerColor = BgOled
                    )
                )
                Button(
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty()) {
                            when (activeTab) {
                                0 -> onAddWhitelist(trimmed)
                                1 -> onAddBlacklist(trimmed)
                                else -> onAddForeignCode(trimmed)
                            }
                            inputText = ""
                        }
                    },
                    modifier = Modifier.height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEmerald,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Aggiungi")
                }
            }
        }

        // Active items list
        val currentList = when (activeTab) {
            0 -> whitelist
            1 -> blacklist
            else -> allowedForeignCodes.map { "+$it" }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
        ) {
            if (currentList.isEmpty()) {
                Text(
                    text = "Nessuna voce presente in questa lista.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                currentList.forEachIndexed { idx, entry ->
                    if (idx > 0) HorizontalDivider(color = BorderSubtle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry,
                            color = when (activeTab) {
                                0 -> AccentEmeraldLight
                                1 -> DangerRose
                                else -> InfoSky
                            },
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        IconButton(
                            onClick = {
                                when (activeTab) {
                                    0 -> onRemoveWhitelist(entry)
                                    1 -> onRemoveBlacklist(entry)
                                    else -> onRemoveForeignCode(entry.removePrefix("+"))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Rimuovi",
                                tint = TextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpamListsScreen(
    feeds: List<SpamFeedInfo>,
    autoSyncEnabled: Boolean,
    syncIntervalHours: Long,
    wifiOnly: Boolean,
    chargingOnly: Boolean,
    offlineMode: Boolean,
    lastSyncMs: Long,
    isSyncing: Boolean,
    onToggleFeed: (String, Boolean) -> Unit,
    onToggleAllFeeds: (Boolean) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSelectIntervalHours: (Long) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onToggleChargingOnly: (Boolean) -> Unit,
    onSyncNow: () -> Unit
) {
    val activeFeeds = feeds.filter { it.enabled }
    val activePublicNumbers = activeFeeds.sumOf { it.publicRecordsCount }
    val activeLocalNumbers = activeFeeds.sumOf { it.localNumbersCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Liste Spam Pubbliche", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Prominent Active Count & Manual Sync Summary Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF122126), SurfaceCard)))
                .border(1.dp, Color(0x3310B981), RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Numeri coperti dalle liste selezionate",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = ShieldStorage.formatItalianCount(activePublicNumbers),
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${activeFeeds.size} di ${feeds.size} liste pubbliche attive · $activeLocalNumbers firme in cache offline · Ultimo sync: ${ShieldStorage.formatTimestamp(lastSyncMs)}",
                color = AccentEmeraldLight,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            if (offlineMode) {
                Text(
                    text = "Modalità Offline attiva: sincronizzazione in pausa per la privacy.",
                    color = WarningAmber,
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEmerald,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isSyncing) "Aggiornamento..." else "Sincronizza ora",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Individually Selectable Public Spam Lists with Number Counts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seleziona liste singole",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tutte",
                        color = AccentEmeraldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onToggleAllFeeds(true) }
                    )
                    Text(
                        text = "Nessuna",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onToggleAllFeeds(false) }
                    )
                }
            }

            feeds.forEach { feed ->
                HorizontalDivider(color = BorderSubtle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFeed(feed.id, !feed.enabled) }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = feed.name,
                            color = if (feed.enabled) TextPrimary else TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = feed.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "${ShieldStorage.formatItalianCount(feed.publicRecordsCount)} numeri · ${feed.localNumbersCount} locali · ${feed.provider}",
                            color = if (feed.enabled) AccentEmeraldLight else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    ShieldSwitch(
                        checked = feed.enabled,
                        onCheckedChange = { onToggleFeed(feed.id, it) }
                    )
                }
            }
        }

        // Automated Sync Schedule Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Aggiornamento automatico", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("WorkManager in background a basso consumo", color = TextSecondary, fontSize = 12.sp)
                }
                ShieldSwitch(checked = autoSyncEnabled, onCheckedChange = onToggleAutoSync)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(6L to "6h", 12L to "12h", 24L to "24h", 0L to "Manuale").forEach { (hours, label) ->
                    val selected = if (hours == 0L) !autoSyncEnabled else (autoSyncEnabled && syncIntervalHours == hours)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) AccentEmeraldSoft else SurfaceElevated)
                            .border(1.dp, if (selected) AccentEmerald else BorderSubtle, CircleShape)
                            .clickable { onSelectIntervalHours(hours) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) AccentEmeraldLight else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sincronizza solo su Wi-Fi", color = TextPrimary, fontSize = 14.sp)
                ShieldSwitch(checked = wifiOnly, onCheckedChange = onToggleWifiOnly)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sincronizza solo in ricarica", color = TextPrimary, fontSize = 14.sp)
                ShieldSwitch(checked = chargingOnly, onCheckedChange = onToggleChargingOnly)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    blockAction: String,
    notificationStyle: String,
    offlineMode: Boolean,
    homeCountryCode: String,
    hasContactsPermission: Boolean,
    onSelectBlockAction: (String) -> Unit,
    onSelectNotificationStyle: (String) -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onChangeHomeCountry: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onShareBackup: () -> Unit,
    onImportBackupJson: (String) -> Unit
) {
    var countryInput by remember(homeCountryCode) { mutableStateOf(homeCountryCode) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Impostazioni Avanzate", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Block Action & Notifications
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Azione su chiamata bloccata", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HANG_UP" to "Rifiuta", "SILENCE" to "Silenzia", "VOICEMAIL" to "Segreteria").forEach { (id, label) ->
                    val selected = blockAction == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) AccentEmeraldSoft else SurfaceElevated)
                            .border(1.dp, if (selected) AccentEmerald else BorderSubtle, CircleShape)
                            .clickable { onSelectBlockAction(id) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) AccentEmeraldLight else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle)

            Text("Notifiche personalizzabili", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HEADS_UP" to "Banner", "SILENT_BADGE" to "Silenziose", "GHOST_MODE" to "Fantasma").forEach { (id, label) ->
                    val selected = notificationStyle == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) AccentEmeraldSoft else SurfaceElevated)
                            .border(1.dp, if (selected) AccentEmerald else BorderSubtle, CircleShape)
                            .clickable { onSelectNotificationStyle(id) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) AccentEmeraldLight else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Privacy Offline & Home Prefix
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = WarningAmber)
                    Column {
                        Text("Modalità Offline Privacy", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("100% locale, blocca ogni sync esterno", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                ShieldSwitch(checked = offlineMode, onCheckedChange = onToggleOfflineMode)
            }

            HorizontalDivider(color = BorderSubtle)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Prefisso nazionale (+)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                OutlinedTextField(
                    value = countryInput,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        countryInput = digits
                        if (digits.isNotEmpty()) onChangeHomeCountry(digits)
                    },
                    singleLine = true,
                    modifier = Modifier.width(96.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = BgOled,
                        unfocusedContainerColor = BgOled
                    )
                )
            }
        }

        // Permissions & Cloud Backup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Permessi e Backup Cloud", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

            if (!hasContactsPermission) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Consenti accesso Rubrica e Notifiche", fontSize = 13.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShareBackup,
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEmerald,
                        contentColor = Color(0xFF031A12)
                    )
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Esporta Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                    shape = CircleShape
                ) {
                    Text("Ripristina JSON", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Ripristina Backup JSON", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("Incolla qui il contenuto JSON...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onImportBackupJson(importText)
                        importText = ""
                        showImportDialog = false
                    }
                ) {
                    Text("Ripristina", color = AccentEmeraldLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun CallSimulatorDialog(
    onDismiss: () -> Unit,
    onSimulate: (number: String, isAnonymous: Boolean, isHiya: Boolean) -> FilterDecision
) {
    var customNumber by remember { mutableStateOf("") }
    var lastDecision by remember { mutableStateOf<Pair<String, FilterDecision>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = { Text("Simula chiamata", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customNumber,
                    onValueChange = { customNumber = it },
                    placeholder = { Text("+39 333 1234567", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
                Button(
                    onClick = {
                        val num = customNumber.trim()
                        val res = onSimulate(num, num.isEmpty(), false)
                        lastDecision = (if (num.isEmpty()) "Numero Privato" else num) to res
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald, contentColor = Color(0xFF031A12))
                ) {
                    Text("Testa numero", fontWeight = FontWeight.Bold)
                }

                // Quick test presets
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val res = onSimulate("+390694801122", false, false)
                            lastDecision = "+39 06 94801122" to res
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("AGCOM", color = TextPrimary, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val res = onSimulate("+390299441122", false, true)
                            lastDecision = "Hiya Spam" to res
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Hiya", color = TextPrimary, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val res = onSimulate("", true, false)
                            lastDecision = "Anonimo" to res
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Anonimo", color = TextPrimary, fontSize = 11.sp)
                    }
                }

                lastDecision?.let { (num, decision) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (decision.shouldBlock) DangerRoseSoft else AccentEmeraldSoft)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = (if (decision.shouldBlock) "BLOCCATA · " else "CONSENTITA · ") + num,
                            color = if (decision.shouldBlock) DangerRose else AccentEmeraldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = decision.reasonDescription,
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi", color = AccentEmeraldLight, fontWeight = FontWeight.Bold)
            }
        }
    )
}
