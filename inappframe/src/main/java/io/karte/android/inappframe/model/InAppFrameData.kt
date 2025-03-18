package io.karte.android.inappframe.model

import org.json.JSONObject

internal sealed class InAppFrameData {
    abstract val version: IAFVersion
    abstract val componentType: String

    companion object {
        fun parseVersionOrThrow(json: JSONObject): IAFVersion {
            return IAFVersion.fromString(json.getString("version"))
        }

        fun parseComponentTypeOrThrow(json: JSONObject): String {
            return json.getString("componentType")
        }
    }
}

// 何も表示しないパターン
internal object Empty : InAppFrameData() {
    override val componentType: String = "empty"
    override val version: IAFVersion = IAFVersion.UNKNOWN
}
