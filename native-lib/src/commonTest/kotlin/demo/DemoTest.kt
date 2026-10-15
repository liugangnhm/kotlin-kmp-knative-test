package demo

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoTest {
    @Test
    fun summarizesValues() {
        val summary = summarize(intArrayOf(10, 20, 30, 40))
        assertEquals(Summary(4, 100L), summary)
        assertEquals(25.0, summary.average)
    }

    @Test
    fun handlesEmptyInput() {
        assertEquals(Summary(0, 0L), summarize(intArrayOf()))
        assertEquals(0.0, summarize(intArrayOf()).average)
    }

    @Test
    fun handlesNegativeValues() {
        assertEquals(-15L, summarize(intArrayOf(-20, 5)).total)
    }

    @Test
    fun accumulatesBeyondIntRange() {
        assertEquals(4294967294L, summarize(intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE)).total)
    }
}
