package com.github.codeworkscreativehub.mlauncher.data.repository

import android.os.UserHandle
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages app aliases, tags, pinned/hidden/locked app sets,
 * and profile counters. Extracted from Prefs to isolate
 * app-management concerns from general settings.
 */
interface AppManagementRepository {

    val hiddenApps: StateFlow<Set<String>>
    val lockedApps: StateFlow<Set<String>>
    val pinnedApps: StateFlow<Set<String>>
    val hiddenContacts: StateFlow<Set<String>>
    val pinnedContacts: StateFlow<Set<String>>

    fun getAppAlias(appPackage: String): String
    fun setAppAlias(appPackage: String, alias: String)

    fun getAppTag(appPackage: String, userHandle: UserHandle? = null): String
    fun setAppTag(appPackage: String, tag: String, userHandle: UserHandle? = null)

    fun setHiddenApps(apps: Set<String>)
    fun setLockedApps(apps: Set<String>)
    fun setPinnedApps(apps: Set<String>)
    fun setHiddenContacts(contacts: Set<String>)
    fun setPinnedContacts(contacts: Set<String>)

    fun setProfileCounter(profile: String, count: Int)
    fun getProfileCounter(profile: String): Int
}
