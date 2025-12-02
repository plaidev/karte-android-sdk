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

import io.karte.android.KarteApp
import io.karte.android.core.library.ActionModule
import io.karte.android.core.library.Library
import io.karte.android.core.library.UserModule
import io.karte.android.core.logger.Logger
import io.karte.android.core.repository.Repository
import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName
import io.karte.android.tracking.MessageEvent
import io.karte.android.tracking.MessageEventType
import io.karte.android.tracking.Tracker
import io.karte.android.tracking.Values
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.tracking.client.TrackResponse
import io.karte.android.utilities.merge
import io.karte.android.utilities.toValues
import io.karte.android.variables.BuildConfig
import io.karte.android.variables.FetchCompletion
import io.karte.android.variables.Variable
import io.karte.android.variables.VariablesPredicate
import org.json.JSONObject

private const val LOG_TAG = "Karte.Variables"
private const val REPOSITORY_NAMESPACE_VARIABLES = "Variables_"

internal class VariablesService : Library, ActionModule, UserModule {

    internal companion object {
        private var self: VariablesService? = null
            get() {
                if (field == null) Logger.w(LOG_TAG, "Variables not initialized!")
                return field
            }

        @JvmStatic
        @JvmOverloads
        fun fetch(completion: FetchCompletion? = null) {
            Tracker.track(FetchVariablesEvent(), completion)
        }

        @JvmStatic
        fun get(key: String): Variable {
            self?.repository?.get<String?>(key, null)?.also {
                return Variable.deserialize(key, it) ?: Variable.empty(key)
            } ?: run {
                Logger.w(LOG_TAG, "Variable is not found. name=$key")
            }
            return Variable.empty(key)
        }

        @JvmStatic
        fun getAllKeys(): List<String> =
            self?.repository?.getAllKeys() ?: run {
                Logger.e(LOG_TAG, "Repository not found.")
                emptyList()
            }

        @JvmStatic
        fun filter(predicate: VariablesPredicate<String>): List<Variable> =
            self?.repository?.getAllKeys()?.filter {
                predicate.test(it)
            }?.map {
                get(it)
            } ?: run {
                Logger.e(LOG_TAG, "Repository not found.")
                emptyList()
            }

        @JvmStatic
        fun clearCacheAll() {
            self?.repository?.removeAll()
        }

        @JvmStatic
        fun clearCacheByKey(key: String) {
            self?.repository?.remove(key)
        }

        @JvmStatic
        @JvmOverloads
        fun trackOpen(variables: List<Variable>, values: Values? = null) {
            self?.track(variables, MessageEventType.Open, values)
        }

        @JvmStatic
        fun trackOpen(variables: List<Variable>, jsonObject: JSONObject? = null) {
            self?.track(variables, MessageEventType.Open, jsonObject?.toValues())
        }

        @JvmStatic
        @JvmOverloads
        fun trackClick(variables: List<Variable>, values: Values? = null) {
            self?.track(variables, MessageEventType.Click, values)
        }

        @JvmStatic
        fun trackClick(variables: List<Variable>, jsonObject: JSONObject? = null) {
            self?.track(variables, MessageEventType.Click, jsonObject?.toValues())
        }
    }

    private lateinit var repository: Repository

    private fun track(variables: List<Variable>, type: MessageEventType, values: Values?) {
        val alreadySentCampaignIds = mutableSetOf<String>()
        variables.forEach { variable ->
            variable.campaignId ?: return@forEach
            variable.shortenId ?: return@forEach
            if (alreadySentCampaignIds.contains(variable.campaignId)) return@forEach
            alreadySentCampaignIds.add(variable.campaignId)
            track(
                type,
                variable.campaignId,
                variable.shortenId,
                values,
                variable.timestamp,
                variable.eventHash
            )
        }
    }

    private fun track(type: MessageEventType, campaignId: String, shortenId: String, values: Values?, timestamp: String?, eventHash: String?) {
        var base = (values ?: mapOf()).merge(mapOf(
            "message" to mapOf(
                "frequency_type" to "access"
            )
        ))

        timestamp?.let {
            base = base.merge(mapOf(
                "message" to mapOf(
                    "response_id" to "${it}_$shortenId",
                    "response_timestamp" to it
                )
            ))
        }

        eventHash?.let {
            base = base.merge(mapOf(
                "message" to mapOf(
                    "trigger" to mapOf(
                        "event_hashes" to eventHash
                    )
                )
            ))
        }

        Tracker.track(MessageEvent(type, campaignId, shortenId, base))
    }
    private fun trackReady(message: VariableMessage) {
        val campaignId = message.campaign.campaignId ?: return
        val shortenId = message.action.shortenId ?: return

        val noAction = message.action.noAction ?: false
        val values = mutableMapOf<String, Any>("no_action" to noAction)
        if (noAction) {
            message.action.reason?.let { values["reason"] = it }
        }

        track(
            MessageEventType.Ready,
            campaignId,
            shortenId,
            values,
            message.action.responseTimestamp,
            message.trigger.eventHashes
        )
    }

    //region Libraary
    override val name: String = "variables"
    override val version: String = BuildConfig.LIB_VERSION
    override val isPublic: Boolean = true

    override fun configure(app: KarteApp) {
        self = this
        repository = app.repository(REPOSITORY_NAMESPACE_VARIABLES)
        app.register(this)
    }

    override fun unconfigure(app: KarteApp) {
        self = null
        app.unregister(this)
    }
    //endregion

    //region ActionModule
    override fun receive(trackResponse: TrackResponse, trackRequest: TrackRequest) {
        if (trackRequest.contains(VariablesEventName.FetchVariables)) {
            repository.removeAll()
        }
        val messages = trackResponse.messages
            .mapNotNull { parse(it) }
            .filter { it.isEnabled }
            // 元々reversedが使われていたが、compileSDK=36で、java.lang.NoSuchMethodErrorが出るようになった。
            // kotlinのバグではないかという指摘があるが、解決していない。
            // https://issuetracker.google.com/issues/350432371#comment23
            .asReversed()
        messages.forEach messages@{ message ->
            val shortenId = message.action.shortenId?.let { it } ?: return@messages
            val campaignId = message.campaign.campaignId?.let { it } ?: return@messages

            if (!message.isControlGroup) {
                message.action.content?.inlinedVariables?.forEach variables@{ inlinedVariable ->
                    val name = inlinedVariable.name ?: return@variables
                    val value = inlinedVariable.value ?: return@variables
                    val timestamp = message.action.responseTimestamp
                    val eventHash = message.trigger.eventHashes
                    Logger.d(
                        LOG_TAG,
                        "Write variable: $name. campaignId=$campaignId, shortenId=$shortenId"
                    )
                    repository.put(name, Variable(name, campaignId, shortenId, value, timestamp, eventHash).serialize())
                }
            }

            trackReady(message)
        }
    }

    override fun reset() {
    }

    override fun resetAll() {
    }
    //endregion

    //region UserModule
    override fun renewVisitorId(current: String, previous: String?) {
        repository.removeAll()
    }
    //endregion
}

private class FetchVariablesEvent :
    Event(VariablesEventName.FetchVariables, values = null, isRetryable = false)

private enum class VariablesEventName(override val value: String) : EventName {
    FetchVariables("_fetch_variables"),
}
