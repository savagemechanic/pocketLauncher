package com.github.codeworkscreativehub.mlauncher.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.Constants
import com.github.codeworkscreativehub.mlauncher.data.RepositoryProvider
import com.github.codeworkscreativehub.mlauncher.data.repository.AppRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.SettingsRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.ThemeRepository
import com.github.codeworkscreativehub.mlauncher.helper.ismlauncherDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the home screen fragment.
 * Exposes settings StateFlows and home app list management.
 */
class HomeViewModel(
    application: Application,
    val settings: SettingsRepository,
    val theme: ThemeRepository,
    private val appRepo: AppRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val _homeAppsOrder = MutableStateFlow<List<AppListItem>>(emptyList())
    val homeAppsOrder: StateFlow<List<AppListItem>> = _homeAppsOrder.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(false)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    fun checkDefaultLauncher() {
        _isDefaultLauncher.value = ismlauncherDefault(appContext)
    }

    fun loadAppOrder() {
        val saved = (0 until settings.homeAppsNum.value)
            .map { settings.getHomeAppModel(it) }
        _homeAppsOrder.value = saved
    }

    fun updateAppOrder(fromPosition: Int, toPosition: Int) {
        val current = _homeAppsOrder.value.toMutableList()
        val app = current.removeAt(fromPosition)
        current.add(toPosition, app)
        _homeAppsOrder.value = current
        current.forEachIndexed { index, item -> settings.setHomeAppModel(index, item) }
    }

    fun updateHomeAlignment(gravity: Constants.Gravity, onBottom: Boolean) {
        settings.update(settings.homeAlignment, gravity)
        settings.update(settings.homeAlignmentBottom, onBottom)
    }

    fun updateClockAlignment(gravity: Constants.Gravity) =
        settings.update(settings.clockAlignment, gravity)

    fun updateDateAlignment(gravity: Constants.Gravity) =
        settings.update(settings.dateAlignment, gravity)

    fun updateAlarmAlignment(gravity: Constants.Gravity) =
        settings.update(settings.alarmAlignment, gravity)

    fun updateDailyWordAlignment(gravity: Constants.Gravity) =
        settings.update(settings.dailyWordAlignment, gravity)

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val provider = RepositoryProvider
            return HomeViewModel(
                application,
                provider.settingsRepository,
                provider.themeRepository,
                provider.appRepository
            ) as T
        }
    }
}
