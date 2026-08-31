package com.example.glance

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition




data class MemoirWidgetState(
    val photoPath: String = "",
    val caption: String = "",
    val dateLabel: String = "",
    val yearsAgo: Int = 0,
    val hasMemory: Boolean = false
) {
    companion object {
        val KEY_PHOTO_PATH = stringPreferencesKey("widget_photo_path")
        val KEY_CAPTION = stringPreferencesKey("widget_caption")
        val KEY_DATE_LABEL = stringPreferencesKey("widget_date_label")
        val KEY_YEARS_AGO = intPreferencesKey("widget_years_ago")
        val KEY_HAS_MEMORY = booleanPreferencesKey("widget_has_memory")

        fun load(context: Context): MemoirWidgetState {
            
            return MemoirWidgetState()
        }

        val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    }
}
