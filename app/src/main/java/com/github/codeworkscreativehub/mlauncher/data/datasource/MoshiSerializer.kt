package com.github.codeworkscreativehub.mlauncher.data.datasource

import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.data.Message
import com.github.codeworkscreativehub.mlauncher.data.MessageWrong
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType

/**
 * Extracted serialization logic that was previously inlined in Prefs.
 * Centralizes all Moshi adapter creation and JSON conversion.
 */
class MoshiSerializer {

    private val moshi: Moshi = Moshi.Builder().build()

    private val messageListType: ParameterizedType =
        Types.newParameterizedType(List::class.java, Message::class.java)
    private val messageAdapter: JsonAdapter<List<Message>> =
        moshi.adapter(messageListType)

    private val messageWrongListType: ParameterizedType =
        Types.newParameterizedType(List::class.java, MessageWrong::class.java)
    private val messageWrongAdapter: JsonAdapter<List<MessageWrong>> =
        moshi.adapter(messageWrongListType)

    private val mapType: ParameterizedType =
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter: JsonAdapter<Map<String, Any?>> =
        moshi.adapter(mapType)

    private val stringMapType: ParameterizedType =
        Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val stringMapAdapter: JsonAdapter<Map<String, String>> =
        moshi.adapter(stringMapType)

    fun serializeMessages(messages: List<Message>): String =
        messageAdapter.toJson(messages)

    fun deserializeMessages(json: String): List<Message> =
        runCatching { messageAdapter.fromJson(json) }
            .onFailure { AppLogger.e("MoshiSerializer", "Failed to parse messages", it) }
            .getOrNull() ?: emptyList()

    fun deserializeMessagesWrong(json: String): List<MessageWrong> =
        runCatching { messageWrongAdapter.fromJson(json) }
            .onFailure { AppLogger.e("MoshiSerializer", "Failed to parse wrong messages", it) }
            .getOrNull() ?: emptyList()

    fun serializePrefsToJson(prefs: Map<String, Any?>): String =
        mapAdapter.indent("  ").toJson(prefs)

    fun deserializePrefsFromJson(json: String): Map<String, Any?> =
        runCatching { mapAdapter.fromJson(json) }
            .onFailure { AppLogger.e("MoshiSerializer", "Failed to parse prefs JSON", it) }
            .getOrNull() ?: emptyMap()

    fun serializeThemeToJson(colors: Map<String, String>): String =
        stringMapAdapter.indent("  ").toJson(colors)

    fun deserializeThemeFromJson(json: String): Map<String, Any?>? =
        runCatching { mapAdapter.fromJson(json) }
            .onFailure { AppLogger.e("MoshiSerializer", "Failed to parse theme JSON", it) }
            .getOrNull()
}
