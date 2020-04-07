package io.karte.android.gradleplugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.tasks.ManifestProcessorTask
import io.karte.android.gradleplugin.visualtracking.AndroidManifestTransform
import io.karte.android.gradleplugin.visualtracking.ByteCodeTransform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
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
        android.registerTransform(
            ByteCodeTransform(
                project
            )
        )

        // Based on https://developer.android.com/studio/build/gradle-plugin-3-0-0-migration?hl=ja#variant_output
        android.applicationVariants.all { variant ->
            variant.outputs.all { output ->
                output.processManifestCompat().doLast { task ->
                    val manifestDirFiles =
                        (task as ManifestProcessorTask).manifestOutputDirectoryCompat()
                    val manifestPaths = manifestDirFiles.filter { it.name == "AndroidManifest.xml" }
                        .map { it.absolutePath }
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
        return try {
            manifestOutputDirectory.get().asFileTree.files
        } catch (e: NoSuchMethodError) {
            val dir = javaClass.getMethod("getManifestOutputDirectory").invoke(this)
            if (dir is Provider<*>) {
                // less than 3.5.0.
                (dir.get() as Directory).asFileTree.files
            } else if (dir is File) {
                // less than 3.3.0.
                // Greater than or equal to 3.3.0, there isn't even a deprecated method of getManifestOutputDirectory returning File.
                dir.walkTopDown().toSet()
            } else {
                throw e
            }
        }
    }
}
