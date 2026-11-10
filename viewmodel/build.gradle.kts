import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
//    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.de.ktorfit)
    alias(libs.plugins.buildConfig)
    id("maven-publish")
}

group = "com.github.cybernhl.SDK"
version = "1.0-SNAPSHOT"

buildConfig {
    useKotlinOutput()
    packageName("chat.im.trilivechat.chatedin.viewmodel")
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, "seconds")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvm()
    android {
        namespace = "chat.im.trilivechat.chatedin.viewmodel"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        lint {
            warningsAsErrors = true
            abortOnError = true
            disable.addAll(
                listOf(
                    "MissingTranslation",
                    "ExtraTranslation",
                    "TypographyEllipsis",
                    "UnspecifiedImmutableFlag",
                    "UnusedResources",
                    "TypographyDashes",
                    "UseTomlInstead"
                )
            )
        }

        // 放在這裡
        packaging {
            resources {
                excludes += "META-INF/INDEX.LIST"
                //千萬不要在 library  庫模組中排除 *.kotlin_module 這會把 Kotlin 的頂層符號元資料全部濾掉 excludes += "META-INF/*.kotlin_module" 只能用在最終打包 App 的 APK 專案中防止重複檔案衝突，絕不能用在要發布給別人引用的 Library 專案中
//                excludes += "META-INF/*.kotlin_module"
                excludes += "META-INF/LICENSE"
                excludes += "META-INF/LICENSE.txt"
                excludes += "META-INF/NOTICE"
                excludes += "META-INF/io.netty.versions.properties"
                excludes += "META-INF/AL2.0"
                excludes += "META-INF/LGPL2.1"
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "viewmodel"
            isStatic = true
            binaryOption("bundleId", "chat.im.trilivechat.chatedin.viewmodel")
        }
    }

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    sourceSets {

        commonMain.dependencies {
            implementation(project(":sdk_core"))

            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.jetbrains.compose.components.resources)

            implementation(libs.jetbrains.androidx.lifecycle.viewmodel)
            implementation(libs.jetbrains.androidx.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.androidx.lifecycle.runtime.compose)
            implementation(libs.jetbrains.androidx.navigation.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
        }

        iosMain {
            dependencies {
            }
        }

        jsMain.dependencies {
            implementation(libs.wrappers.kotlin.browser)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "chat.im.trilivechat.chatedin.viewmodel"
    generateResClass = always
}

tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn(tasks.matching { it.name.startsWith("ksp") })
}
