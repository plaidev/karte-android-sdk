@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "DEPRECATION")
package io.karte.android.gradleplugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.sun.beans.finder.ClassFinder.findClass
import io.karte.android.gradleplugin.visualtracking.AndroidManifestTransform
import io.karte.android.gradleplugin.visualtracking.ByteCodeTransform
import io.karte.android.gradleplugin.visualtracking.asm.KarteClassVisitorFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import java.io.File

val logger: Logger = Logging.getLogger("karte-logger")

class KartePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project.plugins.withType(AppPlugin::class.java).isEmpty()) {
            throw IllegalStateException("'android' plugin required.")
        }
        logger.debug("Karte plugin applied")
        val android: AppExtension = project.extensions.findByName("android") as AppExtension

        val version = findClass("com.android.Version").getField("ANDROID_GRADLE_PLUGIN_VERSION").get(null) as String
        val agpVersion = AGPVersion.fromVersionString(version)

        if (agpVersion < AGPVersion.VERSION_7_0_0) {
            logger.debug("Use Transform API")
            android.registerTransform(
                ByteCodeTransform(
                    project
                )
            )
        } else {
            logger.debug("Use ASM")
            val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
            androidComponents.onVariants { variant ->
                variant.transformClassesWith(
                    KarteClassVisitorFactory::class.java,
                    InstrumentationScope.ALL
                ) { }
                @Suppress("UnstableApiUsage")
                variant.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
            }
        }

        // Based on https://developer.android.com/studio/build/gradle-plugin-3-0-0-migration?hl=ja#variant_output
        android.applicationVariants.all { variant ->
            variant.outputs.all { output ->
                output.processManifestCompat().doLast { task ->
                    val manifestDirFiles =
                        (task as ManifestProcessorTask).manifestOutputDirectoryCompat()
                    val manifestPaths = manifestDirFiles.filter { it.name == "AndroidManifest.xml" }
                        .map { it.absolutePath }
                    assert(manifestPaths.isEmpty())
                    manifestPaths.forEach {
                        AndroidManifestTransform(
                            it
                        ).execute()
                    }
                }
            }
        }
    }

    /*
     * Methods for compatibility with Android Gradle plugin less than 3.3.0.
     * https://developer.android.com/studio/releases/gradle-plugin#3-3-0
     *
     * Using deprecated method will print noisy warning at app build time.
     * @Suppress("DEPRECATION") annotation just suppress warning at plugin compile time.
     */
    private fun BaseVariantOutput.processManifestCompat(): ManifestProcessorTask {
        return try {
            processManifestProvider.get()
        } catch (e: NoSuchMethodError) {
            // less than 3.3.0
            @Suppress("DEPRECATION")
            processManifest
        }
    }

    private fun ManifestProcessorTask.manifestOutputDirectoryCompat(): Set<File> {
        val methods = javaClass.methods
        // [4.1.0, 4.2.1]
        methods.firstOrNull { it.name == "getMultiApkManifestOutputDirectory" }?.let {
            val dir: DirectoryProperty = it.invoke(this) as DirectoryProperty
            return dir.asFileTree.files
        }
        // (, 4.1.0)
        methods.firstOrNull { it.name == "getManifestOutputDirectory" }?.let {
            val dir = it.invoke(this)
            return when (dir) {
                // [3.5.0, 4.1.0)
                is DirectoryProperty -> dir.get().asFileTree.files
                // [3.3.0, 3.5.0)
                is Provider<*> -> (dir.get() as Directory).asFileTree.files
                // [3.1.0, 3.3.0)
                // Greater than or equal to 3.3.0, there isn't even a deprecated method of getManifestOutputDirectory returning File.
                is File -> dir.walkTopDown().toSet()
                // (, 3.1.0)
                else -> throw NoSuchMethodError(
                    "Not found expected return type method: getManifestOutputDirectory."
                )
            }
        }
        throw NoSuchMethodError("getMultiApkManifestOutputDirectory or getManifestOutputDirectory.")
    }
}
