package com.github.codeworkscreativehub.mlauncher.data.datasource

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over SharedPreferences for testability.
 * All reads/writes go through this interface so we can swap
 * the implementation in tests with an in-memory fake.
 */
interface PreferencesDataSource {
    fun getString(key: String, default: String): String
    fun getInt(key: String, default: Int): Int
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getFloat(key: String, default: Float): Float
    fun getStringSet(key: String, default: Set<String>): Set<String>

    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putBoolean(key: String, value: Boolean)
    fun putFloat(key: String, value: Float)
    fun putStringSet(key: String, value: Set<String>)

    fun remove(key: String)
    fun clear()
    fun all(): Map<String, Any?>

    /** Emits the key that changed whenever a preference is modified. */
    fun observeChanges(): Flow<String>
}
