package consumer

import demo.PlatformCalculator
import demo.Summary
import demo.summarize

fun runConsumer() {
    println("Dependency: com.example:native-lib:1.0.0 (Maven artifact)")
    println("Platform: ${PlatformCalculator.platform}")
    println("Backend: ${PlatformCalculator.backend}")
    val summary = summarize(intArrayOf(7, 8, 9))
    check(summary == Summary(3, 24L))
    check(summary.average == 8.0)
    check(summarize(intArrayOf()).total == 0L)
    check(summarize(intArrayOf(-20, 5)).total == -15L)
    check(summarize(intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE)).total == 4294967294L)
    println("Consumer summary: count=${summary.count}, total=${summary.total}, average=${summary.average}")
    println("Consumer self-check: OK")
}
