package io.karte.android.inappframe.model

import io.karte.android.tracking.MessageEvent
import io.karte.android.tracking.MessageEventType
import io.karte.android.tracking.Tracker
import io.karte.android.utilities.merge
import io.karte.android.variables.Variable

internal class IAFTracker(
    private val campaignId: String,
    private val shortenId: String,
    private val templateType: String,
    private val timestamp: String?,
    private val eventHash: String?
) {
    /**
     * IAFが依存する設定値に関連するキャンペーン情報を元に効果測定用のイベント（message_open）を発火します。
     */
    fun trackOpen() {
        val values = mapOf(
            "in_app_frame" to mapOf(
                "template_type" to templateType
            )
        )
        track(MessageEventType.Open, campaignId, shortenId, values, timestamp, eventHash)
    }

    /**
     * IAFが依存する設定値に関連するキャンペーン情報を元に効果測定用のイベント（message_click）を発火します。
     *
     * @param positionNo クリックした位置
     * @param url クリックしたurl
     */
    fun trackClick(positionNo: Int, url: String) {
        val values = mapOf(
            "url" to url,
            "in_app_frame" to mapOf(
                "template_type" to templateType,
                "position_no" to positionNo
            )
        )
        track(MessageEventType.Click, campaignId, shortenId, values, timestamp, eventHash)
    }

    private fun track(
        type: MessageEventType,
        campaignId: String,
        shortenId: String,
        values: Map<String, Any>,
        timestamp: String?,
        eventHash: String?
    ) {
        val base = values.toMutableMap()

        timestamp?.let {
            base.merge(
                mapOf(
                    "message" to mapOf(
                        "response_id" to "${it}_$shortenId",
                        "response_timestamp" to it
                    )
                )
            )
        }

        eventHash?.let {
            base.merge(
                mapOf(
                    "message" to mapOf(
                        "trigger" to mapOf(
                            "event_hashes" to it
                        )
                    )
                )
            )
        }

        val event = MessageEvent(type, campaignId, shortenId, base)
        Tracker.track(event)
    }

    companion object {
        /**
         * IAFTrackerを生成します。
         *
         * @param variable IAF用設定値
         */
        @JvmStatic
        fun create(variable: Variable, templateType: String): IAFTracker {
            val campaignId = variable.campaignId
            val shortenId = variable.shortenId

            if (campaignId == null) {
                throw Exception("campaignId is null")
            }
            if (shortenId == null) {
                throw Exception("shortenId is null")
            }

            return IAFTracker(campaignId, shortenId, templateType, variable.timestamp, variable.eventHash)
        }
    }
}
