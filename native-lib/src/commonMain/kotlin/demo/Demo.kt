package demo

// Shared code depends on this contract, without importing platform or C APIs.
expect object PlatformCalculator {
    val platform: String
    val backend: String
    fun sum(values: IntArray): Long
}

data class Summary(val count: Int, val total: Long) {
    val average: Double
        get() = if (count == 0) 0.0 else total.toDouble() / count
}

fun summarize(values: IntArray): Summary =
    Summary(values.size, PlatformCalculator.sum(values))
