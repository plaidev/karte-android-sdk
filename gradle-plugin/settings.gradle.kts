pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                // Android Build Tools (gradle-plugin compileOnly dependency)
                includeGroupByRegex("com\\.android.*")
            }
        }
        mavenCentral {
            content {
                // 1. Kotlin-related (allow organization-wide)
                includeGroupByRegex("org\\.jetbrains.*")

                // 2. Bytecode manipulation & utilities (organization-wide to resolve transitive dependencies)
                includeGroupByRegex("org\\.ow2\\.asm.*")
                includeGroup("org.javassist")
                includeGroup("commons-io")

                // 3. Google libraries only available in Maven Central
                includeGroupByRegex("com\\.google\\.guava.*")
                includeGroupByRegex("com\\.google\\.code\\..*")
                includeGroupByRegex("com\\.google\\.errorprone.*")
                includeGroupByRegex("com\\.google\\.j2objc.*")

                // 4. Other transitive dependencies
                includeGroupByRegex("com\\.squareup.*")  // Required by com.android.tools.build:gradle
                includeGroup("org.checkerframework")

                // 5. Parent POMs for transitive dependencies
                includeGroup("org.apache.commons")
                includeGroup("org.sonatype.oss")
                includeGroup("org.apache")
                includeGroup("com.google.guava")
                includeGroupByRegex("org\\.junit.*")

                // 6. Defense in depth: prevent Android artifacts from being resolved here
                excludeGroupByRegex("com\\.android.*")
                excludeGroupByRegex("com\\.google\\.android.*")
                excludeGroupByRegex("com\\.google\\.firebase.*")
                excludeGroupByRegex("androidx.*")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "gradle-plugin"
