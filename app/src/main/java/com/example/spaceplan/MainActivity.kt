package com.example.spaceplan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spaceplan.ui.SpacePlanViewModel
import com.example.spaceplan.ui.navigation.SpacePlanApp
import com.example.spaceplan.ui.splash.SplashScreen
import com.example.spaceplan.ui.theme.SpacePlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SpacePlanViewModel = viewModel(factory = SpacePlanViewModel.Factory)
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
            var splashDone by rememberSaveable { mutableStateOf(false) }

            SpacePlanTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Crossfade(targetState = splashDone && settingsLoaded, label = "splash") { ready ->
                        if (ready) {
                            SpacePlanApp(viewModel)
                        } else {
                            SplashScreen(onFinished = { splashDone = true })
                        }
                    }
                }
            }
        }
    }
}
