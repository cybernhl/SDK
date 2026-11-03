import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
//    alias(libs.plugins.android.kotlin.multiplatform.library)//FIXME do not use!! can not access library !!
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.de.ktorfit)
    id("maven-publish")
}

group = "com.github.cybernhl.SDK"
version = "1.0-SNAPSHOT"

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
//    android {
//        namespace = "chat.im.trilivechat.chatedin"
//        compileSdk = 36
//        minSdk = 23
//
//        compilerOptions {
//            jvmTarget = JvmTarget.JVM_11
//        }
//        androidResources {
//            enable = true
//        }
//        withHostTest {
//            isIncludeAndroidResources = true
//        }
//        withDeviceTestBuilder {
//            sourceSetTreeName = "test"
//        }.configure {
//            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        }
//    }

    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }


    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "sdk_core"
            isStatic = true
            binaryOption("bundleId", "chat.im.trilivechat.chatedin.sdkcore") // 明確指定 Bundle ID
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
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.koin.core)

            implementation(libs.de.ktorfit)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            api("com.github.cybernhl.GeneralModule:DI_TAGS:0.0.1")
            api("com.github.cybernhl.GeneralModule:utils:0.0.1")
            api("com.github.cybernhl.Entity:general:0.0.7")
            api("com.github.cybernhl.Entity:trilivechat:0.0.7")

            api("com.github.cybernhl.GeneralModule:network:0.0.1")
            api("com.github.cybernhl.GeneralModule:resfulclient:0.0.1")

        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)

        }

        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.jetbrains.compose.ui.tooling)
        }

        iosMain {
            dependencies{
                implementation(libs.ktor.client.darwin)
            }
        }

        jsMain.dependencies {
//            implementation(libs.wrappers.browser)
        }
    }
}

android {
    namespace = "chat.im.trilivechat.chatedin"
    compileSdk = 36

    publishing {
        singleVariant("release")
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
                "TypographyDashes"
            )
        )

        // disable("MissingTranslation",
        //         "ExtraTranslation",
        //         "TypographyEllipsis",
        //         "UnspecifiedImmutableFlag",
        //         "UnusedResources",
        //         "TypographyDashes")
    }

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

//    packagingOptions
    packaging {
        resources {
            excludes += "META-INF/*"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }
}

tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn(tasks.matching { it.name.startsWith("ksp") })
}

