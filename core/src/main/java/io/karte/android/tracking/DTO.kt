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
package io.karte.android.tracking

import org.json.JSONObject

/** Data Transfer Object from JSON */
interface DTO<T> {
    /** Load from JSONObject to Object. */
    fun load(jsonObject: JSONObject?): T
}

/**
 * アクション情報を保持するデータクラスです。
 * @property[shortenId] shorten_id フィールド
 * @property[type] type フィールド
 * @property[content] content フィールド
 */
data class Action<T : DTO<T>>(
    var shortenId: String? = null,
    var type: String? = null,
    var content: T? = null
) : DTO<Action<T>> {
    override fun load(jsonObject: JSONObject?): Action<T> = apply {
        shortenId = jsonObject?.optString("shorten_id")
        type = jsonObject?.optString("type")
        content = content?.load(jsonObject?.optJSONObject("content"))
    }
}

/**
 * キャンペーン情報を保持するデータクラスです。
 * @property[campaignId] campaign_id フィールド
 * @property[serviceActionType] service_action_type フィールド
 */
data class Campaign(
    var campaignId: String? = null,
    var serviceActionType: String? = null
) :
    DTO<Campaign> {
    override fun load(jsonObject: JSONObject?): Campaign = apply {
        campaignId = jsonObject?.optString("_id")
        serviceActionType = jsonObject?.optString("service_action_type")
    }
}
