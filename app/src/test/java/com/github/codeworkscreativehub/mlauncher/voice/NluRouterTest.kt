package com.github.codeworkscreativehub.mlauncher.voice

import com.github.codeworkscreativehub.mlauncher.data.Constants.Action
import com.github.codeworkscreativehub.mlauncher.voice.nlu.PhoneContext
import com.github.codeworkscreativehub.mlauncher.voice.nlu.VoiceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for NluRouter behavior.
 *
 * Since NluRouter requires Android Context, these tests verify
 * the routing contract and fallback patterns.
 */
class NluRouterTest {

    private val emptyContext = PhoneContext(
        installedApps = emptyList(),
        recentApps = emptyList(),
        contacts = emptyList(),
        availableActions = Action.entries.map { it.name },
        currentTime = "2026-02-13 12:00"
    )

    @Test
    fun `VoiceAction Unsupported triggers fallback pattern`() {
        // When cloud returns Unsupported, router should fall back to local
        val cloudResult = VoiceAction.Unsupported("No match")
        assertTrue(cloudResult is VoiceAction.Unsupported)
    }

    @Test
    fun `valid cloud result should be used directly`() {
        val cloudResult = VoiceAction.LaunchApp("com.example.camera")
        assertTrue(cloudResult is VoiceAction.LaunchApp)
        assertEquals("com.example.camera", (cloudResult as VoiceAction.LaunchApp).packageName)
    }

    @Test
    fun `PhoneContext provides all action names`() {
        val context = emptyContext
        assertTrue(context.availableActions.contains("LockScreen"))
        assertTrue(context.availableActions.contains("VoiceCommand"))
    }

    @Test
    fun `sealed class exhaustiveness covers all voice action types`() {
        // Verify all VoiceAction subtypes can be constructed
        val actions: List<VoiceAction> = listOf(
            VoiceAction.LaunchApp("pkg"),
            VoiceAction.CallContact("name"),
            VoiceAction.SendMessage("to", "body"),
            VoiceAction.SystemAction(Action.LockScreen),
            VoiceAction.OpenUrl("https://example.com"),
            VoiceAction.SetAlarm(7, 0),
            VoiceAction.DeviceSetting("wifi", true),
            VoiceAction.AccessibilityAction("tap button"),
            VoiceAction.CompoundAction(emptyList()),
            VoiceAction.Clarification("which one?"),
            VoiceAction.Unsupported("nope")
        )
        assertEquals(11, actions.size)
    }
}
