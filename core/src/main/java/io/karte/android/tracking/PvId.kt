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
import io.karte.android.core.library.ActionModule
import io.karte.android.core.logger.Logger
import io.karte.android.utilities.IdContainer
import java.util.UUID

private const val LOG_TAG = "Karte.PvId"

internal fun generateOriginalPvId(): String {
    return UUID.randomUUID().toString()
}

internal class PvId(initVal: String? = null) : IdContainer {

    private var _value = initVal ?: renew()
    override val value: String
        get() = _value

    init {
        Logger.i(LOG_TAG, "pv id: $value")
    }

    override fun renew(): String {
        KarteApp.self.modules.filterIsInstance<ActionModule>().forEach { it.reset() }
        _value = super.renew()
        return value
    }

    fun set(value: String) {
        _value = value
    }
}
