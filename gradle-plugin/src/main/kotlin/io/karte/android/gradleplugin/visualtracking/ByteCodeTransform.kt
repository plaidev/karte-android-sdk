package io.karte.android.gradleplugin.visualtracking

import com.android.SdkConstants
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.AppExtension
import io.karte.android.gradleplugin.logger
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.bytecode.AccessFlag
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ByteCodeTransform(private val project: Project) : Transform() {

    private lateinit var classPool: ClassPool
    private var incremental: Boolean = false

    override fun getName(): String {
        return "ByteCodeTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return mutableSetOf(
            QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES
        )
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun isCacheable(): Boolean {
        return true
    }

    override fun transform(invocation: TransformInvocation?) {
        super.transform(invocation)
        incremental = invocation!!.isIncremental
        logger.debug("Start transform. incremental:$incremental")
        classPool = getClassPool(invocation.inputs, project)
        val dirMods = invocation.inputs.flatMap { it.directoryInputs }
            .map { getDirTransform(it, invocation.outputProvider) }
        val jarMods = invocation.inputs.flatMap { it.jarInputs }
            .mapNotNull { getJarTransform(it, invocation.outputProvider) }

        logger.debug("Executing modifications.")

        // ENHANCE: Tuning pool count, or use other module like ForkJoinTask.
        val futures = Executors.newFixedThreadPool(4).invokeAll(jarMods + dirMods)
        // Call get() to throw Exception occurred during Callable execution.
        futures.forEach { it.get() }
        logger.debug("Finished Karte transform.")
    }

    private fun getClassPool(
        inputs: MutableCollection<TransformInput>,
        project: Project
    ): ClassPool {
        val pool = ClassPool.getDefault()
        inputs.flatMap { return@flatMap it.directoryInputs + it.jarInputs }
            .forEach { pool.appendClassPath(it.file.absolutePath) }
        val ext: AppExtension = project.extensions.findByName("android") as AppExtension
        ext.bootClasspath.forEach { pool.appendClassPath(it.absolutePath) }
        return pool
    }

    private fun getDirTransform(
        input: DirectoryInput,
        outputProvider: TransformOutputProvider
    ): Callable<Unit> {
        return Callable {
            val outDir = outputProvider.getContentLocation(
                input.name,
                input.contentTypes,
                input.scopes,
                Format.DIRECTORY
            )!!
            val files: List<File> = if (incremental) {
                input.changedFiles.entries.filter {
                    it.value in arrayOf(
                        Status.ADDED,
                        Status.CHANGED
                    )
                }.map { it.key }
            } else {
                input.file.walkTopDown().toList()
            }
            logger.debug(
                "Processing ${input.name}:${input.file.canonicalPath}." +
                    " Number of files ${files.size}. Output dir is ${outDir.canonicalPath}"
            )

            val filePath2Exec =
                files.fold(mutableMapOf()) { acc: MutableMap<String, ModificationExec>, cur: File ->
                    val className = classNameOrNull(cur.toRelativeString(input.file))
                    val exec = gatherModExec(className) ?: return@fold acc
                    acc[cur.canonicalPath] = exec
                    return@fold acc
                }

            filePath2Exec.values.forEach { it.exec(outDir.canonicalPath) }
            logger.debug("Copying directory ${input.name}:${input.file.canonicalPath}")
            if (incremental) {
                files.filter { !filePath2Exec.containsKey(it.canonicalPath) }
                    .forEach {
                        val output = File(outDir, it.toRelativeString(input.file))
                        if (it.isDirectory) {
                            FileUtils.copyDirectory(it, output)
                        } else {
                            FileUtils.copyFile(it, output)
                        }
                    }
            } else {
                FileUtils.copyDirectory(
                    input.file,
                    outDir
                ) { !filePath2Exec.containsKey(it.canonicalPath) }
            }
            logger.debug(
                "Processed ${input.name}:${input.file.canonicalPath}." +
                    " Output dir is ${outDir.canonicalPath}"
            )
        }
    }

    private fun getJarTransform(
        jarInput: JarInput,
        outputProvider: TransformOutputProvider
    ): Callable<Unit>? {
        if (incremental && (jarInput.status in arrayOf(Status.NOTCHANGED, Status.REMOVED))) {
            logger.debug("Skip transform ${jarInput.name} for incremental build.")
            return null
        }
        return Callable {
            val outDir = outputProvider.getContentLocation(
                jarInput.name,
                jarInput.contentTypes,
                jarInput.scopes,
                Format.JAR
            )!!
            logger.debug("Processing ${jarInput.name}. Output dir is ${outDir.canonicalPath}")

            val zf = ZipFile(jarInput.file)
            val entries = zf.entries().toList()
            val entryName2Exec = entries
                .fold(mutableMapOf()) { acc: MutableMap<String, ModificationExec>, cur: ZipEntry ->
                    acc[cur.name] = gatherModExec(classNameOrNull(cur.name)) ?: return@fold acc
                    return@fold acc
                }

            if (entryName2Exec.isEmpty()) {
                FileUtils.copyFile(jarInput.file, outDir)
                return@Callable
            }

            val out = ZipOutputStream(outDir.outputStream())
            entryName2Exec.entries.forEach {
                val newEntry = ZipEntry(it.key)
                val modifiedByteArray = it.value.exec()
                newEntry.size = modifiedByteArray.size.toLong()
                out.putNextEntry(newEntry)
                out.write(modifiedByteArray)
                out.closeEntry()
            }

            entries.filter { !entryName2Exec.containsKey(it.name) }
                .forEach {
                    out.putNextEntry(ZipEntry(it.name))
                    if (!it.isDirectory) {
                        zf.getInputStream(it).use { i -> IOUtils.copy(i, out) }
                    }
                    out.closeEntry()
                }

            out.close()
            zf.close()
        }
    }

    private fun classNameOrNull(fileName: String): String? {
        if (!fileName.endsWith(SdkConstants.DOT_CLASS)) return null
        return fileName
            .replace(File.separatorChar, '.') // OSごとのseparatorを置換.
            .replace('/', '.') // zipの時はWindowsでも`/`区切り.
            .substring(0, fileName.length - SdkConstants.DOT_CLASS.length)
    }

    private fun gatherModExec(className: String?): ModificationExec? {
        val ctClass = classPool.getOrNull(className) ?: return null
        try {
            ctClass.classFile
        } catch (e: RuntimeException) {
            // META-INF.versions.9.module-info等のJarに含まれるがclassファイルがsrcに存在しないclassはスキップする
            logger.info("Skip modification $className because class file not found. $e")
            return null
        }

        val methodModPairs = ctClass.declaredMethods
            .filter { it.modifiers and AccessFlag.ABSTRACT == 0 }
            .mapNotNull { method ->
                val modsCandidates = METHOD_SIG_TO_MOD_LIST[method.name + method.signature]
                    ?: return@mapNotNull null

                val mod = modsCandidates.find {
                    val target = classPool.getOrNull(it.target.className)
                    if (target == null) {
                        logger.debug(
                            "Skip modification ${it.name}" +
                                " because the class is not in classpath."
                        )
                        return@find false
                    }
                    return@find ctClass.subtypeOf(target)
                } ?: return@mapNotNull null
                return@mapNotNull Pair(method, mod)
            }
        if (methodModPairs.isEmpty()) return null
        return ModificationExec(
            ctClass,
            methodModPairs
        )
    }

    class ModificationExec(
        private val ctClass: CtClass,
        private val operations: List<Pair<CtMethod, Modification>>
    ) {
        fun exec(): ByteArray {
            execInternal()
            val ret = ctClass.toBytecode()
            ctClass.detach()
            return ret
        }

        fun exec(outputDirPath: String) {
            execInternal()
            ctClass.writeFile(outputDirPath)
            ctClass.detach()
        }

        private fun execInternal() {
            ctClass.defrost()
            operations.forEach {
                val (method, mod) = it
                logger.debug("Hook ${ctClass.name} ${method.name} ${method.signature} ${mod.name}")
                try {
                    method.insertBefore("$HOOK_ACTION_METHOD(\"${mod.name}\",\$args);")
                } catch (e: CannotCompileException) {
                    throw CannotCompileException(
                        "Failed to hook ${mod.name} for" +
                            " ${ctClass.name} ${method.name} ${method.signature} ",
                        e
                    )
                }
            }
        }
    }
}
