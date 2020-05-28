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
package io.karte.android.notifications

import io.karte.android.tracking.DTO
import org.json.JSONObject

private const val KEY_TITLE = "title"
private const val KEY_BODY = "body"
private const val KEY_SOUND = "sound"
private const val KEY_CHANNEL_ID = "android_channel_id"
private const val KEY_URL = "url"
private const val KEY_FILE_URL = "attachment_url"
private const val KEY_FILE_TYPE = "attachment_type"

/**
 * KARTEが自動で処理するプッシュ通知のデータを取り扱うクラスです。
 *
 * @property[title] プッシュ通知のタイトルを返します。これはプッシュ通知アクションに指定した静的変数の`title`に対応しています。
 * @property[body] プッシュ通知の本文を返します。これはプッシュ通知アクションに指定した静的変数の`body`に対応しています。
 * @property[sound]プッシュ通知の通知音可否を返します。これはプッシュ通知アクションに指定した静的変数の`sound_for_android`に対応しています。
 * @property[channel]プッシュ通知のチャンネルIDを返します。これはプッシュ通知アクションに指定した静的変数の`android_channel_id`に対応しています。
 * @property[link] プッシュ通知のクリックリンクを返します。これはプッシュ通知アクションに指定した静的変数の`url_android`に対応しています。
 * @property[type] プッシュ通知の添付タイプを返します。これはプッシュ通知アクションに指定した静的変数の`attachment_type`に対応しています。
 * @property[fileUrl] プッシュ通知の画像を返します。これはプッシュ通知アクションに指定した静的変数の`attachment_url`に対応しています。
 */
data class KarteAttributes(
    @JvmField var title: String = "",
    @JvmField var body: String = "",
    @JvmField var sound: Boolean = false,
    @JvmField var channel: String = "",
    @JvmField var link: String = "",
    internal var type: String = "",
    @JvmField var fileUrl: String = ""
) : DTO<KarteAttributes> {
    override fun load(jsonObject: JSONObject?): KarteAttributes = apply {
        title = jsonObject?.optString(KEY_TITLE) ?: ""
        body = jsonObject?.optString(KEY_BODY) ?: ""
        type = jsonObject?.optString(KEY_FILE_TYPE) ?: ""
        fileUrl = jsonObject?.optString(KEY_FILE_URL) ?: ""
        link = jsonObject?.optString(KEY_URL) ?: ""
        channel = jsonObject?.optString(KEY_CHANNEL_ID) ?: ""
        sound = jsonObject?.optBoolean(KEY_SOUND, false) ?: false
    }

    /** @suppress */
    override fun toString(): String {
        return "KarteAttributes{" +
            "title='" + title + '\''.toString() +
            ", body='" + body + '\''.toString() +
            ", sound=" + sound +
            ", channel='" + channel + '\''.toString() +
            ", link='" + link + '\''.toString() +
            ", type='" + type + '\''.toString() +
            ", fileUrl=" + fileUrl +
            '}'.toString()
    }
}
