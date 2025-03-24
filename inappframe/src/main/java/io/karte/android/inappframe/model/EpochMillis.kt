package io.karte.android.inappframe.model

// テスト時にSystem.currentTimeMillis()がMock化できるようにするためのラッパーオブジェクト
internal object EpochMillis {
    // System.currentTimeMillis() をラップ
    fun now() = System.currentTimeMillis()
}
