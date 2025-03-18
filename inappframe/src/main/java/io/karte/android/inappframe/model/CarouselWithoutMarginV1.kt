package io.karte.android.inappframe.model

import org.json.JSONObject

internal data class CarouselWithoutMarginConfig(
    val paddingTop: Int,
    val paddingBottom: Int,
    val radius: Int,
    val ratio: Int,
    val autoplaySpeed: Double?,
    val templateType: String
) {
    companion object {
        fun parseOrThrow(jsonObject: JSONObject): CarouselWithoutMarginConfig {
            val config = jsonObject.getJSONObject("content").getJSONObject("config")
            return config.run {
                CarouselWithoutMarginConfig(
                    autoplaySpeed = runCatching { getDouble("autoplaySpeed") }.getOrNull(),
                    paddingTop = getInt("paddingTop"),
                    paddingBottom = getInt("paddingBottom"),
                    radius = getInt("radius"),
                    ratio = getInt("ratio"),
                    templateType = getString("templateType")
                )
            }
        }
    }
}

internal data class CarouselWithoutMarginContent(
    val data: List<Image>,
    val config: CarouselWithoutMarginConfig
) {
    companion object {
        suspend fun parseOrThrow(json: JSONObject): CarouselWithoutMarginContent {
            return CarouselWithoutMarginContent(
                data = Image.parseToListOrThrow(json),
                config = CarouselWithoutMarginConfig.parseOrThrow(json)
            )
        }
    }
}

internal data class CarouselWithoutMarginV1(
    override val componentType: String,
    override val version: IAFVersion,
    val content: CarouselWithoutMarginContent
) : InAppFrameData() {
    companion object {
        const val TEMPLATE_TYPE = "carouselWithoutMargin"
        suspend fun parseOrThrow(json: JSONObject): CarouselWithoutMarginV1 {
            return CarouselWithoutMarginV1(
                componentType = parseComponentTypeOrThrow(json),
                content = CarouselWithoutMarginContent.parseOrThrow(json),
                version = parseVersionOrThrow(json)
            )
        }
    }
}
