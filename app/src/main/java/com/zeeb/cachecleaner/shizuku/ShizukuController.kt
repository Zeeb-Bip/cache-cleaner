package com.zeeb.cachecleaner.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import com.zeeb.cachecleaner.BuildConfig
import com.zeeb.cachecleaner.IShellService
import com.zeeb.cachecleaner.service.ShellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    NO_PERMISSION,
    READY
}

class ShizukuController(private val context: Context) {

    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var currentService: IShellService? = null
    private var activeConnection: ServiceConnection? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        currentService = null
        activeConnection = null
        checkStatus()
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                _status.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ShizukuStatus.READY
                } else {
                    ShizukuStatus.NO_PERMISSION
                }
            }
        }

    fun init() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        checkStatus()
    }

    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        unbindService()
    }

    fun checkStatus() {
        if (!isShizukuInstalled()) {
            _status.value = ShizukuStatus.NOT_INSTALLED
            return
        }

        val isPingSuccess = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }

        if (!isPingSuccess) {
            _status.value = ShizukuStatus.NOT_RUNNING
            return
        }

        val hasPermission = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }

        _status.value = if (hasPermission) ShizukuStatus.READY else ShizukuStatus.NO_PERMISSION
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }
        } catch (_: Exception) {
        }
    }

    fun openShizukuApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            openShizukuDownloadPage()
        }
    }

    fun openShizukuDownloadPage() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun executeCommand(cmd: String): Result<String> {
        if (_status.value != ShizukuStatus.READY) {
            return Result.failure(IllegalStateException("Shizuku belum siap: ${_status.value}"))
        }
        return try {
            val service = withTimeout(10_000) {
                withContext(Dispatchers.Main) { getOrBindUserService() }
            }
            Result.success(service.runCommand(cmd))
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Gagal terhubung ke layanan Shizuku (timeout)"))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            unbindService()
        }
    }

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShellService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private suspend fun getOrBindUserService(): IShellService {
        currentService?.let { return it }
        return suspendCancellableCoroutine { cont ->
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val stub = IShellService.Stub.asInterface(binder)
                    currentService = stub
                    if (cont.isActive) cont.resume(stub)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    currentService = null
                }
            }
            activeConnection = conn
            try {
                Shizuku.bindUserService(userServiceArgs, conn)
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
    }

    private fun unbindService() {
        try { currentService?.destroy() } catch (_: Exception) {}
        activeConnection?.let {
            try { Shizuku.unbindUserService(userServiceArgs, it, true) } catch (_: Exception) {}
        }
        activeConnection = null
        currentService = null
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
}
