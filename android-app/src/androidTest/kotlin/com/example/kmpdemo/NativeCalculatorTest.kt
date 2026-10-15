package com.example.kmpdemo

import androidx.test.ext.junit.runners.AndroidJUnit4
import demo.PlatformCalculator
import demo.Summary
import demo.summarize
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeCalculatorTest {
    @Test
    fun callsAndroidCBackend() {
        assertEquals("C shared library (Android NDK / JNI)", PlatformCalculator.backend)
    }

    @Test
    fun summarizesValuesThroughJni() {
        val summary = summarize(intArrayOf(10, 20, 30, 40))
        assertEquals(Summary(4, 100L), summary)
        assertEquals(25.0, summary.average, 0.0)
    }

    @Test
    fun handlesEmptyArrayThroughJni() {
        assertEquals(Summary(0, 0L), summarize(intArrayOf()))
    }

    @Test
    fun handlesNegativeValuesThroughJni() {
        assertEquals(-15L, summarize(intArrayOf(-20, 5)).total)
    }

    @Test
    fun accumulatesBeyondIntRangeThroughJni() {
        assertEquals(4294967294L, summarize(intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE)).total)
    }
}
