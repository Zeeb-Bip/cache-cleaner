package com.zeeb.cachecleaner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeeb.cachecleaner.shizuku.ShizukuStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    viewModel: CleanerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Cache Cleaner",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        ShizukuStatusChip(status = state.shizukuStatus)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshStorage() },
                        enabled = !state.isCleaning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang Penyimpanan",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Kartu Penyimpanan & Progress Ring
            item {
                StorageOverviewCard(
                    freeGb = state.storageStats.freeGb,
                    totalGb = state.storageStats.totalGb,
                    usedPercentage = state.storageStats.usedPercentage
                )
            }

            // 2. Tombol Utama Dinamis Berdasarkan Status Shizuku
            item {
                ActionSection(
                    state = state,
                    onCleanClick = { viewModel.clean() },
                    onRequestPermissionClick = { viewModel.requestShizukuPermission() },
                    onOpenShizukuClick = { viewModel.openShizukuApp() },
                    onInstallShizukuClick = { viewModel.openShizukuDownloadPage() }
                )
            }

            // 3. Kartu Hasil Pembersihan (Jika Berhasil)
            if (state.freedMb > 0) {
                item {
                    ResultCard(freedMb = state.freedMb)
                }
            }

            // 4. Catatan Jujur & Pengingat Keamanan
            item {
                NoticeCard()
            }

            // 5. Konsol Log
            item {
                LogCard(logs = state.logs, isCleaning = state.isCleaning)
            }
        }
    }
}

@Composable
fun ShizukuStatusChip(status: ShizukuStatus) {
    val (bgColor, textColor, label) = when (status) {
        ShizukuStatus.READY -> Triple(Color(0xFF1B4332), Color(0xFF52B788), "READY")
        ShizukuStatus.NO_PERMISSION -> Triple(Color(0xFF4D3800), Color(0xFFFFC107), "NO PERM")
        ShizukuStatus.NOT_RUNNING -> Triple(Color(0xFF4A1515), Color(0xFFFF6B6B), "OFFLINE")
        ShizukuStatus.NOT_INSTALLED -> Triple(Color(0xFF333333), Color(0xFFB0B0B0), "NOT FOUND")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StorageOverviewCard(
    freeGb: Double,
    totalGb: Double,
    usedPercentage: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = usedPercentage,
        label = "storageProgress"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                // Background Track Ring
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )

                // Foreground Progress Ring (Teal Accent)
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "%.1f".format(freeGb),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "GB Bebas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total Penyimpanan: %.1f GB • Terpakai: %d%%".format(
                    totalGb,
                    (usedPercentage * 100).toInt()
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionSection(
    state: UiState,
    onCleanClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onOpenShizukuClick: () -> Unit,
    onInstallShizukuClick: () -> Unit
) {
    when (state.shizukuStatus) {
        ShizukuStatus.READY -> {
            Button(
                onClick = onCleanClick,
                enabled = !state.isCleaning,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                if (state.isCleaning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sedang Membersihkan...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bersihkan Cache",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        ShizukuStatus.NO_PERMISSION -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRequestPermissionClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB703),
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text(
                        text = "Beri Izin Shizuku",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Ketuk untuk memberikan otorisasi ADB Shizuku ke Cache Cleaner.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ShizukuStatus.NOT_RUNNING -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenShizukuClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE63946),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text(
                        text = "Buka Shizuku",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Layanan Shizuku belum aktif. Aktifkan lewat menu Wireless Debugging di Developer Options Poco F4 Anda.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ShizukuStatus.NOT_INSTALLED -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onInstallShizukuClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Text(
                        text = "Pasang Shizuku",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Aplikasi Shizuku tidak ditemukan. Pasang dari GitHub atau Google Play Store.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ResultCard(freedMb: Double) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F3D35)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14B8A6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Pembersihan Selesai!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "Terbebas: %.1f MB".format(freedMb),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF2DD4BF)
                )
            }
        }
    }
}

@Composable
fun NoticeCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Hanya membersihkan cache aplikasi. Cache sistem tidak bisa dihapus tanpa root.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFB703),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Matikan Wireless Debugging setelah selesai agar aplikasi mbanking tidak menolak berjalan.",
                    fontSize = 12.sp,
                    color = Color(0xFFFFD166),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun LogCard(logs: List<String>, isCleaning: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Log",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                if (isCleaning) {
                    Text(
                        text = "AKTIF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14B8A6)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "Belum ada riwayat aktivitas.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    logs.takeLast(8).forEach { log ->
                        Text(
                            text = "> $log",
                            fontSize = 11.sp,
                            color = if (log.contains("GAGAL") || log.contains("ERROR")) {
                                Color(0xFFF87171)
                            } else if (log.contains("Sukses")) {
                                Color(0xFF34D399)
                            } else {
                                Color(0xFFCBD5E1)
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
