package io.karte.android.gradleplugin.visualtracking.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import io.karte.android.gradleplugin.logger
import io.karte.android.gradleplugin.visualtracking.METHOD_SIG_TO_MOD_LIST
import io.karte.android.gradleplugin.visualtracking.Modification
import io.karte.android.gradleplugin.visualtracking.ModificationLambdaSpecification
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

        if (KarteEventListenerMethodVisitor.isInstrumentable(classData, name, descriptor)) {
            return KarteEventListenerMethodVisitor(classData, apiVersion, methodVisitor, access, name, descriptor)
        }
        if (KarteLambdaMethodVisitor.isInstrumentable(name, descriptor)) {
            return KarteLambdaMethodVisitor(classData, apiVersion, methodVisitor, access, name, descriptor)
        }

        return methodVisitor
    }
}

class KarteEventListenerMethodVisitor(private val classData: ClassData, apiVersion: Int, methodVisitor: MethodVisitor, access: Int, name: String?, private val descriptor: String?) : AdviceAdapter(apiVersion, methodVisitor, access, name, descriptor) {

    override fun onMethodEnter() {
        logger.debug("Hook ${classData.className} $name$descriptor")
        val modification = findModification(classData, name, descriptor)
            ?: throw RuntimeException(
                "Failed to hook for ${classData.className} $name $descriptor"
            )

        visitLdcInsn(modification.name)
        loadArgArray()
        visitMethodInsn(Opcodes.INVOKESTATIC, "io/karte/android/visualtracking/internal/VTHook", "hookAction", "(Ljava/lang/String;[Ljava/lang/Object;)V", false)
    }

    companion object {
        private fun findModification(classData: ClassData, name: String?, descriptor: String?): Modification? {
            val modifications = METHOD_SIG_TO_MOD_LIST[name + descriptor]
            return modifications?.find { modification ->
                (classData.superClasses + classData.interfaces)
                    .find { superClass ->
                        "$superClass#$name" == modification.name
                    } != null
            }
        }

        fun isInstrumentable(classData: ClassData, name: String?, descriptor: String?): Boolean {
            return findModification(classData, name, descriptor) != null
        }
    }
}

class KarteLambdaMethodVisitor(private val classData: ClassData, apiVersion: Int, methodVisitor: MethodVisitor, access: Int, name: String?, private val descriptor: String?) : AdviceAdapter(apiVersion, methodVisitor, access, name, descriptor) {

    override fun onMethodEnter() {
        logger.debug("Hook ${classData.className} $name$descriptor")
        loadArgArray()
        visitMethodInsn(Opcodes.INVOKESTATIC, "io/karte/android/visualtracking/internal/VTHook", "hookDynamicInvoke", "([Ljava/lang/Object;)V", false)
    }

    companion object {
        fun isInstrumentable(name: String?, descriptor: String?): Boolean {
            return ModificationLambdaSpecification.isSatisfied(name + descriptor)
        }
    }
}
