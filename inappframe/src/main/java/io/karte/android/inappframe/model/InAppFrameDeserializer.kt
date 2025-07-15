package io.karte.android.inappframe.model

import io.karte.android.variables.Variable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal object InAppFrameDeserializer {
    suspend fun deserialize(variable: Variable): Pair<InAppFrameData, IAFTracker> = withContext(Dispatchers.IO) {
        val json = variable.jsonObject(JSONObject())
        val templateType = json
            .getJSONObject("content")
            .getJSONObject("config")
            .getString("templateType")
        val version = InAppFrameData.parseVersionOrThrow(json)

        val iafTracker = IAFTracker.create(variable, templateType)

        // 一旦V1の状態だけ定義
        val inAppFrameData = when (version) {
            IAFVersion.V1 -> when (templateType) {
                CarouselWithMarginV1.TEMPLATE_TYPE -> CarouselWithMarginV1.parseOrThrow(json)
                CarouselWithoutMarginV1.TEMPLATE_TYPE -> CarouselWithoutMarginV1.parseOrThrow(json)
                CarouselWithoutPagingV1.TEMPLATE_TYPE -> CarouselWithoutPagingV1.parseOrThrow(json)
                SimpleBannerV1.TEMPLATE_TYPE -> SimpleBannerV1.parseOrThrow(json)
                else -> throw Exception("No templateType Matched.")
            }
            IAFVersion.UNKNOWN -> throw Exception("IAFVersion is unknown.")
        }
        Pair(inAppFrameData, iafTracker)
    }
}
