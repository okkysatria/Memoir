package com.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MemoirApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.MemoirViewModelFactory
import com.example.worker.SmbSyncScheduler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class MainActivity : AppCompatActivity() {
    private val viewModel: MemoirViewModel by viewModels {
        MemoirViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        SmbSyncScheduler.schedule(this)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val selectedColorTheme by viewModel.selectedColorTheme.collectAsStateWithLifecycle()
            val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()

            androidx.compose.runtime.LaunchedEffect(language) {
                if (language.isNotEmpty()) {
                    val languageTag = if (language == "id") "in" else language
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageTag)
                    
                    if (AppCompatDelegate.getApplicationLocales() != appLocale) {
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }
                    
                    val locale = when (language) {
                        "id" -> java.util.Locale("in", "ID")
                        "zh" -> java.util.Locale.SIMPLIFIED_CHINESE
                        "ja" -> java.util.Locale.JAPANESE
                        "ru" -> java.util.Locale("ru", "RU")
                        "ar" -> java.util.Locale("ar", "SA")
                        else -> java.util.Locale.ENGLISH
                    }
                    java.util.Locale.setDefault(locale)
                }
            }

            androidx.compose.runtime.LaunchedEffect(isAppLocked) {
                if (isAppLocked) {
                    window.setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_SECURE,
                        android.view.WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            MyApplicationTheme(darkTheme = isDarkMode, colorTheme = selectedColorTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    MemoirApp(viewModel)
                }
            }
        }
    }
}
