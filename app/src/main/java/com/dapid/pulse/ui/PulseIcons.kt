package com.dapid.pulse.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// The web app's line icons (src/components/icons.tsx, bottom-nav.tsx), same 24×24 paths and stroke.
// <rect> and <circle> are written as path data. Icon() tints them with the current content colour.
object PulseIcons {
    val Home = line("Home", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")
    val Calendar = line("Calendar", RECT_CALENDAR, "M3.5 10h17M8 3v4M16 3v4")
    val Plus = line("Plus", "M12 5v14M5 12h14", width = 2.4f)
    val Wallet = line(
        "Wallet",
        "M3 7a2 2 0 0 1 2-2h13v4",
        "M3 7v11a2 2 0 0 0 2 2h15V9H5a2 2 0 0 1-2-2z",
        "M16 14.5h.01",
    )
    val More = dots()
    val Expense = line("Expense", "M7 4h10M12 4v16M8.5 9.5h7M8.5 13.5h7M8 20l4-6.5L16 20")
    val Event = Calendar
    val ChevronLeft = line("Previous", "M15 5l-7 7 7 7")
    val ChevronRight = line("Next", "M9 5l7 7-7 7")
    val Deadline = line("Deadline", "M19.5 13a7.5 7.5 0 1 1-15 0a7.5 7.5 0 1 1 15 0z", "M12 9.5V13l2.5 1.5M9.5 2.5h5")
}

private const val RECT_CALENDAR =
    "M6 5h12a2.5 2.5 0 0 1 2.5 2.5v10.5a2.5 2.5 0 0 1-2.5 2.5H6a2.5 2.5 0 0 1-2.5-2.5V7.5A2.5 2.5 0 0 1 6 5z"

private fun builder(name: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)

private fun line(name: String, vararg paths: String, width: Float = 1.8f): ImageVector =
    builder(name).apply {
        paths.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

// Three filled dots (r = 1.2) at x = 5.5, 12, 18.5.
private fun dots(): ImageVector =
    builder("More").apply {
        listOf(5.5f, 12f, 18.5f).forEach { cx ->
            addPath(
                pathData = addPathNodes("M${cx + 1.2f} 12a1.2 1.2 0 1 1-2.4 0a1.2 1.2 0 1 1 2.4 0z"),
                fill = SolidColor(Color.Black),
            )
        }
    }.build()
