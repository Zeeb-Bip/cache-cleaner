package com.zeeb.cachecleaner.util

import android.os.Environment
import android.os.StatFs

data class StorageStats(
    val freeBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeGb: Double = 0.0,
    val totalGb: Double = 0.0,
    val usedPercentage: Float = 0f
)

object StorageInfo {

    fun getStorageStats(): StorageStats {
        return try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            val gbDivisor = 1024.0 * 1024.0 * 1024.0
            val freeGb = (freeBytes / gbDivisor * 100).toLong() / 100.0
            val totalGb = (totalBytes / gbDivisor * 100).toLong() / 100.0
            val usedPercentage = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f

            StorageStats(
                freeBytes = freeBytes,
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeGb = freeGb,
                totalGb = totalGb,
                usedPercentage = usedPercentage.coerceIn(0f, 1f)
            )
        } catch (e: Exception) {
            StorageStats()
        }
    }
}
