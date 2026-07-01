package com.coder.videocrawler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coder.videocrawler.ui.screen.HomeScreen
import com.coder.videocrawler.ui.screen.HomeViewModel
import com.coder.videocrawler.ui.theme.VideoCrawlerTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VideoCrawlerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: HomeViewModel = viewModel()
                    LaunchedEffect(Unit) {
                        viewModel.init(applicationContext)
                    }
                    HomeScreen(viewModel)
                }
            }
        }
    }
}
