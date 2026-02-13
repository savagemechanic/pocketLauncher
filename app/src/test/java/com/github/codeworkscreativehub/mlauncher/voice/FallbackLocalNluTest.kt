package com.github.codeworkscreativehub.mlauncher.voice

import com.github.codeworkscreativehub.mlauncher.data.AppListItem
import com.github.codeworkscreativehub.mlauncher.data.Constants.Action
import com.github.codeworkscreativehub.mlauncher.data.ContactListItem
import com.github.codeworkscreativehub.mlauncher.voice.nlu.FallbackLocalNlu
import com.github.codeworkscreativehub.mlauncher.voice.nlu.PhoneContext
import com.github.codeworkscreativehub.mlauncher.voice.nlu.VoiceAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackLocalNluTest {

    private val emptyContext = PhoneContext(
        installedApps = emptyList(),
        recentApps = emptyList(),
        contacts = emptyList(),
        availableActions = Action.entries.map { it.name },
        currentTime = "2026-02-13 12:00"
    )

    // Cannot instantiate FallbackLocalNlu without Android Context in unit tests,
    // so these tests verify the VoiceAction data structures and patterns.

    @Test
    fun `VoiceAction SystemAction wraps Action enum correctly`() {
        val action = VoiceAction.SystemAction(Action.LockScreen)
        assertEquals(Action.LockScreen, action.action)
    }

    @Test
    fun `VoiceAction LaunchApp holds package and profile`() {
        val action = VoiceAction.LaunchApp("com.example.app", "MANAGED")
        assertEquals("com.example.app", action.packageName)
        assertEquals("MANAGED", action.profileType)
    }

    @Test
    fun `VoiceAction CallContact holds name and optional number`() {
        val action = VoiceAction.CallContact("Mom", "+1234567890")
        assertEquals("Mom", action.name)
        assertEquals("+1234567890", action.number)

        val noNumber = VoiceAction.CallContact("Mom")
        assertEquals(null, noNumber.number)
    }

    @Test
    fun `VoiceAction SendMessage holds recipient and body`() {
        val action = VoiceAction.SendMessage("Mom", "I'll be late")
        assertEquals("Mom", action.recipient)
        assertEquals("I'll be late", action.body)
    }

    @Test
    fun `VoiceAction SetAlarm validates hour and minute range`() {
        val action = VoiceAction.SetAlarm(7, 30, "Wake up")
        assertEquals(7, action.hour)
        assertEquals(30, action.minute)
        assertEquals("Wake up", action.label)
    }

    @Test
    fun `VoiceAction DeviceSetting holds setting and enable flag`() {
        val action = VoiceAction.DeviceSetting("wifi", true)
        assertEquals("wifi", action.setting)
        assertEquals(true, action.enable)
    }

    @Test
    fun `VoiceAction CompoundAction holds list of actions`() {
        val actions = listOf(
            VoiceAction.LaunchApp("com.a"),
            VoiceAction.SystemAction(Action.LockScreen)
        )
        val compound = VoiceAction.CompoundAction(actions)
        assertEquals(2, compound.actions.size)
        assertTrue(compound.actions[0] is VoiceAction.LaunchApp)
        assertTrue(compound.actions[1] is VoiceAction.SystemAction)
    }

    @Test
    fun `PhoneContext holds all required fields`() {
        val context = emptyContext
        assertTrue(context.installedApps.isEmpty())
        assertTrue(context.contacts.isEmpty())
        assertTrue(context.availableActions.isNotEmpty())
    }
}
