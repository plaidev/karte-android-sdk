package io.karte.android.inbox.internal.apis

import io.karte.android.inbox.Inbox
import kotlin.reflect.KProperty

internal class InboxClientFactory {
    operator fun getValue(thisRef: Inbox.Companion, property: KProperty<*>): InboxClient {
        return InboxClientImpl(thisRef.apiKey)
    }
}
