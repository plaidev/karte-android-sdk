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
package io.karte.android.unit

import io.karte.android.test_lib.parseBody
import io.karte.android.utilities.gzip
import io.karte.android.utilities.http.CONTENT_ENCODING_GZIP
import io.karte.android.utilities.http.CONTENT_TYPE_JSON
import io.karte.android.utilities.http.Client
import io.karte.android.utilities.http.HEADER_CONTENT_ENCODING
import io.karte.android.utilities.http.HEADER_CONTENT_TYPE
import io.karte.android.utilities.http.JSONRequest
import io.karte.android.utilities.http.METHOD_POST
import io.mockk.every
import io.mockk.mockkStatic
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HttpClientTest {
    lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @Test
    fun postRequestShouldReturnResponse() {
        server.enqueue(
            MockResponse().setBody("hello, world!").addHeader(
                "Content-Type",
                "text/html; charset=utf-8"
            )
        )
        val url = server.url("/sample").toString()

        val response = Client.execute(JSONRequest(url, METHOD_POST).apply { body = "mybody" })

        Assert.assertEquals(200, response.code)
        Assert.assertEquals("hello, world!", response.body)
        Assert.assertEquals(
            listOf("text/html; charset=utf-8"),
            response.headers["Content-Type"]
        )
        Assert.assertTrue(response.isSuccessful)
        val request = server.takeRequest()
        Assert.assertEquals(CONTENT_TYPE_JSON, request.headers[HEADER_CONTENT_TYPE])
        Assert.assertEquals(CONTENT_ENCODING_GZIP, request.headers[HEADER_CONTENT_ENCODING])
        Assert.assertEquals("mybody", request.parseBody())
    }

    @Test
    fun postRequestWithJsonWhenGzipFailed() {
        server.enqueue(
            MockResponse().setResponseCode(200)
        )
        val url = server.url("/sample").toString()
        mockkStatic("io.karte.android.utilities.GzipUtilKt")
        every { gzip(any()) } returns null

        val response = Client.execute(JSONRequest(url, METHOD_POST).apply { body = "mybody" })

        Assert.assertTrue(response.isSuccessful)
        val request = server.takeRequest()
        Assert.assertEquals(CONTENT_TYPE_JSON, request.headers[HEADER_CONTENT_TYPE])
        Assert.assertNull(request.headers[HEADER_CONTENT_ENCODING])
        Assert.assertEquals("mybody", request.body.readUtf8())
    }

    @Test(expected = IOException::class)
    fun networkErrorAtPostRequestRaiseIoException() {
        val url = server.url("/sample").toString()
        server.shutdown()

        Client.execute(JSONRequest(url, METHOD_POST).apply { body = "mybody" })
    }

    @Test()
    fun httpErrorAtPostRequestReturnResponseContainsError() {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("No file")
                .addHeader("Content-Type", "text/html; charset=utf-8")
        )
        val url = server.url("/sample").toString()

        val response = Client.execute(JSONRequest(url, METHOD_POST).apply { body = "mybody" })
        Assert.assertEquals(400, response.code)
        Assert.assertEquals("No file", response.body)
        Assert.assertEquals(
            listOf("text/html; charset=utf-8"),
            response.headers["Content-Type"]
        )
        Assert.assertFalse(response.isSuccessful)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}
