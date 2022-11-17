package io.karte.android.gradleplugin.visualtracking.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.karte.android.gradleplugin.logger
import io.karte.android.gradleplugin.visualtracking.METHOD_SIG_TO_MOD_LIST
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

abstract class KarteClassVisitorFactory : AsmClassVisitorFactory<InstrumentationParameters.None> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return KarteClassVisitor(
            classContext.currentClassData,
            instrumentationContext.apiVersion.get(),
            nextClassVisitor
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return true
    }
}

class KarteClassVisitor(private val classData: ClassData, private val apiVersion: Int, classVisitor: ClassVisitor) : ClassVisitor(apiVersion, classVisitor) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)

        if (isInstrumentable(name, descriptor)) {
            logger.debug("Hook ${classData.className} $name$descriptor")
            return KarteMethodVisitor(classData, apiVersion, methodVisitor, access, name, descriptor)
        }

        return methodVisitor
    }

    private fun isInstrumentable(name: String?, descriptor: String?): Boolean {
        name ?: return false
        descriptor ?: return false

        return isInstrumentableWithInterfaceCondition(name, descriptor) ||
            isInstrumentableWithLambdaCondition(name, descriptor)
    }

    private fun isInstrumentableWithInterfaceCondition(name: String, descriptor: String): Boolean {
        return METHOD_SIG_TO_MOD_LIST[name + descriptor] != null
    }

    private fun isInstrumentableWithLambdaCondition(name: String, descriptor: String): Boolean {
        if (!name.contains(Regex("\\\$lambda-[0-9]"))) {
            return false
        }
        return descriptor.contains(Regex("\\(.*Landroid/view/View;.*\\)"))
    }
}

class KarteMethodVisitor(private val classData: ClassData, apiVersion: Int, methodVisitor: MethodVisitor, access: Int, name: String?, private val descriptor: String?) : AdviceAdapter(apiVersion, methodVisitor, access, name, descriptor) {
    override fun onMethodEnter() {
        val modifications = METHOD_SIG_TO_MOD_LIST[name + descriptor]

        val modification = modifications?.find { modification ->
            classData.interfaces.find { `interface` ->
                "${`interface`}#$name" == modification.name
            } != null
        }

        val trackingName = modification?.name ?: "${classData.className}#$name"

        visitLdcInsn(trackingName)
        loadArgArray()
        visitMethodInsn(Opcodes.INVOKESTATIC, "io/karte/android/visualtracking/internal/VTHook", "hookAction", "(Ljava/lang/String;[Ljava/lang/Object;)V", false)
    }
}
