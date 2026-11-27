package com.independent.xxxyaoireader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.independent.xxxyaoireader.ui.MainViewModel
import com.independent.xxxyaoireader.ui.XXXYaoiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val factory = remember {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(this@MainActivity.application) as T
                    }
                }
            }
            val vm: MainViewModel = viewModel(factory = factory)
            XXXYaoiApp(viewModel = vm)
        }
    }
}
