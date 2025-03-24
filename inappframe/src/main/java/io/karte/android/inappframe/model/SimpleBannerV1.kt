package io.karte.android.inappframe.model

import org.json.JSONObject

internal data class SimpleBannerConfig(
    val paddingTop: Int,
    val paddingBottom: Int,
    val paddingStart: Int,
    val paddingEnd: Int,
    val radius: Int,
    val ratio: Int
) {
    companion object {
        fun parseOrThrow(jsonObject: JSONObject): SimpleBannerConfig {
            val config = jsonObject.getJSONObject("content").getJSONObject("config")
            return config.run {
                SimpleBannerConfig(
                    paddingStart = getInt("paddingStart"),
                    paddingEnd = getInt("paddingEnd"),
                    paddingTop = getInt("paddingTop"),
                    paddingBottom = getInt("paddingBottom"),
                    radius = getInt("radius"),
                    ratio = getInt("ratio")
                )
            }
        }
    }
}

internal data class SimpleBannerContent(
    val data: List<Image>,
    val config: SimpleBannerConfig
) {
    companion object {
        suspend fun parseOrThrow(json: JSONObject): SimpleBannerContent {
            return SimpleBannerContent(
                data = Image.parseToListOrThrow(json),
                config = SimpleBannerConfig.parseOrThrow(json)
            )
        }
    }
}

internal data class SimpleBannerV1(
    override val componentType: String,
    override val version: IAFVersion,
    val content: SimpleBannerContent
) : InAppFrameData() {
    companion object {
        const val TEMPLATE_TYPE = "simpleBanner"
        suspend fun parseOrThrow(json: JSONObject): SimpleBannerV1 {
            return SimpleBannerV1(
                componentType = parseComponentTypeOrThrow(json),
                version = parseVersionOrThrow(json),
                content = SimpleBannerContent.parseOrThrow(json)
            )
        }
    }
}
