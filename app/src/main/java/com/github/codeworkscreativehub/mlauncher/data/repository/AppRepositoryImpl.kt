package com.github.codeworkscreativehub.mlauncher.data.repository

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.BuildConfig
import com.github.codeworkscreativehub.mlauncher.data.AppCategory
import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.cache.AppCacheManager
import com.github.codeworkscreativehub.mlauncher.helper.analytics.AppUsageMonitor
import com.github.codeworkscreativehub.mlauncher.helper.utils.PrivateSpaceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AppRepositoryImpl(
    private val context: Context,
    private val appMgmt: AppManagementRepository,
    private val settings: SettingsRepository,
    private val cacheManager: AppCacheManager,
    private val scope: CoroutineScope
) : AppRepository {

    private val _appList = MutableStateFlow<List<AppListItem>>(emptyList())
    override val appList: StateFlow<List<AppListItem>> = _appList.asStateFlow()

    private val _hiddenApps = MutableStateFlow<List<AppListItem>>(emptyList())
    override val hiddenApps: StateFlow<List<AppListItem>> = _hiddenApps.asStateFlow()

    private val _appScrollMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val appScrollMap: StateFlow<Map<String, Int>> = _appScrollMap.asStateFlow()

    private var memoryCache: MutableList<AppListItem>? = null
    private val refreshing = AtomicBoolean(false)

    init {
        refreshAppList()
    }

    override fun refreshAppList(includeHiddenApps: Boolean, includeRecentApps: Boolean) {
        memoryCache?.let { _appList.value = it }
            ?: cacheManager.loadApps()?.let { cached ->
                memoryCache = cached.toMutableList()
                _appList.value = cached
            }

        if (refreshing.compareAndSet(false, true)) {
            scope.launch {
                try {
                    val fresh = fetchApps(includeRegularApps = true, includeHiddenApps, includeRecentApps)
                    memoryCache = fresh
                    cacheManager.saveApps(fresh)
                    _appList.value = fresh
                } finally {
                    refreshing.set(false)
                }
            }
        }
    }

    override fun refreshHiddenApps() {
        scope.launch {
            _hiddenApps.value = fetchApps(includeRegularApps = false, includeHiddenApps = true)
        }
    }

    override fun invalidateCache() {
        memoryCache = null
    }

    private suspend fun fetchApps(
        includeRegularApps: Boolean = true,
        includeHiddenApps: Boolean = false,
        includeRecentApps: Boolean = true
    ): MutableList<AppListItem> = withContext(Dispatchers.IO) {
        val hiddenAppsSet = appMgmt.hiddenApps.value
        val pinnedPackages = appMgmt.pinnedApps.value
        val seenAppKeys = mutableSetOf<String>()
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val profiles = userManager.userProfiles.toList()
        val privateManager = PrivateSpaceManager(context)

        fun appKey(pkg: String, cls: String, profileHash: Int) = "$pkg|$cls|$profileHash"
        fun isHidden(pkg: String, key: String) =
            listOf(pkg, key, "$pkg|${key.hashCode()}").any { it in hiddenAppsSet }

        data class RawApp(
            val pkg: String, val cls: String, val label: String,
            val user: android.os.UserHandle, val profileType: String, val category: AppCategory
        )

        val rawApps = mutableListOf<RawApp>()

        if (settings.recentAppsDisplayed.value && includeRecentApps) {
            runCatching {
                AppUsageMonitor.createInstance(context)
                    .getLastTenAppsUsed(context)
                    .forEach { (pkg, name, activity) ->
                        val key = appKey(pkg, activity, 0)
                        if (seenAppKeys.add(key)) {
                            rawApps.add(RawApp(pkg, activity, name, Process.myUserHandle(), "SYSTEM", AppCategory.RECENT))
                        }
                    }
            }.onFailure { AppLogger.e("AppRepo", "Failed to add recent apps", it) }
        }

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val deferreds = profiles.map { profile ->
            async {
                if (privateManager.isPrivateSpaceProfile(profile) && privateManager.isPrivateSpaceLocked()) {
                    emptyList()
                } else {
                    val profileType = when {
                        privateManager.isPrivateSpaceProfile(profile) -> "PRIVATE"
                        profile != Process.myUserHandle() -> "WORK"
                        else -> "SYSTEM"
                    }
                    runCatching { launcherApps.getActivityList(null, profile) }
                        .getOrElse { emptyList() }
                        .mapNotNull { info ->
                            val pkg = info.applicationInfo.packageName
                            val cls = info.componentName.className
                            if (pkg == BuildConfig.APPLICATION_ID) return@mapNotNull null
                            val key = appKey(pkg, cls, profile.hashCode())
                            if (!seenAppKeys.add(key)) return@mapNotNull null
                            if ((isHidden(pkg, key) && !includeHiddenApps) ||
                                (!isHidden(pkg, key) && !includeRegularApps)
                            ) return@mapNotNull null
                            val category = if (pkg in pinnedPackages) AppCategory.PINNED else AppCategory.REGULAR
                            RawApp(pkg, cls, info.label.toString(), profile, profileType, category)
                        }
                }
            }
        }
        deferreds.forEach { rawApps.addAll(it.await()) }

        listOf("SYSTEM", "PRIVATE", "WORK", "USER").forEach { type ->
            appMgmt.setProfileCounter(type, rawApps.count { it.profileType == type })
        }

        val allApps = rawApps.map { raw ->
            AppListItem(
                activityLabel = raw.label, activityPackage = raw.pkg, activityClass = raw.cls,
                user = raw.user, profileType = raw.profileType,
                customTag = appMgmt.getAppTag(raw.pkg, raw.user), category = raw.category
            )
        }

        val sorted = allApps.sortedWith(
            compareByDescending<AppListItem> { it.category == AppCategory.PINNED }
                .thenBy { item ->
                    val alias = appMgmt.getAppAlias(item.activityPackage)
                    normalizeForSort(alias.takeIf { it.isNotBlank() } ?: item.activityLabel)
                }
        ).toMutableList()

        // Build scroll map
        val scrollMap = mutableMapOf<String, Int>()
        sorted.forEachIndexed { index, item ->
            val alias = appMgmt.getAppAlias(item.activityPackage)
            val label = alias.takeIf { it.isNotBlank() } ?: item.activityLabel
            val pinned = item.activityPackage in pinnedPackages
            val key = if (pinned) "★" else label.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            scrollMap.putIfAbsent(key, index)
        }
        _appScrollMap.value = scrollMap

        sorted
    }

    private fun normalizeForSort(s: String): String {
        val sb = StringBuilder(s.length)
        var lastWasSpace = false
        for (ch in s) {
            if (ch.isLetterOrDigit()) { sb.append(ch.lowercaseChar()); lastWasSpace = false }
            else if (ch.isWhitespace() && !lastWasSpace) { sb.append(' '); lastWasSpace = true }
        }
        return sb.toString().trim()
    }
}
