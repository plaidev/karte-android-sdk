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
import io.karte.android.unpipeLog
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

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28])
class FileAppenderTest {
    lateinit var server: MockWebServer
    private val fileAppender = FileAppender()
    val appKey = "sampleappkey"
    private val targetDate = Calendar.getInstance().apply { set(2020, 3, 10) }.time
    private val logDir: File
        get() = File(KarteApp.self.application.cacheDir, "io.karte.android/log")
    private val cacheFiles: List<File>
        get() = logDir.listFiles()?.filter { it.isFile } ?: listOf()

    private fun proceedBufferedCall() {
        Thread.getAllStackTraces().keys.filter { it.name == THREAD_NAME }
            .forEach { proceedBufferedCall(it) }
    }

    @Before
    fun setup() {
        pipeLog()

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
        KarteApp.setup(application(), appKey,
            Config.build { logCollectionUrl = server.url("/nativeAppLogUrl").toString() })

        mockkObject(Clock)
        every { Clock.now() } returns targetDate
        println("target date: $targetDate")
    }

    @After
    fun tearDown() {
        KarteApp.self.teardown()
        unpipeLog()
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
