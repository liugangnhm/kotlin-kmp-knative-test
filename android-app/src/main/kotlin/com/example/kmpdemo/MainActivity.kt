package com.example.kmpdemo

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import demo.PlatformCalculator
import demo.Summary
import demo.summarize

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        val output = TextView(this).apply { textSize = 18f }
        val runButton = Button(this).apply {
            text = "Run shared Kotlin + C"
            setOnClickListener { output.text = runDemo() }
        }
        layout.addView(output, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        layout.addView(runButton)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            layout.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                view.setPadding(padding + bars.left, padding + bars.top,
                    padding + bars.right, padding + bars.bottom)
                insets
            }
        }
        setContentView(layout)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.setSystemBarsAppearance(
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        }
        output.text = runDemo()
    }

    private fun runDemo(): String {
        val values = intArrayOf(10, 20, 30, 40)
        val result = summarize(values)
        check(result == Summary(4, 100L))
        check(summarize(intArrayOf()).total == 0L)
        check(summarize(intArrayOf(-20, 5)).total == -15L)
        check(summarize(intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE)).total == 4294967294L)
        return """
            KMP + Kotlin + C

            Platform: ${PlatformCalculator.platform}
            Backend: ${PlatformCalculator.backend}

            Shared input: ${values.joinToString()}
            Count: ${result.count}
            Total: ${result.total}
            Average: ${result.average}

            Self-check: OK
        """.trimIndent()
    }
}
