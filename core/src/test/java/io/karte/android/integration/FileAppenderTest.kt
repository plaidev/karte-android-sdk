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
import io.karte.android.core.config.Config
import io.karte.android.core.logger.Clock
import io.karte.android.core.logger.FileAppender
import io.karte.android.core.logger.LogEvent
import io.karte.android.core.logger.LogLevel
import io.karte.android.test_lib.application
import io.karte.android.test_lib.pipeLog
import io.karte.android.test_lib.proceedBufferedCall
import io.karte.android.test_lib.tearDownKarteApp
import io.karte.android.test_lib.unpipeLog
import io.mockk.every
import io.mockk.mockkObject
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
const val THREAD_NAME = "file_appender_test"

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28])
abstract class BaseFileAppenderTest {
    internal val fileAppender = FileAppender(THREAD_NAME)
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
        proceedBufferedCall(threadName = THREAD_NAME)
    }
}

class FileAppenderTest : BaseFileAppenderTest() {
    lateinit var server: MockWebServer
    private val targetDate = Calendar.getInstance().apply { set(2020, 3, 10) }.time
    private var isMockResponseFailure = false

    @Before
    fun setup() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/nativeAppLogUrl" -> {
                        val response = MockResponse().setBody(JSONObject().toString())
                        if (isMockResponseFailure) response.setResponseCode(500)
                        return response
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
        isMockResponseFailure = false
        cacheFiles.forEach { it.delete() }
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
    fun アップロードを試行したファイルは削除される() {
        isMockResponseFailure = false
        fileAppender.append(LogEvent(LogLevel.DEBUG, "Test", "test event", null))
        fileAppender.flush()
        proceedBufferedCall()
        // 当日のファイルはアップロードされない
        assertThat(logDir.list()).hasLength(1)
        assertThat(server.requestCount).isEqualTo(0)

        testFiles.forEach { File(logDir, it).writeText("test") }
        assertThat(logDir.list()).hasLength(6)
        fileAppender.flush()
        proceedBufferedCall()

        // アップロード試行は当日分以外行われる
        assertThat(server.requestCount).isEqualTo(5)
        // 試行が成功(upload urlの取得が成功)したファイルは削除される
        assertThat(logDir.list()).hasLength(1)
    }

    @Test
    fun アップロード失敗時には一定時間過去のファイルのみ削除される() {
        isMockResponseFailure = true
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
    }
}

class FileAppenderWithoutAppTest : BaseFileAppenderTest() {

    @Test
    fun KarteApp_setup前はファイル書き込みをしない() {
        assertThat(cacheFiles).isEmpty()
        fileAppender.append(LogEvent(LogLevel.DEBUG, "Test", "test event", null))
        fileAppender.flush()
        proceedBufferedCall()
        assertThat(cacheFiles).isEmpty()
    }
}
