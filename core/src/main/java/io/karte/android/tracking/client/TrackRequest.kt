//
//  Copyright 2020 PLAID, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
package io.karte.android.tracking.client

import io.karte.android.KarteApp
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.utilities.http.HEADER_APP_KEY
import io.karte.android.utilities.http.JSONRequest
import io.karte.android.utilities.http.METHOD_POST
import org.json.JSONArray
import org.json.JSONObject

internal fun requestOf(
    visitorId: String,
    originalPvId: String,
    pvId: String,
    events: List<Event>
): TrackRequest {
    return TrackRequest(
        "${KarteApp.self.config.baseUrl}/track",
        visitorId,
        originalPvId,
        pvId,
        events
    )
}

/**
 * Track API のリクエスト情報を保持するクラスです。
 *
 * **SDK内部で利用するタイプであり、通常のSDK利用でこちらのタイプを利用することはありません。**
 *
 * @property url リクエストURL
 * @property visitorId ビジターID
 * @property originalPvId オリジナルページビューID
 * @property pvId ページビューID
 *
 * @constructor クラスを初期化します。
 */
class TrackRequest(
    url: String,
    private val visitorId: String,
    val originalPvId: String,
    val pvId: String,
    private val events: List<Event>
) : JSONRequest(url, METHOD_POST) {
    /** bodyに書き込む内容を[JSONObject]として返します。 */
    val json: JSONObject
        get() {
            return JSONObject()
                .put(
                    "keys", JSONObject()
                        .put("visitor_id", visitorId)
                        .put("original_pv_id", originalPvId)
                        .put("pv_id", pvId)
                )
                .put("app_info", KarteApp.self.appInfo?.json)
                .put("events", JSONArray(events.map { it.toJSON() }))
        }
    override var body: String?
        get() = runCatching { json.toString() }.getOrNull()
        set(_) {}

    init {
        headers[HEADER_APP_KEY] = KarteApp.self.appKey
    }

    /**
     * リクエストに指定されたイベントが含まれているかチェックする。
     *
     * @param eventName イベント名
     * @return 指定されたイベント名が含まれる場合は `true` を返し、含まれない場合は `false` を返します。
     */
    fun contains(eventName: EventName): Boolean {
        return events.any { it.eventName == eventName || it.eventName.value == eventName.value }
    }
}
