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
package io.karte.android.inappmessaging.internal.javascript

private const val INITIALIZED = "initialized"
private const val ERROR = "error"

internal enum class State {
    LOADING,
    READY,
    DESTROYED;

    companion object {
        fun of(nameInCallback: String): State {
            when (nameInCallback) {
                INITIALIZED -> return READY
                ERROR -> return DESTROYED
                else -> throw IllegalArgumentException("Unknown overlay state: $nameInCallback")
            }
        }
    }
}
