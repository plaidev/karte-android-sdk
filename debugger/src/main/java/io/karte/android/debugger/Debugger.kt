package io.karte.android.debugger

import android.content.Intent
import io.karte.android.KarteApp
import io.karte.android.core.library.DeepLinkModule
import io.karte.android.core.library.Library
import io.karte.android.core.library.TrackModule
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.core.logger.Logger
import io.karte.android.core.repository.Repository
import io.karte.android.utilities.http.Client
import io.karte.android.utilities.http.HEADER_APP_KEY
import io.karte.android.utilities.http.JSONRequest
import io.karte.android.utilities.http.METHOD_POST
import java.util.concurrent.Executors

private const val LOG_TAG = "EVENT VIEWER"
private const val REPOSITORY_NAMESPACE = "Debugger_"
private const val REPOSITORY_KEY = "id"

private const val HEADER_ACCOUNT_ID = "X-KARTE-Auto-Track-Account-Id"
private const val HEADER__api_auth_data__ = "__api_auth_data__"

private const val ENDPOINT_POST_TRACE = "/auto-track/app-trace"

class Debugger : Library, TrackModule, DeepLinkModule {

    private val traceSendExecutor = Executors.newCachedThreadPool()
    private lateinit var id: String
    private lateinit var app: KarteApp
    private lateinit var repository: Repository

    override val name: String = Debugger.name
    override val version: String = BuildConfig.LIB_VERSION
    override val isPublic: Boolean = true

    override fun intercept(request: TrackRequest): TrackRequest {

        // OptOutの場合はセキュリティ観点からイベントを送らない
        if (KarteApp.isOptOut) return request

        traceSendExecutor.execute(Runnable {
            try {
                val traceBody = request.json

                val url = app.config.baseUrl + ENDPOINT_POST_TRACE
                val debuggerRequest = JSONRequest(url, METHOD_POST, false)

                debuggerRequest.body = traceBody.toString()
                debuggerRequest.headers[HEADER_APP_KEY] = app.appKey

                if (id == "") return@Runnable
                debuggerRequest.headers[HEADER_ACCOUNT_ID] = id
                debuggerRequest.headers[ HEADER__api_auth_data__] = app.config.apiKey

                Client.execute(debuggerRequest)
            } catch (e: Throwable) {
                Logger.e(LOG_TAG, "Failed to send action info.", e)
            }
        })
        return request
    }

    override fun handle(intent: Intent?) {
        val uri = intent?.data ?: return
        val path = uri.path ?: return
        if (uri.host != "karte.io" || !path.startsWith("/_krt_app_sdk_debugger")) return
        id = path.split("/").last()
        repository.put(REPOSITORY_KEY, id)
    }

    override fun configure(app: KarteApp) {
        self = this
        this.app = app
        repository = app.repository(REPOSITORY_NAMESPACE)
        app.register(this)

        id = repository.get(REPOSITORY_KEY, "")
    }

    override fun unconfigure(app: KarteApp) {
        repository.remove(REPOSITORY_KEY)
        self = null
        app.unregister(this)
    }

    private companion object {
        var self: Debugger? = null
        var name: String = "debugger"
    }
}
