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
import io.karte.android.KarteApp
import io.karte.android.core.library.ActionModule
import io.karte.android.core.library.Library
import io.karte.android.core.library.TrackModule
import io.karte.android.core.logger.Logger
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.tracking.client.TrackResponse
import io.karte.android.utilities.getLowerClassName
import io.karte.android.visualtracking.internal.DefinitionList
import io.karte.android.visualtracking.internal.LifecycleHook
import io.karte.android.visualtracking.internal.PairingManager
import io.karte.android.visualtracking.internal.Trace
import io.karte.android.visualtracking.internal.TraceBuilder
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executors

private const val LOG_TAG = "Karte.VT"

private const val HEADER_IF_MODIFIED_SINCE = "X-KARTE-Auto-Track-If-Modified-Since"
private const val HEADER_OS = "X-KARTE-Auto-Track-OS"

internal class VisualTracking : Library, ActionModule, TrackModule {
    //region Library
    override val name: String = getLowerClassName()
    override val version: String = BuildConfig.VERSION_NAME
    override val isPublic: Boolean = true

    override fun configure(app: KarteApp) {
        this.app = app
        self = this
        app.appInfo?.let { traceBuilder = TraceBuilder(it.json) }
        pairingManager = PairingManager(app)
        app.application.registerActivityLifecycleCallbacks(lifecycleHook)
        app.register(this)
    }

    override fun unconfigure(app: KarteApp) {
        self = null
        app.application.unregisterActivityLifecycleCallbacks(lifecycleHook)
        app.unregister(this)
    }
    //endregion

    //region ActionModule
    override fun receive(trackResponse: TrackResponse, trackRequest: TrackRequest) {
        try {
            val list = DefinitionList.buildIfNeeded(trackResponse.json!!) ?: return
            synchronized(DefinitionList::class.java) {
                this.definitions = list
            }
            Logger.i(LOG_TAG, "Updated Visual Tracking settings: $definitions")
        } catch (e: Exception) {
            Logger.e(LOG_TAG, "Failed to parse definitions.", e)
        }
    }

    override fun reset() {
    }

    override fun resetAll() {
    }
    //endregion

    //region TrackModule
    override fun intercept(request: TrackRequest): TrackRequest {
        definitions?.let { request.headers[HEADER_IF_MODIFIED_SINCE] = it.lastModified.toString() }
        request.headers[HEADER_OS] = "android"
        return request
    }
    //endregion

    private val lifecycleHook = LifecycleHook(this)
    private val executor = Executors.newCachedThreadPool()
    private lateinit var app: KarteApp
    private lateinit var traceBuilder: TraceBuilder
    internal lateinit var pairingManager: PairingManager
    private var definitions: DefinitionList? = null

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

    private fun handleTrace(trace: Trace) {
        pairingManager.sendTraceIfInPairing(trace)

        if (definitions == null) return

        val traceValues = trace.values
        executor.execute(Runnable {
            Logger.d(LOG_TAG, "Handling trace: $traceValues")
            val events: List<JSONObject>
            try {
                synchronized(DefinitionList::class.java) {
                    events = definitions!!.traceToEvents(traceValues)
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

    companion object {
        internal var self: VisualTracking? = null
    }
}
