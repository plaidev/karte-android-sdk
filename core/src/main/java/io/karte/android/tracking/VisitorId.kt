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
import io.karte.android.core.library.UserModule
import io.karte.android.core.logger.Logger
import io.karte.android.core.repository.Repository
import io.karte.android.utilities.IdContainer

private const val LOG_TAG = "Karte.VisitorId"
private const val KEY_VISITOR_ID = "visitor_id"

internal class VisitorId(private val repository: Repository) : IdContainer {

    override val value: String
        get() {
            return repository.get<String?>(KEY_VISITOR_ID, null) ?: renew()
        }

    init {
        Logger.i(LOG_TAG, "Visitor id: $value")
    }

    override fun renew(): String {
        val oldVisitorId = repository.get<String?>(KEY_VISITOR_ID, null)
        val newVisitorId = super.renew()
        repository.put(KEY_VISITOR_ID, newVisitorId)
        if (oldVisitorId == null) return newVisitorId

        Tracker.track(RenewVisitorIdEvent(newVisitorId = newVisitorId), oldVisitorId)
        Tracker.track(RenewVisitorIdEvent(oldVisitorId = oldVisitorId))

        KarteApp.self.modules.forEach {
            if (it is UserModule) it.renewVisitorId(
                newVisitorId,
                oldVisitorId
            )
        }
        return newVisitorId
    }
}
