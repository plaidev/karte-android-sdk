package io.karte.android.inappframe.model

import org.json.JSONObject

internal data class CarouselWithoutPagingConfig(
    val paddingTop: Int,
    val paddingBottom: Int,
    val paddingStart: Int,
    val paddingEnd: Int,
    val spacing: Int,
    val radius: Int,
    val bannerHeight: Int,
    val ratio: Int,
    val autoplaySpeed: Double?
) {
    companion object {
        fun parseOrThrow(jsonObject: JSONObject): CarouselWithoutPagingConfig {
            val config = jsonObject.getJSONObject("content").getJSONObject("config")
            return config.run {
                CarouselWithoutPagingConfig(
                    autoplaySpeed = runCatching { getDouble("autoplaySpeed") }.getOrNull(),
                    paddingStart = getInt("paddingStart"),
                    paddingEnd = getInt("paddingEnd"),
                    paddingTop = getInt("paddingTop"),
                    paddingBottom = getInt("paddingBottom"),
                    bannerHeight = getInt("bannerHeight"),
                    radius = getInt("radius"),
                    ratio = getInt("ratio"),
                    spacing = getInt("spacing")
                )
            }
        }
    }
}

internal data class CarouselWithoutPagingContent(
    val data: List<Image>,
    val config: CarouselWithoutPagingConfig
) {
    companion object {
        suspend fun parseOrThrow(json: JSONObject): CarouselWithoutPagingContent {
            return CarouselWithoutPagingContent(
                data = Image.parseToListOrThrow(json),
                config = CarouselWithoutPagingConfig.parseOrThrow(json)
            )
        }
    }
}

internal data class CarouselWithoutPagingV1(
    override val componentType: String,
    override val version: IAFVersion,
    val content: CarouselWithoutPagingContent
) : InAppFrameData() {
    companion object {
        const val TEMPLATE_TYPE = "carouselWithoutPaging"
        suspend fun parseOrThrow(json: JSONObject): CarouselWithoutPagingV1 {
            return CarouselWithoutPagingV1(
                componentType = parseComponentTypeOrThrow(json),
                version = parseVersionOrThrow(json),
                content = CarouselWithoutPagingContent.parseOrThrow(json)
            )
        }
    }
}
