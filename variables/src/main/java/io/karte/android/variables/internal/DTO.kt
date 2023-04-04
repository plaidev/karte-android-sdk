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
package io.karte.android.variables.internal

import io.karte.android.tracking.Action
import io.karte.android.tracking.Campaign
import io.karte.android.tracking.DTO
import io.karte.android.tracking.Trigger
import org.json.JSONObject

internal fun parse(jsonObject: JSONObject): VariableMessage? {
    return VariableMessage().load(jsonObject)
}

internal data class InlinedVariable(var name: String? = null, var value: String? = null) :
    DTO<InlinedVariable> {
    override fun load(jsonObject: JSONObject?): InlinedVariable = apply {
        name = jsonObject?.optString("name")
        value = jsonObject?.optString("value")
    }
}

internal data class Content(var inlinedVariables: List<InlinedVariable> = emptyList()) :
    DTO<Content> {
    override fun load(jsonObject: JSONObject?): Content = apply {
        val array = jsonObject?.optJSONArray("inlined_variables")
        array?.let {
            val list = mutableListOf<InlinedVariable>()
            repeat(array.length()) {
                list.add(InlinedVariable().load(array.optJSONObject(it)))
            }
            inlinedVariables = list
        }
    }
}

internal data class VariableMessage(
    val action: Action<Content> = Action(content = Content()),
    var campaign: Campaign = Campaign(),
    var trigger: Trigger = Trigger()
) : DTO<VariableMessage> {
    val isEnabled: Boolean
        get() = campaign.campaignId != null && action.shortenId != null &&
            campaign.serviceActionType == "remote_config"
    val isControlGroup: Boolean
        get() = action.type == "control"

    override fun load(jsonObject: JSONObject?): VariableMessage = apply {
        action.load(jsonObject?.optJSONObject("action"))
        campaign.load(jsonObject?.optJSONObject("campaign"))
        trigger.load(jsonObject?.optJSONObject("trigger"))
    }
}
