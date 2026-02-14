package com.github.codeworkscreativehub.mlauncher.data

import android.content.Context
import com.github.codeworkscreativehub.mlauncher.data.cache.AppCacheManager
import com.github.codeworkscreativehub.mlauncher.data.datasource.MoshiSerializer
import com.github.codeworkscreativehub.mlauncher.data.datasource.PreferencesDataSource
import com.github.codeworkscreativehub.mlauncher.data.datasource.SharedPrefsDataSource
import com.github.codeworkscreativehub.mlauncher.data.repository.AppManagementRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.AppManagementRepositoryImpl
import com.github.codeworkscreativehub.mlauncher.data.repository.AppRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.AppRepositoryImpl
import com.github.codeworkscreativehub.mlauncher.data.repository.ContactRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.ContactRepositoryImpl
import com.github.codeworkscreativehub.mlauncher.data.repository.SettingsRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.SettingsRepositoryImpl
import com.github.codeworkscreativehub.mlauncher.data.repository.ThemeRepository
import com.github.codeworkscreativehub.mlauncher.data.repository.ThemeRepositoryImpl
import com.github.codeworkscreativehub.mlauncher.domain.dialer.ContactDialer
import com.github.codeworkscreativehub.mlauncher.domain.launcher.AppLauncher
import com.github.codeworkscreativehub.mlauncher.domain.launcher.AppLauncherImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/**
 * Manual dependency injection singleton.
 * Provides repository instances scoped to the application lifecycle.
 * Initialize once from Application.onCreate() or lazily on first access.
 */
object RepositoryProvider {

    @Volatile
    private var initialized = false

    private lateinit var _dataSource: PreferencesDataSource
    private lateinit var _serializer: MoshiSerializer
    private lateinit var _settingsRepository: SettingsRepository
    private lateinit var _appManagementRepository: AppManagementRepository
    private lateinit var _themeRepository: ThemeRepository
    private lateinit var _appRepository: AppRepository
    private lateinit var _contactRepository: ContactRepository
    private lateinit var _appLauncher: AppLauncher
    private lateinit var _contactDialer: ContactDialer

    val dataSource: PreferencesDataSource get() = _dataSource
    val serializer: MoshiSerializer get() = _serializer
    val settingsRepository: SettingsRepository get() = _settingsRepository
    val appManagementRepository: AppManagementRepository get() = _appManagementRepository
    val themeRepository: ThemeRepository get() = _themeRepository
    val appRepository: AppRepository get() = _appRepository
    val contactRepository: ContactRepository get() = _contactRepository
    val appLauncher: AppLauncher get() = _appLauncher
    val contactDialer: ContactDialer get() = _contactDialer

    fun initialize(context: Context, scope: CoroutineScope = MainScope()) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            val appContext = context.applicationContext
            _dataSource = SharedPrefsDataSource(appContext)
            _serializer = MoshiSerializer()
            _settingsRepository = SettingsRepositoryImpl(_dataSource, _serializer, appContext, scope)
            _appManagementRepository = AppManagementRepositoryImpl(_dataSource, scope)
            _themeRepository = ThemeRepositoryImpl(_dataSource, _serializer, appContext, scope)

            val cacheManager = AppCacheManager(appContext.cacheDir)
            _appRepository = AppRepositoryImpl(appContext, _appManagementRepository, _settingsRepository, cacheManager, scope)
            _contactRepository = ContactRepositoryImpl(appContext, _appManagementRepository, cacheManager, scope)
            _appLauncher = AppLauncherImpl(appContext, _appManagementRepository)
            _contactDialer = ContactDialer(appContext)

            initialized = true
        }
    }
}
