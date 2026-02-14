package com.github.codeworkscreativehub.mlauncher.ui.appdrawer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.Constants.AppDrawerFlag
import com.github.codeworkscreativehub.mlauncher.data.RepositoryProvider
import com.github.codeworkscreativehub.mlauncher.data.repository.AppManagementRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.AppRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.SettingsRepository
import com.github.codeworkscreativehub.mlauncher.domain.launcher.AppLauncher
import com.github.codeworkscreativehub.mlauncher.domain.launcher.AuthCallback
import kotlinx.coroutines.flow.StateFlow

/**
 * Sealed interface for app drawer UI state.
 */
sealed interface AppListUiState {
    data object Loading : AppListUiState
    data class Success(val apps: List<AppListItem>) : AppListUiState
    data class Error(val message: String) : AppListUiState
}

/**
 * ViewModel for the app drawer fragment.
 * Manages app list state, search, and launch actions.
 */
class AppListViewModel(
    application: Application,
    private val appRepo: AppRepository,
    private val settings: SettingsRepository,
    private val appMgmt: AppManagementRepository,
    private val appLauncher: AppLauncher
) : AndroidViewModel(application) {

    val appList: StateFlow<List<AppListItem>> = appRepo.appList
    val hiddenApps: StateFlow<List<AppListItem>> = appRepo.hiddenApps
    val appScrollMap: StateFlow<Map<String, Int>> = appRepo.appScrollMap

    fun refreshApps(includeHidden: Boolean = true, includeRecent: Boolean = true) {
        appRepo.refreshAppList(includeHidden, includeRecent)
    }

    fun refreshHiddenApps() = appRepo.refreshHiddenApps()

    fun selectedApp(
        app: AppListItem,
        flag: AppDrawerFlag,
        n: Int = 0,
        onAuthRequired: (AppListItem, AuthCallback) -> Unit
    ) {
        when (flag) {
            AppDrawerFlag.SetHomeApp -> settings.setHomeAppModel(n, app)
            AppDrawerFlag.SetShortSwipeUp -> settings.setSwipeApp("SHORT_SWIPE_UP", app)
            AppDrawerFlag.SetShortSwipeDown -> settings.setSwipeApp("SHORT_SWIPE_DOWN", app)
            AppDrawerFlag.SetShortSwipeLeft -> settings.setSwipeApp("SHORT_SWIPE_LEFT", app)
            AppDrawerFlag.SetShortSwipeRight -> settings.setSwipeApp("SHORT_SWIPE_RIGHT", app)
            AppDrawerFlag.SetLongSwipeUp -> settings.setSwipeApp("LONG_SWIPE_UP", app)
            AppDrawerFlag.SetLongSwipeDown -> settings.setSwipeApp("LONG_SWIPE_DOWN", app)
            AppDrawerFlag.SetLongSwipeLeft -> settings.setSwipeApp("LONG_SWIPE_LEFT", app)
            AppDrawerFlag.SetLongSwipeRight -> settings.setSwipeApp("LONG_SWIPE_RIGHT", app)
            AppDrawerFlag.SetClickClock -> settings.setSwipeApp("CLICK_CLOCK", app)
            AppDrawerFlag.SetAppUsage -> settings.setSwipeApp("CLICK_USAGE", app)
            AppDrawerFlag.SetFloating -> settings.setSwipeApp("CLICK_FLOATING", app)
            AppDrawerFlag.SetClickDate -> settings.setSwipeApp("CLICK_DATE", app)
            AppDrawerFlag.SetDoubleTap -> settings.setSwipeApp("DOUBLE_TAP", app)
            AppDrawerFlag.LaunchApp, AppDrawerFlag.HiddenApps, AppDrawerFlag.PrivateApps ->
                appLauncher.launch(app, onAuthRequired)
            AppDrawerFlag.None -> {}
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val p = RepositoryProvider
            return AppListViewModel(
                application, p.appRepository, p.settingsRepository,
                p.appManagementRepository, p.appLauncher
            ) as T
        }
    }
}
