package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException


private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "memoir_user_prefs")





class UserPreferencesRepository(context: Context) {

    private val dataStore = context.userPrefsDataStore

    companion object {

        val KEY_PROFILE_NAME = stringPreferencesKey("profile_name")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_PROFILE_BIO = stringPreferencesKey("profile_bio")
        val KEY_PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")
        val KEY_APP_PIN = stringPreferencesKey("app_pin")
        val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_COLOR_THEME = stringPreferencesKey("color_theme")
        val KEY_IS_BIOMETRIC = booleanPreferencesKey("is_biometric")
        val KEY_IS_LOCATION_HIDDEN = booleanPreferencesKey("is_location_hidden")
        val KEY_IS_AUTO_SYNC = booleanPreferencesKey("is_auto_sync")
        val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val KEY_HAS_GRANTED_PERMISSIONS = booleanPreferencesKey("has_granted_permissions")
        val KEY_LANGUAGE = stringPreferencesKey("language")


        const val DEFAULT_PROFILE_NAME = "Pemilik Memoir"
        const val DEFAULT_USERNAME = "me_memoir"
        const val DEFAULT_BIO = "Ini adalah ruang memori offline pribadi saya. Tanpa server, tanpa likes dari luar. Murni tempat untuk bernostalgia."
        const val DEFAULT_IMAGE_PATH = "gradient_sunset"
        const val DEFAULT_COLOR_THEME = "DEFAULT"
        const val DEFAULT_LANGUAGE = "en"
    }

    val userPrefsFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserPreferences(
                profileName = prefs[KEY_PROFILE_NAME] ?: DEFAULT_PROFILE_NAME,
                username = prefs[KEY_USERNAME] ?: DEFAULT_USERNAME,
                profileBio = prefs[KEY_PROFILE_BIO] ?: DEFAULT_BIO,
                profileImagePath = prefs[KEY_PROFILE_IMAGE_PATH] ?: DEFAULT_IMAGE_PATH,
                appPin = prefs[KEY_APP_PIN],
                isDarkMode = prefs[KEY_IS_DARK_MODE] ?: false,
                colorTheme = prefs[KEY_COLOR_THEME] ?: DEFAULT_COLOR_THEME,
                isBiometricEnabled = prefs[KEY_IS_BIOMETRIC] ?: false,
                isLocationHidden = prefs[KEY_IS_LOCATION_HIDDEN] ?: false,
                isAutoSyncEnabled = prefs[KEY_IS_AUTO_SYNC] ?: true,
                hasSeenOnboarding = prefs[KEY_HAS_SEEN_ONBOARDING] ?: false,
                hasGrantedPermissions = prefs[KEY_HAS_GRANTED_PERMISSIONS] ?: false,
                language = prefs[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE,
            )
        }

    suspend fun saveProfileName(name: String) {
        dataStore.edit { it[KEY_PROFILE_NAME] = name }
    }

    suspend fun saveUsername(username: String) {
        dataStore.edit { it[KEY_USERNAME] = username }
    }

    suspend fun saveProfileBio(bio: String) {
        dataStore.edit { it[KEY_PROFILE_BIO] = bio }
    }

    suspend fun saveProfileImagePath(path: String?) {
        dataStore.edit {
            if (path != null) it[KEY_PROFILE_IMAGE_PATH] = path
            else it.remove(KEY_PROFILE_IMAGE_PATH)
        }
    }

    suspend fun saveAppPin(pin: String?) {
        dataStore.edit {
            if (pin != null) it[KEY_APP_PIN] = pin
            else it.remove(KEY_APP_PIN)
        }
    }

    suspend fun saveDarkMode(enabled: Boolean) {
        dataStore.edit { it[KEY_IS_DARK_MODE] = enabled }
    }

    suspend fun saveColorTheme(theme: String) {
        dataStore.edit { it[KEY_COLOR_THEME] = theme }
    }

    suspend fun saveBiometric(enabled: Boolean) {
        dataStore.edit { it[KEY_IS_BIOMETRIC] = enabled }
    }

    suspend fun saveLocationHidden(hidden: Boolean) {
        dataStore.edit { it[KEY_IS_LOCATION_HIDDEN] = hidden }
    }

    suspend fun saveAutoSync(enabled: Boolean) {
        dataStore.edit { it[KEY_IS_AUTO_SYNC] = enabled }
    }

    suspend fun saveHasSeenOnboarding(seen: Boolean) {
        dataStore.edit { it[KEY_HAS_SEEN_ONBOARDING] = seen }
    }

    suspend fun saveHasGrantedPermissions(granted: Boolean) {
        dataStore.edit { it[KEY_HAS_GRANTED_PERMISSIONS] = granted }
    }

    suspend fun saveLanguage(language: String) {
        dataStore.edit { it[KEY_LANGUAGE] = language }
    }
}




data class UserPreferences(
    val profileName: String,
    val username: String,
    val profileBio: String,
    val profileImagePath: String?,
    val appPin: String?,
    val isDarkMode: Boolean,
    val colorTheme: String,
    val isBiometricEnabled: Boolean,
    val isLocationHidden: Boolean,
    val isAutoSyncEnabled: Boolean,
    val hasSeenOnboarding: Boolean = false,
    val hasGrantedPermissions: Boolean = false,
    val language: String = UserPreferencesRepository.DEFAULT_LANGUAGE,
)
