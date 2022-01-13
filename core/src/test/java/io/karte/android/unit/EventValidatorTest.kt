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
import io.karte.android.tracking.CustomEventName
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventValidator
import io.karte.android.tracking.IdentifyEvent
import io.karte.android.tracking.ViewEvent
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
            ViewEvent("viewName", "title")
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
    fun invalidEvents() {
        // Required fields are invalid
        with(ViewEvent("")) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()

            val messages = EventValidator.getInvalidMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("Failed to push Event")
        }
        with(Event(CustomEventName("view"), mapOf("viewName" to ""))) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()

            val messages = EventValidator.getInvalidMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("Failed to push Event")
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
            assertThat(messages[0]).startsWith("Failed to push Event")
        }
        with(Event(CustomEventName("identify"), mapOf("user_id" to ""))) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()

            val messages = EventValidator.getInvalidMessages(this)
            assertThat(messages).isNotEmpty()
            assertThat(messages.size).isEqualTo(1)
            assertThat(messages[0]).startsWith("Failed to push Event")
        }
        with(Event(CustomEventName("identify"), values = null)) {
            assertThat(EventValidator.getDeprecatedMessages(this)).isEmpty()
            assertThat(EventValidator.getInvalidMessages(this)).isEmpty()
        }
    }
}
