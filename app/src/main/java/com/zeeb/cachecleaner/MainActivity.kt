package com.zeeb.cachecleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.zeeb.cachecleaner.ui.CleanerScreen
import com.zeeb.cachecleaner.ui.CleanerViewModel
import com.zeeb.cachecleaner.ui.theme.CacheCleanerTheme

class MainActivity : ComponentActivity() {

    private val vm: CleanerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CacheCleanerTheme {
                CleanerScreen(viewModel = vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.shizukuController.checkStatus()
    }
}
