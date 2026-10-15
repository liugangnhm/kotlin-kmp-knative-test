package demo

fun runDemo() {
    val values = intArrayOf(10, 20, 30, 40)
    val summary = summarize(values)
    println("Platform: ${PlatformCalculator.platform}")
    println("Backend: ${PlatformCalculator.backend}")
    println("Shared input: ${values.joinToString()}")
    println("Shared summary: count=${summary.count}, total=${summary.total}, average=${summary.average}")
    check(summary == Summary(4, 100L))
    check(summarize(intArrayOf()).total == 0L)
    check(summarize(intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE)).total == 4294967294L)
    println("Self-check: OK")
}
