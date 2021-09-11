package io.karte.android.gradleplugin.visualtracking

import javassist.CtClass
import javassist.CtMethod

object ModificationLambdaSpecification {
    fun isSatisfied(method: CtMethod): Boolean {
        return method.parameterTypes.map(CtClass::getName).contains("android.view.View") &&
            method.name.contains(Regex("\\\$lambda-[0-9]"))
    }
}
