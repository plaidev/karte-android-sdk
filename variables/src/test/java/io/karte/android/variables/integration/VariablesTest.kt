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
package io.karte.android.variables.integration

import android.os.Looper
import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.RobolectricTestCase
import io.karte.android.assertThat
import io.karte.android.createControlGroupMessage
import io.karte.android.createMessage
import io.karte.android.createMessagesResponse
import io.karte.android.parseBody
import io.karte.android.proceedBufferedCall
import io.karte.android.setupKarteApp
import io.karte.android.tearDownKarteApp
import io.karte.android.variables.Variable
import io.karte.android.variables.Variables
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

data class Var(val name: String, val value: String)

private fun createRemoteConfigMessage(
    campaignId: String = "sample_campaign",
    shortenId: String = "sample_shorten",
    variables: Array<Var> = emptyArray()
): JSONObject {
    val inlinedVariables = JSONArray()
    for (variable in variables) {
        inlinedVariables.put(
            JSONObject().put("name", variable.name).put("value", variable.value)
        )
    }
    return createMessage(
        campaignId,
        shortenId,
        JSONObject().put("inlined_variables", inlinedVariables),
        "remote_config"
    )
}

private fun createRemoteConfigCGMessage(
    campaignId: String = "sample_campaign",
    shortenId: String = "sample_shorten"
): JSONObject {
    return createControlGroupMessage(campaignId, shortenId, "remote_config")
}

abstract class VariablesTestCase : RobolectricTestCase() {
    private val appKey = "sampleappkey"

    lateinit var server: MockWebServer

    @Before
    open fun init() {
        server = MockWebServer()
        server.start()

        setupKarteApp(server, appKey)
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        server.shutdown()
    }

    //_fetch_variablesに対する接客のレスポンスと、その後必ず来るmessage_openトラッキングへの空200レスポンスをenqueueする
    fun enqMsgRespAndMsgOpenResp(messages: JSONArray) {
        server.enqueue(MockResponse().setBody(createMessagesResponse(messages).toString()))
        server.enqueue(MockResponse().setBody(createMessagesResponse(JSONArray()).toString()))
    }

    fun enqueueFailedResponse() {
        server.enqueue(
            MockResponse().setBody(
                JSONObject().put("response", JSONObject()).toString()
            ).setResponseCode(400).addHeader("Content-Type", "text/html; charset=utf-8")
        )
    }
}

@Suppress("TestFunctionName", "ClassName")
@RunWith(Enclosed::class)
class VariablesTest {

    class _fetch_variablesイベントの発行 : VariablesTestCase() {

        @Test
        fun _fetch_variablesイベントが発行されること() {
            enqMsgRespAndMsgOpenResp(JSONArray().put(createRemoteConfigMessage()))
            Variables.fetch()
            proceedBufferedCall()

            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            val eventName = events.getJSONObject(0).getString("event_name")
            assertThat(eventName).isEqualTo("_fetch_variables")
        }

        @Test
        fun _fetch_variablesイベントはリトライされないこと() {
            enqueueFailedResponse()
            Variables.fetch()
            proceedBufferedCall()

            assertThat(server.requestCount).isEqualTo(1)
            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            val eventName = events.getJSONObject(0).getString("event_name")
            assertThat(eventName).isEqualTo("_fetch_variables")
        }
    }

    @RunWith(Enclosed::class)
    class 変数の参照 {
        class getString : VariablesTestCase() {
            @Test
            fun 文字列の変数が参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "fuga")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").string("undef")).isEqualTo("fuga")
            }

            @Test
            fun 整数文字列の変数が参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "1")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").string("undef")).isEqualTo("1")
            }

            @Test
            fun json文字列の変数が参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var(
                                    "hoge",
                                    "{\"hoge\":\"fuga\"}"
                                )
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").string("undef")).isEqualTo("{\"hoge\":\"fuga\"}")
            }

            @Test
            fun 存在しない変数を参照した場合_デフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var(
                                    "hoge",
                                    "{\"hoge\":\"fuga\"}"
                                )
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hogeh").string("default")).isEqualTo("default")
            }
        }

        class getLong : VariablesTestCase() {

            @Test
            fun 整数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(3)
            }

            @Test
            fun 整数の最大値を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var(
                                    "hoge",
                                    "9223372036854775807"
                                )
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(9223372036854775807)
            }

            @Test
            fun 小数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "3.3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(3)
            }

            @Test
            fun 負の符号付き整数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "-3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(-3)
            }

            @Test
            fun 負の符号付き小数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "-3.3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(-3)
            }

            @Test
            fun 正の符号付き整数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "+3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(+3)
            }

            @Test
            fun 正の符号付き小数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "+3.3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(+3)
            }

            @Test
            fun 数字フォーマットではない変数を参照するとデフォルト値が返却される1() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hogea", "5hoge")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hogea").long(0)).isEqualTo(0)
            }

            @Test
            fun 数字フォーマットではない変数を参照するとデフォルト値が返却される2() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "fugafuga")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(0)
            }

            @Test
            fun 数字フォーマットではない変数を参照するとデフォルト値が返却される3() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").long(0)).isEqualTo(0)
            }

            @Test
            fun 存在しない変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("undefvar").long(4)).isEqualTo(4)
            }
        }

        class getDouble : VariablesTestCase() {

            @Test
            fun 整数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").double(.0)).isEqualTo(3.0)
            }

            @Test
            fun 小数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "3.3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").double(.0)).isEqualTo(3.3)
            }

            @Test
            fun 負の符号付き整数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "-3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").double(.0)).isEqualTo(-3.0)
            }

            @Test
            fun 負の符号付き小数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "-3.3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").double(.0)).isEqualTo(-3.3)
            }

            @Test
            fun 正の符号付き整数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "+3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").double(.0)).isEqualTo(+3.0)
            }

            @Test
            fun 正の符号付き小数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "+3.3")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").double(.0)).isEqualTo(+3.3)
            }

            @Test
            fun 数字フォーマットではない変数を参照するとデフォルト値が返却される1() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "3hoge")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").double(.0)).isEqualTo(.0)
            }

            @Test
            fun 数字フォーマットではない変数を参照するとデフォルト値が返却される2() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "fugafuga")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").double(.0)).isEqualTo(.0)
            }

            @Test
            fun 数字フォーマットではない変数を参照するとデフォルト値が返却される3() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hogea", "")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hogea").double(.0)).isEqualTo(.0)
            }

            @Test
            fun 存在しない変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("undefvar").double(4.0)).isEqualTo(4.0)
            }
        }

        class getBoolean : VariablesTestCase() {

            @Test
            fun 文字列trueを参照するとtrueが返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "true")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").boolean(false)).isTrue()
            }

            @Test
            fun 文字列falseを参照するとfalseが返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "false")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").boolean(true)).isFalse()
            }

            @Test
            fun 適当な文字列を参照するとfalseが返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "fuga")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").boolean(true)).isFalse()
            }

            @Test
            fun 存在しない変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "true")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("undefvar").boolean(false)).isFalse()
                assertThat(Variables.get("undefvar").boolean(true)).isTrue()
            }
        }

        class getJSONArray : VariablesTestCase() {

            @Test
            fun 配列の変数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "[1,2]")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").jsonArray(JSONArray()))
                    .isEqualTo(JSONArray().put(1).put(2))
            }

            @Test
            fun オブジェクトの変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var(
                                    "hoge",
                                    "{\"hoge\":\"fuga\"}"
                                )
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").jsonArray(JSONArray().put(1)))
                    .isEqualTo(JSONArray().put(1))
            }

            @Test
            fun 適当な文字列の変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "foo")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").jsonArray(JSONArray().put(1)))
                    .isEqualTo(JSONArray().put(1))
            }

            @Test
            fun 存在しない変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "[1,2]")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(
                    Variables.get("undefvar").jsonArray(
                        JSONArray().put(1).put(
                            3
                        )
                    )
                ).isEqualTo(JSONArray().put(1).put(3))
            }
        }

        class getJSONObject : VariablesTestCase() {

            @Test
            fun オブジェクトの変数を参照できる() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var(
                                    "hoge",
                                    "{\"hoge\":\"fuga\"}"
                                )
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(Variables.get("hoge").jsonObject(JSONObject()))
                    .isEqualTo(JSONObject().put("hoge", "fuga"))
            }

            @Test
            fun 配列の変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "[1,2]")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(
                    Variables.get("hoge").jsonObject(
                        JSONObject().put(
                            "hoge",
                            "fuga"
                        )
                    )
                ).isEqualTo(JSONObject().put("hoge", "fuga"))
            }

            @Test
            fun 適当な文字列の変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "foo")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(
                    Variables.get("hoge").jsonObject(
                        JSONObject().put(
                            "hoge",
                            "fuga"
                        )
                    )
                ).isEqualTo(JSONObject().put("hoge", "fuga"))
            }

            @Test
            fun 存在しない変数を参照するとデフォルト値が返却される() {
                enqMsgRespAndMsgOpenResp(
                    JSONArray().put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("hoge", "[1,2]")
                            )
                        )
                    )
                )
                Variables.fetch()
                proceedBufferedCall()
                assertThat(
                    Variables.get("undefvar").jsonObject(
                        JSONObject().put(
                            "hoge",
                            "fuga"
                        )
                    )
                ).isEqualTo(JSONObject().put("hoge", "fuga"))
            }
        }
    }

    class 複数接客の配信 : VariablesTestCase() {

        @Test
        fun 変数が全て保存されること() {
            enqMsgRespAndMsgOpenResp(
                JSONArray()
                    .put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("strvar", "fuga")
                            )
                        )
                    )
                    .put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("longvar", "111")
                            )
                        )
                    )
            )

            Variables.fetch()
            proceedBufferedCall()
            assertThat(Variables.get("strvar").string("def")).isEqualTo("fuga")
            assertThat(Variables.get("longvar").long(1)).isEqualTo(111)
        }

        @Test
        fun 同名の変数がある場合は優先度の高い変数が適用されること() {

            enqMsgRespAndMsgOpenResp(
                JSONArray().put(
                    createRemoteConfigMessage(
                        variables = arrayOf(
                            Var("strvar", "fuga"),
                            Var("doublevar", "333.333")
                        )
                    )
                ).put(
                    createRemoteConfigMessage(
                        variables = arrayOf(
                            Var("strvar", "fugafuga"),
                            Var("doublevar", "222.222")
                        )
                    )
                )
            )

            Variables.fetch()
            proceedBufferedCall()
            assertThat(Variables.get("strvar").string("def")).isEqualTo("fuga")
            assertThat(Variables.get("doublevar").double(111.111)).isEqualTo(333.333)
        }

        @Test
        fun 未実施の接客がある場合も変数が全て保存されること() {

            enqMsgRespAndMsgOpenResp(
                JSONArray()
                    .put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("strvar", "fuga"),
                                Var("doublevar", "333.333")
                            )
                        )
                    )
                    .put(
                        createRemoteConfigCGMessage(
                            "cgCampaignId",
                            "cgShortenId"
                        )
                    )
                    .put(
                        createRemoteConfigMessage(
                            variables = arrayOf(
                                Var("strvar", "fugafuga"),
                                Var("doublevar", "222.222")
                            )
                        )
                    )
            )

            Variables.fetch()
            proceedBufferedCall()
            assertThat(Variables.get("strvar").string("def")).isEqualTo("fuga")
            assertThat(Variables.get("doublevar").double(111.111)).isEqualTo(333.333)
        }
    }

    class キャッシュのクリア処理 : VariablesTestCase() {
        @Before
        override fun init() {
            super.init()
            enqMsgRespAndMsgOpenResp(
                JSONArray().put(
                    createRemoteConfigMessage(
                        variables = arrayOf(
                            Var("strvar", "fuga"),
                            Var("longvar", "111")
                        )
                    )
                )
            )
            Variables.fetch()
            proceedBufferedCall()
        }

        @Test
        fun 接客が一つもない場合_キャッシュがクリアされること() {
            enqMsgRespAndMsgOpenResp(JSONArray())

            Variables.fetch()
            proceedBufferedCall()

            assertThat(Variables.get("strvar").string("def")).isEqualTo("def")
            assertThat(Variables.get("longvar").long(1)).isEqualTo(1)
        }

        @Test
        fun 接客がある場合_キャッシュがクリアされること() {
            enqMsgRespAndMsgOpenResp(JSONArray().put(createRemoteConfigMessage()))

            Variables.fetch()
            proceedBufferedCall()

            assertThat(Variables.get("strvar").string("def")).isEqualTo("def")
            assertThat(Variables.get("longvar").long(222)).isEqualTo(222)
        }

        @Test
        fun 未実施の接客がある場合_キャッシュがクリアされること() {
            enqMsgRespAndMsgOpenResp(
                JSONArray().put(createRemoteConfigMessage()).put(
                    createRemoteConfigCGMessage(
                        "cgCampaignId",
                        "cgShortenId"
                    )
                )
            )

            Variables.fetch()
            proceedBufferedCall()

            assertThat(Variables.get("strvar").string("def")).isEqualTo("def")
            assertThat(Variables.get("longvar").long(222)).isEqualTo(222)
        }

        @Test
        fun リクエストに失敗した場合_キャッシュがクリアされないこと() {
            enqueueFailedResponse()

            Variables.fetch()
            proceedBufferedCall()

            assertThat(Variables.get("strvar").string("def")).isEqualTo("fuga")
            assertThat(Variables.get("longvar").long(1)).isEqualTo(111)
        }
    }

    class 接客配信イベントのトラッキング : VariablesTestCase() {
        private fun <E> mapJsonArrayToArrayList(
            array: JSONArray,
            mapper: (JSONObject) -> E
        ): ArrayList<E> {
            val ret: ArrayList<E> = ArrayList()
            for (i in 0 until array.length()) ret.add(mapper(array.optJSONObject(i)))
            return ret
        }

        @Test
        fun message_readyが接客の個数分トラッキングされること() {
            enqMsgRespAndMsgOpenResp(
                JSONArray().put(createRemoteConfigMessage()).put(
                    createRemoteConfigCGMessage()
                )
            )

            Variables.fetch()

            proceedBufferedCall()
            server.takeRequest()

            proceedBufferedCall()
            val messageOpenRequest = server.takeRequest()
            val events = JSONObject(messageOpenRequest.parseBody()).getJSONArray("events")
            val eventNames = mapJsonArrayToArrayList(events) { evt -> evt.getString("event_name") }

            assertThat(eventNames).isEqualTo(arrayListOf("_message_ready", "_message_ready"))
        }

        @Test
        fun message_readyのmessageのcampaign_idが正しいこと() {
            enqMsgRespAndMsgOpenResp(
                JSONArray().put(
                    createRemoteConfigMessage(
                        campaignId = "campaign1"
                    )
                ).put(
                    createRemoteConfigCGMessage(campaignId = "campaign2")
                )
            )

            Variables.fetch()

            proceedBufferedCall()
            server.takeRequest()

            proceedBufferedCall()
            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            val campaignIds = mapJsonArrayToArrayList(events) { evt ->
                evt.getJSONObject("values").getJSONObject("message").getString("campaign_id")
            }

            assertThat(campaignIds).isEqualTo(arrayListOf("campaign2", "campaign1"))
        }

        @Test
        fun message_readyのmessageのshorten_idが正しいこと() {
            enqMsgRespAndMsgOpenResp(
                JSONArray().put(
                    createRemoteConfigMessage(
                        shortenId = "shorten1"
                    )
                ).put(
                    createRemoteConfigCGMessage(shortenId = "shorten2")
                )
            )

            Variables.fetch()

            proceedBufferedCall()
            server.takeRequest()

            proceedBufferedCall()
            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            val shortenIds = mapJsonArrayToArrayList(events) { evt ->
                evt.getJSONObject("values").getJSONObject("message").getString("shorten_id")
            }

            assertThat(shortenIds).isEqualTo(arrayListOf("shorten2", "shorten1"))
        }
    }

    class completionHandler : VariablesTestCase() {

        @Test
        fun completionHandlerがUIスレッドで実行されること() {
            enqMsgRespAndMsgOpenResp(JSONArray().put(createRemoteConfigMessage()))

            var looper: Looper? = null
            Variables.fetch {
                looper = Looper.myLooper()
            }
            proceedBufferedCall()
            assertThat(looper).isEqualTo(Looper.getMainLooper())
        }

        @Test
        fun 取得した変数がcompletionHandler内で利用できること() {
            enqMsgRespAndMsgOpenResp(
                JSONArray().put(
                    createRemoteConfigMessage(
                        variables = arrayOf(
                            Var("hoge", "fuga")
                        )
                    )
                )
            )

            var value: String? = null
            Variables.fetch {
                value = Variables.get("hoge").string("default")
            }
            proceedBufferedCall()
            assertThat(value).isEqualTo("fuga")
        }

        @Test
        fun リクエストに成功した場合_completionHandlerに渡されるisSuccessfulの値がtrueであること() {
            enqMsgRespAndMsgOpenResp(JSONArray().put(createRemoteConfigMessage()))

            var isSuccessful: Boolean? = null
            Variables.fetch {
                isSuccessful = it
            }
            proceedBufferedCall()
            assertThat(isSuccessful).isTrue()
        }

        @Test
        fun リクエストに失敗した場合_completionHandlerに渡されるisSuccessfulの値がfalseであること() {
            enqueueFailedResponse()

            var isSuccessful: Boolean? = null
            Variables.fetch {
                isSuccessful = it
            }
            proceedBufferedCall()
            assertThat(isSuccessful).isFalse()
        }
    }

    class イベントトラッキング : VariablesTestCase() {
        private val campaignId1 = "campaignId1"
        private val campaignId2 = "campaignId2"
        private val shortenId1 = "shortenId1"
        private val shortenId2 = "shortenId2"

        private val campaign1Var1 = Var("var1", "hoge")
        private val campaign1Var2 = Var("var2", "hoge")
        private val campaign2Var1 = Var("var3", "hoge")
        private val campaign2Var2 = Var("var4", "hoge")

        @Before
        override fun init() {
            super.init()
            enqMsgRespAndMsgOpenResp(
                JSONArray().put(
                    createRemoteConfigMessage(
                        campaignId = campaignId1,
                        shortenId = shortenId1,
                        variables = arrayOf(campaign1Var1, campaign1Var2)
                    )
                )
                    .put(
                        createRemoteConfigMessage(
                            campaignId = campaignId2,
                            shortenId = shortenId2,
                            variables = arrayOf(campaign2Var1, campaign2Var2)
                        )
                    )
            )
            Variables.fetch()
            //dequeue _fetch_variables event request
            proceedBufferedCall()
            server.takeRequest()

            //dequeue message_ready event request
            proceedBufferedCall()
            server.takeRequest()

            //enqueue response to handle message_click trackings
            enqMsgRespAndMsgOpenResp(JSONArray())
        }

        @Test
        fun 接客ごとにユニークなトラッキングができること_Click() {
            Variables.trackClick(getVariables())
            proceedBufferedCall()

            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            assertThat(events.length()).isEqualTo(2)
            assertRequest(events, "message_click")
        }

        @Test
        fun 設定したvaluesが送信されること_Click_Map() {
            Variables.trackClick(
                getVariables(),
                HashMap<String, Any?>().apply { put("hoge", "fuga") })
            proceedBufferedCall()

            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            assertThat(events.length()).isEqualTo(2)
            assertValues(events, "hoge", "fuga")
        }

        @Test
        fun 設定したvaluesが送信されること_Click_Json() {
            Variables.trackClick(getVariables(), JSONObject().put("hoge", "fuga"))
            proceedBufferedCall()

            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            assertThat(events.length()).isEqualTo(2)
            assertValues(events, "hoge", "fuga")
        }

        @Test
        fun 接客ごとにユニークなトラッキングができること_Open() {
            Variables.trackOpen(getVariables())
            proceedBufferedCall()

            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            assertThat(events.length()).isEqualTo(2)
            assertRequest(events, "message_open")
        }

        @Test
        fun 設定したvaluesが送信されること_Open_Map() {
            Variables.trackOpen(
                getVariables(),
                HashMap<String, Any?>().apply { put("hoge", "fuga") })
            proceedBufferedCall()

            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            assertThat(events.length()).isEqualTo(2)
            assertValues(events, "hoge", "fuga")
        }

        @Test
        fun 設定したvaluesが送信されること_Open_Json() {
            Variables.trackOpen(getVariables(), JSONObject().put("hoge", "fuga"))
            proceedBufferedCall()

            val events = JSONObject(server.takeRequest().parseBody()).getJSONArray("events")
            assertThat(events.length()).isEqualTo(2)
            assertValues(events, "hoge", "fuga")
        }

        private fun getVariables(): List<Variable> {
            return listOf(
                Variables.get(campaign1Var1.name),
                Variables.get(campaign1Var2.name),
                Variables.get(campaign2Var1.name),
                Variables.get(campaign2Var2.name)
            )
        }

        private fun assertRequest(events: JSONArray, expectedEventName: String) {
            assertThat(events.getJSONObject(0).getString("event_name")).isEqualTo(expectedEventName)
            assertThat(
                events.getJSONObject(0).getJSONObject("values").getJSONObject("message").getString(
                    "campaign_id"
                )
            ).isEqualTo(campaignId1)
            assertThat(
                events.getJSONObject(0).getJSONObject("values").getJSONObject("message").getString(
                    "shorten_id"
                )
            ).isEqualTo(shortenId1)
            assertThat(
                events.getJSONObject(1).getJSONObject("values").getJSONObject("message").getString(
                    "campaign_id"
                )
            ).isEqualTo(campaignId2)
            assertThat(
                events.getJSONObject(1).getJSONObject("values").getJSONObject("message").getString(
                    "shorten_id"
                )
            ).isEqualTo(shortenId2)
        }

        private fun assertValues(events: JSONArray, key: String, value: String) {
            assertThat(events.getJSONObject(0).getJSONObject("values").getString(key)).isEqualTo(
                value
            )
            assertThat(events.getJSONObject(1).getJSONObject("values").getString(key)).isEqualTo(
                value
            )
        }
    }

    class logout : VariablesTestCase() {
        @Test
        fun logutが呼ばれた場合_設定値がリセットされる() {
            enqMsgRespAndMsgOpenResp(
                JSONArray().put(
                    createRemoteConfigMessage(
                        variables = arrayOf(
                            Var("hoge", "3")
                        )
                    )
                )
            )
            Variables.fetch()
            proceedBufferedCall()
            assertThat(Variables.get("hoge").long(0)).isEqualTo(3)

            // renewVisitorIdイベント用
            server.enqueue(MockResponse().setBody(createMessagesResponse(JSONArray()).toString()))
            server.enqueue(MockResponse().setBody(createMessagesResponse(JSONArray()).toString()))
            KarteApp.renewVisitorId()
            proceedBufferedCall()

            assertThat(Variables.get("hoge").long(0)).isEqualTo(0)
        }
    }
}
