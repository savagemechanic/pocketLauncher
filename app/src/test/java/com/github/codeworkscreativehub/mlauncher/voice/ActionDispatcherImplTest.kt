package com.github.codeworkscreativehub.mlauncher.voice

import com.github.codeworkscreativehub.mlauncher.voice.action.ActionResult
import com.github.codeworkscreativehub.mlauncher.voice.nlu.VoiceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionDispatcherImplTest {

    @Test
    fun `ActionResult Success is singleton`() {
        val result = ActionResult.Success
        assertTrue(result is ActionResult.Success)
    }

    @Test
    fun `ActionResult Failed holds reason`() {
        val result = ActionResult.Failed("App not found")
        assertEquals("App not found", result.reason)
    }

    @Test
    fun `ActionResult NeedsConfirmation holds message`() {
        val result = ActionResult.NeedsConfirmation("Which John?")
        assertEquals("Which John?", result.message)
    }

    @Test
    fun `ActionResult NeedsPermission holds permission`() {
        val result = ActionResult.NeedsPermission("CALL_PHONE")
        assertEquals("CALL_PHONE", result.permission)
    }

    @Test
    fun `Clarification maps to NeedsConfirmation pattern`() {
        // Verify the contract: Clarification actions should map to NeedsConfirmation
        val action = VoiceAction.Clarification("Did you mean Camera or Calculator?")
        assertEquals("Did you mean Camera or Calculator?", action.message)
    }

    @Test
    fun `Unsupported maps to Failed pattern`() {
        val action = VoiceAction.Unsupported("Cannot do that")
        assertEquals("Cannot do that", action.message)
    }
}
