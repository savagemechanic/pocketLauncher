package com.github.codeworkscreativehub.mlauncher.data.repository

import android.os.UserHandle
import com.github.codeworkscreativehub.mlauncher.data.HIDDEN_APPS
import com.github.codeworkscreativehub.mlauncher.data.HIDDEN_CONTACTS
import com.github.codeworkscreativehub.mlauncher.data.LOCKED_APPS
import com.github.codeworkscreativehub.mlauncher.data.PINNED_APPS
import com.github.codeworkscreativehub.mlauncher.data.PINNED_CONTACTS
import com.github.codeworkscreativehub.mlauncher.data.datasource.PreferencesDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppManagementRepositoryImpl(
    private val dataSource: PreferencesDataSource,
    scope: CoroutineScope
) : AppManagementRepository {

    private val _hiddenApps = MutableStateFlow(dataSource.getStringSet(HIDDEN_APPS, emptySet()))
    override val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    private val _lockedApps = MutableStateFlow(dataSource.getStringSet(LOCKED_APPS, emptySet()))
    override val lockedApps: StateFlow<Set<String>> = _lockedApps.asStateFlow()

    private val _pinnedApps = MutableStateFlow(dataSource.getStringSet(PINNED_APPS, emptySet()))
    override val pinnedApps: StateFlow<Set<String>> = _pinnedApps.asStateFlow()

    private val _hiddenContacts = MutableStateFlow(dataSource.getStringSet(HIDDEN_CONTACTS, emptySet()))
    override val hiddenContacts: StateFlow<Set<String>> = _hiddenContacts.asStateFlow()

    private val _pinnedContacts = MutableStateFlow(dataSource.getStringSet(PINNED_CONTACTS, emptySet()))
    override val pinnedContacts: StateFlow<Set<String>> = _pinnedContacts.asStateFlow()

    init {
        scope.launch {
            dataSource.observeChanges().collect { key ->
                when (key) {
                    HIDDEN_APPS -> _hiddenApps.value = dataSource.getStringSet(HIDDEN_APPS, emptySet())
                    LOCKED_APPS -> _lockedApps.value = dataSource.getStringSet(LOCKED_APPS, emptySet())
                    PINNED_APPS -> _pinnedApps.value = dataSource.getStringSet(PINNED_APPS, emptySet())
                    HIDDEN_CONTACTS -> _hiddenContacts.value = dataSource.getStringSet(HIDDEN_CONTACTS, emptySet())
                    PINNED_CONTACTS -> _pinnedContacts.value = dataSource.getStringSet(PINNED_CONTACTS, emptySet())
                }
            }
        }
    }

    override fun getAppAlias(appPackage: String): String =
        dataSource.getString("${appPackage}_ALIAS", "")

    override fun setAppAlias(appPackage: String, alias: String) {
        dataSource.putString("${appPackage}_ALIAS", alias)
    }

    override fun getAppTag(appPackage: String, userHandle: UserHandle?): String {
        val baseKey = "${appPackage}_TAG"
        val userKey = userHandle?.let { "${baseKey}_${it.hashCode()}" }
        return dataSource.getString(userKey ?: baseKey, "")
    }

    override fun setAppTag(appPackage: String, tag: String, userHandle: UserHandle?) {
        dataSource.remove("${appPackage}_TAG")
        userHandle?.let {
            dataSource.remove("${appPackage}_TAG_${it}")
            dataSource.putString("${appPackage}_TAG_${it.hashCode()}", tag)
        }
    }

    override fun setHiddenApps(apps: Set<String>) {
        dataSource.putStringSet(HIDDEN_APPS, apps)
        _hiddenApps.value = apps
    }

    override fun setLockedApps(apps: Set<String>) {
        dataSource.putStringSet(LOCKED_APPS, apps)
        _lockedApps.value = apps
    }

    override fun setPinnedApps(apps: Set<String>) {
        dataSource.putStringSet(PINNED_APPS, apps)
        _pinnedApps.value = apps
    }

    override fun setHiddenContacts(contacts: Set<String>) {
        dataSource.putStringSet(HIDDEN_CONTACTS, contacts)
        _hiddenContacts.value = contacts
    }

    override fun setPinnedContacts(contacts: Set<String>) {
        dataSource.putStringSet(PINNED_CONTACTS, contacts)
        _pinnedContacts.value = contacts
    }

    override fun setProfileCounter(profile: String, count: Int) {
        dataSource.putInt(profile, count)
    }

    override fun getProfileCounter(profile: String): Int =
        dataSource.getInt(profile, 0)
}
