package com.github.codeworkscreativehub.mlauncher.data.repository

import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages app list fetching, caching, and scroll index.
 * Replaces the app-list portion of MainViewModel.
 */
interface AppRepository {
    val appList: StateFlow<List<AppListItem>>
    val hiddenApps: StateFlow<List<AppListItem>>
    val appScrollMap: StateFlow<Map<String, Int>>

    fun refreshAppList(includeHiddenApps: Boolean = true, includeRecentApps: Boolean = true)
    fun refreshHiddenApps()
    fun invalidateCache()
}
