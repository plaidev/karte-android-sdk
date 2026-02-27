package io.karte.android.inappframe.components.shared

import android.content.Context

internal interface DensityConvertible {
    // デフォルト実装:
    fun dpToPx(context: Context, dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
}
