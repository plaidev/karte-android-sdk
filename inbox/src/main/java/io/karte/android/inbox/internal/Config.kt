package io.karte.android.inbox.internal

internal interface Config {
    val baseUrl: String
}

internal class ProductionConfig : Config {
    override val baseUrl: String
        get() = "https://api.karte.io"
}

internal class EvaluationConfig : Config {
    override val baseUrl: String
        get() = "https://api-evaluation.dev-karte.com"
}
