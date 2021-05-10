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

import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.database.sqlite.SQLiteBlobTooBigException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import io.karte.android.core.logger.Logger

private const val LOG_TAG = "Karte.DataStore"

private fun getCursorWindowSize(): Int {
    return runCatching {
        Resources.getSystem().getInteger(
            Resources.getSystem()
                .getIdentifier("config_cursorWindowSize", "integer", "android")
        ) * 1024
    }.getOrNull() ?: 1024 * 1024
}

private class DbHelper(context: Context) :
    SQLiteOpenHelper(context, "krt_cache.db", null, persistableContracts.sumBy { it.version }) {
    override fun onCreate(db: SQLiteDatabase) {
        persistableContracts.forEach { contract ->
            createTable(db, contract)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Logger.d(LOG_TAG, "onUpgrade $oldVersion -> $newVersion")
        persistableContracts.forEach { contract ->
            db.execSQL("DROP TABLE IF EXISTS ${contract.namespace}")
            createTable(db, contract)
        }
    }

    private fun createTable(db: SQLiteDatabase, contract: Contract<*>) {
        val columns = contract.columns.map {
            val type = when (it.value) {
                Cursor.FIELD_TYPE_INTEGER -> "INTEGER"
                Cursor.FIELD_TYPE_STRING -> "TEXT"
                Cursor.FIELD_TYPE_FLOAT -> "REAL"
                Cursor.FIELD_TYPE_BLOB -> "BLOB"
                Cursor.FIELD_TYPE_NULL -> "NULL"
                else -> ""
            }
            "${it.key}  $type"
        }.joinToString(", ")
        Logger.d(LOG_TAG, "onCreate, $columns")
        db.execSQL(
            "CREATE TABLE ${contract.namespace}" +
                " (${BaseColumns._ID} INTEGER PRIMARY KEY, $columns)"
        )
    }

    companion object {
        internal val persistableContracts: MutableSet<Contract<*>> = mutableSetOf()
    }
}

internal class DataStore private constructor(context: Context) {
    private val dbHelper = DbHelper(context)
    private val cache: MutableMap<Long, Persistable> = mutableMapOf()
    private val subscribers = mutableSetOf<Subscriber>()
    private val windowSize = getCursorWindowSize()

    private fun contentValues(persistable: Persistable): ContentValues {
        val values = ContentValues()
        persistable.onPersisted().forEach {
            when (persistable.contract.columns[it.key]) {
                Cursor.FIELD_TYPE_INTEGER -> values.put(it.key, it.value as Int)
                Cursor.FIELD_TYPE_STRING -> values.put(it.key, it.value as String)
                Cursor.FIELD_TYPE_FLOAT -> values.put(it.key, it.value as Double)
                Cursor.FIELD_TYPE_BLOB -> values.put(it.key, it.value as ByteArray)
                else -> values.putNull(it.key)
            }
        }
        return values
    }

    companion object : Persister, Transactional {
        private lateinit var instance: DataStore

        fun setup(context: Context, vararg contracts: Contract<*>) {
            DbHelper.persistableContracts.addAll(contracts)
            synchronized(this) {
                if (::instance.isInitialized) return
                instance = DataStore(context)
            }
        }

        internal fun teardown() {
            instance.dbHelper.close()
        }

        //region Transactional
        override fun transaction(): Transaction {
            return Transaction(this, this)
        }

        override fun begin() {
            instance.dbHelper.writableDatabase.beginTransaction()
        }

        override fun success() {
            instance.dbHelper.writableDatabase.setTransactionSuccessful()
        }

        override fun end() {
            instance.dbHelper.writableDatabase.endTransaction()
        }
        // endregion

        // region Persister
        override fun put(persistable: Persistable): Long {
            if (persistable.size > instance.windowSize) {
                Logger.e(LOG_TAG, "Too big: persistable size: ${persistable.size}.")
                return -1L
            }
            val result = try {
                instance.dbHelper.writableDatabase.insert(
                    persistable.contract.namespace,
                    null,
                    instance.contentValues(persistable)
                )
            } catch (e: SQLiteException) {
                -1L
            } catch (e: SQLiteFullException) {
                -1L
            }
            if (result != -1L) {
                persistable.id = result
                instance.cache[persistable.id] = persistable
                instance.subscribers.forEach { it.notified() }
            }
            return result
        }

        override fun <T : Persistable> read(
            contract: Contract<T>,
            query: List<Triple<String, RelationalOperator, String>>,
            order: String?
        ): List<T> {
            @Suppress("UNCHECKED_CAST")
            val cached = instance.cache.filter { entry ->
                query.all {
                    it.second.run(entry.value.values[it.first].toString(), it.third)
                }
            }.values.toList() as? List<T>
            if (cached?.isNotEmpty() == true) return cached

            val selection = query.joinToString(" AND ") { "${it.first} ${it.second.value} ?" }
            val selectionArgs = query.map { it.third }.toTypedArray()
            Logger.d(LOG_TAG, "read from db, $selection, ${selectionArgs.joinToString("")}")
            instance.dbHelper.readableDatabase.query(
                contract.namespace,
                null,
                selection,
                selectionArgs,
                null,
                null,
                order ?: "${BaseColumns._ID} ASC"
            ).use { cursor ->
                val persistables = mutableListOf<T>()
                try {
                    repeat(cursor.count) {
                        cursor.moveToPosition(it)
                        val persistable =
                            contract.create(cursor.columnNames.mapIndexed { index, s ->
                                if (index == -1) return@mapIndexed s to null
                                when (contract.columns[s]) {
                                    Cursor.FIELD_TYPE_INTEGER -> s to cursor.getInt(index)
                                    Cursor.FIELD_TYPE_STRING -> s to cursor.getString(index)
                                    Cursor.FIELD_TYPE_FLOAT -> s to cursor.getDouble(index)
                                    Cursor.FIELD_TYPE_BLOB -> s to cursor.getBlob(index)
                                    else -> s to null
                                }
                            }.toMap())
                        persistable.id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
                        persistables.add(persistable)
                        instance.cache[persistable.id] = persistable
                    }
                } catch (e: SQLiteBlobTooBigException) {
                    // 大きすぎるデータを保持してしまった場合はall deleteする.
                    Logger.w(
                        LOG_TAG,
                        "drop table:${contract.namespace}, because too big row and cannot read."
                    )
                    instance.dbHelper.writableDatabase.delete(contract.namespace, null, null)
                } catch (e: Exception) {
                    // for catch CursorWindowAllocationException
                    Logger.e(LOG_TAG, "Error occurred: ${e.message}", e)
                }
                return persistables
            }
        }

        override fun delete(persistable: Persistable) {
            instance.cache.remove(persistable.id)
            instance.dbHelper.writableDatabase.delete(
                persistable.contract.namespace,
                "${BaseColumns._ID} = ?",
                arrayOf(persistable.id.toString())
            )
        }

        override fun update(persistable: Persistable): Int {
            try {
                val result = instance.dbHelper.writableDatabase.update(
                    persistable.contract.namespace,
                    instance.contentValues(persistable),
                    "${BaseColumns._ID} = ?",
                    arrayOf(persistable.id.toString())
                )
                instance.cache[persistable.id] = persistable
                return result
            } catch (e: SQLiteException) {
            } catch (e: SQLiteFullException) {
            }
            return 0
        }

        fun subscribe(subscriber: Subscriber) {
            instance.subscribers.add(subscriber)
        }
    }
}
