package demo

actual object PlatformCalculator {
    actual val platform: String = "JVM / ${System.getProperty("os.name")}"
    actual val backend: String = "Pure Kotlin (JVM implementation)"

    actual fun sum(values: IntArray): Long =
        values.fold(0L) { total, value -> total + value }
}
