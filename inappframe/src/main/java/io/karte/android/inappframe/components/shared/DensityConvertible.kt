package io.karte.android.inappframe.components.shared

import android.content.Context

internal interface DensityConvertible {
    // デフォルト実装:
    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
