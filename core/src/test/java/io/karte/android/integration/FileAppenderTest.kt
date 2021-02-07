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
package io.karte.android.integration

import com.google.common.truth.Truth.assertThat
import io.karte.android.KarteApp
import io.karte.android.application
import io.karte.android.core.config.Config
import io.karte.android.core.logger.Clock
import io.karte.android.core.logger.FileAppender
import io.karte.android.core.logger.LogEvent
import io.karte.android.core.logger.LogLevel
import io.karte.android.core.logger.THREAD_NAME
import io.karte.android.pipeLog
import io.karte.android.proceedBufferedCall
import io.karte.android.tearDownKarteApp
import io.karte.android.unpipeLog
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Calendar
import java.util.regex.Pattern

val testFiles = listOf(
    "2020-04-11_test.log",
    "2020-04-09_test.log",
    "2020-04-08_test.log",
    "2020-04-07_test.log",
    "2020-04-06_test.log"
)

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28])
abstract class BaseFileAppenderTest {
    internal val fileAppender = FileAppender()
    protected val logDir: File
        get() = File(application().cacheDir, "io.karte.android/log")
    protected val cacheFiles: List<File>
        get() = logDir.listFiles()?.filter { it.isFile } ?: listOf()

    @Before
    fun before() {
        pipeLog()
    }

    @After
    fun after() {
        unpipeLog()
    }

    protected fun proceedBufferedCall() {
        Thread.getAllStackTraces().keys.filter { it.name == THREAD_NAME }
            .forEach { proceedBufferedCall(it) }
    }
}

class FileAppenderTest : BaseFileAppenderTest() {
    lateinit var server: MockWebServer
    private val targetDate = Calendar.getInstance().apply { set(2020, 3, 10) }.time

    @Before
    fun setup() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/nativeAppLogUrl" -> {
                        MockResponse().setResponseCode(500).setBody(JSONObject().toString())
                    }
                    else -> MockResponse().setResponseCode(400)
                }
            }
        }
        server.start()
        KarteApp.setup(
            application(),
            Config.build { logCollectionUrl = server.url("/nativeAppLogUrl").toString() })

        mockkObject(Clock)
        every { Clock.now() } returns targetDate
        println("target date: $targetDate")
    }

    @After
    fun tearDown() {
        tearDownKarteApp()
        unmockkObject(Clock)
    }

    @Test
    fun append_flushでファイルが作成されること() {
        assertThat(cacheFiles).isEmpty()
        fileAppender.append(LogEvent(LogLevel.DEBUG, "Test", "test event", null))
        fileAppender.flush()
        proceedBufferedCall()

        val files = cacheFiles
        assertThat(files).isNotEmpty()
        assertThat(files).hasSize(1)
        assertThat(files.first().name).matches(Pattern.compile("""^2020-04-10_.*\.log$"""))
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun 一定時間過去のファイルのみ削除される() {
        fileAppender.append(LogEvent(LogLevel.DEBUG, "Test", "test event", null))
        fileAppender.flush()
        proceedBufferedCall()
        assertThat(logDir.list()).hasLength(1)

        testFiles.forEach { File(logDir, it).writeText("test") }
        assertThat(logDir.list()).hasLength(6)
        fileAppender.flush()
        proceedBufferedCall()

        // アップロード試行は当日分以外行われる
        assertThat(server.requestCount).isEqualTo(5)
        // 一つのファイルのみ削除される
        assertThat(logDir.list()).hasLength(5)
        assertThat(File(logDir, "2020-04-06_test.log").exists()).isFalse()

        testFiles.forEach { File(logDir, it).run { if (exists()) delete() } }
    }
}

class FileAppenderWithoutAppTest : BaseFileAppenderTest() {
    private lateinit var mock: KarteApp

    @Before
    fun setup() {
        mock = spyk(KarteApp.self)
        every { mock.application } throws UninitializedPropertyAccessException()
    }

    @After
    fun tearDown() {
        clearMocks(mock)
    }

    @Test
    fun KarteApp_setup前はファイル書き込みをしない() {
        assertThat(cacheFiles).isEmpty()
        fileAppender.append(LogEvent(LogLevel.DEBUG, "Test", "test event", null))
        fileAppender.flush()
        proceedBufferedCall()
        assertThat(cacheFiles).isEmpty()
    }
}
