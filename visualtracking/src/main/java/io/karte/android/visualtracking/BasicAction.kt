//
//  Copyright 2021 PLAID, Inc.
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
package io.karte.android.visualtracking

/**
 * 必要最低限のアクションを表現する型です。
 *
 * @constructor Actionを返します。
 * アクションIDにはアプリ再起動時も変化しない一意なIDを設定してください。
 *
 * @param [action] アクション名
 * @param [actionId] アクションID
 * @param [targetText] ターゲット文字列（Viewコンポーネントのタイトルなどを設定します。）
 * @param [imageProvider] 操作ログに添付する画像を返す関数（ペアリング時の操作ログ送信でのみ利用されます。）
 */
class BasicAction @JvmOverloads constructor(
    override val action: String,
    override val actionId: String?,
    override val targetText: String?,
    override val imageProvider: ImageProvider? = null
) : Action {
    override fun toString(): String {
        return "BasicAction(action=$action, actionId=$actionId, targetText=$targetText)"
    }
}
