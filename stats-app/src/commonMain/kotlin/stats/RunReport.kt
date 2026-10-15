package stats

import demo.PlatformCalculator
import demo.Summary
import demo.summarize

fun runReport() {
    println("Application: stats-report")
    println("Platform: ${PlatformCalculator.platform}")
    println("Backend: ${PlatformCalculator.backend}")

    val morning = summarize(intArrayOf(12, 18, 30))
    val afternoon = summarize(intArrayOf(25, 35))
    check(morning == Summary(3, 60L))
    check(afternoon == Summary(2, 60L))
    println("Morning: count=${morning.count}, total=${morning.total}, average=${morning.average}")
    println("Afternoon: count=${afternoon.count}, total=${afternoon.total}, average=${afternoon.average}")
    println("Report self-check: OK")
}
