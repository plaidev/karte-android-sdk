package io.karte.android.gradleplugin.visualtracking

import javassist.CtClass
import javassist.CtMethod

object ModificationLambdaSpecification {
    private fun isLambda(methodName: String) = methodName.contains(Regex("\\\$lambda-[0-9]"))
    private fun isViewProcessor(method: CtMethod): Boolean {
        return runCatching {
            method.parameterTypes.map(CtClass::getName).contains("android.view.View")
        }.getOrDefault(false)
    }

    fun isSatisfied(method: CtMethod): Boolean {
        return isViewProcessor(method) && isLambda(method.name)
    }
}
