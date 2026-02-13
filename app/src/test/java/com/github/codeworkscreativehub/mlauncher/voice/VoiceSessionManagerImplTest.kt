package com.github.codeworkscreativehub.mlauncher.voice

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VoiceSessionManagerImplTest {

    private lateinit var manager: VoiceSessionManagerImpl

    @Before
    fun setup() {
        manager = VoiceSessionManagerImpl()
    }

    @Test
    fun `initial state is IDLE`() {
        assertEquals(VoiceSessionState.IDLE, manager.state)
    }

    @Test
    fun `startSession transitions IDLE to LISTENING`() {
        manager.startSession()
        assertEquals(VoiceSessionState.LISTENING, manager.state)
    }

    @Test
    fun `transitionToProcessing from LISTENING works`() {
        manager.startSession()
        manager.transitionToProcessing()
        assertEquals(VoiceSessionState.PROCESSING, manager.state)
    }

    @Test
    fun `transitionToExecuting from PROCESSING works`() {
        manager.startSession()
        manager.transitionToProcessing()
        manager.transitionToExecuting()
        assertEquals(VoiceSessionState.EXECUTING, manager.state)
    }

    @Test
    fun `transitionToFeedback from EXECUTING works`() {
        manager.startSession()
        manager.transitionToProcessing()
        manager.transitionToExecuting()
        manager.transitionToFeedback()
        assertEquals(VoiceSessionState.FEEDBACK, manager.state)
    }

    @Test
    fun `full happy path IDLE to FEEDBACK to IDLE`() {
        manager.startSession()
        manager.transitionToProcessing()
        manager.transitionToExecuting()
        manager.transitionToFeedback()
        manager.transitionToIdle()
        assertEquals(VoiceSessionState.IDLE, manager.state)
    }

    @Test
    fun `invalid transition does not change state`() {
        // Can't go from IDLE to PROCESSING directly
        manager.transitionToProcessing()
        assertEquals(VoiceSessionState.IDLE, manager.state)
    }

    @Test
    fun `transitionToError from any state works`() {
        manager.startSession()
        manager.transitionToError()
        assertEquals(VoiceSessionState.ERROR, manager.state)
    }

    @Test
    fun `cancelSession resets to IDLE from any state`() {
        manager.startSession()
        manager.transitionToProcessing()
        manager.cancelSession()
        assertEquals(VoiceSessionState.IDLE, manager.state)
    }

    @Test
    fun `destroy resets to IDLE`() {
        manager.startSession()
        manager.destroy()
        assertEquals(VoiceSessionState.IDLE, manager.state)
    }

    @Test
    fun `stateFlow emits correct values`() {
        assertEquals(VoiceSessionState.IDLE, manager.stateFlow.value)
        manager.startSession()
        assertEquals(VoiceSessionState.LISTENING, manager.stateFlow.value)
    }
}
