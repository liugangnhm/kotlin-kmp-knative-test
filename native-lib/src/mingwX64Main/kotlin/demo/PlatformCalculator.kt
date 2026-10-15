@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package demo

import demo.cinterop.demo_backend
import demo.cinterop.demo_sum
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned

actual object PlatformCalculator {
    actual val platform: String = "Windows x64 / Kotlin Native"
    actual val backend: String
        get() = demo_backend()!!.toKString()

    actual fun sum(values: IntArray): Long {
        // An empty array has no addressOf(0). C accepts null when count is zero.
        if (values.isEmpty()) return demo_sum(null, 0)
        // Keep the Kotlin array at a stable address during this synchronous C call.
        return values.usePinned { pinned ->
            demo_sum(pinned.addressOf(0), values.size)
        }
    }
}
