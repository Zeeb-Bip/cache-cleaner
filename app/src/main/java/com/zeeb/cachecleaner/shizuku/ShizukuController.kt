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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

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
    private var isBound = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        currentService = null
        isBound = false
        checkStatus()
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    _status.value = ShizukuStatus.READY
                } else {
                    _status.value = ShizukuStatus.NO_PERMISSION
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
        val isInstalled = isShizukuInstalled()
        if (!isInstalled) {
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

        if (hasPermission) {
            _status.value = ShizukuStatus.READY
        } else {
            _status.value = ShizukuStatus.NO_PERMISSION
        }
    }

    fun requestPermission() {
        if (Shizuku.pingBinder()) {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }

    fun openShizukuApp() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
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

    /**
     * Memanggil UserService Shizuku dan mengeksekusi shell command secara aman.
     * Setelah eksekusi selesai atau gagal, service akan di-unbind otomatis.
     */
    suspend fun executeCommand(cmd: String): Result<String> {
        if (_status.value != ShizukuStatus.READY) {
            return Result.failure(IllegalStateException("Shizuku belum siap: ${_status.value}"))
        }

        return try {
            val service = getOrBindUserService()
            val result = service.runCommand(cmd)
            Result.success(result)
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

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            currentService = IShellService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            currentService = null
            isBound = false
        }
    }

    private suspend fun getOrBindUserService(): IShellService {
        currentService?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            val tempConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val stub = IShellService.Stub.asInterface(service)
                    currentService = stub
                    isBound = true
                    if (continuation.isActive) {
                        continuation.resume(stub)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    currentService = null
                    isBound = false
                }
            }

            try {
                Shizuku.bindUserService(userServiceArgs, tempConnection)
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(e))
                }
            }
        }
    }

    private fun unbindService() {
        if (isBound) {
            try {
                currentService?.destroy()
            } catch (_: Exception) {}

            try {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            } catch (_: Exception) {}

            currentService = null
            isBound = false
        }
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
}
