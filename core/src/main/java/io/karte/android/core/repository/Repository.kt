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
package io.karte.android.core.repository

/**
 * リポジトリに対するデータの読み書き等の操作を表現するインターフェースです。
 */
interface Repository {
    /**
     * リポジトリに存在するデータを取得します。
     * @param[key] 取得するデータのキー
     * @param[default] 取得するデータのデフォルト値
     * @return 取得されたデータ。データが存在しない場合はデフォルト値。
     */
    fun <T> get(key: String, default: T): T

    /**
     * リポジトリにデータを追加します。
     * @param[key]追加するキー
     * @param[value]追加するデータ
     * @return 追加したデータ
     */
    fun <T> put(key: String, value: T)

    /**
     * リポジトリから指定したデータを削除します。
     * @param[key] 削除するデータのキー
     */
    fun remove(key: String)

    /**
     * リポジトリから全てのデータを削除します。
     */
    fun removeAll()

    /**
     * リポジトリにある全てのデータのキーを取得します
     */
    fun getAllKeys(): List<String>
}
