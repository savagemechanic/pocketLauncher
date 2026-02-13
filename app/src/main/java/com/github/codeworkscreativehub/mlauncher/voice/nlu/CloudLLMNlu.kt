package com.github.codeworkscreativehub.mlauncher.voice.nlu

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Cloud-based NLU using the Claude Messages API for intent classification.
 *
 * Sends the transcript with dynamic phone context (installed apps, contacts,
 * recent apps) and receives a structured JSON response mapped to VoiceAction.
 *
 * Uses EncryptedSharedPreferences for secure API key storage.
 */
class CloudLLMNlu(private val context: Context) : VoiceNLU {

    private val moshi = Moshi.Builder().build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    override suspend fun resolveIntent(transcript: String, phoneContext: PhoneContext): VoiceAction {
        val apiKey = getApiKey() ?: return VoiceAction.Unsupported("No API key configured")

        val systemPrompt = buildSystemPrompt(phoneContext)
        val requestJson = buildRequestJson(systemPrompt, transcript)

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext VoiceAction.Unsupported("Empty response")

            if (!response.isSuccessful) {
                return@withContext VoiceAction.Unsupported("API error: ${response.code}")
            }

            parseResponse(body)
        }
    }

    private fun buildSystemPrompt(phoneContext: PhoneContext): String {
        val appNames = phoneContext.installedApps
            .take(200)
            .joinToString(", ") { it.activityLabel }

        val recentApps = phoneContext.recentApps
            .take(10)
            .joinToString(", ")

        val contacts = phoneContext.contacts
            .take(100)
            .joinToString(", ") { it.displayName }

        val actions = phoneContext.availableActions.joinToString(", ")

        return """
            You are a voice command classifier for an Android launcher app.
            Given a voice transcript, classify it into exactly one action.

            Available apps: $appNames
            Recent apps: $recentApps
            Contacts: $contacts
            System actions: $actions
            Current time: ${phoneContext.currentTime}

            Respond with ONLY a JSON object matching one of these schemas:
            {"type":"launch_app","package_name":"<pkg>","profile_type":"SYSTEM"}
            {"type":"call_contact","name":"<name>","number":"<number or null>"}
            {"type":"send_message","recipient":"<name or number>","body":"<text>"}
            {"type":"system_action","action":"<action_name>"}
            {"type":"open_url","url":"<url>"}
            {"type":"set_alarm","hour":<int>,"minute":<int>,"label":"<optional>"}
            {"type":"device_setting","setting":"<name>","enable":<bool>}
            {"type":"accessibility_action","description":"<what to do>"}
            {"type":"compound_action","actions":[<array of above actions>]}
            {"type":"clarification","message":"<ask user for clarification>"}
            {"type":"unsupported","message":"<reason>"}

            Match app names fuzzily. Prefer recently used apps when ambiguous.
            For compound requests (e.g. "open X and call Y"), use compound_action.
        """.trimIndent()
    }

    private fun buildRequestJson(systemPrompt: String, transcript: String): String {
        val escapedSystem = escapeJson(systemPrompt)
        val escapedTranscript = escapeJson(transcript)

        return """
            {
                "model": "$MODEL",
                "max_tokens": 256,
                "system": "$escapedSystem",
                "messages": [
                    {"role": "user", "content": "$escapedTranscript"}
                ]
            }
        """.trimIndent()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseResponse(body: String): VoiceAction {
        return try {
            val adapter = moshi.adapter(Map::class.java)
            val root = adapter.fromJson(body) as? Map<String, Any?> ?: return fallback("Invalid JSON")
            val content = (root["content"] as? List<Map<String, Any?>>)?.firstOrNull()
                ?: return fallback("No content")
            val text = content["text"] as? String ?: return fallback("No text")

            // Extract JSON from the text (may be wrapped in markdown code blocks)
            val jsonStr = extractJson(text)
            val action = adapter.fromJson(jsonStr) as? Map<String, Any?> ?: return fallback("Invalid action JSON")

            mapToVoiceAction(action)
        } catch (e: Exception) {
            VoiceAction.Unsupported("Parse error: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToVoiceAction(json: Map<String, Any?>): VoiceAction {
        return when (json["type"]) {
            "launch_app" -> VoiceAction.LaunchApp(
                packageName = json["package_name"] as? String ?: "",
                profileType = json["profile_type"] as? String ?: "SYSTEM"
            )
            "call_contact" -> VoiceAction.CallContact(
                name = json["name"] as? String ?: "",
                number = json["number"] as? String
            )
            "send_message" -> VoiceAction.SendMessage(
                recipient = json["recipient"] as? String ?: "",
                body = json["body"] as? String ?: ""
            )
            "system_action" -> {
                val actionName = json["action"] as? String ?: ""
                val action = try {
                    com.github.codeworkscreativehub.mlauncher.data.Constants.Action.valueOf(actionName)
                } catch (_: IllegalArgumentException) {
                    return VoiceAction.Unsupported("Unknown system action: $actionName")
                }
                VoiceAction.SystemAction(action)
            }
            "open_url" -> VoiceAction.OpenUrl(json["url"] as? String ?: "")
            "set_alarm" -> VoiceAction.SetAlarm(
                hour = (json["hour"] as? Number)?.toInt() ?: 0,
                minute = (json["minute"] as? Number)?.toInt() ?: 0,
                label = json["label"] as? String
            )
            "device_setting" -> VoiceAction.DeviceSetting(
                setting = json["setting"] as? String ?: "",
                enable = json["enable"] as? Boolean ?: true
            )
            "accessibility_action" -> VoiceAction.AccessibilityAction(
                description = json["description"] as? String ?: ""
            )
            "compound_action" -> {
                val actions = (json["actions"] as? List<Map<String, Any?>>)
                    ?.map { mapToVoiceAction(it) }
                    ?: emptyList()
                VoiceAction.CompoundAction(actions)
            }
            "clarification" -> VoiceAction.Clarification(json["message"] as? String ?: "")
            else -> VoiceAction.Unsupported(json["message"] as? String ?: "Unknown action type")
        }
    }

    private fun extractJson(text: String): String {
        // Try to extract JSON from markdown code block
        val codeBlockPattern = Regex("```(?:json)?\\s*\\n?(\\{.*?})\\s*\\n?```", RegexOption.DOT_MATCHES_ALL)
        codeBlockPattern.find(text)?.let { return it.groupValues[1] }

        // Otherwise assume the whole text is JSON
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun fallback(reason: String) = VoiceAction.Unsupported(reason)

    private fun getApiKey(): String? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            prefs.getString(API_KEY_PREF, null)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-sonnet-4-5-20250929"
        private const val ENCRYPTED_PREFS_NAME = "voice_secure_prefs"
        private const val API_KEY_PREF = "claude_api_key"

        fun saveApiKey(context: Context, apiKey: String) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            prefs.edit().putString(API_KEY_PREF, apiKey).apply()
        }

        fun getApiKey(context: Context): String? {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val prefs = EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                prefs.getString(API_KEY_PREF, null)?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
    }
}
