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

import io.karte.android.utilities.http.Response
import org.json.JSONObject

/**
 * Track API のレスポンス情報を保持する構造体です。
 *
 * **SDK内部で利用するタイプであり、通常のSDK利用でこちらのタイプを利用することはありません。**
 */
class TrackResponse internal constructor(response: Response) :
    Response(response.code, response.headers, response.body) {
    /**接客サービスの一覧*/
    val messages: MutableList<JSONObject> = mutableListOf()
    val json: JSONObject? = runCatching { JSONObject(body) }.getOrNull()?.optJSONObject("response")

    init {
        json?.optJSONArray("messages")?.let {
            for (i in 0 until it.length()) {
                messages.add(it.getJSONObject(i))
            }
        }
    }
}
