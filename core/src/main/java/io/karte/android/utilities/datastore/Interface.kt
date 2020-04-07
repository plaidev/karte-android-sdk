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
package io.karte.android.utilities.datastore

import java.io.Closeable

internal interface Contract<T : Persistable> {
    val namespace: String
    val version: Int
    val columns: Map<String, Int>
    fun create(map: Map<String, Any?>): T
}

internal abstract class Persistable {
    var id: Long = -1
    val values: MutableMap<String, Any?> = mutableMapOf()
    abstract val contract: Contract<*>
    abstract fun onPersisted(): Map<String, Any?>
}

internal interface Persister {
    fun put(persistable: Persistable): Long
    fun <T : Persistable> read(
        contract: Contract<T>,
        query: List<Triple<String, RelationalOperator, String>>,
        order: String? = null
    ): List<T>

    fun delete(persistable: Persistable)
    fun update(persistable: Persistable): Int
}

internal class Transaction(
    private val persister: Persister,
    private val transactional: Transactional
) :
    Closeable, Persister {
    init {
        transactional.begin()
    }

    fun success() {
        transactional.success()
    }

    override fun close() {
        transactional.end()
    }

    override fun put(persistable: Persistable): Long {
        return persister.put(persistable)
    }

    override fun <T : Persistable> read(
        contract: Contract<T>,
        query: List<Triple<String, RelationalOperator, String>>,
        order: String?
    ): List<T> {
        return persister.read(contract, query, order)
    }

    override fun delete(persistable: Persistable) {
        persister.delete(persistable)
    }

    override fun update(persistable: Persistable): Int {
        return persister.update(persistable)
    }
}

internal interface Transactional {
    fun transaction(): Transaction
    fun begin()
    fun success()
    fun end()
}

internal interface Subscriber {
    fun notified()
}

internal enum class RelationalOperator(val value: String) {
    Equal("=") {
        override fun run(a: Any, b: Any): Boolean {
            return a == b
        }
    },
    Unequal("!=") {
        override fun run(a: Any, b: Any): Boolean {
            return a != b
        }
    };

    abstract fun run(a: Any, b: Any): Boolean
}
