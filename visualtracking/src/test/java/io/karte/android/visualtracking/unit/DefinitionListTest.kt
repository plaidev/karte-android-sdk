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
package io.karte.android.visualtracking.unit

import com.google.common.truth.Truth.assertThat
import io.karte.android.RobolectricTestCase
import io.karte.android.eventNameTransform
import io.karte.android.visualtracking.buildDefinitionList
import io.karte.android.visualtracking.condition
import io.karte.android.visualtracking.definition
import io.karte.android.visualtracking.trigger
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.json.JSONObject
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

private val sampleTrace1 = JSONObject()
    .put("action", "action1_a")
    .put("target_text", "text1_a")
    .put("app_info", JSONObject().put("model", "model1"))
private val sampleTrace2 = JSONObject()
    .put("action", "action2_a")
    .put("target_text", "text2_a")
    .put("app_info", JSONObject().put("model", "model2"))

@RunWith(Enclosed::class)
class DefinitionListTest {

    class 比較条件 : RobolectricTestCase() {

        @Test
        fun eq() {
            val definitionList = buildDefinitionList(
                definition("event1", trigger(condition("action", "\$eq", "action1_a")))
            )

            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1")

            val trace2Result = definitionList?.traceToEvents(sampleTrace2)
            assertThat(trace2Result).isEmpty()
        }

        @Test
        fun ne() {
            val definitionList = buildDefinitionList(
                definition("event1", trigger(condition("action", "\$ne", "action2_a")))
            )

            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1")

            val trace2Result = definitionList?.traceToEvents(sampleTrace2)
            assertThat(trace2Result).isEmpty()
        }

        @Test
        fun startsWith() {
            val definitionList = buildDefinitionList(
                definition(
                    "event1",
                    trigger(condition("action", "\$startsWith", "action1"))
                )
            )

            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1")

            val trace2Result = definitionList?.traceToEvents(sampleTrace2)
            assertThat(trace2Result).isEmpty()
        }

        @Test
        fun contains() {
            val definitionList = buildDefinitionList(
                definition(
                    "event1",
                    trigger(condition("action", "\$contains", "1_"))
                )
            )

            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1")

            val trace2Result = definitionList?.traceToEvents(sampleTrace2)
            assertThat(trace2Result).isEmpty()
        }

        @Test
        fun endsWith() {
            val definitionList = buildDefinitionList(
                definition(
                    "event1",
                    trigger(condition("action", "\$endsWith", "1_a"))
                )
            )

            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1")

            val trace2Result = definitionList?.traceToEvents(sampleTrace2)
            assertThat(trace2Result).isEmpty()
        }

        @Test
        fun and() {
            val definitionList = buildDefinitionList(
                definition(
                    "event1",
                    trigger(
                        condition("action", "\$startsWith", "action"),
                        condition("target_text", "\$ne", "text2_a")
                    )
                )
            )

            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1")

            val trace2Result = definitionList?.traceToEvents(sampleTrace2)
            assertThat(trace2Result).isEmpty()
        }

        @Test
        fun 階層() {
            val definitionList = buildDefinitionList(
                definition(
                    "event1",
                    trigger(
                        condition("action", "\$startsWith", "action"),
                        condition("app_info.model", "\$eq", "model1")
                    )
                )
            )

            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1")

            val trace2Result = definitionList?.traceToEvents(sampleTrace2)
            assertThat(trace2Result).isEmpty()
        }
    }

    class 静的フィールド : RobolectricTestCase() {
        @Test
        fun 設定したフィールドが付加される() {

            val definitionList = buildDefinitionList(
                definition(
                    "event1",
                    trigger(
                        condition("target_text", "\$contains", "text"),
                        fields = JSONObject().put("field1", "value1").put("field2", "value2")
                    )
                )
            )
            val values =
                definitionList?.traceToEvents(sampleTrace1)?.getOrNull(0)?.getJSONObject("values")
            assertThatJson(values).node("field1").isString.isEqualTo("value1")
            assertThatJson(values).node("field2").isString.isEqualTo("value2")
        }
    }

    class 複数条件の合致 : RobolectricTestCase() {
        @Test
        fun 複数のdefinitionと一致したら全て返す() {
            val definitionList = buildDefinitionList(
                definition(
                    "event1",
                    trigger(
                        condition("target_text", "\$contains", "text")
                    )
                ),
                definition(
                    "event2",
                    trigger(
                        condition("action", "\$contains", "action")
                    )
                )
            )
            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1", "event2")
        }

        @Test
        fun 一つのdefinitionの複数トリガーと一致したら1つだけ返す() {
            val definitionList = buildDefinitionList(
                definition(
                    "event1",
                    trigger(
                        condition("target_text", "\$contains", "text"),
                        fields = JSONObject().put("attr", 1)
                    ),
                    trigger(
                        condition("action", "\$contains", "action"),
                        fields = JSONObject().put("attr", 2)
                    )
                )
            )
            val trace1Result = definitionList?.traceToEvents(sampleTrace1)
            assertThat(trace1Result).comparingElementsUsing(eventNameTransform)
                .containsExactly("event1")
            assertThatJson(trace1Result?.getOrNull(0)).node("values").node("attr").isEqualTo(1)
        }
    }
}
