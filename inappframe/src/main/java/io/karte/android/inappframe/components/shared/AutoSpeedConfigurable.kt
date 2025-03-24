package io.karte.android.inappframe.components.shared

internal interface AutoSpeedConfigurable {
    // デフォルト実装
    fun getTransitionStopMillis(autoPlaySpeed: Double?): Long? {
        autoPlaySpeed?.run {
            val waitTime = (autoPlaySpeed * 1000).toLong()
            return waitTime
        }
        return null
    }
}
