//
//  Copyright 2021 PLAID, Inc.
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

package io.karte.android.unit

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.karte.android.tracking.CustomEventName
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventValidator
import io.karte.android.tracking.IdentifyEvent
import io.karte.android.tracking.MessageEventName
import io.karte.android.tracking.ViewEvent
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventValidatorTest {

    @Test
    fun validEvents() {
        val events = listOf(
            Event(CustomEventName("custom_event"), jsonObject = null),
            IdentifyEvent("user_id"),
            ViewEvent("viewName", "title"),
            Event(CustomEventName("identify"), values = mapOf("foo" to "bar"))
        )
        events.forEach {
            assertThat(EventValidator.getDeprecatedMessages(it)).isEmpty()
            assertThat(EventValidator.getInvalidMessages(it)).isEmpty()
        }
    }

    @Test
    fun deprecatedEvents() {
        // event name must not Multi-byte character
        with(Event(CustomEventName("テストイベント"), jsonObject = null)) {
            val messages = EventValidator.getDeprecatedMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(2)
            assertThat(messages[0]).startsWith("Multi-byte character")
            assertThat(messages[1]).startsWith("[^a-z0-9_]")

            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }

        // event name must not use unsupported letter
        with(Event(CustomEventName("aaa.bbb"), jsonObject = null)) {
            val messages = EventValidator.getDeprecatedMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("[^a-z0-9_]")

            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }
        with(Event(CustomEventName("_my_custom_event"), jsonObject = null)) {
            val messages = EventValidator.getDeprecatedMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("[^a-z0-9_]")

            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }

        // values fields name must not invalid start letter or reserved word.
        with(IdentifyEvent("user_id", mapOf("address.prefectures" to "Tokyo"))) {
            val messages = EventValidator.getDeprecatedMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("Contains dots")

            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }
        with(IdentifyEvent("user_id", mapOf("\$js" to "alert(\"test\");"))) {
            val messages = EventValidator.getDeprecatedMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("Contains dots")

            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }
        with(IdentifyEvent("user_id", mapOf("_source" to "super_native_sdk"))) {
            val messages = EventValidator.getDeprecatedMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("Contains dots")

            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }
    }

    @Test
    fun isDeprecatedEventName() {
        data class Case(val description: String, val eventName: String, val expected: Boolean)

        // 許可: 半角小文字英字 (a-z)・半角数字 (0-9)・下線 (_)
        // 非推奨: 上記以外の文字、または _ 始まり（内部イベント allowlist を除く）
        val cases = listOf(
            Case("[許可] a-z, 0-9, _ のみ", "custom_event", false),
            Case("[許可] 数字を含む", "event123", false),
            Case("[対象外] 空文字", "", false),
            Case("[非推奨] [a-z0-9_] 以外の文字(.)", "aaa.bbb", true),
            Case("[非推奨] 大文字を含む", "CustomEvent", true),
            Case("[非推奨] _ で始まる（allowlist 外）", "_my_custom_event", true),
            Case("[許可] allowlist: _message_ready", MessageEventName.MessageReady.value, false),
            Case("[許可] allowlist: _message_suppressed", MessageEventName.MessageSuppressed.value, false),
            Case("[許可] allowlist: _fetch_variables", "_fetch_variables", false)
        )
        cases.forEach { case ->
            assertWithMessage(case.description)
                .that(EventValidator.isDeprecatedEventName(case.eventName))
                .isEqualTo(case.expected)
        }
    }

    @Test
    fun isDeprecatedEventFieldName() {
        data class Case(val description: String, val values: JSONObject, val expected: Boolean)

        // 非推奨: キーにドット(.)・$ 始まり・予約語（INVALID_FIELD_NAMES）
        val cases = listOf(
            Case("[対象外] values が空", JSONObject(), false),
            Case("[許可] 通常のフィールド名", JSONObject(mapOf("name" to "value")), false),
            Case("[許可] 数字を含むフィールド名", JSONObject(mapOf("field1" to "value")), false),
            Case("[非推奨] キーにドット(.)", JSONObject(mapOf("address.city" to "Tokyo")), true),
            Case("[非推奨] キーが $ 始まり", JSONObject(mapOf("\$js" to "alert(\"test\");")), true),
            Case("[非推奨] 予約語 _source", JSONObject(mapOf("_source" to "super_native_sdk")), true)
        )
        cases.forEach { case ->
            assertWithMessage(case.description)
                .that(EventValidator.isDeprecatedEventFieldName(case.values))
                .isEqualTo(case.expected)
        }
    }

    @Test
    fun invalidEvents() {
        // Required fields are invalid
        with(ViewEvent("")) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()

            val messages = EventValidator.getInvalidMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("view_name or user_id is empty")
        }
        with(Event(CustomEventName("view"), mapOf("viewName" to ""))) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()

            val messages = EventValidator.getInvalidMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("view_name or user_id is empty")
        }
        with(Event(CustomEventName("view"), values = null)) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()
            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }
        with(IdentifyEvent("")) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()

            val messages = EventValidator.getInvalidMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("view_name or user_id is empty")
        }
        with(Event(CustomEventName("identify"), mapOf("user_id" to ""))) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()

            val messages = EventValidator.getInvalidMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("view_name or user_id is empty")
        }
        with(Event(CustomEventName("identify"), values = null)) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()
            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }
    }
}
