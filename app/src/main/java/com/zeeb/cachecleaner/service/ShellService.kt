package com.zeeb.cachecleaner.service

import androidx.annotation.Keep
import com.zeeb.cachecleaner.IShellService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * ShellService diinstansiasi oleh Shizuku daemon pada konteks shell berprivilese (UID 2000).
 * Menggunakan @Keep agar tidak terhapus saat minify/obfuscation.
 */
@Keep
class ShellService : IShellService.Stub() {

    override fun runCommand(cmd: String): String {
        return try {
            // Jalankan shell command dengan lingkungan sh -c
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            // Timeout keamanan 30 detik agar tidak terjadi hang tak terhingga
            val completed = process.waitFor(30, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return "ERROR: Command timed out after 30 seconds"
            }

            val stdout = stdoutReader.readText().trim()
            val stderr = stderrReader.readText().trim()

            when {
                stderr.isNotEmpty() && stdout.isNotEmpty() -> "$stdout\n$stderr"
                stderr.isNotEmpty() -> stderr
                stdout.isNotEmpty() -> stdout
                else -> "SUCCESS: Command completed (exit code: ${process.exitValue()})"
            }
        } catch (e: Exception) {
            "EXCEPTION: ${e.localizedMessage ?: e.message}"
        }
    }

    override fun destroy() {
        // Matikan proses shell service agar tidak membocorkan memori di background
        exitProcess(0)
    }
}
