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
package io.karte.android.visualtracking

import android.app.Activity
import android.os.Bundle
import io.karte.android.KarteApp
import io.karte.android.core.config.ExperimentalConfig
import io.karte.android.core.config.OperationMode
import io.karte.android.core.library.ActionModule
import io.karte.android.core.library.Library
import io.karte.android.core.library.TrackModule
import io.karte.android.core.logger.Logger
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.tracking.client.TrackResponse
import io.karte.android.utilities.ActivityLifecycleCallback
import io.karte.android.utilities.http.JSONRequest
import io.karte.android.visualtracking.internal.LifecycleHook
import io.karte.android.visualtracking.internal.PairingManager
import io.karte.android.visualtracking.internal.tracing.Trace
import io.karte.android.visualtracking.internal.tracing.TraceBuilder
import io.karte.android.visualtracking.internal.tracking.DefinitionList
import io.karte.android.visualtracking.internal.tracking.GetDefinitions
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private const val LOG_TAG = "Karte.VT"

private const val HEADER_IF_MODIFIED_SINCE = "X-KARTE-Auto-Track-If-Modified-Since"
private const val HEADER_OS = "X-KARTE-Auto-Track-OS"
private const val OS_ANDROID = "android"

class VisualTracking : Library, ActionModule, TrackModule {
    //region Library
    override val name: String = "visualtracking"
    override val version: String = BuildConfig.LIB_VERSION
    override val isPublic: Boolean = true
    private var currentActiveActivity: WeakReference<Activity>? = null

    override fun configure(app: KarteApp) {
        this.app = app
        self = this
        app.appInfo?.let { traceBuilder = TraceBuilder(it.json) }
        pairingManager = PairingManager(app)
        app.application.registerActivityLifecycleCallbacks(lifecycleHook)
        app.register(this)

        app.application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallback() {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                (app.config as? ExperimentalConfig)?.let {
                    if (it.operationMode != OperationMode.INGEST) return@let
                    getDefinitions()
                }
                currentActiveActivity = WeakReference(activity)
                app.application.unregisterActivityLifecycleCallbacks(this)
            }

            override fun onActivityResumed(activity: Activity) {
                currentActiveActivity = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                currentActiveActivity = null
            }
        })
    }

    override fun unconfigure(app: KarteApp) {
        self = null
        app.application.unregisterActivityLifecycleCallbacks(lifecycleHook)
        app.unregister(this)
    }
    //endregion

    //region ActionModule
    override fun receive(trackResponse: TrackResponse, trackRequest: TrackRequest) {
        val definitionList =
            trackResponse.json?.optJSONObject("auto_track_definition") ?: return
        updateDefinitionList(definitionList)
    }

    override fun reset() {
    }

    override fun resetAll() {
    }
    //endregion

    //region TrackModule
    override fun intercept(request: TrackRequest): TrackRequest {
        setHeader(request)
        return request
    }
    //endregion

    private val lifecycleHook = LifecycleHook(this)
    private val executor = Executors.newCachedThreadPool()
    private lateinit var app: KarteApp
    private lateinit var traceBuilder: TraceBuilder
    internal lateinit var pairingManager: PairingManager
    private var definitions: DefinitionList? = null
    private var delegate: VisualTrackingDelegate? = null

    @Throws(JSONException::class)
    internal fun handleLifecycle(name: String, activity: Activity) {
        Logger.d(LOG_TAG, "Start handling lifecycle action. action=$name")
        handleTrace(traceBuilder.buildTrace(name, activity))
    }

    @Throws(JSONException::class)
    internal fun handleAction(name: String, args: Array<Any>) {
        Logger.d(LOG_TAG, "Start handling action. action=$name")
        handleTrace(traceBuilder.buildTrace(name, args))
    }

    @Throws(JSONException::class)
    internal fun handleAction(action: Action) {
        Logger.d(LOG_TAG, "Start handling action. action=$action")
        handleTrace(traceBuilder.buildTrace(action))
    }

    private fun handleTrace(trace: Trace) {
        pairingManager.sendTraceIfInPairing(trace)

        if (definitions == null) return

        val traceValues = trace.values
        executor.execute(Runnable {
            Logger.d(LOG_TAG, "Handling trace: $traceValues")
            val events: List<JSONObject>
            try {
                synchronized(DefinitionList::class.java) {
                    events = definitions!!.traceToEvents(traceValues, currentActiveActivity?.get()?.window)
                }
            } catch (e: Exception) {
                Logger.w(LOG_TAG, "Failed to check VT event.", e)
                return@Runnable
            }

            for (event in events) {
                try {
                    Tracker.track(event.getString("event_name"), event.getJSONObject("values"))
                } catch (e: Exception) {
                    Logger.e(LOG_TAG, "Failed to send VT event.", e)
                }
            }
        })
    }

    /** DefinitionList取得APIを1秒の遅延実行. リトライはしない. */
    internal fun getDefinitions(
        executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    ) {
        executor.schedule({
            Logger.d(LOG_TAG, "getDefinitions")
            GetDefinitions.get(app, { request -> setHeader(request) }) { result ->
                if (result == null) return@get
                updateDefinitionList(result)
            }
        }, 1, TimeUnit.SECONDS)
    }

    private fun setHeader(request: JSONRequest) {
        definitions?.let { request.headers[HEADER_IF_MODIFIED_SINCE] = it.lastModified.toString() }
        request.headers[HEADER_OS] = OS_ANDROID
    }

    private fun updateDefinitionList(definitionJson: JSONObject) {
        try {
            val list = DefinitionList.buildIfNeeded(definitionJson) ?: return
            synchronized(DefinitionList::class.java) {
                this.definitions = list
            }
            Logger.i(LOG_TAG, "Updated Visual Tracking settings: $definitions")
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to parse definitions.", e)
        }
    }

    companion object {
        internal var self: VisualTracking? = null

        /**
         * ビジュアルトラッキングで発生するイベント等を委譲するためのデリゲートインスタンスを取得・設定します。
         */
        @JvmStatic
        var delegate: VisualTrackingDelegate?
            get() = self?.delegate
            set(value) {
                self?.delegate = value
            }

        /**
         * ペアリング状態を取得します。
         *
         * 端末がペアリングされていればtrue、それ以外はfalseを返します。
         */
        @JvmStatic
        val isPaired: Boolean
            get() = self?.pairingManager?.isPaired ?: false

        /**
         * 操作ログをハンドルします。
         *
         * 操作ログはペアリング時のみ送信されます。
         * イベント発火条件定義に操作ログがマッチした際にビジュアルイベントが送信されます。
         *
         * @param action アクションを表現する型
         */
        @JvmStatic
        fun handle(action: Action) {
            try {
                Logger.d(LOG_TAG, "handle action=$action")
                val instance = self
                if (instance == null) {
                    Logger.e(LOG_TAG, "Tried to handle action but VisualTracking is not enabled.")
                    return
                }
                instance.handleAction(action)
            } catch (e: Exception) {
                Logger.e(LOG_TAG, "Failed to handle action.", e)
            }
        }
    }
}
