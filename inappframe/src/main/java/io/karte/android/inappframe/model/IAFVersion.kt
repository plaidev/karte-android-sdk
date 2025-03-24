package io.karte.android.inappframe.model

internal enum class IAFVersion(val versionName: String) {
    V1("v1"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(version: String): IAFVersion {
            return values().find { it.versionName == version } ?: UNKNOWN
        }
    }
}
