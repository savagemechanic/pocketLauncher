package com.github.codeworkscreativehub.mlauncher.data.cache

import android.os.Process
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.data.AppCategory
import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.ContactCategory
import com.github.codeworkscreativehub.mlauncher.data.ContactListItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset

/**
 * File-based JSON cache for app and contact lists.
 * Extracted from MainViewModel to decouple caching from UI logic.
 *
 * Uses a simple JSON format with timestamp for staleness checks.
 * This is a O(n) serialization where n = number of apps/contacts.
 */
class AppCacheManager(cacheDir: File) {

    private val appsCacheFile = File(cacheDir, "apps_cache.json")
    private val contactsCacheFile = File(cacheDir, "contacts_cache.json")

    fun saveApps(list: List<AppListItem>) {
        runCatching {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("label", item.activityLabel)
                    put("package", item.activityPackage)
                    put("class", item.activityClass)
                    put("userHash", item.user.hashCode())
                    put("profileType", item.profileType)
                    put("customTag", item.customTag)
                    put("category", item.category.ordinal)
                }
                array.put(obj)
            }
            val top = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("items", array)
            }
            FileOutputStream(appsCacheFile).use { fos ->
                fos.write(top.toString().toByteArray(Charset.forName("UTF-8")))
            }
        }.onFailure { AppLogger.e("AppCacheManager", "Failed to save apps cache", it) }
    }

    fun loadApps(): List<AppListItem>? = runCatching {
        if (!appsCacheFile.exists()) return null
        val text = FileInputStream(appsCacheFile).use {
            String(it.readBytes(), Charset.forName("UTF-8"))
        }
        val top = JSONObject(text)
        val array = top.getJSONArray("items")
        val list = mutableListOf<AppListItem>()
        val userHandle = Process.myUserHandle()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                AppListItem(
                    activityLabel = obj.optString("label", ""),
                    activityPackage = obj.optString("package", ""),
                    activityClass = obj.optString("class", ""),
                    user = userHandle,
                    profileType = obj.optString("profileType", "SYSTEM"),
                    customTag = obj.optString("customTag", ""),
                    category = AppCategory.entries.getOrNull(obj.optInt("category", 1))
                        ?: AppCategory.REGULAR
                )
            )
        }
        list
    }.onFailure {
        AppLogger.e("AppCacheManager", "Failed to load apps cache", it)
    }.getOrNull()

    fun saveContacts(list: List<ContactListItem>) {
        runCatching {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("displayName", item.displayName)
                    put("phoneNumber", item.phoneNumber)
                    put("email", item.email)
                    put("category", item.category.ordinal)
                }
                array.put(obj)
            }
            val top = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("items", array)
            }
            FileOutputStream(contactsCacheFile).use { fos ->
                fos.write(top.toString().toByteArray(Charset.forName("UTF-8")))
            }
        }.onFailure { AppLogger.e("AppCacheManager", "Failed to save contacts cache", it) }
    }

    fun loadContacts(): List<ContactListItem>? = runCatching {
        if (!contactsCacheFile.exists()) return null
        val text = FileInputStream(contactsCacheFile).use {
            String(it.readBytes(), Charset.forName("UTF-8"))
        }
        val top = JSONObject(text)
        val array = top.getJSONArray("items")
        val list = mutableListOf<ContactListItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ContactListItem(
                    displayName = obj.optString("displayName", ""),
                    phoneNumber = obj.optString("phoneNumber", ""),
                    email = obj.optString("email", ""),
                    category = ContactCategory.entries.getOrNull(obj.optInt("category", 1))
                        ?: ContactCategory.REGULAR
                )
            )
        }
        list
    }.onFailure {
        AppLogger.e("AppCacheManager", "Failed to load contacts cache", it)
    }.getOrNull()
}
