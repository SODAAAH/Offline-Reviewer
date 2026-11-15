package com.studentreviewer.offline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.studentreviewer.offline.app.ReviewerApp
import com.studentreviewer.offline.ui.navigation.MainAppNavigation
import com.studentreviewer.offline.ui.theme.OfflineReviewerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as ReviewerApp
        val preferences = app.container.userPreferences

        enableEdgeToEdge()
        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = "SYSTEM")
            val scope = rememberCoroutineScope()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            OfflineReviewerTheme(darkTheme = isDark) {
                MainAppNavigation(
                    container = app.container,
                    currentTheme = themeMode,
                    onThemeChange = { newTheme ->
                        scope.launch {
                            preferences.setThemeMode(newTheme)
                        }
                    }
                )
            }
        }
    }
}
