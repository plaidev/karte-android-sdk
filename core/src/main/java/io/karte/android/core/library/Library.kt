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
package io.karte.android.core.library

import io.karte.android.KarteApp

/**
 * ライブラリを表すInterfaceです。
 *
 * **サブモジュールと連携するために用意している機能であり、通常利用で使用することはありません。**
 */
interface Library {
    /**ライブラリ名*/
    val name: String
    /**バージョン*/
    val version: String
    /**公開モジュールであるかどうか*/
    val isPublic: Boolean

    /**
     * ライブラリを初期化します。
     * @param[app]初期化済みの[KarteApp]インスタンス
     */
    fun configure(app: KarteApp)

    /**
     * ライブラリを破棄します。
     * @param[app]初期化済みの[KarteApp]インスタンス
     */
    fun unconfigure(app: KarteApp)
}
