package io.karte.android.tracking.queue

import io.karte.android.tracking.Event
import io.karte.android.tracking.EventName

/**
 * イベントの送信拒絶フィルタルールを定義するためのインターフェース。
 */
interface TrackEventRejectionFilterRule {
    /** 送信拒絶対象イベントの発火元ライブラリ名 */
    var libraryName: String
    /** 送信拒絶対象イベントのイベント名 */
    var eventName: EventName

    /**
     * イベントのフィルタリングを行う
     * @param[event] イベント
     * @return イベントを送信対象から除外する場合は true を返します。
     */
    fun reject(event: Event): Boolean
}
