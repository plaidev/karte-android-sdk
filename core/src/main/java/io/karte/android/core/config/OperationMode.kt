package io.karte.android.core.config

/**
 * 動作モードを表す列挙型です。
 */
enum class OperationMode {
    /** イベント解析あり */
    DEFAULT {
        override val trackEndpointPath: String = "track"
    },
    /** イベント解析なし */
    INGEST {
        override val trackEndpointPath: String = "ingest"
    };

    abstract val trackEndpointPath: String
}
