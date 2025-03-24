package io.karte.android.inappframe.model

import org.json.JSONObject

internal data class CarouselWithMarginConfig(
    val bannerHeight: Int,
    val paddingTop: Int,
    val paddingBottom: Int,
    val radius: Int,
    val spacing: Int,
    val ratio: Int,
    val autoplaySpeed: Double?,
    val templateType: String
) {
    companion object {
        fun parseOrThrow(jsonObject: JSONObject): CarouselWithMarginConfig {
            val config = jsonObject.getJSONObject("content").getJSONObject("config")
            return config.run {
                CarouselWithMarginConfig(
                    bannerHeight = getInt("bannerHeight"),
                    autoplaySpeed = runCatching { getDouble("autoplaySpeed") }.getOrNull(),
                    paddingTop = getInt("paddingTop"),
                    paddingBottom = getInt("paddingBottom"),
                    radius = getInt("radius"),
                    spacing = getInt("spacing"),
                    ratio = getInt("ratio"),
                    templateType = getString("templateType")
                )
            }
        }
    }
}

internal data class CarouselWithMarginContent(
    val data: List<Image>,
    val config: CarouselWithMarginConfig
) {
    companion object {
        suspend fun parseOrThrow(json: JSONObject): CarouselWithMarginContent {
            return CarouselWithMarginContent(
                data = Image.parseToListOrThrow(json),
                config = CarouselWithMarginConfig.parseOrThrow(json)
            )
        }
    }
}

internal data class CarouselWithMarginV1(
    override val componentType: String,
    val content: CarouselWithMarginContent,
    override val version: IAFVersion
) : InAppFrameData() {
    companion object {
        const val TEMPLATE_TYPE = "carouselWithMargin"
        suspend fun parseOrThrow(json: JSONObject): CarouselWithMarginV1 {
            return CarouselWithMarginV1(
                componentType = parseComponentTypeOrThrow(json),
                content = CarouselWithMarginContent.parseOrThrow(json),
                version = parseVersionOrThrow(json)
            )
        }
    }
}
