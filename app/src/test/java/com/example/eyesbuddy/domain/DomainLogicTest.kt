package com.example.eyesbuddy.domain

import com.example.eyesbuddy.domain.clock.DigitFlipState
import com.example.eyesbuddy.domain.motion.MotionInterpreter
import com.example.eyesbuddy.domain.perimeter.PerimeterTimerController
import com.example.eyesbuddy.domain.quotes.Quote
import com.example.eyesbuddy.domain.quotes.QuoteCategory
import com.example.eyesbuddy.domain.quotes.QuoteScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainLogicTest {
    @Test fun scheduler_emitsOnlyWhenDue() {
        val scheduler = QuoteScheduler(intervalMillis = 1_000L)
        val quotes = listOf(Quote("pause", QuoteCategory.BREAK))
        assertNotNull(scheduler.nextIfDue(0L, quotes))
        assertNull(scheduler.nextIfDue(999L, quotes))
        assertNotNull(scheduler.nextIfDue(1_000L, quotes))
    }

    @Test fun perimeter_progress_wrapsEachMinute() {
        val controller = PerimeterTimerController(60_000L)
        assertEquals(0f, controller.progressAt(0L), 0.001f)
        assertEquals(0.5f, controller.progressAt(30_000L), 0.001f)
        assertEquals(0f, controller.progressAt(60_000L), 0.001f)
    }

    @Test fun digit_state_reportsOnlyActualChanges() {
        val state = DigitFlipState('2')
        assertFalse(state.update('2'))
        assertTrue(state.update('3'))
        assertEquals('3', state.value)
    }

    @Test fun motion_interpreter_clamps_acceleration_tilt() {
        val tilt = MotionInterpreter().tiltFromAcceleration(30f, -30f, 10f)
        assertEquals(1f, tilt.first, 0.001f)
        assertEquals(-1f, tilt.second, 0.001f)
    }
}
