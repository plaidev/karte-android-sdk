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
package io.karte.android.tracking

import io.karte.android.KarteApp
import io.karte.android.core.logger.Logger
import io.karte.android.tracking.queue.Dispatcher
import io.karte.android.tracking.queue.EventRecord
import io.karte.android.utilities.isAscii
import io.karte.android.utilities.toValues
import org.json.JSONObject

private const val LOG_TAG = "Karte.Tracker"

internal class TrackingService internal constructor() {

    private val dispatcher = Dispatcher()
    private var delegate: TrackerDelegate? = null

    internal fun track(
        inEvent: Event,
        visitorId: String? = null,
        completion: TrackCompletion? = null
    ) {
        if (KarteApp.isOptOut) return
        if (!inEvent.eventName.value.isAscii())
            Logger.w(
                LOG_TAG,
                "Multi-byte character in event name is deprecated: Event=${inEvent.eventName.value}"
            )

        Logger.d(LOG_TAG, "track")
        val event = delegate?.intercept(inEvent) ?: inEvent
        if (event.eventName == BaseEventName.View) {
            KarteApp.self.pvIdContainer.renew()
        }
        dispatcher.push(
            EventRecord(
                visitorId ?: KarteApp.visitorId,
                KarteApp.self.originalPvId,
                KarteApp.self.pvId,
                event
            ), completion
        )
    }

    internal fun teardown() {
        dispatcher.teardown()
        delegate = null
    }

    internal companion object {
        @JvmStatic
        fun track(event: Event, visitorId: String? = null, completion: TrackCompletion? = null) {
            KarteApp.self.tracker?.track(event, visitorId, completion)
        }

        @JvmStatic
        fun track(name: String, values: Values?, completion: TrackCompletion? = null) {
            track(Event(CustomEventName(name), values), completion = completion)
        }

        @JvmStatic
        fun track(name: String, jsonObject: JSONObject?, completion: TrackCompletion? = null) {
            track(Event(CustomEventName(name), jsonObject), completion = completion)
        }

        @JvmStatic
        fun identify(values: Values, completion: TrackCompletion? = null) {
            track(IdentifyEvent(values), completion = completion)
        }

        @JvmStatic
        fun identify(jsonObject: JSONObject, completion: TrackCompletion? = null) {
            identify(jsonObject.toValues(), completion)
        }

        @JvmStatic
        fun view(
            viewName: String,
            title: String?,
            values: Values? = null,
            completion: TrackCompletion? = null
        ) {
            track(
                ViewEvent(
                    viewName,
                    values?.get("view_id") as? String,
                    title,
                    values
                ), completion = completion
            )
        }

        @JvmStatic
        fun view(
            viewName: String,
            title: String?,
            jsonObject: JSONObject?,
            completion: TrackCompletion? = null
        ) {
            view(viewName, title, jsonObject?.toValues(), completion)
        }

        @JvmStatic
        fun setDelegate(delegate: TrackerDelegate?) {
            KarteApp.self.tracker?.delegate = delegate
        }
    }
}
