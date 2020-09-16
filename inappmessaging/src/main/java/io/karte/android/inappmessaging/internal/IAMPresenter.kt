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
package io.karte.android.inappmessaging.internal

import io.karte.android.core.logger.Logger
import java.util.LinkedList

private const val LOG_TAG = "Karte.IAMPresenter"
internal typealias OnDestroyListener = () -> Unit

internal interface Window {
    var presenter: IAMPresenter?
    val isShowing: Boolean
    fun destroy(isForceClose: Boolean = true)
}

internal class IAMPresenter(
    private val window: Window,
    private val messageView: MessageModel.MessageView,
    private val onDestroyListener: OnDestroyListener? = null
) : MessageModel.MessageAdapter {
    private val messages = LinkedList<MessageModel>()

    val isVisible: Boolean
        get() = window.isShowing

    init {
        messageView.adapter = this
        window.presenter = this
    }

    override fun dequeue(): MessageModel? {
        synchronized(messages) {
            return messages.pollLast()
        }
    }

    fun addMessage(message: MessageModel) {
        synchronized(messages) {
            messages.offerFirst(message)
            messageView.notifyChanged()
        }
    }

    fun destroy(isForceClose: Boolean = true) {
        Logger.d(LOG_TAG, "destroy")
        window.destroy(isForceClose)
        messageView.adapter = null

        onDestroyListener?.invoke()
    }
}
