package demo

import demo.android.NativeBridge

actual object PlatformCalculator {
    actual val platform: String = "Android / ${android.os.Build.SUPPORTED_ABIS.first()}"
    actual val backend: String
        get() = NativeBridge.backend()

    actual fun sum(values: IntArray): Long = NativeBridge.sum(values)
}
