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
package io.karte.android.variables

import io.karte.android.core.logger.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val LOG_TAG = "Karte.Variable"
private const val JSON_KEY_VALUE = "value"
private const val JSON_KEY_CAMPAIGN_ID = "campaign_id"
private const val JSON_KEY_SHORTEN_ID = "shorten_id"

/**
 * 設定値とそれに付随する情報を保持するためのクラスです。
 *
 * 設定値の他に、接客サービスIDやアクションIDを保持しています。
 *
 * @property[campaignId] キャンペーンIDを返します。
 *
 * 設定値が未定義の場合は`null`を返します。
 * @property[shortenId] アクションIDを返します。
 *
 * 設定値が未定義の場合は`null`を返します。
 * @property[name] 設定値名を返します。
 * @property[value] 設定値を返します。
 *
 * 設定値が未定義の場合は`null`を返します。
 */
data class Variable internal constructor(
    val name: String,
    val campaignId: String? = null,
    val shortenId: String? = null,
    val value: Any? = null
) {

    /**
     * 設定値が定義済みであるかどうか返します。
     *
     * 定義済みの場合は `true` を、未定義の場合は `false` を返します。
     */
    val isDefined: Boolean = value != null

    /**
     * 設定値（文字列）を返します。
     *
     * 設定値が未定義の場合は `null` を返します。
     */
    private var string: String? = value as? String

    /**
     * 設定値（配列）を返します。
     *
     * 以下の場合において `null` を返します。
     * - 設定値が未定義の場合
     * - 設定値（JSON文字列）のパースができない場合
     */
    private val jsonArray: JSONArray?
        get() = try {
            JSONArray(string(""))
        } catch (e: JSONException) {
            Logger.e(LOG_TAG, "Failed to parse JSON: $e")
            null
        }
    /**
     * 設定値（辞書）を返します。
     *
     * 以下の場合において `null を返します。
     * - 設定値が未定義の場合
     * - 設定値（JSON文字列）のパースができない場合
     */
    private val jsonObject: JSONObject?
        get() = try {
            JSONObject(string(""))
        } catch (e: JSONException) {
            Logger.e(LOG_TAG, "Failed to parse JSON: $e")
            null
        }

    /**
     * 設定値（文字列）を返します。
     *
     * なお設定値が未定義の場合は、デフォルト値を返します。
     * @param[default] デフォルト値
     * @return 設定値（文字列）
     */
    @JvmName("getString")
    fun string(default: String): String = string ?: default

    /**
     * 設定値（整数）を返します。
     *
     * なお設定値が数値でない場合は、デフォルト値を返します。
     * @param[default]  デフォルト値
     * @return 設定値（整数）
     */
    @JvmName("getLong")
    fun long(default: Long): Long = string?.toDoubleOrNull()?.toLong() ?: default

    /**
     * 設定値（浮動小数点数）を返します。
     *
     * なお設定値が数値でない場合は、デフォルト値を返します。
     * @param[default] デフォルト値
     * @return 設定値（浮動小数点数）
     */
    @JvmName("getDouble")
    fun double(default: Double): Double = string?.toDoubleOrNull() ?: default

    /**
     * 設定値（ブール値）を返します。
     *
     * なおブール値への変換ルールについては [こちら](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/to-boolean.html) を参照してください。
     *
     * 設定値が未定義の場合は、デフォルト値を返します。
     * @param[default] デフォルト値
     * @return 設定値（ブール値）
     */
    @JvmName("getBoolean")
    fun boolean(default: Boolean): Boolean = string?.toBoolean() ?: default

    /**
     * 設定値（[JSONArray]）を返します。
     *
     * 以下の場合においてデフォルト値を返します。
     * - 設定値が未定義の場合
     * - 設定値（JSON文字列）のパースができない場合
     *
     * @param[default] デフォルト値
     * @return 設定値（[JSONArray]）
     */
    @JvmName("getJSONArray")
    fun jsonArray(default: JSONArray): JSONArray = jsonArray ?: default

    /**
     * 設定値（[JSONObject]）を返します。
     *
     * 以下の場合においてデフォルト値を返します。
     * - 設定値が未定義の場合
     * - 設定値（JSON文字列）のパースができない場合
     *
     * @param[default] デフォルト値
     * @return 設定値（[JSONObject]）
     */
    @JvmName("getJSONObject")
    fun jsonObject(default: JSONObject): JSONObject = jsonObject ?: default

    internal fun serialize(): String? {
        return try {
            JSONObject()
                .put(JSON_KEY_CAMPAIGN_ID, campaignId)
                .put(JSON_KEY_SHORTEN_ID, shortenId)
                .put(JSON_KEY_VALUE, value)
                .toString()
        } catch (e: JSONException) {
            null
        }
    }

    companion object {

        internal fun deserialize(key: String, values: String): Variable? {
            return try {
                val json = JSONObject(values)
                Variable(
                    key,
                    json.getString(JSON_KEY_CAMPAIGN_ID),
                    json.getString(JSON_KEY_SHORTEN_ID),
                    json.getString(JSON_KEY_VALUE)
                )
            } catch (e: JSONException) {
                Logger.e(LOG_TAG, "Failed to load saved variable:", e)
                null
            }
        }

        internal fun empty(key: String): Variable {
            return Variable(key)
        }
    }
}
