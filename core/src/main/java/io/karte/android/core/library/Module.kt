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

import android.content.Intent
import io.karte.android.tracking.client.TrackRequest
import io.karte.android.tracking.client.TrackResponse

/**
 * モジュールを表すInterfaceです。
 *
 * **サブモジュールと連携するために用意している機能であり、通常利用で使用することはありません。**
 */
interface Module {
    /**モジュール名*/
    val name: String
}

/**
 * アクションに関連する処理をフックするためのモジュールタイプです。
 *
 * **サブモジュールと連携するために用意している機能であり、通常利用で使用することはありません。**
 */
interface ActionModule : Module {
    /**
     *  Trackサーバーのレスポンスデータをハンドルします。
     *  @param[trackResponse]レスポンス
     *  @param[trackRequest]リクエスト
     */
    fun receive(trackResponse: TrackResponse, trackRequest: TrackRequest)

    /**
     * 各画面に対するリセット要求をハンドルします。
     * 画面遷移やdismiss等の特定画面に対して表示を停止する際に呼ばれます。
     */
    fun reset()

    /**
     * 全体に対するリセット要求をハンドルします。
     * オプトアウト等の全体の接客表示を停止する際に呼ばれます。
     */
    fun resetAll()
}

/**
 * ユーザー情報に関連する処理をフックするためのモジュールタイプです。
 *
 * **サブモジュールと連携するために用意している機能であり、通常利用で使用することはありません。**
 */
interface UserModule : Module {
    /**
     * ビジターIDの再生成をハンドルします。
     *
     * @param[current] 現在のビジターID
     * @param[previous] 直前のビジターID
     */
    fun renewVisitorId(current: String, previous: String?)
}

/**
 * 通知関連処理を委譲するためのモジュールタイプです。
 *
 * **サブモジュールと連携するために用意している機能であり、通常利用で使用することはありません。**
 */
interface NotificationModule : Module {
    /**プッシュ通知の受信登録を解除します。*/
    fun unsubscribe()
}

/**
 * ディープリンク処理をフックするためのモジュールタイプです。
 *
 * **サブモジュールと連携するために用意している機能であり、通常利用で使用することはありません。**
 */
interface DeepLinkModule : Module {
    /**
     * ディープリンクを処理します。
     *
     * @param[intent] 処理対象のURLが含まれる[Intent]
     */
    fun handle(intent: Intent?)
}

/**
 * イベント送信処理に割り込むためのモジュールタイプです。
 *
 * **サブモジュールと連携するために用意している機能であり、通常利用で使用することはありません。**
 */
interface TrackModule : Module {
    /**
     * リクエスト処理に割り込みます。
     *
     * 編集済みのリクエストを返すことで、リクエスト内容を編集することが可能です。
     * @param[request]  リクエスト
     * @return 編集済みのリクエストを返します。
     */
    fun intercept(request: TrackRequest): TrackRequest
}
