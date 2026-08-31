package com.example.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeCategory {
    COMPACT,  
    MEDIUM,   
    EXPANDED  
}

data class WindowSizeInfo(
    val category: WindowSizeCategory,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isPortrait: Boolean,
    val gridColumns: Int
)

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    return remember(widthDp, heightDp, isPortrait) {
        val category = when {
            widthDp < 600.dp -> WindowSizeCategory.COMPACT
            widthDp < 840.dp -> WindowSizeCategory.MEDIUM
            else -> WindowSizeCategory.EXPANDED
        }

        val columns = when (category) {
            WindowSizeCategory.COMPACT -> if (isPortrait) 3 else 5
            WindowSizeCategory.MEDIUM -> if (isPortrait) 4 else 6
            WindowSizeCategory.EXPANDED -> if (isPortrait) 6 else 8
        }

        WindowSizeInfo(
            category = category,
            screenWidthDp = widthDp,
            screenHeightDp = heightDp,
            isPortrait = isPortrait,
            gridColumns = columns
        )
    }
}
