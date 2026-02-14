package com.github.codeworkscreativehub.mlauncher.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.codeworkscreativehub.mlauncher.data.Constants
import com.github.codeworkscreativehub.mlauncher.data.RepositoryProvider
import com.github.codeworkscreativehub.mlauncher.data.repository.AppManagementRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.AppRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.SettingsRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.ThemeRepository
import com.github.codeworkscreativehub.mlauncher.helper.ismlauncherDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for all settings screens.
 * Provides read/write access to settings, theme, and app management repositories.
 */
class SettingsViewModel(
    application: Application,
    val settings: SettingsRepository,
    val theme: ThemeRepository,
    val appMgmt: AppManagementRepository,
    private val appRepo: AppRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    fun checkDefaultLauncher() {
        _isDefaultLauncher.value = ismlauncherDefault(appContext)
    }

    fun refreshApps() = appRepo.refreshAppList()

    fun updateDrawerAlignment(gravity: Constants.Gravity) =
        settings.update(settings.drawerAlignment, gravity)

    fun saveToString(): String = settings.saveToString()
    fun loadFromString(json: String) = settings.loadFromString(json)

    fun saveToTheme(colorNames: List<String>): String = theme.saveToTheme(colorNames)
    fun loadFromTheme(json: String) = theme.loadFromTheme(json)

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val p = RepositoryProvider
            return SettingsViewModel(
                application, p.settingsRepository, p.themeRepository,
                p.appManagementRepository, p.appRepository
            ) as T
        }
    }
}
