package com.zeeb.cachecleaner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeeb.cachecleaner.shizuku.ShizukuController
import com.zeeb.cachecleaner.shizuku.ShizukuStatus
import com.zeeb.cachecleaner.util.StorageInfo
import com.zeeb.cachecleaner.util.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val shizukuStatus: ShizukuStatus = ShizukuStatus.NOT_INSTALLED,
    val storageStats: StorageStats = StorageStats(),
    val freeBytesBefore: Long = 0L,
    val freeBytesAfter: Long = 0L,
    val freedMb: Double = 0.0,
    val isCleaning: Boolean = false,
    val logs: List<String> = emptyList(),
    val error: String? = null
)

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    val shizukuController = ShizukuController(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        shizukuController.init()

        // Amati status Shizuku secara reaktif
        viewModelScope.launch {
            shizukuController.status.collect { status ->
                _uiState.update { it.copy(shizukuStatus = status) }
                addLog("Status Shizuku: $status")
            }
        }

        // Muat data penyimpanan saat pertama dibuka
        refreshStorage()
    }

    fun refreshStorage() {
        val stats = StorageInfo.getStorageStats()
        _uiState.update { it.copy(storageStats = stats) }
    }

    fun requestShizukuPermission() {
        shizukuController.requestPermission()
    }

    fun openShizukuApp() {
        shizukuController.openShizukuApp()
    }

    fun openShizukuDownloadPage() {
        shizukuController.openShizukuDownloadPage()
    }

    fun clean() {
        if (_uiState.value.isCleaning) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isCleaning = true,
                    error = null,
                    logs = emptyList()
                )
            }

            addLog("== Memulai Proses Pembersihan Cache ==")
            addLog("Perangkat: Poco F4 (HyperOS / Android 14)")

            // 1. Ukur ruang kosong sebelum
            val statsBefore = StorageInfo.getStorageStats()
            val beforeFree = statsBefore.freeBytes
            _uiState.update {
                it.copy(
                    storageStats = statsBefore,
                    freeBytesBefore = beforeFree,
                    freedMb = 0.0
                )
            }
            addLog("Ruang kosong awal: ${statsBefore.freeGb} GB")

            // 2. Eksekusi pm trim-caches 999G via Shizuku UserService
            addLog("Menjalankan: pm trim-caches 999G via Shizuku UserService...")
            val result = shizukuController.executeCommand("pm trim-caches 999G")

            result.fold(
                onSuccess = { output ->
                    addLog("Output shell:\n$output")

                    // 3. Tunggu 2 detik agar sistem file dan VFS selesai menyinkronkan data blok
                    addLog("Sinkronisasi I/O sistem file (2 detik)...")
                    delay(2000)

                    // 4. Ukur ruang kosong sesudah
                    val statsAfter = StorageInfo.getStorageStats()
                    val afterFree = statsAfter.freeBytes
                    val diffBytes = (afterFree - beforeFree).coerceAtLeast(0L)
                    val mb = (diffBytes / (1024.0 * 1024.0) * 10).toLong() / 10.0

                    _uiState.update {
                        it.copy(
                            storageStats = statsAfter,
                            freeBytesAfter = afterFree,
                            freedMb = mb,
                            isCleaning = false
                        )
                    }

                    addLog("Ruang kosong akhir: ${statsAfter.freeGb} GB")
                    addLog("Sukses! Berhasil membebaskan $mb MB cache aplikasi.")
                },
                onFailure = { throwable ->
                    val errorMsg = throwable.localizedMessage ?: "Terjadi kesalahan yang tidak diketahui"
                    addLog("GAGAL: $errorMsg")
                    _uiState.update {
                        it.copy(
                            isCleaning = false,
                            error = errorMsg
                        )
                    }
                }
            )
        }
    }

    private suspend fun addLog(message: String) {
        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(logs = it.logs + message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        shizukuController.destroy()
    }
}
