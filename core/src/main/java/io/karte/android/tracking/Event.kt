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

import io.karte.android.utilities.format
import io.karte.android.utilities.merge
import io.karte.android.utilities.toMap
import io.karte.android.utilities.toValues
import org.json.JSONObject
import java.util.regex.Pattern

/** イベントに追加できるカスタムオブジェクトの型を示すエイリアスです。 */
typealias Values = Map<String, Any>

private fun valuesOf(
    values: Values? = null,
    applyBlock: (MutableMap<String, Any>.() -> Unit)? = null
): Values {
    val map = values?.toMutableMap() ?: mutableMapOf()
    if (applyBlock != null) map.apply(applyBlock)
    return map
}

/** JSON文字列をValuesオブジェクトに変換します。 */
fun valuesOf(string: String?): Values {
    if (string == null) return mapOf()
    return runCatching { JSONObject(string) }.getOrDefault(JSONObject()).toValues()
}

/**
 * イベントを表現するクラスです。
 *
 * @property[eventName] イベント名
 *
 * イベント名には半角小文字英字 ( a-z )、半角数字 ( 0-9 ) と下線 ( _ ) のみ使用できます。
 * なお下線 ( _ ) で始まるイベント名はシステム上のイベントと干渉するため使用できません。
 */
open class Event {
    /**
     * イベントに紐付けるカスタムオブジェクト
     *
     * ### フィールド名の制限
     * * ドット ( `.` ) を含む名称は使用できません。
     * * `$` で始まる名前は使用できません。
     * * フィールド名として `_source` `any` `avg` `cache` `count` `count_sets` `date` `f_t` `first` `keys` `l_t` `last` `lrus` `max` `min` `o` `prev` `sets` `size` `span` `sum` `type` `v` を指定することはできません。
     *
     * ### 値の制限
     * 送信可能なデータ型は、下記の通りです。
     * * [String]
     * * [Int]
     * * [UInt]
     * * [Double] [Float]
     * * [Boolean]
     * * [java.util.Date]
     * * [Array]
     * * [Map]
     */
    var values: JSONObject
    private val date = System.currentTimeMillis() / 1000L
    internal var isRetry = false
    val eventName: EventName
    internal val isRetryable: Boolean
    internal val isDeprecatedEventName: Boolean
    internal val isDeprecatedEventFieldName: Boolean
    internal val isInvalidEventFieldValue: Boolean
    val libraryName: String?

    /** [JSONObject] による初期化 */
    constructor(
        eventName: EventName,
        jsonObject: JSONObject? = null,
        isRetryable: Boolean? = null,
        libraryName: String? = null
    ) {
        this.eventName = eventName
        this.values = jsonObject?.format() ?: JSONObject()
        this.isRetryable = isRetryable ?: true
        this.isDeprecatedEventName = validateEventName(eventName.value)
        this.isDeprecatedEventFieldName = validateEventFieldName(values)
        this.isInvalidEventFieldValue = validateEventFieldValue(eventName.value, values)
        this.libraryName = libraryName
    }

    /** [JSONObject] による初期化 */
    constructor(eventName: EventName, jsonObject: JSONObject? = null, isRetryable: Boolean? = null) : this(eventName, jsonObject, isRetryable, null)

    /** [Values] による初期化 */
    constructor(eventName: EventName, values: Values? = null, isRetryable: Boolean? = null, libraryName: String? = null) : this(
        eventName,
        values?.let { JSONObject(values.format()) },
        isRetryable,
        libraryName
    )

    /** [Values] による初期化 */
    constructor(eventName: EventName, values: Values? = null, isRetryable: Boolean? = null) : this(eventName, values, isRetryable, null)

    internal fun toJSON(forSerialize: Boolean = false): JSONObject {
        return JSONObject()
            .put("event_name", eventName.value)
            .put("values", values
                .put("_local_event_date", date)
                .apply { if (isRetry) put("_retry", true) }
            ).apply {
                if (forSerialize) {
                    put("_is_retryable", isRetryable)
                    put("library_name", libraryName)
                }
            }
    }

    companion object {
        internal fun fromJSON(json: String): Event? {
            return runCatching {
                val jsonObject = JSONObject(json)
                val isRetryable = runCatching { jsonObject.getBoolean("_is_retryable") }.getOrNull()
                val libraryName = runCatching { jsonObject.getString("library_name") }.getOrNull()
                Event(
                    CustomEventName(jsonObject.getString("event_name")),
                    jsonObject.getJSONObject("values"),
                    isRetryable,
                    libraryName
                )
            }.getOrNull()
        }
    }

    private val EVENT_NAME_REGEX = Pattern.compile("[^a-z0-9_]")

    /** 非推奨なイベント名が含まれるかを返します。 */
    private fun validateEventName(eventName: String): Boolean {
        if (eventName.isEmpty()) {
            return false
        }
        when (eventName) {
            MessageEventName.MessageReady.value, MessageEventName.MessageSuppressed.value, "_fetch_variables" -> {
                return false
            }
        }
        val m = EVENT_NAME_REGEX.matcher(eventName)
        return m.find() || eventName.startsWith("_")
    }

    internal val INVALID_FIELD_NAMES = listOf("_source", "_system", "any", "avg", "cache", "count", "count_sets", "date", "f_t", "first", "keys", "l_t", "last", "lrus", "max", "min", "o", "prev", "sets", "size", "span", "sum", "type", "v")

    /** 非推奨なフィールド名が含まれるかを返します。 */
    private fun validateEventFieldName(values: JSONObject): Boolean {
        if (values.length() == 0) {
            return false
        }
        val result = values.toMap().any {
            it.key.startsWith("$") || it.key.contains(".") || INVALID_FIELD_NAMES.contains(it.key)
        }
        return result
    }

    /** 無効な値が含まれるかを返します。 */
    private fun validateEventFieldValue(eventName: String, values: JSONObject): Boolean {
        if (values.length() == 0) {
            return false
        }

        when (eventName) {
            BaseEventName.View.value -> {
                val viewName = values.optString("view_name")
                if (viewName.isEmpty()) {
                    return true
                }
            }
            BaseEventName.Identify.value -> {
                val userId = values.optString("user_id")
                if (userId.isEmpty()) {
                    return true
                }
            }
        }
        return false
    }
}

/** `identify` イベント */
internal class IdentifyEvent(
    userId: String,
    values: Values? = null
) : Event(
    BaseEventName.Identify, valuesOf(values) {
        this["user_id"] = userId
    }
)

/** `view` イベント */
internal class ViewEvent(
    viewName: String,
    viewId: String? = null,
    title: String? = null,
    values: Values? = null
) : Event(
    BaseEventName.View, valuesOf(values) {
        this["view_name"] = viewName
        this.getOrPut("title", { title ?: viewName })
        if (!this.contains("view_id") && viewId != null) this["view_id"] = viewId
    })

/** `attribute` イベント */
internal class AttributeEvent(values: Values? = null) : Event(BaseEventName.Attribute, values)

/** `native_app_renew_visitor_id` イベント */
internal class RenewVisitorIdEvent(newVisitorId: String? = null, oldVisitorId: String? = null) :
    Event(
        AutoEventName.NativeAppRenewVisitorId, valuesOf {
            newVisitorId?.let { this["new_visitor_id"] = it }
            oldVisitorId?.let { this["old_visitor_id"] = it }
        }
    )

/**
 *  `message_xxx` イベント
 *  @property[campaignId] イベント対象のcampaign_id
 *  @property[shortenId] イベント対象のshorten_id
 */
class MessageEvent(
    type: MessageEventType,
    val campaignId: String,
    val shortenId: String,
    values: Values? = null,
    libraryName: String? = null
) : Event(CustomEventName(type.eventNameStr), valuesOf(values) {
    val merged = merge(
        mapOf(
            "message" to mapOf(
                "campaign_id" to campaignId,
                "shorten_id" to shortenId
            )
        )
    )
    putAll(merged)
}, libraryName = libraryName) {
    constructor(type: MessageEventType, campaignId: String, shortenId: String, values: Values?) : this(type, campaignId, shortenId, values, null)
}

/**各イベント名を示すインターフェースです。*/
interface EventName {
    /** イベント名の文字列 */
    val value: String
}

internal enum class BaseEventName(override val value: String) : EventName {
    View("view"),
    Identify("identify"),
    Attribute("attribute")
}

internal enum class AutoEventName(override val value: String) : EventName {
    NativeAppInstall("native_app_install"),
    NativeAppUpdate("native_app_update"),
    NativeAppOpen("native_app_open"),
    NativeAppForeground("native_app_foreground"),
    NativeAppBackground("native_app_background"),
    NativeAppRenewVisitorId("native_app_renew_visitor_id"),
}

/**message_xxx イベント名を定義した列挙型です。*/
enum class MessageEventName(override val value: String) : EventName {
    /** _message_ready イベント*/
    MessageReady("_message_ready"),

    /** message_open イベント*/
    MessageOpen("message_open"),

    /** message_close イベント*/
    MessageClose("message_close"),

    /** message_click イベント*/
    MessageClick("message_click"),

    /** _message_suppressed イベント */
    MessageSuppressed("_message_suppressed"),
}

/**カスタムイベント名を保持するクラスです。*/
class CustomEventName(override val value: String) : EventName

/**
 * message_xxx イベントのタイプを定義した列挙型です。
 */
enum class MessageEventType(internal val eventNameStr: String) {
    /** _message_ready イベント*/
    Ready(MessageEventName.MessageReady.value),

    /** message_open イベント*/
    Open(MessageEventName.MessageOpen.value),

    /** message_close イベント*/
    Close(MessageEventName.MessageClose.value),

    /** message_click イベント*/
    Click(MessageEventName.MessageClick.value),

    /** _message_suppressed イベント */
    Suppressed(MessageEventName.MessageSuppressed.value);

    /** 対応するイベント名 */
    @Deprecated("This property is no longer used. It's always 'MessageOpen' as dummy.")
    val eventName = MessageEventName.MessageOpen
}
