package com.zeeb.cachecleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeeb.cachecleaner.ui.CleanerScreen
import com.zeeb.cachecleaner.ui.CleanerViewModel
import com.zeeb.cachecleaner.ui.theme.CacheCleanerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CacheCleanerTheme {
                val viewModel: CleanerViewModel = viewModel()
                CleanerScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Cek status Shizuku ulang bila pengguna baru saja berpindah dari aplikasi Shizuku
        // atau baru saja menyalakan Wireless Debugging di Setelan Pengembang
    }
}
