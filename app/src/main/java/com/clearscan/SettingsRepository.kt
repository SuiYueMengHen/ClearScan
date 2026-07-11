package com.clearscan

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.clearScanSettingsDataStore by preferencesDataStore(
    name = "clearscan-settings-v2",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "clearscan-settings")) },
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val loggedIn = booleanPreferencesKey("loggedIn")
        val accountName = stringPreferencesKey("accountName")
        val accountEmail = stringPreferencesKey("accountEmail")
        val passwords = stringPreferencesKey("passwords")
        val defaultSavePath = stringPreferencesKey("defaultSavePath")
        val autoCheckUpdates = booleanPreferencesKey("autoCheckUpdates")
        val autoDownloadUpdates = booleanPreferencesKey("autoDownloadUpdates")
        val wifiOnlyUpdates = booleanPreferencesKey("wifiOnlyUpdates")
        val cameraGrid = booleanPreferencesKey("cameraGrid")
        val cameraEnhance = booleanPreferencesKey("cameraEnhance")
        val cameraResolution = stringPreferencesKey("cameraResolution")
    }

    suspend fun load(fallback: AppSettings = AppSettings()): AppSettings {
        val values = context.clearScanSettingsDataStore.data.first()
        val passwords = values[Keys.passwords].orEmpty().split('|').mapNotNull { item ->
            val parts = item.split(':', limit = 2)
            parts.firstOrNull()?.toLongOrNull()?.let { id -> id to parts.getOrElse(1) { "" } }
        }.filter { it.second.isNotBlank() }.toMap()
        return AppSettings(
            language = values[Keys.language] ?: fallback.language,
            theme = values[Keys.theme] ?: fallback.theme,
            loggedIn = values[Keys.loggedIn] ?: fallback.loggedIn,
            accountName = values[Keys.accountName] ?: fallback.accountName,
            accountEmail = values[Keys.accountEmail] ?: fallback.accountEmail,
            passwordMap = passwords.ifEmpty { fallback.passwordMap },
            defaultSavePath = values[Keys.defaultSavePath] ?: fallback.defaultSavePath,
            autoCheckUpdates = values[Keys.autoCheckUpdates] ?: fallback.autoCheckUpdates,
            autoDownloadUpdates = values[Keys.autoDownloadUpdates] ?: fallback.autoDownloadUpdates,
            wifiOnlyUpdates = values[Keys.wifiOnlyUpdates] ?: fallback.wifiOnlyUpdates,
            cameraGrid = values[Keys.cameraGrid] ?: fallback.cameraGrid,
            cameraEnhance = values[Keys.cameraEnhance] ?: fallback.cameraEnhance,
            cameraResolution = values[Keys.cameraResolution] ?: fallback.cameraResolution,
        )
    }

    suspend fun save(settings: AppSettings) {
        context.clearScanSettingsDataStore.edit { values ->
            values[Keys.language] = settings.language
            values[Keys.theme] = settings.theme
            values[Keys.loggedIn] = settings.loggedIn
            values[Keys.accountName] = settings.accountName
            values[Keys.accountEmail] = settings.accountEmail
            values[Keys.passwords] = settings.passwordMap.entries.joinToString("|") { "${it.key}:${it.value}" }
            values[Keys.defaultSavePath] = settings.defaultSavePath
            values[Keys.autoCheckUpdates] = settings.autoCheckUpdates
            values[Keys.autoDownloadUpdates] = settings.autoDownloadUpdates
            values[Keys.wifiOnlyUpdates] = settings.wifiOnlyUpdates
            values[Keys.cameraGrid] = settings.cameraGrid
            values[Keys.cameraEnhance] = settings.cameraEnhance
            values[Keys.cameraResolution] = settings.cameraResolution
        }
    }
}
