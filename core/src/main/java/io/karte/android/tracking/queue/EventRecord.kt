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
package io.karte.android.tracking.queue

import android.database.Cursor
import io.karte.android.tracking.Event
import io.karte.android.utilities.datastore.Contract
import io.karte.android.utilities.datastore.Persistable

internal class EventRecord() : Persistable() {
    val visitorId: String get() = values[EventContract.VISITOR_ID] as String
    val originalPvId: String get() = values[EventContract.ORIGINAL_PV_ID] as String
    val pvId: String get() = values[EventContract.PV_ID] as String
    val event: Event get() = Event.fromJSON(values[EventContract.EVENT] as String)!!
    var retry: Int
        get() = values[EventContract.RETRY] as Int
        set(value) {
            values[EventContract.RETRY] = value
        }
    var state: State
        get() = State.values()[values[EventContract.STATE] as Int]
        set(value) {
            values[EventContract.STATE] = value.ordinal
        }

    constructor(visitorId: String, originalPvId: String, pvId: String, event: Event) : this() {
        values[EventContract.VISITOR_ID] = visitorId
        values[EventContract.ORIGINAL_PV_ID] = originalPvId
        values[EventContract.PV_ID] = pvId
        values[EventContract.EVENT] = event.toJSON(true).toString()
        values[EventContract.RETRY] = 0
        values[EventContract.STATE] = State.Queued.ordinal
    }

    enum class State { Queued, Requesting, Failed }
    object EventContract : Contract<EventRecord> {
        const val VISITOR_ID = "visitor_id"
        const val ORIGINAL_PV_ID = "original_pv_id"
        const val PV_ID = "pv_id"
        const val EVENT = "event"
        const val RETRY = "retry"
        const val STATE = "state"

        override val namespace = "events"
        override val version: Int = 1
        override val columns: Map<String, Int> = mapOf(
            VISITOR_ID to Cursor.FIELD_TYPE_STRING,
            ORIGINAL_PV_ID to Cursor.FIELD_TYPE_STRING,
            PV_ID to Cursor.FIELD_TYPE_STRING,
            EVENT to Cursor.FIELD_TYPE_STRING,
            RETRY to Cursor.FIELD_TYPE_INTEGER,
            STATE to Cursor.FIELD_TYPE_INTEGER
        )

        override fun create(map: Map<String, Any?>): EventRecord {
            return EventRecord().apply {
                values.putAll(map)
            }
        }
    }

    override val contract = EventContract

    override fun onPersisted(): Map<String, Any?> {
        return mapOf(
            EventContract.VISITOR_ID to visitorId,
            EventContract.ORIGINAL_PV_ID to originalPvId,
            EventContract.PV_ID to pvId,
            EventContract.EVENT to values[EventContract.EVENT] as String,
            EventContract.RETRY to retry,
            EventContract.STATE to values[EventContract.STATE] as Int
        )
    }

    /** パフォーマンス優先で簡易的にlengthで計算する.
     * マルチバイト文字列だと溢れるが許容する. */
    override val size: Int by lazy {
        visitorId.length +
            originalPvId.length +
            pvId.length +
            (values[EventContract.EVENT] as String).length
    }
}
