package io.karte.android.gradleplugin.visualtracking

object ModificationLambdaSpecification {
    fun isSatisfied(signature: String): Boolean {
        return signature.contains(Regex("\\\$lambda-[0-9]+\\(.*Landroid/view/View;.*\\)")) ||
            signature.contains(Regex("\\\$lambda\\\$[0-9]+\\(.*Landroid/view/View;.*\\)"))
    }
}
